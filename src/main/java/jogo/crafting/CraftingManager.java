package jogo.crafting;

import jogo.gameobject.character.Player;
import jogo.gameobject.item.ItemStack;
import jogo.voxel.VoxelPalette;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


//Gere as receitas e a lógica de criar itens.
public class CraftingManager {

    // Lista de todas as receitas disponíveis
    private final List<Recipe> recipes = new ArrayList<>();
    // Construtor que inicializa as receitas
    public CraftingManager() {
        initRecipes();
    }
    // Inicializa as receitas básicas
    private void initRecipes() {
        // --- RECEITAS BÁSICAS ---

        // 1 Madeira -> 4 Tábuas
        recipes.add(new Recipe("Planks", VoxelPalette.Wood_ID, 1, VoxelPalette.PLANKS_ID, 4));

        // 2 Tábuas -> 4 Paus
        recipes.add(new Recipe("Sticks", VoxelPalette.PLANKS_ID, 2, VoxelPalette.STICK_ID, 4));

        // 4 Tábuas -> 1 Mesa de Trabalho
        recipes.add(new Recipe("Table", VoxelPalette.PLANKS_ID, 4, VoxelPalette.CRAFTING_TABLE_ID, 1));

    }

    // Retorna a lista de receitas disponíveis
    public List<Recipe> getRecipes() {
        return Collections.unmodifiableList(recipes);
    }

    //Tenta criar o item da receita para o jogador dado.
    public boolean craft(Recipe recipe, Player player) {
        // 1. verifica se o jogador tem todos os materiais necessários
        for (ItemStack requiredItem : recipe.inputs) {
            if (!player.hasItem(requiredItem.getId(), requiredItem.getAmount())) {
                // Se faltar algum item da lista, cancela
                System.out.println("Faltam materiais: " + requiredItem.getId());
                return false;
            }
        }

        // 2. ADICIONAR: Tentar adicionar o produto final (verifica espaço)
        if (player.addItem(recipe.outputId, recipe.outputCount)) {

            // 3. REMOVER: Consumir todos os materiais da lista
            for (ItemStack requiredItem : recipe.inputs) {
                player.removeItem(requiredItem.getId(), requiredItem.getAmount());
            }

            System.out.println("Sucesso! Criado: " + recipe.name);
            return true;
        } else {
            System.out.println("Sem espaço no inventário para: " + recipe.name);
            return false;
        }
    }
}