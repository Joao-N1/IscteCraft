package jogo.voxel;

import com.jme3.asset.AssetManager;
import com.jme3.bullet.PhysicsSpace;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.texture.Texture2D;
import jogo.util.Hit;
import jogo.util.ProcTextures;
import Noise.OpenSimplexNoise;
import java.util.Random;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import jogo.system.GameSaveData;

/**
 * Gere a grelha de vóxeis, a geração do terreno e a gestão dos Chunks.
 */
public class VoxelWorld {
    private final AssetManager assetManager;
    private final int sizeX, sizeY, sizeZ;
    private final VoxelPalette palette;

    private final Node node = new Node("VoxelWorld");
    private final Map<Byte, Geometry> geoms = new HashMap<>();
    private final Map<Byte, Material> materials = new HashMap<>();

    // Render Flags
    private boolean lit = true;
    private boolean wireframe = false;
    private boolean culling = true;
    private int groundHeight = 8;

    // Dados dos Chunks
    private final int chunkSize = Chunk.SIZE;
    private final int chunkCountX, chunkCountY, chunkCountZ;
    private final Chunk[][][] chunks;

    public VoxelWorld(AssetManager assetManager, int sizeX, int sizeY, int sizeZ) {
        this.assetManager = assetManager;
        this.sizeX = sizeX;
        this.sizeY = sizeY;
        this.sizeZ = sizeZ;
        this.palette = VoxelPalette.defaultPalette();

        // Calcular quantidade de chunks necessários
        this.chunkCountX = (int)Math.ceil(sizeX / (float)chunkSize);
        this.chunkCountY = (int)Math.ceil(sizeY / (float)chunkSize);
        this.chunkCountZ = (int)Math.ceil(sizeZ / (float)chunkSize);

        this.chunks = new Chunk[chunkCountX][chunkCountY][chunkCountZ];
        for (int cx = 0; cx < chunkCountX; cx++)
            for (int cy = 0; cy < chunkCountY; cy++)
                for (int cz = 0; cz < chunkCountZ; cz++)
                    chunks[cx][cy][cz] = new Chunk(cx, cy, cz);

        initMaterials();
    }

    /**
     * Guarda o estado atual dos chunks modificados para o objeto de save.
     */
    public void saveChunksToData(GameSaveData data) {
        for (int cx = 0; cx < chunkCountX; cx++) {
            for (int cy = 0; cy < chunkCountY; cy++) {
                for (int cz = 0; cz < chunkCountZ; cz++) {
                    Chunk c = chunks[cx][cy][cz];

                    // Otimização: Verificar se o chunk tem algo antes de alocar memória massiva
                    // (Esta verificação é simplificada, idealmente o Chunk teria uma flag 'isEmpty')
                    byte[][][] chunkData = new byte[Chunk.SIZE][Chunk.SIZE][Chunk.SIZE];
                    boolean hasBlocks = false;

                    for(int x=0; x<Chunk.SIZE; x++){
                        for(int y=0; y<Chunk.SIZE; y++){
                            for(int z=0; z<Chunk.SIZE; z++){
                                byte b = c.get(x,y,z);
                                chunkData[x][y][z] = b;
                                if(b != VoxelPalette.AIR_ID) hasBlocks = true;
                            }
                        }
                    }

                    if (hasBlocks) {
                        String key = cx + "," + cy + "," + cz;
                        data.modifiedChunks.put(key, chunkData);
                    }
                }
            }
        }
    }

