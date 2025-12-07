package jogo.appstate;

import com.jme3.app.Application;
import com.jme3.app.state.BaseAppState;
import com.jme3.asset.AssetManager;
import com.jme3.bullet.PhysicsSpace;
import com.jme3.bullet.collision.shapes.BoxCollisionShape;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.light.AmbientLight;
import com.jme3.light.DirectionalLight;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.shape.Box;
import jogo.gameobject.item.DroppedItem;
import jogo.gameobject.item.ItemStack;
import jogo.voxel.VoxelWorld;
import jogo.voxel.VoxelPalette; // Importante

import java.util.ArrayList;
import java.util.List;

public class WorldAppState extends BaseAppState {

    private final Node rootNode;
    private final AssetManager assetManager;
    private final PhysicsSpace physicsSpace;
    private final Camera cam;
    private final InputAppState input;
    private PlayerAppState playerAppState;

    private float breakTimer = 0f;
    private VoxelWorld.Vector3i lastTargetBlock = null;

    // world root for easy cleanup
    private Node worldNode;
    private VoxelWorld voxelWorld;
    private com.jme3.math.Vector3f spawnPosition;

    // Lista de Itens
    private final List<DroppedItem> droppedItems = new ArrayList<>();
    private Node droppedItemsNode = new Node("DroppedItems");

    public WorldAppState(Node rootNode, AssetManager assetManager, PhysicsSpace physicsSpace, Camera cam, InputAppState input) {
        this.rootNode = rootNode;
        this.assetManager = assetManager;
        this.physicsSpace = physicsSpace;
        this.cam = cam;
        this.input = input;
    }

    public void registerPlayerAppState(PlayerAppState playerAppState) {
        this.playerAppState = playerAppState;
    }

    @Override
    protected void initialize(Application app) {
        worldNode = new Node("World");
        rootNode.attachChild(worldNode);

        // --- CORREÇÃO DO ERRO: Inicializar o mundo PRIMEIRO ---
        voxelWorld = new VoxelWorld(assetManager, 256, 64, 256);
        voxelWorld.generateLayers();
        voxelWorld.buildMeshes();
        voxelWorld.clearAllDirtyFlags();

        // SÓ AGORA é que podemos anexar o nó, porque o voxelWorld já existe
        worldNode.attachChild(voxelWorld.getNode());
        voxelWorld.buildPhysics(physicsSpace);

        // Anexar nó dos itens soltos
        rootNode.attachChild(droppedItemsNode);

        // Lighting
        AmbientLight ambient = new AmbientLight();
        ambient.setColor(ColorRGBA.White.mult(0.05f));
        worldNode.addLight(ambient);

        DirectionalLight sun = new DirectionalLight();
        sun.setDirection(new Vector3f(-0.35f, -1.3f, -0.25f).normalizeLocal());
        sun.setColor(ColorRGBA.White.mult(1.3f));
        worldNode.addLight(sun);

        // compute recommended spawn
        spawnPosition = voxelWorld.getRecommendedSpawn();
    }

    public com.jme3.math.Vector3f getRecommendedSpawnPosition() {
        return spawnPosition != null ? spawnPosition.clone() : new com.jme3.math.Vector3f(25.5f, 12f, 25.5f);
    }

    public VoxelWorld getVoxelWorld() {
        return voxelWorld;
    }

    public PhysicsSpace getPhysicsSpace() {
        return physicsSpace;
    }

