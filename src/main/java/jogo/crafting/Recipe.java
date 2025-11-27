package jogo.crafting;

public class Recipe {
    public final String name;
    public final byte inputId;
    public final int inputCount;
    public final byte outputId;
    public final int outputCount;

    public Recipe(String name, byte inputId, int inputCount, byte outputId, int outputCount) {
        this.name = name;
        this.inputId = inputId;
        this.inputCount = inputCount;
        this.outputId = outputId;
        this.outputCount = outputCount;
    }
}