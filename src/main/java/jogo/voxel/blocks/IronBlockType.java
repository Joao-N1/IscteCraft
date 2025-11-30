package jogo.voxel.blocks;

import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.texture.Texture2D;
import jogo.voxel.VoxelBlockType;

public class IronBlockType extends VoxelBlockType {
    private Material cachedMaterial;

    public IronBlockType() {
        super("Iron Ore");
    }



    @Override
    public Material getMaterial(AssetManager assetManager) {
        if (cachedMaterial == null) {
            Texture2D tex = (Texture2D) assetManager.loadTexture("Textures/IronBlock.png");
            cachedMaterial = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
            cachedMaterial.setTexture("DiffuseMap", tex);
            cachedMaterial.setBoolean("UseMaterialColors", true);
            cachedMaterial.setColor("Diffuse", ColorRGBA.White);
            cachedMaterial.setColor("Specular", ColorRGBA.White.mult(0.1f)); // Um pouco mais brilhante
            cachedMaterial.setFloat("Shininess", 32f);
        }
        return cachedMaterial;
    }

    @Override
    public float getHardness() {
        return 10.50f;
    }
}