    /**
     * Carrega os chunks a partir dos dados guardados.
     */
    public void loadChunksFromData(GameSaveData data) {
        if (data.modifiedChunks == null) return;

        // 1. Limpar mundo atual (encher de ar)
        for (int cx = 0; cx < chunkCountX; cx++) {
            for (int cy = 0; cy < chunkCountY; cy++) {
                for (int cz = 0; cz < chunkCountZ; cz++) {
                    Chunk c = chunks[cx][cy][cz];
                    for (int x = 0; x < Chunk.SIZE; x++)
                        for (int y = 0; y < Chunk.SIZE; y++)
                            for (int z = 0; z < Chunk.SIZE; z++)
                                c.set(x, y, z, VoxelPalette.AIR_ID);
                    c.markDirty();
                }
            }
        }

        // 2. Aplicar dados do save
        for (Map.Entry<String, byte[][][]> entry : data.modifiedChunks.entrySet()) {
            String[] parts = entry.getKey().split(",");
            int cx = Integer.parseInt(parts[0]);
            int cy = Integer.parseInt(parts[1]);
            int cz = Integer.parseInt(parts[2]);

            if (cx >= 0 && cx < chunkCountX && cy >= 0 && cy < chunkCountY && cz >= 0 && cz < chunkCountZ) {
                Chunk c = chunks[cx][cy][cz];
                byte[][][] savedVoxels = entry.getValue();

                for (int x = 0; x < Chunk.SIZE; x++) {
                    for (int y = 0; y < Chunk.SIZE; y++) {
                        for (int z = 0; z < Chunk.SIZE; z++) {
                            c.set(x, y, z, savedVoxels[x][y][z]);
                        }
                    }
                }
                c.markDirty();
            }
        }
    }

    // --- Acesso a Blocos e Chunks ---

    private Chunk getChunk(int x, int y, int z) {
        int cx = x / chunkSize;
        int cy = y / chunkSize;
        int cz = z / chunkSize;
        if (cx < 0 || cy < 0 || cz < 0 || cx >= chunkCountX || cy >= chunkCountY || cz >= chunkCountZ) return null;
        return chunks[cx][cy][cz];
    }

    private int lx(int x) { return x % chunkSize; }
    private int ly(int y) { return y % chunkSize; }
    private int lz(int z) { return z % chunkSize; }

    public byte getBlock(int x, int y, int z) {
        Chunk c = getChunk(x, y, z);
        if (c == null || !inBounds(x,y,z)) return VoxelPalette.AIR_ID;
        return c.get(lx(x), ly(y), lz(z));
    }

    public void setBlock(int x, int y, int z, byte id) {
        Chunk c = getChunk(x, y, z);
        if (c != null) {
            c.set(lx(x), ly(y), lz(z), id);
            c.markDirty();

            // Atualizar chunks vizinhos se estivermos na borda
            if (lx(x) == 0) markNeighborChunkDirty(x-1, y, z);
            if (lx(x) == chunkSize-1) markNeighborChunkDirty(x+1, y, z);
            if (ly(y) == 0) markNeighborChunkDirty(x, y-1, z);
            if (ly(y) == chunkSize-1) markNeighborChunkDirty(x, y+1, z);
            if (lz(z) == 0) markNeighborChunkDirty(x, y, z-1);
            if (lz(z) == chunkSize-1) markNeighborChunkDirty(x, y, z+1);
        }
    }

    private void markNeighborChunkDirty(int x, int y, int z) {
        Chunk n = getChunk(x, y, z);
        if (n != null) n.markDirty();
    }

    public boolean breakAt(int x, int y, int z) {
        if (!inBounds(x,y,z)) return false;
        byte blockId = getBlock(x, y, z);

        if (blockId == VoxelPalette.THEROCK_ID) return false; // Bedrock indestrutível

        setBlock(x, y, z, VoxelPalette.AIR_ID);
        return true;
    }

    // --- Geração Procedimental ---

