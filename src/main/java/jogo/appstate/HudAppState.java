package jogo.appstate;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.BaseAppState;
import com.jme3.asset.AssetManager;
import com.jme3.font.BitmapFont;
import com.jme3.font.BitmapText;
import com.jme3.scene.Node;
import com.jme3.ui.Picture;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector2f;
import com.jme3.material.Material;
import jogo.gameobject.character.Player;
import jogo.gameobject.item.ItemStack;
import jogo.voxel.VoxelPalette;
import jogo.crafting.Recipe;

import java.util.ArrayList;
import java.util.List;

public class HudAppState extends BaseAppState {

    private final Node guiNode;
    private final AssetManager assetManager;
    private BitmapText crosshair;
    private BitmapFont guiFont;

    // --- UI INVENTÁRIO (Hotbar e Principal) ---
    private Node inventoryNode = new Node("InventoryNode");
    private List<Picture> hotbarSlots = new ArrayList<>();
    private List<BitmapText> hotbarTexts = new ArrayList<>();
    private List<Picture> hotbarIcons = new ArrayList<>();

    private List<Picture> mainInvSlots = new ArrayList<>();
    private List<BitmapText> mainInvTexts = new ArrayList<>();
    private List<Picture> mainInvIcons = new ArrayList<>();

    private Picture selector;
    private Picture cursorItemIcon; // Item "preso" ao rato
    private List<Picture> hearts = new ArrayList<>();

    // ======================= UI CRAFTING (VISUAL NOVO) ========================

    // Imagens de Fundo
    private Picture recipeBookBg;

    // Configurações de Tamanho e Posição
    private final float PANEL_HEIGHT = 300f;
    private final float TABLE_WIDTH = 270f;
    private final float BOOK_WIDTH = 270f;
    private final float PANEL_OVERLAP = 0f; // Ajuste de conexão

    // Grelha do Livro
    private final int GRID_COLS = 4;
    private final float GRID_START_X = 21f; // Margem esquerda dentro do livro
    private final float GRID_START_Y = 89f; // Margem topo dentro do livro
    private final float SLOT_SIZE = 44f;    // Tamanho do slot
    private final float SLOT_GAP = 3f;      // Espaço entre slots

    // ==========================================================================
    // Receitas do Jogador (Básico - sempre visível no inventário)
    private List<Recipe> playerRecipes = new ArrayList<>();
    private List<Picture> playerRecipeIcons = new ArrayList<>();

    // Receitas da Mesa (Avançado - só na mesa)
    private Node craftingTableNode = new Node("CraftingTableUI");
    private Picture craftingBg;
    private List<Recipe> tableRecipes = new ArrayList<>();
    private List<Picture> tableRecipeIcons = new ArrayList<>();

    // Visualização da Receita Selecionada na Mesa (Grelha 3x3 + Resultado)
    private List<Picture> gridInputIcons = new ArrayList<>(); // 9 slots visuais para mostrar materiais
    private Picture resultIcon; // O botão final onde clicas para craftar
    private Recipe selectedRecipe = null;

    // Estados
    private boolean isInventoryVisible = false;
    private boolean isCraftingOpen = false;
    private int currentSlotIndex = 0;

    // Configurações Visuais
    private final float HOTBAR_WIDTH = 400f;
    private final float HOTBAR_HEIGHT = 44f;
    private final float HEART_SIZE = 20f;

    // --- NOVO: LEGENDAS (SUBTITLES) ---
    private BitmapText subtitleText;
    private float subtitleTimer = 0f;
    // ----------------------------------

    private Node loadMenuNode = new Node("LoadMenu");
    private BitmapText loadMenuText;

    private BitmapText miniGameText;
    private BitmapText highScoreText;

    // --- LEADERBOARD / SUCCESS MSG ---
    private Node leaderboardNode = new Node("LeaderboardNode");
    private float leaderboardTimer = 0f; // Temporizador para auto-hide
    private boolean isLeaderboardVisible = false; // Estado atual

    public HudAppState(Node guiNode, AssetManager assetManager) {
        this.guiNode = guiNode;
        this.assetManager = assetManager;
    }

    /**
     * Mostra um texto no ecrã por X segundos.
     */
    public void showSubtitle(String text, float duration) {
        if (subtitleText != null) {
            subtitleText.setText(text);
            subtitleTimer = duration;
            centerSubtitle(); // Recalcular posição para ficar centrado
        }
    }

    private void centerSubtitle() {
        SimpleApplication sapp = (SimpleApplication) getApplication();
        float w = sapp.getCamera().getWidth();
        float h = sapp.getCamera().getHeight();

        float textWidth = subtitleText.getLineWidth();
        float x = (w - textWidth) / 2f;
        float y = h * 0.75f; // Posição: 75% da altura do ecrã

        subtitleText.setLocalTranslation(x, y, 0);
    }

