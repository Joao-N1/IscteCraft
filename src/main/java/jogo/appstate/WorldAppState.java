package jogo.appstate;

import com.jme3.app.Application;
import com.jme3.app.state.BaseAppState;
import com.jme3.asset.AssetManager;
import com.jme3.audio.AudioNode;
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
import jogo.voxel.VoxelBlockType;
import jogo.voxel.VoxelPalette;
import jogo.voxel.VoxelWorld;

import java.util.ArrayList;
import java.util.List;

// AppState que gere o mundo voxel, incluindo a lógica de quebra de blocos e drops
public class WorldAppState extends BaseAppState {
    // Referências principais
    private final Node rootNode;
    private final AssetManager assetManager;
    private final PhysicsSpace physicsSpace;
    private final Camera cam;
    private final InputAppState input;
    private PlayerAppState playerAppState;

    // Lógica de quebra de blocos
    private float breakTimer = 0f;
    private VoxelWorld.Vector3i lastTargetBlock = null;

    // Nó do mundo e VoxelWorld
    private Node worldNode;
    private VoxelWorld voxelWorld;
    private com.jme3.math.Vector3f spawnPosition;

    // Lista de Itens soltos no chão
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

        // 1. Criar o VoxelWorld
        voxelWorld = new VoxelWorld(assetManager, 256, 64, 256);
        voxelWorld.generateLayers();
        voxelWorld.buildMeshes();
        voxelWorld.clearAllDirtyFlags();

        // 2. Anexar o nó do VoxelWorld ao nó do mundo
        worldNode.attachChild(voxelWorld.getNode());
        voxelWorld.buildPhysics(physicsSpace);

        // 3. Nó para itens soltos
        rootNode.attachChild(droppedItemsNode);

        // 4. Adicionar luzes básicas
        AmbientLight ambient = new AmbientLight();
        ambient.setColor(ColorRGBA.White.mult(0.05f));
        worldNode.addLight(ambient);

        DirectionalLight sun = new DirectionalLight();
        sun.setDirection(new Vector3f(-0.35f, -1.3f, -0.25f).normalizeLocal());
        sun.setColor(ColorRGBA.White.mult(1.3f));
        worldNode.addLight(sun);

        spawnPosition = voxelWorld.getRecommendedSpawn();
    }

    public com.jme3.math.Vector3f getRecommendedSpawnPosition() {
        return spawnPosition != null ? spawnPosition.clone() : new com.jme3.math.Vector3f(25.5f, 12f, 25.5f);
    }

    public VoxelWorld getVoxelWorld() { return voxelWorld; }
    public PhysicsSpace getPhysicsSpace() { return physicsSpace; }
    public Node getRootNode() { return rootNode; } // Helper para o TheRockBlock
    public Node getWorldNode() { return worldNode; }

    @Override
    public void update(float tpf) {
        // Atualizar itens no chão
        for (DroppedItem item : droppedItems) {
            item.update(tpf);
        }

        // Se input não existe ou rato solto, reset e sai
        if (input == null || !input.isMouseCaptured()) {
            resetMining();
            return;
        }

        if (input.consumeToggleShadingRequested()) {
            voxelWorld.toggleRenderDebug();
        }

        if (!input.isBreaking()) {
            resetMining();
            return;
        }

        // Raycast
        var pick = voxelWorld.pickFirstSolid(cam, 6f);
        if (pick.isEmpty()) {
            resetMining();
            return;
        }

        var hit = pick.get();
        VoxelWorld.Vector3i currentTarget = hit.cell;

        // Verificar se mudou de bloco
        if (lastTargetBlock != null &&
                currentTarget.x == lastTargetBlock.x &&
                currentTarget.y == lastTargetBlock.y &&
                currentTarget.z == lastTargetBlock.z) {
            // Mesmo bloco, continua a contar tempo
        } else {
            lastTargetBlock = currentTarget;
            breakTimer = 0f; // Reset ao mudar de alvo
        }

        // Obter o bloco
        byte blockId = voxelWorld.getBlock(currentTarget.x, currentTarget.y, currentTarget.z);
        VoxelBlockType blockType = voxelWorld.getPalette().get(blockId);

        // --- Delegar a lógica de mineração para o bloco ---
        // O bloco decide se podemos continuar a minar (return true) e pode fazer efeitos secundários (dano, música)
        boolean canMine = blockType.processMining(this, playerAppState, tpf);

        if (!canMine) {
            breakTimer = 0f;
            return;
        }

        // --- LÓGICA DE PICARETAS ---
        float speedMultiplier = 1.0f;
        if (playerAppState != null) {
            byte heldItem = playerAppState.getPlayer().getHeldItem();
            if (heldItem == VoxelPalette.WOOD_PICK_ID) speedMultiplier = 2.0f;
            else if (heldItem == VoxelPalette.STONE_PICK_ID) speedMultiplier = 4.0f;
            else if (heldItem == VoxelPalette.IRON_PICK_ID) speedMultiplier = 6.0f;
        }

        breakTimer += tpf * speedMultiplier;

        // --- QUEBRA ---
        if (breakTimer >= blockType.getHardness()) {
            // REFACTOR: O bloco trata da sua própria quebra
            blockType.onBlockBreak(this, currentTarget, playerAppState);

            // Reset
            breakTimer = 0f;
            lastTargetBlock = null;
        }
    }

    private void resetMining() {
        breakTimer = 0f;
        lastTargetBlock = null;
    }

    // --- UTILS ---
    public void spawnDroppedItem(Vector3f position, Vector3f velocity, ItemStack stack) {
        if (stack == null || stack.getAmount() <= 0) return;

        Geometry geom = new Geometry("ItemDrop", new Box(0.15f, 0.15f, 0.15f));
        var blockType = voxelWorld.getPalette().get(stack.getId());
        Material mat = blockType.getMaterial(assetManager);
        geom.setMaterial(mat);

        Node itemNode = new Node("ItemNode");
        itemNode.attachChild(geom);
        itemNode.setLocalTranslation(position);

        RigidBodyControl phy = new RigidBodyControl(new BoxCollisionShape(0.15f, 0.15f, 0.15f), 5.0f);
        itemNode.addControl(phy);

        droppedItemsNode.attachChild(itemNode);
        physicsSpace.add(phy);

        if (velocity != null) {
            phy.setLinearVelocity(velocity);
        }
        droppedItems.add(new DroppedItem(itemNode, phy, stack));
    }

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

    @Override protected void onEnable() { }
    @Override protected void onDisable() { }
}