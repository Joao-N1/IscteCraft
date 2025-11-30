package jogo.voxel.blocks;

import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.texture.Texture2D;
import jogo.voxel.VoxelBlockType;

public class CraftingTableBlockType extends VoxelBlockType {
    public CraftingTableBlockType() { super("crafting_table"); }

    @Override
    public Material getMaterial(AssetManager assetManager) {
        Texture2D tex = (Texture2D) assetManager.loadTexture("Textures/CraftingTableBlock.png");
        Material m = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
        m.setTexture("DiffuseMap", tex);
        m.setBoolean("UseMaterialColors", true);
        m.setColor("Diffuse", ColorRGBA.White);
        return m;
    }

    @Override
    public float getHardness() {
        return 5.00f;
    }
}