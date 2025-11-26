package jogo.gameobject.character;

import jogo.gameobject.item.ItemStack;
import jogo.voxel.VoxelPalette; // Importante para IDs

public class Player extends Character {

    private ItemStack[] hotbar = new ItemStack[9];
    private ItemStack[] mainInventory = new ItemStack[27];

    private int selectedSlot = 0; // 0 a 8

    public Player() {
        super("Player");

    }

    public boolean addItem(byte id, int amount) {
        if (amount <= 0) return false;

        // 1. & 2. Tentar empilhar em slots existentes (Hotbar depois Main)
        if (tryStackItem(hotbar, id, amount)) return true;
        if (tryStackItem(mainInventory, id, amount)) return true;

        // 3. & 4. Tentar colocar em slots vazios (Hotbar depois Main)
        if (trynNewSlot(hotbar, id, amount)) return true;
        if (trynNewSlot(mainInventory, id, amount)) return true;

        System.out.println("Inventário cheio!");
        return false;
    }

    // Auxiliar: Tenta empilhar num array específico
    private boolean tryStackItem(ItemStack[] inv, byte id, int amount) {
        for (ItemStack stack : inv) {
            if (stack != null && stack.getId() == id && !stack.isFull()) {
                int space = ItemStack.MAX_STACK - stack.getAmount();
                if (space >= amount) {
                    stack.add(amount);
                    return true;
                } else {
                    // Enche este e continua a tentar adicionar o resto noutro loop (simplificado aqui)
                    stack.setAmount(ItemStack.MAX_STACK);
                    amount -= space;
                }
            }
        }
        return false;
    }

    // Auxiliar: Tenta criar novo slot num array específico
    private boolean trynNewSlot(ItemStack[] inv, byte id, int amount) {
        for (int i = 0; i < inv.length; i++) {
            if (inv[i] == null) {
                inv[i] = new ItemStack(id, amount);
                return true;
            }
        }
        return false;
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

    // Retorna o ID do bloco na mão para sabermos o que colocar no mundo
    public byte getHeldItem() {
        ItemStack stack = hotbar[selectedSlot];
        if (stack != null && stack.getAmount() > 0) {
            return stack.getId();
        }
        return 0; // 0 = Nada/Ar
    }

    // Remove 1 item da mão (usado quando colocas um bloco)
    public void consumeHeldItem() {
        ItemStack stack = hotbar[selectedSlot];
        if (stack != null) {
            stack.add(-1);
            if (stack.getAmount() <= 0) {
                hotbar[selectedSlot] = null; // Destrói o stack se chegar a 0
            }
        }
    }

    public ItemStack[] getHotbar() { return hotbar; }
    public ItemStack[] getMainInventory() { return mainInventory; }
}