    @Override
    protected void initialize(Application app) {
        guiFont = assetManager.loadFont("Interface/Fonts/Default.fnt");

        // 1. Mira
        crosshair = new BitmapText(guiFont, false);
        crosshair.setText("+");
        crosshair.setSize(guiFont.getCharSet().getRenderedSize() * 2f);
        guiNode.attachChild(crosshair);

        // 2. Inicializar Elementos UI
        initHotbar(app);
        initHearts(app);
        initInventory(app);
        initCursorItem();

        // 3. Inicializar Crafting (Jogador e Mesa)
        initCraftingSystems();

        // --- 4. INICIALIZAR LEGENDAS ---
        subtitleText = new BitmapText(guiFont, false);
        subtitleText.setSize(guiFont.getCharSet().getRenderedSize() * 1.2f);
        subtitleText.setColor(ColorRGBA.Yellow);
        subtitleText.setText("");
        guiNode.attachChild(subtitleText);
        // -------------------------------

        loadMenuText = new BitmapText(guiFont, false);
        loadMenuText.setSize(guiFont.getCharSet().getRenderedSize() * 1.5f);
        loadMenuText.setColor(ColorRGBA.Green);
        loadMenuText.setText("SAVES DISPONIVEIS:\n(F1) save1\n(F2) save2");
        loadMenuText.setLocalTranslation(50, 600, 0); // Posição no ecrã
        loadMenuNode.attachChild(loadMenuText);

        miniGameText = new BitmapText(guiFont, false);
        miniGameText.setSize(guiFont.getCharSet().getRenderedSize());
        miniGameText.setColor(ColorRGBA.Cyan);
        miniGameText.setLocalTranslation(10, 700, 0); // Topo esquerdo
        guiNode.attachChild(miniGameText);


        // Inicializar Texto do Leaderboard (mas não anexar ainda)
        highScoreText = new BitmapText(guiFont, false);
        highScoreText.setSize(guiFont.getCharSet().getRenderedSize() * 1.2f);
        highScoreText.setColor(ColorRGBA.Yellow);
        highScoreText.setLocalTranslation(400, 500, 0); // Centro (aprox)
        leaderboardNode.attachChild(highScoreText);

        refreshLayout();
        System.out.println("HudAppState initialized.");
    }

    // Novos Métodos
    public void updateMiniGameInfo(float time, int hits, int total) {
        miniGameText.setText(String.format("Tempo: %.1fs | Alvos: %d/%d", time, hits, total));
    }

    public void showHighScores(List<jogo.system.HighScoreManager.ScoreEntry> scores, float currentTime) {
        StringBuilder sb = new StringBuilder("PARABÉNS! TERMINASTE EM " + String.format("%.2f", currentTime) + "s\n\n");
        sb.append("=== TOP SCORES ===\n");

        int pos = 1;
        for (jogo.system.HighScoreManager.ScoreEntry s : scores) {
            sb.append(pos++).append(". ").append(s.toString()).append("\n");
        }

        highScoreText.setText(sb.toString());
        guiNode.attachChild(highScoreText);
    }

    // --- INICIALIZAÇÃO UI BÁSICA ---

    // Substitui o método showLoadMenu por este melhorado:

    public void showLoadMenu(List<String> saves) {
        StringBuilder sb = new StringBuilder("=== MENU DE LOAD ===\n\n");

        if (saves.isEmpty()) {
            sb.append("Nenhum save encontrado.\nJoga e carrega em 'M' para salvar.");
        } else {
            sb.append("Seleciona um mundo com F1, F2, F3...\n\n");
            for (int i = 0; i < saves.size(); i++) {
                // Limitar a visualização a 9 saves para não sair do ecrã
                if (i >= 9) {
                    sb.append("... (mais saves ocultos)");
                    break;
                }
                sb.append("[F").append(i + 1).append("]  ").append(saves.get(i)).append("\n");
            }
        }

        // Adicionar fundo escuro ao texto para melhor leitura (opcional, se quiseres usar o recipeBookBg)
        loadMenuText.setText(sb.toString());

        // Centrar no ecrã
        SimpleApplication sapp = (SimpleApplication) getApplication();
        float x = 50;
        float y = sapp.getCamera().getHeight() - 100;
        loadMenuText.setLocalTranslation(x, y, 0);

        guiNode.attachChild(loadMenuNode);
    }

    public boolean isLoadMenuVisible() {
        return loadMenuNode.getParent() != null;
    }

    public void hideLoadMenu() {
        loadMenuNode.removeFromParent();
    }

