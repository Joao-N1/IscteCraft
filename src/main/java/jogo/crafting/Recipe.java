package jogo.crafting;

public class Recipe {
    public final String name;
    public final byte inputId;
    public final int inputCount;
    public final byte outputId;
    public final int outputCount;
    public final int[] visualSlots;

    public Recipe(String name, byte inputId, int inputCount, byte outputId, int outputCount, int ... visualSlots) {
        this.name = name;
        this.inputId = inputId;
        this.inputCount = inputCount;
        this.outputId = outputId;
        this.outputCount = outputCount;
        // Se não passarmos posições (erro), preenche sequencialmente para não crashar
        if (visualSlots == null || visualSlots.length == 0) {
            this.visualSlots = new int[inputCount];
            for(int i=0; i<inputCount; i++) this.visualSlots[i] = i;
        } else {
            this.visualSlots = visualSlots;
        }
    }
}