    @Override
    public void update(float tpf) {
        // 1. Atualizar itens no chão (rodar/tempo)
        for (DroppedItem item : droppedItems) {
            item.update(tpf);
        }

        if (input == null || !input.isMouseCaptured()) {
            breakTimer = 0f;
            lastTargetBlock = null;
            return;
        }

        if (input.consumeToggleShadingRequested()) {
            voxelWorld.toggleRenderDebug();
        }

        if (!input.isBreaking()) {
            breakTimer = 0f;
            lastTargetBlock = null;
            return;
        }

        var pick = voxelWorld.pickFirstSolid(cam, 6f);
        if (pick.isEmpty()) {
            breakTimer = 0f;
            lastTargetBlock = null;
            return;
        }

        var hit = pick.get();
        VoxelWorld.Vector3i currentTarget = hit.cell;

        if (lastTargetBlock != null &&
                currentTarget.x == lastTargetBlock.x &&
                currentTarget.y == lastTargetBlock.y &&
                currentTarget.z == lastTargetBlock.z) {
            breakTimer += tpf;
        } else {
            lastTargetBlock = currentTarget;
            breakTimer = tpf;
        }

        byte blockId = voxelWorld.getBlock(currentTarget.x, currentTarget.y, currentTarget.z);
        var blockType = voxelWorld.getPalette().get(blockId);

        // --- LÓGICA DE PICARETAS ---
        float speedMultiplier = 1.0f; // Velocidade base (mão vazia)

        if (playerAppState != null) {
            byte heldItem = playerAppState.getPlayer().getHeldItem();

            // Verificar qual picareta está equipada e definir o multiplicador
            if (heldItem == VoxelPalette.WOOD_PICK_ID) {
                speedMultiplier = 2.0f; // 2x mais rápido
            } else if (heldItem == VoxelPalette.STONE_PICK_ID) {
                speedMultiplier = 4.0f; // 4x mais rápido
            } else if (heldItem == VoxelPalette.IRON_PICK_ID) {
                speedMultiplier = 6.0f; // 6x mais rápido
            }
        }

        // Aplicar o multiplicador ao tempo decorrido
        // Se tiveres picareta, o tempo "passa mais depressa" para o bloco, partindo-o logo.
        breakTimer += tpf * speedMultiplier;
        // ---------------------------

        if (breakTimer >= blockType.getHardness()) {

            // --- LÓGICA DE DANO CORRIGIDA (Com som) ---
            if (blockId == VoxelPalette.SpikyWood_ID) {
                if (playerAppState != null) {
                    jogo.appstate.HudAppState hud = getState(jogo.appstate.HudAppState.class);
                    int slot = hud != null ? hud.getSelectedSlotIndex() : 0;
                    ItemStack itemInHand = playerAppState.getPlayer().getHotbar()[slot];

                    boolean isTool = false;
                    // Verificar se é uma ferramenta (Picaretas ou Espada)
                    if (itemInHand != null) {
                        byte id = itemInHand.getId();
                        if (id == VoxelPalette.WOOD_PICK_ID ||
                                id == VoxelPalette.STONE_PICK_ID ||
                                id == VoxelPalette.IRON_PICK_ID ||
                                id == VoxelPalette.SWORD_ID) {
                            isTool = true;
                        }
                    }

                    // Se NÃO for ferramenta (mão vazia ou a segurar blocos), dá dano
                    if (!isTool) {
                        // Chama o método no PlayerAppState que criámos antes
                        // Se ainda não tens este método, vê o passo 2 abaixo
                        playerAppState.takeDamage(5);
                    }
                }
            }
            if (voxelWorld.breakAt(currentTarget.x, currentTarget.y, currentTarget.z)) {
                voxelWorld.rebuildDirtyChunks(physicsSpace);
                if (playerAppState != null) {
                    playerAppState.refreshPhysics();
                    System.out.println("Bloco partido: " + blockType.getName());

                    // --- CORREÇÃO DO DROP ---
                    // Verificar se o bloco define um drop específico (ex: Minério de Ferro -> Item Ferro)
                    byte dropId = blockType.getDropItem();

                    // Se getDropItem() devolver 0, significa que dropa o próprio bloco (comportamento padrão)
                    if (dropId == 0) {
                        dropId = blockId;
                    }

                    // --- CORREÇÃO DO DROP ---
                    // Em vez de dar diretamente ao jogador, atiramos para o chão!
                    spawnDroppedItem(
                            new Vector3f(currentTarget.x + 0.5f, currentTarget.y + 0.5f, currentTarget.z + 0.5f),
                            new Vector3f(0, 3f, 0), // Salta um bocadinho para cima
                            new ItemStack(dropId, 1)
                    );
                    // ------------------------
                }
                breakTimer = 0f;
                lastTargetBlock = null;
            }
        }
    }

    // --- MÉTODO PARA SPAWNAR ITEM ---
    public void spawnDroppedItem(Vector3f position, Vector3f velocity, ItemStack stack) {
        if (stack == null || stack.getAmount() <= 0) return;

        // 1. Criar Visual (Caixa Pequena)
        Geometry geom = new Geometry("ItemDrop", new Box(0.15f, 0.15f, 0.15f));

        // Obter material do bloco correspondente
        var blockType = voxelWorld.getPalette().get(stack.getId());
        Material mat = blockType.getMaterial(assetManager);
        geom.setMaterial(mat);

        Node itemNode = new Node("ItemNode");
        itemNode.attachChild(geom);
        itemNode.setLocalTranslation(position);

        // 2. Criar Física
        RigidBodyControl phy = new RigidBodyControl(new BoxCollisionShape(0.15f, 0.15f, 0.15f), 5.0f);
        itemNode.addControl(phy);

        // 3. Adicionar ao Mundo
        droppedItemsNode.attachChild(itemNode);
        physicsSpace.add(phy);

        // Aplicar velocidade (atirar)
        if (velocity != null) {
            phy.setLinearVelocity(velocity);
        }

        // 4. Registar na lista
        droppedItems.add(new DroppedItem(itemNode, phy, stack));
    }

    // Método para remover item do mundo (quando apanhado)
    public void removeDroppedItem(DroppedItem item) {
        physicsSpace.remove(item.getPhysics());
        item.getNode().removeFromParent();
        droppedItems.remove(item);
    }

    public List<DroppedItem> getDroppedItems() {
        return droppedItems;
    }



    @Override
    protected void cleanup(Application app) {
        if (worldNode != null) {
            worldNode.depthFirstTraversal(spatial -> {
                RigidBodyControl rbc = spatial.getControl(RigidBodyControl.class);
                if (rbc != null) {
                    physicsSpace.remove(rbc);
                    spatial.removeControl(rbc);
                }
            });
            worldNode.removeFromParent();
            worldNode = null;

            droppedItemsNode.removeFromParent();
            for(DroppedItem item : droppedItems) {
                physicsSpace.remove(item.getPhysics());
            }
            droppedItems.clear();
        }
    }

    @Override
    protected void onEnable() { }

    @Override
    protected void onDisable() { }
}