    public void generateLayers() {
        long seed = new Random().nextLong();

        // --- CONFIGURAÇÕES DE GERAÇÃO ---
        final int GROUND_BASE = 20;
        final float NOISE_SCALE = 0.03f;
        final int AMPLITUDE = 6;
        final int MAP_LIMIT = 60;
        final int SAND_WIDTH = 7;
        final int WATER_WIDTH = 10;
        final int WATER_LEVEL = 16;
        final double CAVE_THRESHOLD = 0.60;

        int centerX = sizeX / 2;
        int centerZ = sizeZ / 2;

        System.out.println("A gerar terreno com seed: " + seed);

        // 1. Iterar por todas as colunas do mundo
        for (int x = 0; x < sizeX; x++) {
            for (int z = 0; z < sizeZ; z++) {

                int dist = Math.max(Math.abs(x - centerX), Math.abs(z - centerZ));
                double noise = OpenSimplexNoise.OpenSimplex2S.noise2(seed, x * NOISE_SCALE, z * NOISE_SCALE);
                int height = GROUND_BASE + (int)(noise * AMPLITUDE);

                // Determinar altura máxima do loop (para nivelar a água nas bordas)
                int loopHeight = (dist > MAP_LIMIT) ? sizeY : (height + 1);

                for (int y = 0; y < loopHeight && y < sizeY; y++) {

                    // A. Zona Fora do Mapa (Vazio)
                    if (dist > MAP_LIMIT + SAND_WIDTH + WATER_WIDTH) {
                        setBlock(x, y, z, VoxelPalette.AIR_ID);
                        continue;
                    }

                    // B. Zona de Água (Oceano)
                    if (dist > MAP_LIMIT + SAND_WIDTH) {
                        if (y <= WATER_LEVEL) setBlock(x, y, z, VoxelPalette.WATER_ID);
                        else setBlock(x, y, z, VoxelPalette.AIR_ID);
                        continue;
                    }

                    // C. Zona de Areia (Praia)
                    if (dist > MAP_LIMIT) {
                        if (y <= height) {
                            if (y == height) setBlock(x, y, z, VoxelPalette.SAND_ID);
                            else setBlock(x, y, z, VoxelPalette.STONE_ID);
                        }
                        if (y == 0) setBlock(x, 0, z, VoxelPalette.THEROCK_ID);
                        continue;
                    }

                    // D. Zona Normal (Terra)
                    if (y <= height) {
                        // Geração de Cavernas
                        if (y > 0 && y < 20) {
                            double caveNoise = OpenSimplexNoise.OpenSimplex2S.noise3_Fallback(seed + 9999, x * 0.04, y * 0.04, z * 0.04);
                            if (caveNoise > CAVE_THRESHOLD) {
                                setBlock(x, y, z, VoxelPalette.AIR_ID);
                                continue;
                            }
                        }

                        if (y == height) setBlock(x, y, z, VoxelPalette.GRASS_ID);
                        else if (y > height - 3) setBlock(x, y, z, VoxelPalette.DIRT_ID);
                        else setBlock(x, y, z, VoxelPalette.STONE_ID);
                    }

                    // Bedrock no fundo
                    if (y == 0) setBlock(x, 0, z, VoxelPalette.THEROCK_ID);
                }
            }
        }

        // 2. Decoradores e Minérios
        generateOreVeins(seed);
        generateTrees(seed, centerX, centerZ, MAP_LIMIT);
        generateSpikyTrees(seed, centerX, centerZ, MAP_LIMIT);
        generateTargets(seed);

        System.out.println("Terreno gerado com sucesso!");
    }

    private void generateTrees(long seed, int centerX, int centerZ, int mapLimit) {
        Random random = new Random(seed + 12345);
        int treeChance = 400;

        for (int x = 2; x < sizeX - 2; x++) {
            for (int z = 2; z < sizeZ - 2; z++) {
                int dist = Math.max(Math.abs(x - centerX), Math.abs(z - centerZ));
                if (dist > mapLimit) continue;

                if (random.nextInt(treeChance) != 0) continue;

                int y = getTopSolidY(x, z);
                if (y < 0 || getBlock(x, y, z) != VoxelPalette.GRASS_ID) continue;

                // Construir Árvore
                int treeHeight = 4 + random.nextInt(3);
                for (int h = 1; h <= treeHeight; h++) setBlock(x, y + h, z, VoxelPalette.Wood_ID);

                // Construir Copa
                int top = y + treeHeight;
                for (int lx = -2; lx <= 2; lx++) {
                    for (int lz = -2; lz <= 2; lz++) {
                        for (int ly = 0; ly <= 2; ly++) {
                            if (random.nextInt(6) != 0) { // Pequena aleatoriedade nas folhas
                                setBlock(x + lx, top + ly, z + lz, VoxelPalette.Leaf_ID);
                            }
                        }
                    }
                }
            }
        }
    }

