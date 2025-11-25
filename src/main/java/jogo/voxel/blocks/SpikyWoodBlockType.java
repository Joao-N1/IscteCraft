package jogo.voxel.blocks;

import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.texture.Texture2D;
import jogo.voxel.VoxelBlockType;

public class SpikyWoodBlockType extends VoxelBlockType {
        public SpikyWoodBlockType() {super("spikywood");}

    @Override
    public Material getMaterial(AssetManager assetManager) {
        Texture2D tex = (Texture2D) assetManager.loadTexture("Textures/SpikyWoodBlock.png");
        Material m = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
        m.setTexture("DiffuseMap", tex);
        m.setBoolean("UseMaterialColors", true);
        m.setColor("Diffuse", ColorRGBA.White);
        m.setColor("Specular", ColorRGBA.White.mult(0.05f));
        m.setFloat("Shininess", 12f);
        return m;
    }

    @Override
    public float getHardness() {
        return 3.0f;
    }
}