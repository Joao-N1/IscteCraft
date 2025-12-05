package jogo.voxel.blocks;
import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.texture.Texture2D;
import jogo.voxel.VoxelBlockType;

public class IronItemBlockType extends VoxelBlockType {
    public IronItemBlockType() { super("iron_item"); }
    @Override public boolean isPlaceable() { return false; } // É um item!
    @Override
    public Material getMaterial(AssetManager am) {
        // Usa a textura do "material" que pediste
        Texture2D tex = (Texture2D) am.loadTexture("Materials/IronOre.png");
        Material m = new Material(am, "Common/MatDefs/Light/Lighting.j3md");
        m.setTexture("DiffuseMap", tex);
        m.setBoolean("UseMaterialColors", true);
        m.setColor("Diffuse", ColorRGBA.White);
        return m;
    }
}