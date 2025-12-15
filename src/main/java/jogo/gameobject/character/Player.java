package jogo.gameobject.character;

import jogo.gameobject.item.ItemStack;

// Classe que representa o jogador, com inventário e hotbar
public class Player extends Character {

    private ItemStack[] hotbar = new ItemStack[9];
    private ItemStack[] mainInventory = new ItemStack[27];
    private ItemStack cursorItem = null; // Item na mão do cursor (drag and drop)

    private int selectedSlot = 0; // 0 a 8

    public Player() {
        super("Player");
        setMaxHealth(100);
    }

    // Adiciona um item ao inventário (tenta empilhar ou colocar em novo slot)
    public boolean addItem(byte id, int amount) {
        if (amount <= 0) return false;

        // Tentar empilhar em slots existentes (Hotbar depois Main)
        if (tryStackItem(hotbar, id, amount)) return true;
        if (tryStackItem(mainInventory, id, amount)) return true;

        // Tentar colocar em slots vazios (Hotbar depois Main)
        if (trynNewSlot(hotbar, id, amount)) return true;
        if (trynNewSlot(mainInventory, id, amount)) return true;

        System.out.println("Inventário cheio!");
        return false;
    }

    // Tenta empilhar num array específico
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

    // Tenta criar novo slot num array específico
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

    // Retorna o ID do bloco na mão para saber o que colocar no mundo
    public byte getHeldItem() {
        if (hotbar[selectedSlot] != null && hotbar[selectedSlot].getAmount() > 0) {
            return hotbar[selectedSlot].getId();
        }
        return 0;
    }

    // Remove 1 item da mão do jogador
    public void consumeHeldItem() {
        if (hotbar[selectedSlot] != null) {
            hotbar[selectedSlot].add(-1); // Retira 1
            if (hotbar[selectedSlot].getAmount() <= 0) {
                hotbar[selectedSlot] = null; // Se chegar a 0, limpa o slot
            }
        }
    }

    public ItemStack[] getHotbar() { return hotbar; }
    public ItemStack[] getMainInventory() { return mainInventory; }

    public ItemStack getCursorItem() {
        return cursorItem;
    }

    public void setCursorItem(ItemStack cursorItem) {
        this.cursorItem = cursorItem;
    }


    // Verifica se tem quantidade suficiente de um item (procura no inventario todo)
    public boolean hasItem(byte id, int amount) {
        int count = 0;
        // Verificar Hotbar
        for (ItemStack stack : hotbar) {
            if (stack != null && stack.getId() == id) {
                count += stack.getAmount();
            }
        }
        // Verificar Inventário Principal
        for (ItemStack stack : mainInventory) {
            if (stack != null && stack.getId() == id) {
                count += stack.getAmount();
            }
        }
        return count >= amount;
    }

    // Remove uma quantidade de itens (começa pelos slots mais cheios ou primeiros encontrados)
    public void removeItem(byte id, int amount) {
        int remaining = amount;

        // Remover da Hotbar
        remaining = removeFromInventory(hotbar, id, remaining);
        if (remaining > 0) {
            // Remover do Inventário Principal
            removeFromInventory(mainInventory, id, remaining);
        }
    }

    // Remove itens de um array específico, retorna o que sobrou para remover
    private int removeFromInventory(ItemStack[] inventory, byte id, int amountToRemove) {
        for (int i = 0; i < inventory.length; i++) {
            if (amountToRemove <= 0) break;

            ItemStack stack = inventory[i];
            if (stack != null && stack.getId() == id) {
                if (stack.getAmount() > amountToRemove) {
                    stack.add(-amountToRemove);
                    amountToRemove = 0;
                } else {
                    amountToRemove -= stack.getAmount();
                    inventory[i] = null; // Remove o stack inteiro
                }
            }
        }
        return amountToRemove;
    }

}