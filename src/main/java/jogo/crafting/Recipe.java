package jogo.crafting;

import jogo.gameobject.item.ItemStack;
import java.util.ArrayList;
import java.util.List;

public class Recipe {
    public String name;
    // Agora aceitamos uma lista de itens em vez de um só ID
    public List<ItemStack> inputs = new ArrayList<>();
    public byte outputId;
    public int outputCount;

    // Construtor Simples (1 ingrediente) - Para manter compatibilidade
    public Recipe(String name, byte inputId, int inputCount, byte outputId, int outputCount) {
        this.name = name;
        this.inputs.add(new ItemStack(inputId, inputCount));
        this.outputId = outputId;
        this.outputCount = outputCount;
    }

    // Construtor Avançado (Múltiplos ingredientes)
    // Exemplo de uso: new Recipe("Pickaxe", outputId, count, new ItemStack(plank, 3), new ItemStack(stick, 2))
    public Recipe(String name, byte outputId, int outputCount, ItemStack... requiredInputs) {
        this.name = name;
        this.outputId = outputId;
        this.outputCount = outputCount;
        for (ItemStack is : requiredInputs) {
            this.inputs.add(is);
        }
    }
}