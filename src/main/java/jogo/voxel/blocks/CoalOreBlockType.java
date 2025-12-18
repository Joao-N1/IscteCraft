package jogo.voxel.blocks;
import jogo.voxel.SimpleBlockType;
import jogo.voxel.VoxelPalette;

public class CoalOreBlockType extends SimpleBlockType {
    public CoalOreBlockType() {
        // Define o drop como o ID do item Carvão
        super("Coal Block", "Textures/CoalBlock.png", 7.0f, VoxelPalette.COAL_MAT_ID);
    }
}