    private void generateSpikyTrees(long seed, int centerX, int centerZ, int mapLimit) {
        Random random = new Random(seed + 67890);
        int treeChance = 1000; // Mais raras

        for (int x = 2; x < sizeX - 2; x++) {
            for (int z = 2; z < sizeZ - 2; z++) {
                if (Math.max(Math.abs(x - centerX), Math.abs(z - centerZ)) > mapLimit) continue;
                if (random.nextInt(treeChance) != 0) continue;

                int y = getTopSolidY(x, z);
                if (y < 0 || getBlock(x, y, z) != VoxelPalette.GRASS_ID) continue;

                int treeHeight = 4 + random.nextInt(3);

                // Tronco Espinhoso
                for (int h = 1; h <= treeHeight; h++) {
                    byte current = getBlock(x, y + h, z);
                    if (current == VoxelPalette.AIR_ID || current == VoxelPalette.Leaf_ID) {
                        setBlock(x, y + h, z, VoxelPalette.SpikyWood_ID);
                    }
                }

                // Copa (reutiliza lógica de folhas)
                int top = y + treeHeight;
                for (int lx = -2; lx <= 2; lx++) {
                    for (int lz = -2; lz <= 2; lz++) {
                        for (int ly = 0; ly <= 2; ly++) {
                            if (random.nextInt(6) != 0 && getBlock(x + lx, top + ly, z + lz) == VoxelPalette.AIR_ID) {
                                setBlock(x + lx, top + ly, z + lz, VoxelPalette.Leaf_ID);
                            }
                        }
                    }
                }
            }
        }
    }

    private void generateOreVeins(long seed) {
        Random random = new Random(seed);
        // Densidade: Tentativas por mapa
        int coalAttempts = 2400;
        int ironAttempts = 3000;
        int diamondAttempts = 800;

        for (int i = 0; i < coalAttempts; i++)
            spawnVein(random, VoxelPalette.COAL_ID, sizeY, 6 + random.nextInt(5));

        for (int i = 0; i < ironAttempts; i++)
            spawnVein(random, VoxelPalette.IRON_ID, 40, 3 + random.nextInt(4)); // Apenas abaixo de Y=40

        for (int i = 0; i < diamondAttempts; i++)
            spawnVein(random, VoxelPalette.DIAMOND_ID, 16, 2 + random.nextInt(3)); // Apenas abaixo de Y=16
    }

    private void spawnVein(Random rand, byte oreId, int maxHeight, int size) {
        int x = rand.nextInt(sizeX);
        int z = rand.nextInt(sizeZ);
        int y = rand.nextInt(maxHeight);

        for (int i = 0; i < size; i++) {
            int tx = x + rand.nextInt(3) - 1;
            int ty = y + rand.nextInt(3) - 1;
            int tz = z + rand.nextInt(3) - 1;

            if (inBounds(tx, ty, tz) && getBlock(tx, ty, tz) == VoxelPalette.STONE_ID) {
                setBlock(tx, ty, tz, oreId);
                x = tx; y = ty; z = tz; // Mover centro para crescer organicamente
            }
        }
    }

    private void generateTargets(long seed) {
        Random random = new Random(seed + 555);
        int targetCount = 20;
        int placed = 0;

        for (int i = 0; i < 1000 && placed < targetCount; i++) {
            int x = random.nextInt(sizeX - 4) + 2;
            int z = random.nextInt(sizeZ - 4) + 2;
            int y = getTopSolidY(x, z);

            if (y > 0 && y < sizeY - 5) {
                if (random.nextBoolean()) setBlock(x, y + 1, z, VoxelPalette.TARGET_ID); // Chão
                else setBlock(x, y + 3 + random.nextInt(3), z, VoxelPalette.TARGET_ID); // Flutuante
                placed++;
            }
        }
        System.out.println("Alvos colocados: " + placed);
    }

