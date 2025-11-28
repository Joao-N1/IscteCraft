package jogo.appstate;

import com.jme3.app.Application;
import com.jme3.app.state.BaseAppState;
import com.jme3.math.Ray;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.collision.CollisionResults;
import jogo.engine.RenderIndex;
import jogo.gameobject.GameObject;
import jogo.gameobject.item.Item;
import jogo.voxel.VoxelWorld;
import jogo.util.Hit;
import jogo.framework.math.Vec3;

public class InteractionAppState extends BaseAppState {

    private final Node rootNode;
    private final Camera cam;
    private final InputAppState input;
    private final RenderIndex renderIndex;
    private final WorldAppState world;
    private float reach = 5.5f;

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

        Vector3f origin = cam.getLocation();
        Vector3f dir = cam.getDirection().normalize();

        if (input.consumeInteractRequested()) {
            Ray ray = new Ray(origin, dir);
            ray.setLimit(reach);
            CollisionResults results = new CollisionResults();
            rootNode.collideWith(ray, results);
            if (results.size() > 0) {
                Spatial hit = results.getClosestCollision().getGeometry();
                GameObject obj = findRegistered(hit);
                if (obj instanceof Item item) {
                    item.onInteract();
                    System.out.println("Interacted with item: " + obj.getName());
                    return;
                }
            }

            // Se não for item, verifica se é um bloco (para interagir, não colocar)
            VoxelWorld vw = world != null ? world.getVoxelWorld() : null;
            if (vw != null) {
                vw.pickFirstSolid(cam, reach).ifPresent(hit -> {
                    VoxelWorld.Vector3i cell = hit.cell;
                    System.out.println("TODO: interact with voxel at " + cell.x + "," + cell.y + "," + cell.z);
                });
            }
        }

        // --- NOVA LÓGICA: COLOCAR BLOCO (Botão Direito) ---
        if (input.consumeUseRequested()) {
            // 1. Verificar se temos bloco na mão
            PlayerAppState playerState = getState(PlayerAppState.class);
            if (playerState == null) return;

            byte heldId = playerState.getPlayer().getHeldItem();
            System.out.println("DEBUG: Tentativa de colocar. Item na mão ID: " + heldId);
            if (heldId == 0) return; // Mão vazia

            VoxelWorld placeVw = world != null ? world.getVoxelWorld() : null;
            // --- NOVA VERIFICAÇÃO ---
            VoxelWorld vw = world.getVoxelWorld();
            var type = placeVw != null ? placeVw.getPalette().get(heldId) : null;
            if (type == null) {
                System.out.println("Tipo de bloco desconhecido: " + heldId);
                return;
            }

            if (!type.isPlaceable()) {
                System.out.println("Não podes colocar este item!");
                return; // Sai da função, não coloca nada
            }
            // ------------------------
            if (placeVw != null) {
                // Raycast para encontrar onde colocar
                placeVw.pickFirstSolid(cam, reach).ifPresent(hit -> {
                    // A "célula" é o bloco em que batemos.
                    // A "normal" diz-nos qual a face (ex: 0,1,0 é Cima).
                    // Somamos a normal à célula para obter o vizinho vazio.
                    int x = hit.cell.x + (int) hit.normal.x;
                    int y = hit.cell.y + (int) hit.normal.y;
                    int z = hit.cell.z + (int) hit.normal.z;

                    // Verifica se não estamos a colocar o bloco dentro do próprio jogador!

                    // Se não tiveres o método toVector3f, usa:
                    jogo.framework.math.Vec3 pPos = playerState.getPlayer().getPosition();
                    Vector3f playerPos = new Vector3f(pPos.x, pPos.y, pPos.z);

                    Vector3f playerCenter = playerPos.add(0, 0.9f, 0);

                    // Definir a caixa do jogador (Raio ~0.4, Altura ~1.8 -> Meia-altura ~0.9)
                    // Assumindo que a posição é o centro do corpo (cintura)
                    com.jme3.bounding.BoundingBox playerBox = new com.jme3.bounding.BoundingBox(
                            playerPos,          // Centro
                            0.48f, 0.9f, 0.48f);  // Raio X, Meia-Altura Y, Raio Z

                    /// 4. Criar a caixa do BLOCO NOVO
                    Vector3f blockCenter = new Vector3f(x + 0.5f, y + 0.5f, z + 0.5f);
                    // Usamos 0.49f em vez de 0.5f para evitar que toque nos blocos vizinhos
                    com.jme3.bounding.BoundingBox blockBox = new com.jme3.bounding.BoundingBox(
                            blockCenter,
                            0.49f, 0.49f, 0.49f);

                    System.out.println("DEBUG: Player Box: " + playerBox);
                    System.out.println("DEBUG: Block Box: " + blockBox);

                    // Se as caixas se tocarem, NÃO colocar bloco
                    if (playerBox.intersects(blockBox)) {
                        System.out.println("Não podes colocar blocos dentro do jogador!");
                        return;
                    }
                    System.out.println("SUCESSO: A colocar bloco em " + x + "," + y + "," + z);

                    vw.setBlock(x, y, z, heldId);
                    vw.rebuildDirtyChunks(world.getPhysicsSpace());
                    playerState.getPlayer().consumeHeldItem();
                    playerState.refreshPhysics();
                });
            }
        }
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

    @Override
    protected void cleanup(Application app) { }

    @Override
    protected void onEnable() { }

    @Override
    protected void onDisable() { }
}
