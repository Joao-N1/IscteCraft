package jogo.voxel.blocks;
import jogo.voxel.SimpleBlockType;

public class TargetBlockType extends SimpleBlockType {
    public TargetBlockType() {
        // Dureza muito alta para não ser quebrado facilmente
        super("Target", "TargetBlock.png", 999999f);
    }
}