    // --- Utilitários de Rendering e Física ---

    public int getTopSolidY(int x, int z) {
        if (x < 0 || z < 0 || x >= sizeX || z >= sizeZ) return -1;
        for (int y = sizeY - 1; y >= 0; y--) {
            if (palette.get(getBlock(x, y, z)).isSolid()) return y;
        }
        return -1;
    }

    public Vector3f getRecommendedSpawn() {
        int cx = sizeX / 2;
        int cz = sizeZ / 2;
        int ty = getTopSolidY(cx, cz);
        if (ty < 0) ty = groundHeight;
        return new Vector3f(cx + 0.5f, ty + 3.0f, cz + 0.5f);
    }

    private void initMaterials() {
        Texture2D tex = ProcTextures.checker(128, 4, ColorRGBA.Gray, ColorRGBA.DarkGray);
        materials.put(VoxelPalette.STONE_ID, makeLitTex(tex, 0.08f, 16f));
    }

    private Material makeLitTex(Texture2D tex, float spec, float shininess) {
        Material m = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
        m.setTexture("DiffuseMap", tex);
        m.setBoolean("UseMaterialColors", true);
        m.setColor("Diffuse", ColorRGBA.White);
        m.setColor("Specular", ColorRGBA.White.mult(spec));
        m.setFloat("Shininess", shininess);
        applyRenderFlags(m);
        return m;
    }

    private void applyRenderFlags(Material m) {
        m.getAdditionalRenderState().setFaceCullMode(culling ? RenderState.FaceCullMode.Back : RenderState.FaceCullMode.Off);
        m.getAdditionalRenderState().setWireframe(wireframe);
    }

    public void buildMeshes() {
        node.detachAllChildren();
        for (int cx = 0; cx < chunkCountX; cx++) {
            for (int cy = 0; cy < chunkCountY; cy++) {
                for (int cz = 0; cz < chunkCountZ; cz++) {
                    Chunk chunk = chunks[cx][cy][cz];
                    chunk.buildMesh(assetManager, palette);
                    node.attachChild(chunk.getNode());
                }
            }
        }
    }

    public void buildPhysics(PhysicsSpace space) {
        if (space == null) return;
        for (int cx = 0; cx < chunkCountX; cx++) {
            for (int cy = 0; cy < chunkCountY; cy++) {
                for (int cz = 0; cz < chunkCountZ; cz++) {
                    chunks[cx][cy][cz].updatePhysics(space, palette);
                }
            }
        }
    }

    public void rebuildDirtyChunks(PhysicsSpace physicsSpace) {
        int rebuilt = 0;
        for (int cx = 0; cx < chunkCountX; cx++) {
            for (int cy = 0; cy < chunkCountY; cy++) {
                for (int cz = 0; cz < chunkCountZ; cz++) {
                    Chunk chunk = chunks[cx][cy][cz];
                    if (chunk.isDirty()) {
                        chunk.buildMesh(assetManager, palette);
                        chunk.updatePhysics(physicsSpace, palette);
                        chunk.clearDirty();
                        rebuilt++;
                    }
                }
            }
        }
        if (rebuilt > 0 && physicsSpace != null) physicsSpace.update(0);
    }

    public void clearAllDirtyFlags() {
        for (int cx = 0; cx < chunkCountX; cx++)
            for (int cy = 0; cy < chunkCountY; cy++)
                for (int cz = 0; cz < chunkCountZ; cz++)
                    chunks[cx][cy][cz].clearDirty();
    }

