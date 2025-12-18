package jogo.voxel;


import jogo.voxel.blocks.*;
import jogo.voxel.items.*;
import java.util.ArrayList;
import java.util.List;

// Gerencia o registro e mapeamento de tipos de blocos voxel
public class VoxelPalette {
    private final List<VoxelBlockType> types = new ArrayList<>();

    // Registra um novo tipo de bloco voxel e retorna seu ID
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

    // Cria uma paleta padrão com tipos de blocos instanciados
    public static VoxelPalette defaultPalette() {
        VoxelPalette p = new VoxelPalette();

        // 0. AR
        p.register(new AirBlockType());

        // --- BLOCOS NORMAIS ---
        p.register(new StoneBlockType());      // ID 1
        p.register(new TheRockBlockType());    // ID 2
        p.register(new DirtBlockType());       // ID 3
        p.register(new GrassBlockType());      // ID 4
        p.register(new WoodBlockType());       // ID 5

        // ID 6: Spiky Wood
        p.register(new SpikyWoodBlockType());

        // ID 7: Leaf
        p.register(new LeafBlockType());

        // --- MINÉRIOS ---
        p.register(new CoalOreBlockType());    // ID 8
        p.register(new IronOreBlockType());    // ID 9
        p.register(new DiamondBlockType());    // ID 10

        // ID 11: Tábuas
        p.register(new PlanksBlockType());

        // --- ITENS ---
        p.register(new StickItem());       // ID 12

        // ID 13: Crafting Table
        p.register(new CraftingTableBlockType());

        // --- TERRENO EXTRA ---
        p.register(new SandBlockType());       // ID 14

        // ID 15: ÁGUA
        p.register(new WaterBlockType());

        // ID 16: Target Block
        p.register(new TargetBlockType());

        // --- ITENS MATERIAIS ---
        p.register(new CoalItem());        // ID 17
        p.register(new IronItem());        // ID 18

        // --- LANTERNAS ---
        p.register(new LanternOffBlockType()); // ID 19
        p.register(new LanternOnBlockType()); // ID 20

        // --- FERRAMENTAS ---
        p.register(new WoodPickItem());    // ID 21
        p.register(new StonePickItem());   // ID 22
        p.register(new IronPickItem());    // ID 23

        // ID 24: Spiky Planks (Dano 10)
        p.register(new SpikyPlanksBlockType());

        // ID 25: Espada (Item)
        p.register(new SwordItem());

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
    public static final byte SPIKY_PLANKS_ID = 24;
    public static final byte SWORD_ID = 25;
}