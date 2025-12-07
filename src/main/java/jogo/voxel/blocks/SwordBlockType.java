package jogo.voxel.blocks;

import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.texture.Texture2D;
import jogo.voxel.VoxelBlockType;

public class SwordBlockType extends VoxelBlockType {
    public SwordBlockType() {
        super("Spiky Sword");
    }

    @Override
    public boolean isPlaceable() {
        return false; // É um item, não um bloco
    }

    @Override
    public Material getMaterial(AssetManager assetManager) {
        // Placeholder: Usamos a picareta de madeira pintada de vermelho
        Texture2D tex = (Texture2D) assetManager.loadTexture("Textures/Sword.png");
        Material m = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
        m.setTexture("DiffuseMap", tex);
        m.setBoolean("UseMaterialColors", true);
        m.setColor("Diffuse", ColorRGBA.Red);
        return m;
    }
}
