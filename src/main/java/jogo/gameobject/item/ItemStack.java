package jogo.gameobject.item;

import java.io.Serializable; // <--- ADICIONAR ISTO

public class ItemStack implements Serializable { // <--- ADICIONAR "implements Serializable"
    private static final long serialVersionUID = 1L; // Boa prática

    private byte id;
    // ... resto do código igual ...

    private int amount;
    public static final int MAX_STACK = 64;

    public ItemStack(byte id, int amount) {
        this.id = id;
        this.amount = amount;
    }

    public byte getId() { return id; }
    public int getAmount() { return amount; }

    public void add(int quantity) {
        this.amount += quantity;
    }

    public void setAmount(int amount) {
        this.amount = amount;
    }

    public boolean isFull() {
        return amount >= MAX_STACK;
    }
}