package jogo.voxel;

import com.jme3.asset.AssetManager;
import com.jme3.material.Material;

// Classe base para tipos de blocos voxel
public abstract class VoxelBlockType {
    private final String name;

    protected VoxelBlockType(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    /** Whether this block is physically solid (collides/occludes). */
    public boolean isSolid() { return true; }

    public boolean isPlaceable() {
        return true;
    }

    // ... outros métodos ...

    // Define se o bloco é transparente
    // Por defeito é false (opaco)
    public boolean isTransparent() {
        return false;
    }


    //define o tempo para partir os blocos
    public float getHardness() {
        return 0.1f; // Valor padrão (muito rápido)
    }

    public byte getDropItem() {
        return 0;
    }

    /**
     * Returns the Material for this block type. Override in subclasses for custom materials.
     */
    public abstract Material getMaterial(AssetManager assetManager);

    /**
     * Returns the Material for this block type at a specific block position.
     * Default implementation ignores the position for backward compatibility.
     * Subclasses can override to use blockPos.
     */
    public Material getMaterial(AssetManager assetManager, jogo.framework.math.Vec3 blockPos) {
        return getMaterial(assetManager);
    }

    public int getContactDamage() {
        return 0;
    }
}

