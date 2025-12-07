package jogo.appstate;

import com.jme3.app.Application;
import com.jme3.app.state.BaseAppState;
import com.jme3.asset.AssetManager;
import com.jme3.audio.AudioData;
import com.jme3.audio.AudioNode;
import com.jme3.collision.CollisionResults;
import com.jme3.math.Ray;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import jogo.engine.RenderIndex;
import jogo.gameobject.GameObject;
import jogo.gameobject.character.Trader;
import jogo.gameobject.item.Item;
import jogo.voxel.VoxelBlockType;
import jogo.voxel.VoxelWorld;

public class InteractionAppState extends BaseAppState {

    private final Node rootNode;
    private final Camera cam;
    private final InputAppState input;
    private final RenderIndex renderIndex;
    private final WorldAppState world;
    private float reach = 5.5f;

    // Cooldown de ataque
    private float playerAttackCooldown = 0f;

    public InteractionAppState(Node rootNode, Camera cam, InputAppState input, RenderIndex renderIndex, WorldAppState world) {
        this.rootNode = rootNode;
        this.cam = cam;
        this.input = input;
        this.renderIndex = renderIndex;
        this.world = world;
    }

    @Override
    protected void initialize(Application app) { }

    @Override
    public void update(float tpf) {
        if (!input.isMouseCaptured()) return;

        if (playerAttackCooldown > 0) playerAttackCooldown -= tpf;

        Vector3f origin = cam.getLocation();
        Vector3f dir = cam.getDirection().normalize();

        // --- 1. ATAQUE (Botão Esquerdo) ---
        if (input.isBreaking() && playerAttackCooldown <= 0) {
            CollisionResults results = new CollisionResults();
            Ray ray = new Ray(origin, dir);
            ray.setLimit(4.0f);
            rootNode.collideWith(ray, results);

            if (results.size() > 0) {
                Spatial target = results.getClosestCollision().getGeometry();
                GameObject obj = findRegistered(target);

                if (obj instanceof jogo.gameobject.character.Character npc && !(obj instanceof jogo.gameobject.character.Player)) {
                    if (!npc.isDead()) {
                        npc.takeDamage(5);
                        playerAttackCooldown = 0.5f;

                        // Tocar som de dano (baseado no animal)
                        playNpcHurtSound(npc);

                        System.out.println("Atacaste " + npc.getName() + "!");
                        return;
                    }
                }
            }
        }

        // --- 2. INTERAGIR (Tecla E) ---
        if (input.consumeInteractRequested()) {
            Ray ray = new Ray(origin, dir);
            ray.setLimit(reach);
            CollisionResults results = new CollisionResults();
            rootNode.collideWith(ray, results);

            if (results.size() > 0) {
                Spatial hit = results.getClosestCollision().getGeometry();
                GameObject obj = findRegistered(hit);

                // A. Interagir com Item
                if (obj instanceof Item item) {
                    item.onInteract();
                    return;
                }

                // B. Interagir com Trader
                if (obj instanceof Trader) {
                    // --- ALTERAÇÃO: Som de Falar ---
                    playSound("Sounds/TraderTalking.wav");
                    // -------------------------------

                    jogo.appstate.HudAppState hud = getState(jogo.appstate.HudAppState.class);
                    if (hud != null) {
                        hud.showSubtitle("Trader: 'Bem vindo ao IscteCraft! Acerta todos os alvos no menor tempo possível ao atirar blocos com o G e explora o mundo!! :) .'", 4.0f);
                    }
                    return;
                }
            }

            // C. Interagir com Blocos
            VoxelWorld vw = world != null ? world.getVoxelWorld() : null;
            if (vw != null) {
                vw.pickFirstSolid(cam, reach).ifPresent(hit -> {
                    VoxelWorld.Vector3i cell = hit.cell;
                    byte blockId = vw.getBlock(cell.x, cell.y, cell.z);

                    if (blockId == jogo.voxel.VoxelPalette.CRAFTING_TABLE_ID) {
                        input.setMouseCaptured(false);
                        jogo.appstate.HudAppState hud = getState(jogo.appstate.HudAppState.class);
                        if (hud != null) hud.openCraftingTable();
                    }
                });
            }
        }

        // --- 3. COLOCAR BLOCO (Botão Direito) ---
        if (input.consumeUseRequested()) {
            PlayerAppState playerState = getState(PlayerAppState.class);
            if (playerState == null) return;

            byte heldId = playerState.getPlayer().getHeldItem();
            if (heldId == 0) return;

            VoxelWorld vw = world.getVoxelWorld();
            VoxelBlockType type = vw.getPalette().get(heldId);

            if (!type.isPlaceable()) return;

            if (vw != null) {
                vw.pickFirstSolid(cam, reach).ifPresent(hit -> {
                    int x = hit.cell.x + (int)hit.normal.x;
                    int y = hit.cell.y + (int)hit.normal.y;
                    int z = hit.cell.z + (int)hit.normal.z;

                    jogo.framework.math.Vec3 pPos = playerState.getPlayer().getPosition();
                    Vector3f playerPos = new Vector3f(pPos.x, pPos.y, pPos.z);
                    Vector3f playerCenter = playerPos.add(0, 0.9f, 0);

                    com.jme3.bounding.BoundingBox playerBox = new com.jme3.bounding.BoundingBox(playerCenter, 0.48f, 0.9f, 0.48f);
                    Vector3f blockCenter = new Vector3f(x + 0.5f, y + 0.5f, z + 0.5f);
                    com.jme3.bounding.BoundingBox blockBox = new com.jme3.bounding.BoundingBox(blockCenter, 0.49f, 0.49f, 0.49f);

                    if (playerBox.intersects(blockBox)) return;

                    vw.setBlock(x, y, z, heldId);
                    vw.rebuildDirtyChunks(world.getPhysicsSpace());
                    playerState.getPlayer().consumeHeldItem();
                    playerState.refreshPhysics();
                });
            }
        }
    }

    // --- MÉTODOS DE SOM ---

    // Método genérico para tocar qualquer ficheiro
    private void playSound(String soundFile) {
        if (soundFile == null || soundFile.isEmpty()) return;

        AssetManager am = getApplication().getAssetManager();
        try {
            AudioNode audio = new AudioNode(am, soundFile, AudioData.DataType.Buffer);
            audio.setPositional(false);
            audio.setVolume(1.5f);
            audio.setLooping(false);
            audio.playInstance();
        } catch (Exception e) {
            System.out.println("Som não encontrado: " + soundFile);
        }
    }

    // Método específico para escolher o som de dano
    private void playNpcHurtSound(jogo.gameobject.character.Character npc) {
        String soundFile = "";
        if (npc instanceof jogo.gameobject.character.Sheep) {
            soundFile = "Sounds/Sheep.wav";
        } else if (npc instanceof jogo.gameobject.character.Wolf) {
            soundFile = "Sounds/Wolf.wav";
        } else if (npc instanceof jogo.gameobject.character.Zombie) {
            soundFile = "Sounds/Zombie.wav";
        } else if (npc instanceof Trader) {
            soundFile = "Sounds/Trader.wav"; // Som de dano (diferente do de falar)
        }
        playSound(soundFile);
    }

    private GameObject findRegistered(Spatial s) {
        Spatial cur = s;
        while (cur != null) {
            GameObject obj = renderIndex.lookup(cur);
            if (obj != null) return obj;
            cur = cur.getParent();
        }
        return null;
    }

    @Override protected void cleanup(Application app) { }
    @Override protected void onEnable() { }
    @Override protected void onDisable() { }
}