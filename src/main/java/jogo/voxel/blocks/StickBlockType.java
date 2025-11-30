package jogo.voxel.blocks;

import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.texture.Texture2D;
import jogo.voxel.VoxelBlockType;

public class StickBlockType extends VoxelBlockType {
    public StickBlockType() { super("stick"); }

    @Override
    public Material getMaterial(AssetManager assetManager) {
        // Podes criar Stick.png depois
        Texture2D tex = (Texture2D) assetManager.loadTexture("Textures/Stick.png");
        Material m = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
        m.setTexture("DiffuseMap", tex);
        m.setBoolean("UseMaterialColors", true);
        m.setColor("Diffuse", ColorRGBA.Brown);
        return m;
    }

    @Override
    public boolean isPlaceable() {
        return false; // Não deixa colocar no chão!
    }

    // (Opcional) Podes também por isSolid false para garantir
    @Override
    public boolean isSolid() {
        return false;
    }
}