package jogo.voxel;

import com.jme3.asset.AssetManager;
import com.jme3.bullet.PhysicsSpace;
import com.jme3.bullet.collision.shapes.CollisionShape;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.bullet.util.CollisionShapeFactory;
import com.jme3.material.Material;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import jogo.framework.math.Vec3;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents a chunk of the voxel world (e.g., 16x16x16 blocks).
 */
public class Chunk {
    public static final int SIZE = 16;
    private final int chunkX, chunkY, chunkZ;
    private final byte[][][] vox;
    private final Node node;

    private boolean dirty = true;

    private RigidBodyControl rigidBody;

    public Chunk(int chunkX, int chunkY, int chunkZ) {
        this.chunkX = chunkX;
        this.chunkY = chunkY;
        this.chunkZ = chunkZ;
        this.vox = new byte[SIZE][SIZE][SIZE];
        this.node = new Node("Chunk_" + chunkX + "," + chunkY + "," + chunkZ);
    }

    public Node getNode() { return node; }
    public byte get(int x, int y, int z) { return vox[x][y][z]; }
    public void set(int x, int y, int z, byte id) { vox[x][y][z] = id; }
    public int getChunkX() { return chunkX; }
    public int getChunkY() { return chunkY; }
    public int getChunkZ() { return chunkZ; }

    public void markDirty() { dirty = true; }
    public boolean isDirty() { return dirty; }
    public void clearDirty() { dirty = false; }

    // Build and attach mesh for this chunk
    public void buildMesh(AssetManager assetManager, VoxelPalette palette) {
        long start = System.nanoTime();
        node.detachAllChildren();
        Map<Byte, MeshBuilder> builders = new HashMap<>();
        for (int i = 0; i < palette.size(); i++) {
            if (i == VoxelPalette.AIR_ID) continue;
            MeshBuilder mb = new MeshBuilder();
            mb.setRandomizeUV(true);
            builders.put((byte)i, mb);
        }

        Map<Byte, Vec3> firstBlockPos = new HashMap<>();

        for (int x = 0; x < SIZE; x++) {
            for (int y = 0; y < SIZE; y++) {
                for (int z = 0; z < SIZE; z++) {
                    byte id = vox[x][y][z];

                    // --- CORREÇÃO 1: Permitir desenhar Água ---
                    // Antes: if (!palette.get(id).isSolid()) continue;
                    // Agora: Só ignora se for AR. Se for Água (não sólida), desenha na mesma.
                    if (id == VoxelPalette.AIR_ID) continue;
                    // ------------------------------------------

                    MeshBuilder builder = builders.get(id);
                    int wx = chunkX * SIZE + x;
                    int wy = chunkY * SIZE + y;
                    int wz = chunkZ * SIZE + z;

                    // Verifica vizinhos.
                    // Nota: A água vai desenhar faces se o vizinho não for sólido (Ar) ou se for sólido (Terra).
                    // Podes refinar isto depois se vires água a desenhar contra água.
                    if (!isSolid(wx+1,wy,wz,palette)) builder.addVoxelFace(wx,wy,wz, MeshBuilder.Face.PX);
                    if (!isSolid(wx-1,wy,wz,palette)) builder.addVoxelFace(wx,wy,wz, MeshBuilder.Face.NX);
                    if (!isSolid(wx,wy+1,wz,palette)) builder.addVoxelFace(wx,wy,wz, MeshBuilder.Face.PY);
                    if (!isSolid(wx,wy-1,wz,palette)) builder.addVoxelFace(wx,wy,wz, MeshBuilder.Face.NY);
                    if (!isSolid(wx,wy,wz+1,palette)) builder.addVoxelFace(wx,wy,wz, MeshBuilder.Face.PZ);
                    if (!isSolid(wx,wy,wz-1,palette)) builder.addVoxelFace(wx,wy,wz, MeshBuilder.Face.NZ);

                    if (!firstBlockPos.containsKey(id)) firstBlockPos.put(id, new Vec3(wx, wy, wz));
                }
            }
        }

        int geomCount = 0;
        for (Map.Entry<Byte, MeshBuilder> entry : builders.entrySet()) {
            MeshBuilder meshBuilder = entry.getValue();
            Mesh mesh = meshBuilder.build();
            if (mesh.getTriangleCount() > 0) {
                byte id = entry.getKey();
                Geometry g = new Geometry("chunk_"+chunkX+"_"+chunkY+"_"+chunkZ+"_"+id, mesh);
                Vec3 blockPos = firstBlockPos.getOrDefault(id, new Vec3(chunkX*SIZE, chunkY*SIZE, chunkZ*SIZE));

                VoxelBlockType type = palette.get(id);
                Material mat = type.getMaterial(assetManager, blockPos);
                g.setMaterial(mat);

                // --- CORREÇÃO 2: Bucket Transparente ---
                // Se for água, dizemos ao motor para tratar como vidro/água
                if (type.isTransparent()) {
                    g.setQueueBucket(com.jme3.renderer.queue.RenderQueue.Bucket.Transparent);
                }
                // ---------------------------------------

                node.attachChild(g);
                geomCount++;
            }
        }
        long end = System.nanoTime();
        // System.out.println("Chunk mesh built...");
    }

