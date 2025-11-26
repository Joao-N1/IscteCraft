package jogo.appstate;

import com.jme3.app.Application;
import com.jme3.app.state.BaseAppState;
import com.jme3.asset.AssetManager;
import com.jme3.bullet.PhysicsSpace;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.light.AmbientLight;
import com.jme3.light.DirectionalLight;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.scene.Node;
import jogo.voxel.VoxelWorld;

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

        // Lighting
        AmbientLight ambient = new AmbientLight();
        ambient.setColor(ColorRGBA.White.mult(0.20f)); // slightly increased ambient
        worldNode.addLight(ambient);

        DirectionalLight sun = new DirectionalLight();
        sun.setDirection(new Vector3f(-0.35f, -1.3f, -0.25f).normalizeLocal()); // more top-down to reduce harsh contrast
        sun.setColor(ColorRGBA.White.mult(0.85f)); // slightly dimmer sun
        worldNode.addLight(sun);

        // Voxel world 16x16x16 (reduced size for simplicity)
        voxelWorld = new VoxelWorld(assetManager, 256, 64, 256);
        voxelWorld.generateLayers();
        voxelWorld.buildMeshes();
        voxelWorld.clearAllDirtyFlags();
        worldNode.attachChild(voxelWorld.getNode());
        voxelWorld.buildPhysics(physicsSpace);

        // compute recommended spawn
        spawnPosition = voxelWorld.getRecommendedSpawn();
    }

    public com.jme3.math.Vector3f getRecommendedSpawnPosition() {
        return spawnPosition != null ? spawnPosition.clone() : new com.jme3.math.Vector3f(25.5f, 12f, 25.5f);
    }

    public VoxelWorld getVoxelWorld() {
        return voxelWorld;
    }

    @Override
    public void update(float tpf) {
        if (input == null || !input.isMouseCaptured()) {
            breakTimer = 0f;
            lastTargetBlock = null;
            return;
        }

        // Permitir alternar shading mesmo quando não se está a partir
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
            // Continuar a acumular tempo para partir o mesmo bloco
            breakTimer += tpf;
        } else {
            // Começar a partir de um novo bloco
            lastTargetBlock = currentTarget;
            breakTimer = tpf; // conta já este frame
        }

        byte blockId = voxelWorld.getBlock(currentTarget.x, currentTarget.y, currentTarget.z);
        var blockType = voxelWorld.getPalette().get(blockId);

        if (breakTimer >= blockType.getHardness()) {
            if (voxelWorld.breakAt(currentTarget.x, currentTarget.y, currentTarget.z)) {
                voxelWorld.rebuildDirtyChunks(physicsSpace);
                if (playerAppState != null) {
                    playerAppState.refreshPhysics();
                    System.out.println("Bloco partido: " + blockType.getName());
                    boolean success = playerAppState.getPlayer().addItem(blockId, 1);
                    if (success) {
                        System.out.println("Apanhaste: " + blockType.getName());
                    }
                }
                breakTimer = 0f;
                lastTargetBlock = null;
            }
        }
    }

    @Override
    protected void cleanup(Application app) {
        if (worldNode != null) {
            // Remove all physics controls under worldNode
            worldNode.depthFirstTraversal(spatial -> {
                RigidBodyControl rbc = spatial.getControl(RigidBodyControl.class);
                if (rbc != null) {
                    physicsSpace.remove(rbc);
                    spatial.removeControl(rbc);
                }
            });
            worldNode.removeFromParent();
            worldNode = null;
        }
    }

    @Override
    protected void onEnable() { }

    @Override
    protected void onDisable() { }
}
