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
        p.register(new AirBlockType());        // 0
        p.register(new StoneBlockType());      // 1
        p.register(new TheRockBlockType());    // 2
        p.register(new DirtBlockType());       // 3
        p.register(new GrassBlockType());      // 4
        p.register(new WoodBlockType());       // 5
        p.register(new SpikyWoodBlockType());  // 6
        p.register(new LeafBlockType());       // 7
        p.register(new CoalBlockType());       // 8
        p.register(new IronBlockType());       // 9
        p.register(new DiamondBlockType());    // 10
        p.register(new PlanksBlockType());     // 11
        p.register(new StickBlockType());      // 12
        p.register(new CraftingTableBlockType()); // 13
        p.register(new SandBlockType());       // 14
        p.register(new WaterBlockType());      // 15
        p.register(new TargetBlockType());     // ID 16

        // Novos Itens
        p.register(new CoalItemBlockType());   // 17
        p.register(new IronItemBlockType());   // 18
        p.register(new LanternOffBlockType()); // 19
        p.register(new LanternOnBlockType());  // 20
        p.register(new WoodPickBlockType());   // 21
        p.register(new StonePickBlockType());  // 22
        p.register(new IronPickBlockType());   // 23

        return p;
    }

    // IDs Estáticos
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
    public static final byte SAND_ID = 14;
    public static final byte WATER_ID = 15;
    public static final byte TARGET_ID = 16;


    public static final byte COAL_MAT_ID = 17;
    public static final byte IRON_MAT_ID = 18;
    public static final byte LANTERN_OFF_ID = 19;
    public static final byte LANTERN_ON_ID = 20;
    public static final byte WOOD_PICK_ID = 21;
    public static final byte STONE_PICK_ID = 22;
    public static final byte IRON_PICK_ID = 23;
}