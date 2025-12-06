package jogo.voxel.blocks;

import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.texture.Texture2D;
import jogo.voxel.VoxelBlockType;

public class TargetBlockType extends VoxelBlockType {
    public TargetBlockType() { super("Target"); }

    @Override
    public Material getMaterial(AssetManager assetManager) {
        // Certifica-te que o ficheiro "TargetBlock.png" existe na pasta src/main/resources/Textures/
        Texture2D tex = (Texture2D) assetManager.loadTexture("Textures/TargetBlock.png");

        Material m = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
        m.setTexture("DiffuseMap", tex);

        m.setBoolean("UseMaterialColors", true);

        // --- A CORREÇÃO ESTÁ AQUI ---
        // Definir a cor como White (Branco) faz com que a textura seja renderizada com as suas cores originais.
        m.setColor("Diffuse", ColorRGBA.White);

        m.setColor("Specular", ColorRGBA.White);
        m.setFloat("Shininess", 10f); // Brilhante
        return m;
    }

    @Override
    public float getHardness() {
        return 0.5f; // Fácil de partir se necessário
    }
}