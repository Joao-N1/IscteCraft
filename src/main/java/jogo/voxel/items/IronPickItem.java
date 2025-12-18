package jogo.voxel.items;
import jogo.voxel.SimpleItemType;

public class IronPickItem extends SimpleItemType {
    public IronPickItem() {
        super("iron_pick", "Textures/IronPick.png");
    }

    @Override
    public float getMiningSpeed() {
        return 6.0f;
    }
}