    /**
     * Updates the physics control for this chunk. Call after mesh rebuild.
     */
    /**
     * Atualiza a física.
     * ALTERAÇÃO: Agora recebe 'palette' para filtrar blocos não sólidos (água).
     */
    public void updatePhysics(PhysicsSpace space, VoxelPalette palette) {
        if (rigidBody != null) {
            space.remove(rigidBody);
            node.removeControl(rigidBody);
            rigidBody = null;
        }

        if (node.getQuantity() > 0) {
            // Criar um nó temporário apenas para o que é SÓLIDO
            Node solidNode = new Node("SolidPhysicsNode");
            boolean hasSolidBlocks = false;

            for (int i = 0; i < node.getQuantity(); i++) {
                if (node.getChild(i) instanceof Geometry) {
                    Geometry g = (Geometry) node.getChild(i);

                    // O nome da Geometry é "chunk_x_y_z_ID". Vamos extrair o ID.
                    String[] parts = g.getName().split("_");
                    try {
                        byte id = Byte.parseByte(parts[parts.length - 1]);

                        // SÓ adiciona à física se for sólido! (Água é ignorada aqui)
                        if (palette.get(id).isSolid()) {
                            Geometry gClone = g.clone(false);
                            gClone.setMesh(g.getMesh().deepClone());
                            solidNode.attachChild(gClone);
                            hasSolidBlocks = true;
                        }
                    } catch (Exception e) {
                        // Se falhar o parse, assume que é sólido por segurança
                        Geometry gClone = g.clone(false);
                        gClone.setMesh(g.getMesh().deepClone());
                        solidNode.attachChild(gClone);
                        hasSolidBlocks = true;
                    }
                }
            }

            // Se depois de filtrar houver blocos sólidos, cria a colisão
            if (hasSolidBlocks) {
                CollisionShape shape = CollisionShapeFactory.createMeshShape(solidNode);
                rigidBody = new RigidBodyControl(shape, 0f);
                node.addControl(rigidBody);
                space.add(rigidBody);
            }
        }
    }
    // Helper for solid check in world coordinates
    private boolean isSolid(int wx, int wy, int wz, VoxelPalette palette) {
        if (wx < 0 || wy < 0 || wz < 0) return false;
        int max = SIZE * 1000; // arbitrary large world limit
        if (wx >= max || wy >= max || wz >= max) return false;
        int cx = wx / SIZE, cy = wy / SIZE, cz = wz / SIZE;
        int lx = wx % SIZE, ly = wy % SIZE, lz = wz % SIZE;
        if (cx != chunkX || cy != chunkY || cz != chunkZ) return false; // only check within this chunk
        if (lx < 0 || ly < 0 || lz < 0 || lx >= SIZE || ly >= SIZE || lz >= SIZE) return false;
        byte id = vox[lx][ly][lz];
        return id != VoxelPalette.AIR_ID && palette.get(id).isSolid();
    }
}
