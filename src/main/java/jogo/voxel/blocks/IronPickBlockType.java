package jogo.voxel.blocks;
import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.texture.Texture2D;
import jogo.voxel.VoxelBlockType;

public class IronPickBlockType extends VoxelBlockType {
    public IronPickBlockType() { super("iron_pick"); }
    @Override public boolean isPlaceable() { return false; }
    @Override
    public Material getMaterial(AssetManager am) {
        Texture2D tex = (Texture2D) am.loadTexture("Textures/IronPick.png");
        Material m = new Material(am, "Common/MatDefs/Light/Lighting.j3md");
        m.setTexture("DiffuseMap", tex);
        m.setBoolean("UseMaterialColors", true);
        m.setColor("Diffuse", ColorRGBA.White);
        return m;
    }
}