    private void initHotbar(Application app) {
        float slotWidth = HOTBAR_WIDTH / 9f;
        for (int i = 0; i < 9; i++) {
            // Fundo
            Picture slot = new Picture("HotbarSlot_" + i);
            slot.setImage(assetManager, "Interface/hotbarsquare.png", true);
            slot.setWidth(slotWidth);
            slot.setHeight(HOTBAR_HEIGHT);
            guiNode.attachChild(slot);
            hotbarSlots.add(slot);

            // Ícone
            Picture icon = new Picture("HotbarIcon_" + i);
            icon.setWidth(slotWidth * 0.6f);
            icon.setHeight(HOTBAR_HEIGHT * 0.6f);
            guiNode.attachChild(icon);
            hotbarIcons.add(icon);

            // Texto
            BitmapText count = new BitmapText(guiFont, false);
            count.setSize(guiFont.getCharSet().getRenderedSize() * 0.8f);
            count.setColor(ColorRGBA.White);
            count.setText("");
            guiNode.attachChild(count);
            hotbarTexts.add(count);
        }
        // Selector
        selector = new Picture("Selector");
        selector.setImage(assetManager, "Interface/selector.png", true);
        selector.setWidth(slotWidth);
        selector.setHeight(HOTBAR_HEIGHT);
        guiNode.attachChild(selector);
    }

    private void initHearts(Application app) {
        for (int i = 0; i < 10; i++) {
            Picture heart = new Picture("Heart_" + i);
            heart.setImage(assetManager, "Interface/heart_full.png", true);
            heart.setWidth(HEART_SIZE);
            heart.setHeight(HEART_SIZE);
            guiNode.attachChild(heart);
            hearts.add(heart);
        }
    }

