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
import jogo.voxel.VoxelPalette;
import jogo.voxel.VoxelWorld;

// Gerencia interações do jogador: atacar, interagir, colocar blocos. Liga o jogador ao mundo ao dizer o que acontece quando ele faz algo ao olhar para algo.
public class InteractionAppState extends BaseAppState {

    // Referências para "ver" e "tocar" no mundo
    private final Node rootNode;      // O mundo 3D (para testar colisões)
    private final Camera cam;         // Os olhos do jogador para saber para onde olha
    private final InputAppState input; // Para saber se houve teclas pressionadas
    private final RenderIndex renderIndex; // Para saber que objeto é o modelo 3D
    private final WorldAppState world;     // Para mudar blocos no mundo

    private float reach = 5.5f; // Alcance do braço do jogador em blocos
    private float playerAttackCooldown = 0f; // Temporizador para o ataque do jogador

    //Construtor para receber referências necessárias
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
        // Se o rato tiver solto ignora cliques no mundo
        if (!input.isMouseCaptured()) return;

        // Diminui o tempo de espera do ataque
        if (playerAttackCooldown > 0) playerAttackCooldown -= tpf;

        // Prepara o raio da visão do jogador
        Vector3f origin = cam.getLocation();
        Vector3f dir = cam.getDirection().normalize();

        // --- 1. ATAQUE (Botão Esquerdo) ---

        // input.isBreaking() é true enquanto seguras o botão esquerdo
        if (input.isBreaking() && playerAttackCooldown <= 0) {
            CollisionResults results = new CollisionResults();
            Ray ray = new Ray(origin, dir);
            ray.setLimit(4.0f); // Alcance de ataque mais curto
            rootNode.collideWith(ray, results); // Testa colisões no mundo com o raio

            if (results.size() > 0) {
                // Pega o objeto mais próximo que o raio atingiu
                Spatial target = results.getClosestCollision().getGeometry();
                // Verifica que objeto do jogo é esse modelo 3D
                GameObject obj = findRegistered(target);

                // Verifica se é um NPC (e não o jogador)
                if (obj instanceof jogo.gameobject.character.Character npc && !(obj instanceof jogo.gameobject.character.Player)) {
                    if (!npc.isDead()) {

                        // --- LÓGICA DE DANO DA ESPADA ---
                        int damage = 5; // Dano base (mão)

                        // Verificar se tem a espada equipada
                        PlayerAppState pState = getState(PlayerAppState.class);
                        if (pState != null) {
                            byte heldItem = pState.getPlayer().getHeldItem();
                            if (heldItem == VoxelPalette.SWORD_ID) {
                                damage = 20;
                            }
                        }

                        // Aplica o dano no NPC
                        npc.takeDamage(damage);

                        playerAttackCooldown = 0.5f; // Espera 0.5s até poder atacar de novo

                        // Tocar som de dano (baseado no animal)
                        playNpcHurtSound(npc);

                        System.out.println("Atacaste " + npc.getName() + "!");
                        return;
                    }
                }
            }
        }

