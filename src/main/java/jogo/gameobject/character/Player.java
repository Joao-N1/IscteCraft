package jogo.gameobject.character;

import jogo.voxel.VoxelPalette; // Importante para IDs

public class Player extends Character {

    // Hotbar: 9 slots (IDs dos blocos)
    private byte[] hotbar = new byte[9];

    // Inventário Principal: 27 slots (3 linhas de 9)
    private byte[] mainInventory = new byte[27];

    private int selectedSlot = 0; // 0 a 8

    public Player() {
        super("Player");
        // Exemplo: encher a hotbar para testar
        hotbar[0] = VoxelPalette.STONE_ID;
        hotbar[1] = VoxelPalette.DIRT_ID;
        hotbar[2] = VoxelPalette.GRASS_ID;
        hotbar[3] = VoxelPalette.Wood_ID;
        hotbar[4] = VoxelPalette.Leaf_ID;
    }

    public int getSelectedSlot() {
        return selectedSlot;
    }

    public void setSelectedSlot(int slot) {
        // Garante que o slot está entre 0 e 8
        if (slot < 0) slot = 8;
        if (slot > 8) slot = 0;
        this.selectedSlot = slot;
    }

    public byte getHeldItem() {
        return hotbar[selectedSlot];
    }

    // Getters para o inventário se necessário no futuro
    public byte[] getHotbar() { return hotbar; }
    public byte[] getMainInventory() { return mainInventory; }
}