    public Optional<Hit> pickFirstSolid(Camera cam, float maxDistance) {
        Vector3f origin = cam.getLocation();
        Vector3f dir = cam.getDirection().normalize();

        int x = (int) Math.floor(origin.x);
        int y = (int) Math.floor(origin.y);
        int z = (int) Math.floor(origin.z);

        // Algoritmo de Voxel Traversal (DDA)
        float stepX = dir.x > 0 ? 1 : -1;
        float stepY = dir.y > 0 ? 1 : -1;
        float stepZ = dir.z > 0 ? 1 : -1;

        float tDeltaX = (dir.x != 0) ? stepX / dir.x : Float.POSITIVE_INFINITY;
        float tDeltaY = (dir.y != 0) ? stepY / dir.y : Float.POSITIVE_INFINITY;
        float tDeltaZ = (dir.z != 0) ? stepZ / dir.z : Float.POSITIVE_INFINITY;

        float tMaxX = (dir.x != 0) ? (x + (stepX > 0 ? 1 : 0) - origin.x) / dir.x : Float.POSITIVE_INFINITY;
        float tMaxY = (dir.y != 0) ? (y + (stepY > 0 ? 1 : 0) - origin.y) / dir.y : Float.POSITIVE_INFINITY;
        float tMaxZ = (dir.z != 0) ? (z + (stepZ > 0 ? 1 : 0) - origin.z) / dir.z : Float.POSITIVE_INFINITY;

        if (inBounds(x,y,z) && isSolid(x,y,z)) return Optional.of(new Hit(new Vector3i(x,y,z), new Vector3f(0,0,0), 0f));

        float t = 0f;
        Vector3f lastNormal = new Vector3f(0,0,0);

        while (t <= maxDistance) {
            if (tMaxX < tMaxY) {
                if (tMaxX < tMaxZ) {
                    x += stepX; t = tMaxX; tMaxX += tDeltaX;
                    lastNormal.set(-stepX, 0, 0);
                } else {
                    z += stepZ; t = tMaxZ; tMaxZ += tDeltaZ;
                    lastNormal.set(0, 0, -stepZ);
                }
            } else {
                if (tMaxY < tMaxZ) {
                    y += stepY; t = tMaxY; tMaxY += tDeltaY;
                    lastNormal.set(0, -stepY, 0);
                } else {
                    z += stepZ; t = tMaxZ; tMaxZ += tDeltaZ;
                    lastNormal.set(0, 0, -stepZ);
                }
            }

            if (!inBounds(x,y,z)) {
                if (t > maxDistance) break;
                continue;
            }
            if (isSolid(x,y,z)) {
                return Optional.of(new Hit(new Vector3i(x,y,z), lastNormal.clone(), t));
            }
        }
        return Optional.empty();
    }

    private boolean isSolid(int x, int y, int z) {
        if (!inBounds(x,y,z)) return false;
        return palette.get(getBlock(x, y, z)).isSolid();
    }

    private boolean inBounds(int x, int y, int z) {
        return x >= 0 && y >= 0 && z >= 0 && x < sizeX && y < sizeY && z < sizeZ;
    }

    public void setLit(boolean lit) {
        if (this.lit == lit) return;
        this.lit = lit;
        // Atualiza materiais para debug
        for (var e : geoms.entrySet()) {
            Geometry g = e.getValue();
            var oldMat = g.getMaterial();
            // Lógica simplificada de troca de material...
            // (Mantida como estava pois funciona para debug)
        }
    }

    public void toggleRenderDebug() {
        setLit(!lit);
        wireframe = !wireframe;
        culling = !culling;
        System.out.println("Debug Render: Lit=" + lit + " Wire=" + wireframe + " Cull=" + culling);
        // Aplicar flags a todos os chunks (geometrias filhas)
        node.depthFirstTraversal(spatial -> {
            if (spatial instanceof Geometry g) applyRenderFlags(g.getMaterial());
        });
    }

    public Node getNode() { return node; }
    public VoxelPalette getPalette() { return palette; }

    public static class Vector3i {
        public final int x, y, z;
        public Vector3i(int x, int y, int z) { this.x=x; this.y=y; this.z=z; }
    }
}