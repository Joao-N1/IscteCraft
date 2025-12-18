package jogo.voxel.items;
import jogo.voxel.SimpleItemType;

public class StonePickItem extends SimpleItemType {
    public StonePickItem() {
        super("stone_pick", "Textures/StonePick.png");
    }

    @Override
    public float getMiningSpeed() {
        return 4.0f;
    }
}