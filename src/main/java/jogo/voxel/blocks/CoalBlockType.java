package jogo.voxel.blocks;

import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.texture.Texture2D;
import jogo.voxel.VoxelBlockType;
import jogo.voxel.VoxelPalette;

public class CoalBlockType extends VoxelBlockType {
    private Material cachedMaterial;

    public CoalBlockType() {
        super("Coal Block");
    }

    @Override
    public Material getMaterial(AssetManager am) {
        // TU PEDISTE: CoalBlock.png é o BLOCO que se parte
        Texture2D tex = (Texture2D) am.loadTexture("Textures/CoalBlock.png");
        Material m = new Material(am, "Common/MatDefs/Light/Lighting.j3md");
        m.setTexture("DiffuseMap", tex);
        m.setBoolean("UseMaterialColors", true);
        m.setColor("Diffuse", ColorRGBA.White);
        return m;
    }

    @Override
    public float getHardness() {
        return 7.0f;
    }

    @Override
    public byte getDropItem() {
        return VoxelPalette.COAL_MAT_ID; // Dropa o item, não o bloco!
    }
}
