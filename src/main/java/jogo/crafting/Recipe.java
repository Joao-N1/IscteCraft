package jogo.crafting;

import jogo.gameobject.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class Recipe {
    public String name;
    // Lista de ingredientes necessários
    public List<ItemStack> inputs = new ArrayList<>();
    public byte outputId;
    public int outputCount;

    // Construtor Simples (1 ingrediente)
    public Recipe(String name, byte inputId, int inputCount, byte outputId, int outputCount) {
        this.name = name;
        this.inputs.add(new ItemStack(inputId, inputCount));
        this.outputId = outputId;
        this.outputCount = outputCount;
    }

    // Construtor Avançado (Múltiplos ingredientes)
    public Recipe(String name, byte outputId, int outputCount, ItemStack... requiredInputs) {
        this.name = name;
        this.outputId = outputId;
        this.outputCount = outputCount;
        for (ItemStack is : requiredInputs) {
            this.inputs.add(is);
        }
    }
}