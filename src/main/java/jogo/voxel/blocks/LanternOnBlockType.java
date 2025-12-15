package jogo.voxel.blocks;

import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.texture.Texture2D;
import jogo.voxel.VoxelBlockType;

public class LanternOnBlockType extends VoxelBlockType {
    public LanternOnBlockType() { super("LanternOn"); }

    @Override
    public Material getMaterial(AssetManager assetManager) {
        Texture2D tex = (Texture2D) assetManager.loadTexture("Textures/LanternOn.png");
        Material m = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
        m.setTexture("DiffuseMap", tex);

        // Faz a lanterna brilhar intensamente
        m.setTexture("GlowMap", tex);
        m.setColor("GlowColor", ColorRGBA.White);

        m.setBoolean("UseMaterialColors", true);
        m.setColor("Diffuse", ColorRGBA.White);
        m.setColor("Specular", ColorRGBA.White);
        m.setFloat("Shininess", 64f);
        return m;
    }

}