package jogo.voxel;

import jogo.voxel.blocks.*;

import java.util.ArrayList;
import java.util.List;

public class VoxelPalette {
    private final List<VoxelBlockType> types = new ArrayList<>();

    public byte register(VoxelBlockType type) {
        types.add(type);
        int id = types.size() - 1;
        if (id > 255) throw new IllegalStateException("Too many voxel block types (>255)");
        return (byte) id;
    }

    public VoxelBlockType get(byte id) {
        int idx = Byte.toUnsignedInt(id);
        if (idx < 0 || idx >= types.size()) return new AirBlockType();
        return types.get(idx);
    }

    public int size() { return types.size(); }

    public static VoxelPalette defaultPalette() {
        VoxelPalette p = new VoxelPalette();
        p.register(new AirBlockType());   // id 0
        p.register(new StoneBlockType()); // id 1
        p.register(new TheRockBlockType()); // id 2
        p.register(new DirtBlockType()); // id 3
        p.register(new GrassBlockType()); // id 4
        p.register(new WoodBlockType()); // id 5
        p.register(new SpikyWoodBlockType()); // id 6
        p.register(new LeafBlockType()); // id 7
        p.register(new CoalBlockType()); // id 8
        p.register(new IronBlockType()); // id 9
        p.register(new DiamondBlockType()); // id 10
        p.register(new PlanksBlockType()); // id 11
        p.register(new StickBlockType());  // id 12
        p.register(new CraftingTableBlockType()); // id 13
        return p;
    }

    public static final byte AIR_ID = 0;
    public static final byte STONE_ID = 1;
    public static final byte THEROCK_ID = 2;
    public static final byte DIRT_ID = 3;
    public static final byte GRASS_ID = 4;
    public static final byte Wood_ID = 5;
    public static final byte SpikyWood_ID = 6;
    public static final byte Leaf_ID = 7;
    public static final byte COAL_ID = 8;
    public static final byte IRON_ID = 9;
    public static final byte DIAMOND_ID = 10;
    public static final byte PLANKS_ID = 11;
    public static final byte STICK_ID = 12;
    public static final byte CRAFTING_TABLE_ID = 13;
}