        // --- 2. INTERAGIR (Tecla E) ---
        if (input.consumeInteractRequested()) {// Verifica se carregaste no E (e limpa o pedido)
            // ... Cria raio e verifica colisões ...
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
                    // Mostrar legenda
                    jogo.appstate.HudAppState hud = getState(jogo.appstate.HudAppState.class);
                    if (hud != null) {
                        hud.showSubtitle("Trader: 'Bem vindo ao IscteCraft! Acerta todos os alvos no menor tempo possível ao atirar blocos com o G e explora o mundo!! :) .'", 4.0f);
                    }
                    return;
                }
            }

            // C. Interagir com Blocos
            // Se o raio não bateu em nenhum modelo 3D (NPC), verifica se bateu no terreno (Vóxeis)
            VoxelWorld vw = world != null ? world.getVoxelWorld() : null;
            if (vw != null) {
                vw.pickFirstSolid(cam, reach).ifPresent(hit -> { // Algoritmo especial para vóxeis
                    VoxelWorld.Vector3i cell = hit.cell;
                    byte blockId = vw.getBlock(cell.x, cell.y, cell.z);

                    // Mesa de Crafting, abre UI
                    if (blockId == jogo.voxel.VoxelPalette.CRAFTING_TABLE_ID) {
                        input.setMouseCaptured(false); // Solta o rato para clicar na UI
                        jogo.appstate.HudAppState hud = getState(jogo.appstate.HudAppState.class);
                        if (hud != null) hud.openCraftingTable();
                    }

                    // --- NOVO: LANTERNA (Ligar/Desligar) ---
                    else if (blockId == jogo.voxel.VoxelPalette.LANTERN_OFF_ID) {
                        // Se está apagada, substitui pelo bloco ACESO
                        vw.setBlock(cell.x, cell.y, cell.z, jogo.voxel.VoxelPalette.LANTERN_ON_ID);
                        vw.rebuildDirtyChunks(world.getPhysicsSpace());
                        playSound("Sounds/Click.wav");
                    }
                    else if (blockId == jogo.voxel.VoxelPalette.LANTERN_ON_ID) {
                        // Se está acesa, substitui pelo bloco APAGADO
                        vw.setBlock(cell.x, cell.y, cell.z, jogo.voxel.VoxelPalette.LANTERN_OFF_ID);
                        vw.rebuildDirtyChunks(world.getPhysicsSpace());
                        playSound("Sounds/Click.wav");
                    }
                });
            }
        }

        // --- 3. COLOCAR BLOCO (Botão Direito) ---
        if (input.consumeUseRequested()) {

            PlayerAppState playerState = getState(PlayerAppState.class);
            if (playerState == null) return;

            // Verifica o item que o jogador está a segurar
            byte heldId = playerState.getPlayer().getHeldItem();
            if (heldId == 0) return;

            VoxelWorld vw = world.getVoxelWorld();

            // Verifica se o bloco é colocável
            VoxelBlockType type = vw.getPalette().get(heldId);
            if (!type.isPlaceable()) return;

            if (vw != null) {
                // Verifica onde o jogador está a olhar no mundo
                vw.pickFirstSolid(cam, reach).ifPresent(hit -> {
                    // Calcular a posição adjacente à face que é atingida
                    // hit.cell = bloco atingido
                    // hit.normal = vetor normal da face (cima, lado)
                    int x = hit.cell.x + (int)hit.normal.x;
                    int y = hit.cell.y + (int)hit.normal.y;
                    int z = hit.cell.z + (int)hit.normal.z;

                    // Verifica se o jogador não está a colocar o bloco dentro de si próprio
                    jogo.framework.math.Vec3 pPos = playerState.getPlayer().getPosition();
                    Vector3f playerPos = new Vector3f(pPos.x, pPos.y, pPos.z);
                    Vector3f playerCenter = playerPos.add(0, 0.9f, 0);

                    com.jme3.bounding.BoundingBox playerBox = new com.jme3.bounding.BoundingBox(playerCenter, 0.48f, 0.9f, 0.48f);
                    Vector3f blockCenter = new Vector3f(x + 0.5f, y + 0.5f, z + 0.5f);
                    com.jme3.bounding.BoundingBox blockBox = new com.jme3.bounding.BoundingBox(blockCenter, 0.49f, 0.49f, 0.49f);

                    if (playerBox.intersects(blockBox)) return;

                    // Coloca o bloco no mundo
                    vw.setBlock(x, y, z, heldId);
                    vw.rebuildDirtyChunks(world.getPhysicsSpace());
                    playerState.getPlayer().consumeHeldItem();
                    playerState.refreshPhysics();
                });
            }
        }
    }

    // --- MÉTODOS DE SOM ---

    // Metodo genérico para tocar som
    private void playSound(String soundFile) {
        if (soundFile == null || soundFile.isEmpty()) return;

        // Carrega e toca o som
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

    // Metodo específico para escolher o som de dano
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

    // Procura o GameObject registado para um Spatial, subindo na hierarquia se necessário
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