package jogo.voxel;

import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import jogo.appstate.PlayerAppState;
import jogo.appstate.WorldAppState;
import jogo.voxel.VoxelWorld.Vector3i;

// Classe base abstrata para representar tipos de blocos voxel no jogo
public abstract class VoxelBlockType {
    private final String name;

    protected VoxelBlockType(String name) {
        this.name = name;
    }

    public String getName() { return name; }

    public boolean isSolid() { return true; }
    public boolean isPlaceable() { return true; }
    public boolean isTransparent() { return false; }
    public float getHardness() { return 0.1f; }
    public byte getDropItem() { return 0; }
    public int getContactDamage() { return 0; }

    public abstract Material getMaterial(AssetManager assetManager);

    public Material getMaterial(AssetManager assetManager, jogo.framework.math.Vec3 blockPos) {
        return getMaterial(assetManager);
    }

    // --- MÉTODOS DE EVENTOS ---

    /**
     * Chamado a cada frame enquanto o jogador mina.
     * @return true se pode continuar a minar.
     */
    public boolean processMining(WorldAppState world, PlayerAppState player, float tpf) {
        return true;
    }

    /**
     * Chamado quando o bloco parte.
     * A classe base apenas remove o voxel do mundo físico.
     */
    public void onBlockBreak(WorldAppState world, Vector3i pos, PlayerAppState player) {
        // 1. Remove o bloco do mundo
        world.getVoxelWorld().breakAt(pos.x, pos.y, pos.z);

        // 2. Atualiza a malha
        world.getVoxelWorld().rebuildDirtyChunks(world.getPhysicsSpace());

        // 3. Atualiza a física do jogador (se estiver em cima do bloco)
        if (player != null) {
            player.refreshPhysics();
        }
    }
}