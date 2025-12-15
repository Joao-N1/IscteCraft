package jogo.voxel.blocks;

import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.material.RenderState.BlendMode;
import com.jme3.math.ColorRGBA;
import com.jme3.texture.Texture2D;
import jogo.voxel.VoxelBlockType;

public class WaterBlockType extends VoxelBlockType {
    public WaterBlockType() { super("water"); }

    @Override
    public boolean isSolid() { return false; } // O player atravessa

    @Override
    public boolean isTransparent() {
        return true; // Diz ao Chunk para usar o modo transparente
    }

    @Override
    public int getContactDamage() { return 50; } // Mata "afogado"

    @Override
    public Material getMaterial(AssetManager assetManager) {
        Texture2D tex = (Texture2D) assetManager.loadTexture("Textures/WaterBlock.png");
        Material m = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
        m.setTexture("DiffuseMap", tex);
        m.setBoolean("UseMaterialColors", true);
        m.setColor("Diffuse", new ColorRGBA(0.2f, 0.4f, 0.8f, 0.6f));

        // Configurar transparência
        m.getAdditionalRenderState().setBlendMode(BlendMode.Alpha);
        return m;
    }
}