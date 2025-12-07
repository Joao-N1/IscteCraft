package jogo.voxel.blocks;

import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.texture.Texture2D;
import jogo.voxel.VoxelBlockType;

public class SpikyPlanksBlockType extends VoxelBlockType {
    public SpikyPlanksBlockType() {
        super("Spiky Planks");
    }

    @Override
    public Material getMaterial(AssetManager assetManager) {
        // Placeholder: Usamos a textura da SpikyWood mas com cor alaranjada
        Texture2D tex = (Texture2D) assetManager.loadTexture("Textures/SpikyPlankBlock.png");
        Material m = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
        m.setTexture("DiffuseMap", tex);
        m.setBoolean("UseMaterialColors", true);
        m.setColor("Diffuse", new ColorRGBA(1.0f, 0.6f, 0.4f, 1f)); // Laranja claro
        return m;
    }

    @Override
    public float getHardness() {
        return 4.0f;
    }

    @Override
    public int getContactDamage() {
        return 10; // Causa dano se o jogador tocar (igual à Spiky Wood)
    }
}