    private void initInventory(Application app) {
        float slotSize = HOTBAR_WIDTH / 9f;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                // Fundo
                Picture slot = new Picture("InvSlot_" + row + "_" + col);
                slot.setImage(assetManager, "Interface/hotbarsquare.png", true);
                slot.setWidth(slotSize);
                slot.setHeight(slotSize);
                inventoryNode.attachChild(slot);
                mainInvSlots.add(slot);

                // Ícone
                Picture icon = new Picture("InvIcon_" + row + "_" + col);
                icon.setWidth(slotSize * 0.6f);
                icon.setHeight(slotSize * 0.6f);
                inventoryNode.attachChild(icon);
                mainInvIcons.add(icon);

                // Texto
                BitmapText count = new BitmapText(guiFont, false);
                count.setSize(guiFont.getCharSet().getRenderedSize() * 0.8f);
                count.setColor(ColorRGBA.White);
                count.setText("");
                inventoryNode.attachChild(count);
                mainInvTexts.add(count);
            }
        }
    }

    private void initCursorItem() {
        cursorItemIcon = new Picture("CursorItem");
        try { cursorItemIcon.setImage(assetManager, "Interface/CursorItem.png", true); } catch(Exception e){}
        cursorItemIcon.setWidth(HOTBAR_WIDTH / 9f * 0.6f);
        cursorItemIcon.setHeight(HOTBAR_WIDTH / 9f * 0.6f);
        cursorItemIcon.setCullHint(Node.CullHint.Always);
        guiNode.attachChild(cursorItemIcon);
    }

    // --- INICIALIZAÇÃO CRAFTING ---

    private void initCraftingSystems() {
        playerRecipes.clear();
        tableRecipes.clear();

        // Receitas
        Recipe rWoodPick = new Recipe("Wood Pick", VoxelPalette.PLANKS_ID, 3, VoxelPalette.WOOD_PICK_ID, 1);
        Recipe rStonePick = new Recipe("Stone Pick", VoxelPalette.STONE_ID, 3, VoxelPalette.STONE_PICK_ID, 1);
        Recipe rIronPick = new Recipe("Iron Pick", VoxelPalette.IRON_MAT_ID, 3, VoxelPalette.IRON_PICK_ID, 1);
        Recipe rPlanks = new Recipe("Planks", VoxelPalette.Wood_ID, 1, VoxelPalette.PLANKS_ID, 4);
        Recipe rSticks = new Recipe("Sticks", VoxelPalette.PLANKS_ID, 2, VoxelPalette.STICK_ID, 4);
        Recipe rTable = new Recipe("Table", VoxelPalette.PLANKS_ID, 4, VoxelPalette.CRAFTING_TABLE_ID, 1);

        Recipe rSpikyPlanks = new Recipe("Spiky Planks", VoxelPalette.SpikyWood_ID, 1, VoxelPalette.SPIKY_PLANKS_ID, 4);
        Recipe rSpikySword = new Recipe("Spiky Sword", VoxelPalette.SPIKY_PLANKS_ID, 2, VoxelPalette.SWORD_ID, 1);

        // Jogador (Inventário normal)
        playerRecipes.add(rPlanks);
        playerRecipes.add(rTable);
        playerRecipes.add(rSticks);
        createRecipeIcons(playerRecipes, playerRecipeIcons, inventoryNode);

        // Mesa (Interface dedicada)
        initTableUI();

        tableRecipes.add(rWoodPick);
        tableRecipes.add(rStonePick);
        tableRecipes.add(rIronPick);
        tableRecipes.add(rPlanks);
        tableRecipes.add(rSticks);
        tableRecipes.add(rSpikyPlanks);
        tableRecipes.add(rSpikySword);

        // tableRecipes.add(rTable);
        createRecipeIcons(tableRecipes, tableRecipeIcons, craftingTableNode);
    }

    private void initTableUI() {
        // 1.Mesa
        craftingBg = new Picture("CraftingBg");
        try { craftingBg.setImage(assetManager, "Interface/CraftingUI.png", true); }
        catch (Exception e) { craftingBg.setImage(assetManager, "Interface/hotbarsquare.png", true); }

        craftingBg.setWidth(TABLE_WIDTH);
        craftingBg.setHeight(PANEL_HEIGHT);
        craftingTableNode.attachChild(craftingBg);

        // 2. LIVRO (Agora adicionado corretamente!)
        recipeBookBg = new Picture("RecipeBookBg");
        try {
            recipeBookBg.setImage(assetManager, "Interface/RecipeBook.png", true);
        } catch (Exception e) {
            System.out.println("Erro ao carregar RecipeBook.png, fallback cor castanha.");
            recipeBookBg.setImage(assetManager, "Interface/hotbarsquare.png", true);
            recipeBookBg.getMaterial().setColor("Color", ColorRGBA.Brown);
        }
        recipeBookBg.setWidth(BOOK_WIDTH);
        recipeBookBg.setHeight(PANEL_HEIGHT);
        craftingTableNode.attachChild(recipeBookBg); // <--- IMPORTANTE

        // 3. Criar a Grelha Visual (3x3) para mostrar onde os materiais ficam
        float slotSize = 30f;
        for (int i = 0; i < 9; i++) {
            Picture gridIcon = new Picture("GridIcon_" + i);
            gridIcon.setWidth(slotSize);
            gridIcon.setHeight(slotSize);
            gridIcon.setCullHint(Node.CullHint.Always); // Invisível por defeito
            craftingTableNode.attachChild(gridIcon);
            gridInputIcons.add(gridIcon);
        }

        // 4. Criar o Ícone de Resultado (Botão de Crafting)
        resultIcon = new Picture("ResultIcon");
        resultIcon.setWidth(slotSize * 1.5f);
        resultIcon.setHeight(slotSize * 1.5f);
        resultIcon.setCullHint(Node.CullHint.Always);
        craftingTableNode.attachChild(resultIcon);
    }

    private void createRecipeIcons(List<Recipe> rList, List<Picture> iList, Node parent) {
        float iconSize = 32f;
        for (int i = 0; i < rList.size(); i++) {
            Recipe r = rList.get(i);
            Picture icon = new Picture("Rec_" + r.name);
            try {
                icon.setImage(assetManager, "Textures/" + getTextureNameById(r.outputId), true);
            } catch(Exception e){}
            icon.setWidth(iconSize);
            icon.setHeight(iconSize);
            parent.attachChild(icon);
            iList.add(icon);

            // Label de custo
            BitmapText lbl = new BitmapText(guiFont, false);
            lbl.setSize(guiFont.getCharSet().getRenderedSize() * 0.7f);
            lbl.setText(r.inputCount + "x");
            lbl.setColor(ColorRGBA.Yellow);
            parent.attachChild(lbl);
            icon.setUserData("label", lbl);
        }
    }

    // --- UPDATE LOOP ---

    @Override
    public void update(float tpf) {
        centerCrosshair();
        refreshLayout();

        // --- LÓGICA DO TIMER DO LEADERBOARD ---
        if (leaderboardTimer > 0) {
            leaderboardTimer -= tpf;
            if (leaderboardTimer <= 0) {
                // O tempo acabou, esconder!
                hideLeaderboard();
            }
        }

        // --- LÓGICA DE TEMPO DA LEGENDA ---
        if (subtitleTimer > 0) {
            subtitleTimer -= tpf;
            if (subtitleTimer <= 0) {
                subtitleText.setText(""); // Limpar texto quando o tempo acaba
            }
        }

        InputAppState input = getState(InputAppState.class);
        PlayerAppState playerState = getState(PlayerAppState.class);

        if (playerState != null) {
            Player player = playerState.getPlayer();

            updateInventoryDisplay(player);
            updateCursorItemVisual(player, getApplication().getInputManager().getCursorPosition());

            if (input != null && input.consumeUiClickRequested()) {
                Vector2f mousePos = getApplication().getInputManager().getCursorPosition();

                // 1. Inventário Aberto (tecla I)
                if (isInventoryVisible) {
                    handleInventoryClick(mousePos, player);
                    // Se a mesa NÃO estiver aberta, o clique na receita faz craft imediato
                    if (!isCraftingOpen) {
                        checkInstantCraftClick(mousePos, player, playerRecipes, playerRecipeIcons);
                    }
                }

                // 2. Mesa de Crafting Aberta (tecla E)
                if (isCraftingOpen) {
                    handleTableInteraction(mousePos, player);
                }
            }
        }
    }

    public void showLeaderboard(List<jogo.system.HighScoreManager.ScoreEntry> scores, float duration, String headerMsg) {
        StringBuilder sb = new StringBuilder(headerMsg + "\n\n");
        sb.append("=== TOP SCORES ===\n");

        if (scores.isEmpty()) {
            sb.append("(Ainda sem registos)");
        } else {
            int pos = 1;
            for (jogo.system.HighScoreManager.ScoreEntry s : scores) {
                sb.append(pos++).append(". ").append(s.toString()).append("\n");
            }
        }

        highScoreText.setText(sb.toString());

        // Centralizar no ecrã
        float w = getApplication().getCamera().getWidth();
        float h = getApplication().getCamera().getHeight();
        float x = (w - highScoreText.getLineWidth()) / 2f;
        float y = h * 0.75f;
        highScoreText.setLocalTranslation(x, y, 0);

        // Mostrar
        if (leaderboardNode.getParent() == null) {
            guiNode.attachChild(leaderboardNode);
        }

        this.leaderboardTimer = duration; // Define o temporizador (pode ser 0 para infinito)
        this.isLeaderboardVisible = true;
    }

    public void hideLeaderboard() {
        leaderboardNode.removeFromParent();
        isLeaderboardVisible = false;
        leaderboardTimer = 0f;
    }

    public boolean isLeaderboardVisible() {
        return isLeaderboardVisible;
    }


    // --- LÓGICA DE INTERAÇÃO ---

    // Interação na Mesa: Selecionar Receita OU Clicar no Resultado
    private void handleTableInteraction(Vector2f mouse, Player p) {
        // Livro
        for (int i = 0; i < tableRecipeIcons.size(); i++) {
            Picture icon = tableRecipeIcons.get(i);
            if (isMouseOver(icon, mouse)) {
                selectedRecipe = tableRecipes.get(i);
                updateGridVisuals();
                return;
            }
        }

        // B. Verificar se clicou no Resultado (para craftar)
        if (isMouseOver(resultIcon, mouse) && selectedRecipe != null) {
            // Tentar craftar
            if (p.hasItem(selectedRecipe.inputId, selectedRecipe.inputCount)) {
                if (p.addItem(selectedRecipe.outputId, selectedRecipe.outputCount)) {
                    p.removeItem(selectedRecipe.inputId, selectedRecipe.inputCount);
                    System.out.println("Crafted na Mesa: " + selectedRecipe.name);
                } else {
                    System.out.println("Inventário cheio!");
                }
            } else {
                System.out.println("Faltam materiais!");
            }
        }
    }

    // Atualiza a visualização da grelha na mesa
    private void updateGridVisuals() {
        if (selectedRecipe == null) {
            // Esconder tudo se nada selecionado
            for (Picture p : gridInputIcons) p.setCullHint(Node.CullHint.Always);
            resultIcon.setCullHint(Node.CullHint.Always);
            return;
        }

        // 1. Mostrar Materiais (Input)
        // Como as receitas são simples, vamos preencher os primeiros slots com o material
        for (int i = 0; i < 9; i++) {
            if (i < selectedRecipe.inputCount) {
                try {
                    gridInputIcons.get(i).setImage(assetManager, "Textures/" + getTextureNameById(selectedRecipe.inputId), true);
                    gridInputIcons.get(i).setCullHint(Node.CullHint.Never);
                } catch (Exception e) {}
            } else {
                gridInputIcons.get(i).setCullHint(Node.CullHint.Always);
            }
        }

        // 2. Mostrar Resultado (Output)
        try {
            resultIcon.setImage(assetManager, "Textures/" + getTextureNameById(selectedRecipe.outputId), true);
            resultIcon.setCullHint(Node.CullHint.Never);
        } catch (Exception e) {}
    }

    // Crafting Instantâneo (Inventário Simples)
    private void checkInstantCraftClick(Vector2f mouse, Player p, List<Recipe> recipes, List<Picture> icons) {
        for (int i = 0; i < icons.size(); i++) {
            if (isMouseOver(icons.get(i), mouse)) {
                Recipe r = recipes.get(i);
                if (p.hasItem(r.inputId, r.inputCount)) {
                    if (p.addItem(r.outputId, r.outputCount)) {
                        p.removeItem(r.inputId, r.inputCount);
                        System.out.println("Crafted (Instant): " + r.name);
                    }
                }
                return;
            }
        }
    }

    private void handleInventoryClick(Vector2f mousePos, Player player) {
        for (int i = 0; i < 9; i++) {
            if (isMouseOver(hotbarSlots.get(i), mousePos)) {
                clickSlot(player.getHotbar(), i, player);
                return;
            }
        }
        for (int i = 0; i < mainInvSlots.size(); i++) {
            if (isMouseOver(mainInvSlots.get(i), mousePos)) {
                clickSlot(player.getMainInventory(), i, player);
                return;
            }
        }
    }

    private void clickSlot(ItemStack[] inventory, int index, Player player) {
        ItemStack slotItem = inventory[index];
        ItemStack handItem = player.getCursorItem();

        if (handItem == null) {
            if (slotItem != null) {
                player.setCursorItem(slotItem);
                inventory[index] = null;
            }
        } else {
            if (slotItem == null) {
                inventory[index] = handItem;
                player.setCursorItem(null);
            } else {
                if (slotItem.getId() == handItem.getId()) {
                    int space = ItemStack.MAX_STACK - slotItem.getAmount();
                    if (space >= handItem.getAmount()) {
                        slotItem.add(handItem.getAmount());
                        player.setCursorItem(null);
                    } else {
                        slotItem.setAmount(ItemStack.MAX_STACK);
                        handItem.add(-space);
                    }
                } else {
                    inventory[index] = handItem;
                    player.setCursorItem(slotItem);
                }
            }
        }
    }

    private boolean isMouseOver(Picture pic, Vector2f mouse) {
        float x = pic.getWorldTranslation().x;
        float y = pic.getWorldTranslation().y;
        float w = pic.getWidth();
        float h = pic.getHeight();
        return mouse.x >= x && mouse.x <= x + w && mouse.y >= y && mouse.y <= y + h;
    }

    // --- VISUALIZADORES ---

    public void updateInventoryDisplay(Player player) {
        updateSlotList(player.getHotbar(), hotbarSlots, hotbarIcons, hotbarTexts);
        if (isInventoryVisible) {
            updateSlotList(player.getMainInventory(), mainInvSlots, mainInvIcons, mainInvTexts);
        }
    }

    private void updateSlotList(ItemStack[] items, List<Picture> bgList, List<Picture> iconList, List<BitmapText> textList) {
        for (int i = 0; i < items.length; i++) {
            ItemStack stack = items[i];
            Picture icon = iconList.get(i);
            BitmapText text = textList.get(i);
            Picture bg = bgList.get(i);

            if (stack != null && stack.getAmount() > 0) {
                String texName = getTextureNameById(stack.getId());
                try {
                    icon.setImage(assetManager, "Textures/" + texName, true);
                    icon.setCullHint(Node.CullHint.Never);
                } catch (Exception e) { icon.setCullHint(Node.CullHint.Always); }

                float x = bg.getWorldTranslation().x + (bg.getWidth() - icon.getWidth()) / 2f;
                float y = bg.getWorldTranslation().y + (bg.getHeight() - icon.getHeight()) / 2f;
                icon.setPosition(x, y);

                if (stack.getAmount() > 1) {
                    text.setText(String.valueOf(stack.getAmount()));
                    float tx = bg.getWorldTranslation().x + bg.getWidth() - text.getLineWidth() - 7f;
                    float ty = bg.getWorldTranslation().y + text.getLineHeight() + 5f;
                    text.setLocalTranslation(tx, ty, 1);
                } else {
                    text.setText("");
                }
            } else {
                icon.setCullHint(Node.CullHint.Always);
                text.setText("");
            }
        }
    }

    private void updateCursorItemVisual(Player player, Vector2f mousePos) {
        ItemStack cursorStack = player.getCursorItem();
        if (cursorStack != null && cursorStack.getAmount() > 0) {
            try {
                cursorItemIcon.setImage(assetManager, "Textures/" + getTextureNameById(cursorStack.getId()), true);
            } catch (Exception e) {}
            cursorItemIcon.setPosition(mousePos.x - cursorItemIcon.getWidth()/2, mousePos.y - cursorItemIcon.getHeight()/2);
            cursorItemIcon.setCullHint(Node.CullHint.Never);
            cursorItemIcon.setLocalTranslation(cursorItemIcon.getLocalTranslation().x, cursorItemIcon.getLocalTranslation().y, 10f);
        } else {
            cursorItemIcon.setCullHint(Node.CullHint.Always);
        }
    }

    private void updateRecipeColors(List<Recipe> rList, List<Picture> iList, Player p) {
        for (int i = 0; i < rList.size(); i++) {
            Recipe r = rList.get(i);
            Picture icon = iList.get(i);
            try {
                // Colorir de cinzento se não houver materiais, branco se houver
                if (icon.getMaterial() != null) {
                    boolean has = p.hasItem(r.inputId, r.inputCount);
                    icon.getMaterial().setColor("Color", has ? ColorRGBA.White : ColorRGBA.Gray);
                }
            } catch (Exception ignored) {}
        }
    }

    private String getTextureNameById(byte id) {
        if (id == VoxelPalette.DIRT_ID) return "DirtBlock.png";
        if (id == VoxelPalette.GRASS_ID) return "GrassBlock.png";
        if (id == VoxelPalette.STONE_ID) return "StoneBlock.png";
        if (id == VoxelPalette.Wood_ID) return "WoodBlock.png";
        if (id == VoxelPalette.Leaf_ID) return "LeafBlock.png";
        if (id == VoxelPalette.SpikyWood_ID) return "SpikyWoodBlock.png";
        if (id == VoxelPalette.COAL_ID) return "CoalBlock.png";
        if (id == VoxelPalette.IRON_ID) return "IronBlock.png";
        if (id == VoxelPalette.DIAMOND_ID) return "DiamondBlock.png";
        if (id == VoxelPalette.PLANKS_ID) return "PlanksBlock.png";
        if (id == VoxelPalette.STICK_ID) return "Stick.png";
        if (id == VoxelPalette.CRAFTING_TABLE_ID) return "CraftingTableBlock.png";

        // --- NOVOS BLOCOS ADICIONADOS ---
        if (id == VoxelPalette.SAND_ID) return "SandBlock.png";
        if (id == VoxelPalette.WATER_ID) return "WaterBlock.png";
        if (id == VoxelPalette.TARGET_ID) return "TargetBlock.png"; // O teu novo bloco de alvo

        // Lanternas
        if (id == VoxelPalette.LANTERN_OFF_ID) return "LanternOff.png";
        if (id == VoxelPalette.LANTERN_ON_ID) return "LanternOn.png";

        // Picaretas
        if (id == VoxelPalette.WOOD_PICK_ID) return "WoodPick.png";
        if (id == VoxelPalette.STONE_PICK_ID) return "StonePick.png";
        if (id == VoxelPalette.IRON_PICK_ID) return "IronPick.png";

        // Materiais (Certifica-te que estas imagens estão na pasta Textures, ou muda o nome aqui)
        if (id == VoxelPalette.COAL_MAT_ID) return "CoalOre.png";
        if (id == VoxelPalette.IRON_MAT_ID) return "IronOre.png";

        // NOVAS TEXTURAS
        if (id == VoxelPalette.SPIKY_PLANKS_ID) return "SpikyPlankBlock.png";
        if (id == VoxelPalette.SWORD_ID) return "Sword.png";

        // Fallback (Padrão)
        return "DirtBlock.png";
    }

    // --- GESTÃO ---

    public void openCraftingTable() {
        if (isCraftingOpen) return;
        isCraftingOpen = true;
        guiNode.attachChild(craftingTableNode);
        setInventoryVisible(true);
        // Reset seleção
        selectedRecipe = null;
        updateGridVisuals();
    }

    public void closeCraftingTable() {
        isCraftingOpen = false;
        craftingTableNode.removeFromParent();
        setInventoryVisible(false);
    }

    public void setInventoryVisible(boolean visible) {
        if (isCraftingOpen && !visible) {
            closeCraftingTable();
            return;
        }
        this.isInventoryVisible = visible;
        if (visible) {
            guiNode.attachChild(inventoryNode);
            crosshair.removeFromParent();
        } else {
            inventoryNode.removeFromParent();
            guiNode.attachChild(crosshair);
        }
    }

    public void updateSelector(int slotIndex) {
        this.currentSlotIndex = slotIndex;
        refreshLayout();
    }

    private void centerCrosshair() {
        SimpleApplication sapp = (SimpleApplication) getApplication();
        int w = sapp.getCamera().getWidth();
        int h = sapp.getCamera().getHeight();
        float x = (w - crosshair.getLineWidth()) / 2f;
        float y = (h + crosshair.getLineHeight()) / 2f;
        crosshair.setLocalTranslation(x, y, 0);
    }

    public void setHealth(int currentHealth) {
        for (int i = 0; i < hearts.size(); i++) {
            Picture heart = hearts.get(i);
            int heartVal = (i + 1) * 10;
            int halfVal = heartVal - 5;
            try {
                if (currentHealth >= heartVal) heart.setImage(assetManager, "Interface/heart_full.png", true);
                else if (currentHealth >= halfVal) heart.setImage(assetManager, "Interface/heart_half.png", true);
                else heart.setImage(assetManager, "Interface/heart_empty.png", true);
            } catch (Exception e) {}
        }
    }

    // --- LAYOUT ---

    private void refreshLayout() {
        SimpleApplication sapp = (SimpleApplication) getApplication();
        float w = sapp.getCamera().getWidth();
        float h = sapp.getCamera().getHeight();

        float startX = (w / 2f) - (HOTBAR_WIDTH / 2f);
        float hotbarY = 10f;

        // 1. Hotbar
        for (int i=0; i<9; i++) {
            hotbarSlots.get(i).setPosition(startX + (i * (HOTBAR_WIDTH/9f)), hotbarY);
        }
        selector.setPosition(startX + (currentSlotIndex * (HOTBAR_WIDTH/9f)), hotbarY);

        // 2. Corações
        float heartsY = hotbarY + HOTBAR_HEIGHT + 10f;
        for (int i=0; i<hearts.size(); i++) hearts.get(i).setPosition(startX + (i * (HEART_SIZE + 2f)), heartsY);

        // 3. Inventário
        float invStartY = heartsY + 50f;
        if (isInventoryVisible) {
            for (int i = 0; i < mainInvSlots.size(); i++) {
                int row = i / 9; int col = i % 9;
                float x = startX + (col * (HOTBAR_WIDTH/9f));
                float y = invStartY + ((2 - row) * (HOTBAR_WIDTH/9f));
                mainInvSlots.get(i).setPosition(x, y);
            }

            // Crafting Básico (JOGADOR) - Só aparece se a mesa estiver fechada
            boolean showPlayerRecipes = !isCraftingOpen;

            float craftX = startX - 60f;
            for (int i = 0; i < playerRecipeIcons.size(); i++) {
                Picture icon = playerRecipeIcons.get(i);

                if (showPlayerRecipes) {
                    float y = invStartY + 100f - (i * 50f);
                    icon.setPosition(craftX, y);
                    icon.setCullHint(Node.CullHint.Never);
                    BitmapText lbl = (BitmapText) icon.getUserData("label");
                    if(lbl!=null) { lbl.setLocalTranslation(craftX+45f, y+25f, 1); lbl.setCullHint(Node.CullHint.Never); }

                    updateRecipeColors(playerRecipes, playerRecipeIcons, getState(PlayerAppState.class).getPlayer());
                } else {
                    icon.setCullHint(Node.CullHint.Always);
                    BitmapText lbl = (BitmapText) icon.getUserData("label");
                    if(lbl!=null) lbl.setCullHint(Node.CullHint.Always);
                }
            }
        }

        // 4.MESA + LIVRO CENTRADOS
        if (isCraftingOpen) {
            float totalWidth = BOOK_WIDTH + TABLE_WIDTH - PANEL_OVERLAP;
            float groupStartX = (w - totalWidth) / 2f;
            float groupStartY = (h - PANEL_HEIGHT) / 2f + 40f;

            // Mesa (Direita)
            float tableScreenX = groupStartX + BOOK_WIDTH - PANEL_OVERLAP;
            craftingTableNode.setLocalTranslation(tableScreenX, groupStartY, 0);
            craftingBg.setPosition(0, 0);

            // Livro (Esquerda)
            recipeBookBg.setPosition(-BOOK_WIDTH + PANEL_OVERLAP, 0);

            // Grelha da Mesa (Ajustada para a tua imagem)
            float gridStartX = (TABLE_WIDTH / 2f) - 101f;
            float gridStartY = PANEL_HEIGHT - 113f;
            float gridSize = 30f;
            float gap = 8f;

            for (int i = 0; i < 9; i++) {
                int r = i / 3; int c = i % 3;
                float gx = gridStartX + c * (gridSize + gap);
                float gy = gridStartY - r * (gridSize + gap);
                gridInputIcons.get(i).setPosition(gx, gy);
            }
            float resX = gridStartX + 3 * (gridSize + gap) + 45f;
            float resY = gridStartY - (gridSize + gap);
            resultIcon.setPosition(resX, resY);

            // Grelha do Livro
            float bookOriginX = -BOOK_WIDTH + PANEL_OVERLAP;
            float currentGridX = bookOriginX + GRID_START_X;
            float currentGridY = PANEL_HEIGHT - GRID_START_Y;

            for (int i = 0; i < tableRecipeIcons.size(); i++) {
                Picture icon = tableRecipeIcons.get(i);
                int col = i % GRID_COLS;
                int row = i / GRID_COLS;
                float x = currentGridX + (col * (SLOT_SIZE + SLOT_GAP));
                float y = currentGridY - (row * (SLOT_SIZE + SLOT_GAP));
                float offset = (SLOT_SIZE - icon.getWidth()) / 2f;
                icon.setPosition(x + offset, y - offset);
                icon.setCullHint(Node.CullHint.Never);

                Recipe r = tableRecipes.get(i);
                Player p = getState(PlayerAppState.class).getPlayer();
                if (icon.getMaterial() != null) {
                    if (p.hasItem(r.inputId, r.inputCount)) icon.getMaterial().setColor("Color", ColorRGBA.White);
                    else icon.getMaterial().setColor("Color", ColorRGBA.Gray);
                }
                BitmapText lbl = (BitmapText) icon.getUserData("label");
                if(lbl != null) lbl.setCullHint(Node.CullHint.Always);
            }
        }
    }

    @Override protected void cleanup(Application app) {
        if(crosshair != null) crosshair.removeFromParent();
        hotbarSlots.forEach(Picture::removeFromParent);
        inventoryNode.removeFromParent();
        craftingTableNode.removeFromParent();
        if (subtitleText != null) subtitleText.removeFromParent();
    }

    @Override protected void onEnable() {}
    @Override protected void onDisable() {}
}