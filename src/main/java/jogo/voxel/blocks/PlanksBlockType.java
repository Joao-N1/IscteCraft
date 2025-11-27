package jogo.voxel.blocks;

import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.texture.Texture2D;
import jogo.voxel.VoxelBlockType;

public class PlanksBlockType extends VoxelBlockType {
    public PlanksBlockType() { super("planks"); }

    @Override
    public Material getMaterial(AssetManager assetManager) {
        // Nota: Deves criar a imagem Planks.png na pasta Textures
        // Por agora, usa WoodBlock.png como placeholder se não tiveres a imagem
        Texture2D tex = (Texture2D) assetManager.loadTexture("Textures/PlanksBlock.png");
        Material m = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
        m.setTexture("DiffuseMap", tex);
        m.setBoolean("UseMaterialColors", true);
        m.setColor("Diffuse", new ColorRGBA(0.8f, 0.6f, 0.4f, 1f)); // Tom mais claro
        m.setFloat("Shininess", 4f);
        return m;
    }
}