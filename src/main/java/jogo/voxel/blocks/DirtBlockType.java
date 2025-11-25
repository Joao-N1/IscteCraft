package jogo.voxel.blocks;

import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.texture.Texture2D;
import jogo.util.ProcTextures;
import jogo.voxel.VoxelBlockType;

public class DirtBlockType extends VoxelBlockType {
        public DirtBlockType() {super("dirt");}

    @Override
    public Material getMaterial(AssetManager assetManager) {
        Texture2D tex = (Texture2D) assetManager.loadTexture("Textures/DirtBlock.png");
        Material m = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
        m.setTexture("DiffuseMap", tex);
        m.setBoolean("UseMaterialColors", true);
        m.setColor("Diffuse", ColorRGBA.White);
        m.setColor("Specular", ColorRGBA.White.mult(0.03f));
        m.setFloat("Shininess", 8f);
        return m;
    }

    @Override
    public float getHardness() {
        return 1.00f;
    }
}

