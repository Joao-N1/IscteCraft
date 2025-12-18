package jogo.voxel.items;
import jogo.voxel.SimpleItemType;

public class WoodPickItem extends SimpleItemType {
    public WoodPickItem() {
        super("wood_pick", "Textures/WoodPick.png");
    }

    @Override
    public float getMiningSpeed() {
        return 2.0f;
    }
}