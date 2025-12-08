package jogo.voxel;

import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.texture.Texture2D;

public class SimpleItemType extends VoxelBlockType {
    private final String texturePath;

    public SimpleItemType(String name, String textureName) {
        super(name);
        this.texturePath = textureName.contains("/") ? textureName : "Textures/" + textureName;
    }

    @Override
    public boolean isPlaceable() {
        return false; // É um item, não vai para o mundo!
    }

    @Override
    public boolean isSolid() {
        return false;
    }

    @Override
    public Material getMaterial(AssetManager assetManager) {
        Texture2D tex = (Texture2D) assetManager.loadTexture(texturePath);
        Material m = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
        m.setTexture("DiffuseMap", tex);
        m.setBoolean("UseMaterialColors", true);
        m.setColor("Diffuse", ColorRGBA.White);
        return m;
    }
}