package jogo.voxel.blocks;
import jogo.voxel.SimpleBlockType;
import jogo.voxel.VoxelPalette;

public class IronOreBlockType extends SimpleBlockType {
    public IronOreBlockType() {
        // Define o drop como o ID do item Ferro
        super("Iron Block", "Textures/IronBlock.png", 8.0f, VoxelPalette.IRON_MAT_ID);
    }
}
