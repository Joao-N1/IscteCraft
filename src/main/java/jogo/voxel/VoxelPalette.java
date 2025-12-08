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

        // 0. AR (Especial)
        p.register(new jogo.voxel.blocks.AirBlockType());

        // --- BLOCOS NORMAIS (Substituídos por SimpleBlockType) ---
        p.register(new SimpleBlockType("stone",    "StoneBlock.png", 6.0f)); // ID 1
        p.register(new SimpleBlockType("therock",  "TheRock.png",    999f)); // ID 2 (Indestrutível)
        p.register(new SimpleBlockType("dirt",     "DirtBlock.png",  1.0f)); // ID 3
        p.register(new SimpleBlockType("grass",    "GrassBlock.png", 1.0f)); // ID 4
        p.register(new SimpleBlockType("wood",     "WoodBlock.png",  3.0f)); // ID 5

        // ID 6: Spiky Wood (Tem dano 10)
        p.register(new SimpleBlockType("spikywood","SpikyWoodBlock.png", 3.0f, (byte)0, 10));

        p.register(new SimpleBlockType("leaf",     "LeafBlock.png",  0.2f)); // ID 7

        // --- MINÉRIOS (Hardness + DropID) ---
        // ID 8: Carvão (Dropa o item ID 17)
        p.register(new SimpleBlockType("Coal Block", "CoalBlock.png", 7.0f, COAL_MAT_ID));

        // ID 9: Ferro (Dropa o item ID 18)
        p.register(new SimpleBlockType("Iron Block", "IronBlock.png", 8.0f, IRON_MAT_ID));

        // ID 10: Diamante (Podes adicionar drop se quiseres item diamante depois)
        p.register(new SimpleBlockType("Diamond",    "DiamondBlock.png", 10.0f));

        p.register(new SimpleBlockType("planks",   "PlanksBlock.png", 4.0f)); // ID 11

        // --- ITENS (SimpleItemType - não placeable) ---
        p.register(new SimpleItemType("stick", "Stick.png")); // ID 12

        // ID 13: Crafting Table
        p.register(new SimpleBlockType("crafting_table", "CraftingTableBlock.png", 5.0f));

        // --- TERRENO EXTRA ---
        p.register(new SimpleBlockType("sand", "SandBlock.png", 1.0f)); // ID 14

        // ID 15: ÁGUA (Mantém a classe original para transparência/física especial)
        p.register(new jogo.voxel.blocks.WaterBlockType());

        p.register(new SimpleBlockType("Target", "TargetBlock.png", 0.5f)); // ID 16

        // --- ITENS MATERIAIS (SimpleItemType) ---
        p.register(new SimpleItemType("coal_item", "CoalOre.png")); // ID 17
        p.register(new SimpleItemType("iron_item", "IronOre.png")); // ID 18

        // --- LANTERNAS ---
        p.register(new SimpleBlockType("lantern_off", "LanternOff.png", 4.0f)); // ID 19
        // ID 20: Lanterna Acesa (Mantém a original para o GLOW)
        p.register(new jogo.voxel.blocks.LanternOnBlockType());

        // --- FERRAMENTAS (SimpleItemType) ---
        p.register(new SimpleItemType("wood_pick",  "WoodPick.png"));  // ID 21
        p.register(new SimpleItemType("stone_pick", "StonePick.png")); // ID 22
        p.register(new SimpleItemType("iron_pick",  "IronPick.png"));  // ID 23

        // --- NOVOS ---
        // ID 24: Spiky Planks (Dano 10)
        p.register(new SimpleBlockType("Spiky Planks", "SpikyPlankBlock.png", 4.0f, (byte)0, 10));

        // ID 25: Espada (Item)
        p.register(new SimpleItemType("Sword", "Sword.png"));

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

    // NOVOS IDs
    public static final byte SPIKY_PLANKS_ID = 24;
    public static final byte SWORD_ID = 25;
}