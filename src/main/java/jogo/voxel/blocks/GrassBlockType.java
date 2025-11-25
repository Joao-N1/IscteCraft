package jogo.voxel.blocks;

import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.texture.Texture2D;
import jogo.util.ProcTextures;
import jogo.voxel.VoxelBlockType;

public class GrassBlockType extends VoxelBlockType {
    public GrassBlockType() {super("grass");}

    @Override
    public Material getMaterial(AssetManager assetManager) {
        Texture2D tex = (Texture2D) assetManager.loadTexture("Textures/GrassBlock.png");
        Material m = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
        m.setTexture("DiffuseMap", tex);
        m.setBoolean("UseMaterialColors", true);
        m.setColor("Diffuse", ColorRGBA.White);
        m.setColor("Specular", ColorRGBA.White.mult(0.05f));
        m.setFloat("Shininess", 16f);
        return m;
    }
    @Override
    public float getHardness() {
        return 1.00f;
    }
}

