package jogo.voxel.blocks;
import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.texture.Texture2D;
import jogo.voxel.VoxelBlockType;

public class LanternOnBlockType extends VoxelBlockType {
    public LanternOnBlockType() { super("lantern_on"); }
    @Override
    public Material getMaterial(AssetManager am) {
        Texture2D tex = (Texture2D) am.loadTexture("Textures/LanternOn.png");
        Material m = new Material(am, "Common/MatDefs/Light/Lighting.j3md");
        m.setTexture("DiffuseMap", tex);
        m.setBoolean("UseMaterialColors", true);
        m.setColor("Diffuse", ColorRGBA.White);
        m.setColor("GlowColor", new ColorRGBA(1f, 1f, 0.8f, 1f)); // LUZ!
        return m;
    }

    @Override
    public float getHardness() {
        return 4.0f;
    }
}