package jogo.crafting;

import jogo.gameobject.character.Player;
import jogo.voxel.VoxelPalette;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Gere as receitas e a lógica de criar itens.
 * Desacoplado da interface gráfica.
 */
public class CraftingManager {

    private final List<Recipe> recipes = new ArrayList<>();

    public CraftingManager() {
        initRecipes();
    }

    private void initRecipes() {
        // Aqui defines todas as receitas do jogo num só lugar
        // 1 Madeira -> 4 Tábuas
        recipes.add(new Recipe("Planks", VoxelPalette.Wood_ID, 1, VoxelPalette.PLANKS_ID, 4));

        // 2 Tábuas -> 4 Paus
        recipes.add(new Recipe("Sticks", VoxelPalette.PLANKS_ID, 2, VoxelPalette.STICK_ID, 4));

    }

    public List<Recipe> getRecipes() {
        return Collections.unmodifiableList(recipes);
    }

    /**
     * Tenta criar o item da receita para o jogador dado.
     * @param recipe A receita a executar
     * @param player O jogador que está a fazer crafting
     * @return true se teve sucesso, false caso contrário
     */
    public boolean craft(Recipe recipe, Player player) {
        // 1. Verificar se tem materiais
        if (!player.hasItem(recipe.inputId, recipe.inputCount)) {
            System.out.println("Faltam materiais para: " + recipe.name);
            return false;
        }

        // 2. Tentar adicionar o produto final (verifica espaço)
        // Nota: A tua lógica atual tenta adicionar primeiro. Se der, remove os materiais.
        // Isto previne perder materiais se o inventário estiver cheio.
        if (player.addItem(recipe.outputId, recipe.outputCount)) {
            // 3. Remover os materiais consumidos
            player.removeItem(recipe.inputId, recipe.inputCount);
            System.out.println("Sucesso! Criado: " + recipe.name);
            return true;
        } else {
            System.out.println("Sem espaço no inventário para: " + recipe.name);
            return false;
        }
    }
}