package jogo.appstate;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.BaseAppState;
import com.jme3.asset.AssetManager;
import com.jme3.font.BitmapFont;
import com.jme3.font.BitmapText;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector2f;
import com.jme3.scene.Node;
import com.jme3.ui.Picture;
import jogo.crafting.Recipe;
import jogo.gameobject.character.Player;
import jogo.gameobject.item.ItemStack;
import jogo.system.HighScoreManager;
import jogo.voxel.VoxelPalette;

import java.util.ArrayList;
import java.util.List;

public class HudAppState extends BaseAppState {

    private final Node guiNode; //O nó "pai" de toda a interface 2D. Tudo anexado aqui aparece no ecrã.
    private final AssetManager assetManager; // Carrega ficheiros (texturas, fontes).
    private BitmapText crosshair; // Mira no centro do ecrã.
    private BitmapFont guiFont;  // Fonte de texto usada na UI.

    // --- UI INVENTÁRIO (Hotbar e Principal) ---

    //Listas declaradas para guardar referências de imagens e textos
    private final Node inventoryNode = new Node("InventoryNode");
    private final List<Picture> hotbarSlots = new ArrayList<>();
    private final List<BitmapText> hotbarTexts = new ArrayList<>();
    private final List<Picture> hotbarIcons = new ArrayList<>();

    private final List<Picture> mainInvSlots = new ArrayList<>();
    private final List<BitmapText> mainInvTexts = new ArrayList<>();
    private final List<Picture> mainInvIcons = new ArrayList<>();

    private Picture selector;
    private Picture cursorItemIcon; // Item "preso" ao rato
    private final List<Picture> hearts = new ArrayList<>();

    // ======================= UI CRAFTING ========================
    // Imagens de Fundo
    private Picture recipeBookBg;

    // Configurações de Tamanho e Posição
    private final float PANEL_HEIGHT = 300f;
    private final float TABLE_WIDTH = 270f;
    private final float BOOK_WIDTH = 270f;
    private final float PANEL_OVERLAP = 0f;

    // Grelha do Livro
    private final int GRID_COLS = 4;
    private final float GRID_START_X = 21f;
    private final float GRID_START_Y = 89f;
    private final float SLOT_SIZE = 44f;
    private final float SLOT_GAP = 3f;

    // Receitas
    private final List<Recipe> playerRecipes = new ArrayList<>();
    private final List<Picture> playerRecipeIcons = new ArrayList<>();

    private final Node craftingTableNode = new Node("CraftingTableUI");
    private Picture craftingBg;
    private final List<Recipe> tableRecipes = new ArrayList<>();
    private final List<Picture> tableRecipeIcons = new ArrayList<>();
    private jogo.crafting.CraftingManager craftingManager;

    // Visualização da Receita Selecionada na Mesa
    private final List<Picture> gridInputIcons = new ArrayList<>(); //As imagens usadas para mostrar visualmente "o que é preciso" na grelha 3x3 da mesa de trabalho.
    private Picture resultIcon;  // Ícone do resultado final
    private Recipe selectedRecipe = null;  // Guarda qual a receita selecionada

    // Estados
    private boolean isInventoryVisible = false;  //Se 'true', desenha o inventário.
    private boolean isCraftingOpen = false;  //Se 'true', desenha a UI da mesa de crafting.
    private int currentSlotIndex = 0;  // Índice do slot selecionado na hotbar

    // Configurações Visuais
    private final float HOTBAR_WIDTH = 400f;
    private final float HOTBAR_HEIGHT = 44f;
    private final float HEART_SIZE = 20f;

    // --- TEXTOS E MENUS ---
    private BitmapText subtitleText;
    private float subtitleTimer = 0f;  // Temporizador para esconder uma legenda

    private final Node loadMenuNode = new Node("LoadMenu");
    private BitmapText loadMenuText;

    private BitmapText miniGameText;
    private BitmapText highScoreText;

    // --- LEADERBOARD ---
    private final Node leaderboardNode = new Node("LeaderboardNode");
    private float leaderboardTimer = 0f;
    private boolean isLeaderboardVisible = false;

    //Chamado no Jogo.java ao fazer new HudAppState para poder desenhar a UI.
    public HudAppState(Node guiNode, AssetManager assetManager) {
        this.guiNode = guiNode;
        this.assetManager = assetManager;
    }

    // Executa quando o jogo começa, onde carrega a fonte, cria a mira, chama os métodos auxiliares para criar os elementos de UI.
    @Override
    protected void initialize(Application app) {
        guiFont = assetManager.loadFont("Interface/Fonts/Default.fnt");

        // 1. Mira
        crosshair = new BitmapText(guiFont, false);
        crosshair.setText("+");
        crosshair.setSize(guiFont.getCharSet().getRenderedSize() * 2f);
        guiNode.attachChild(crosshair);

        // 2. Inicializar Elementos UI
        initHotbar();
        initHearts();
        initInventory();
        initCursorItem();

        // 3. Inicializar Crafting (Jogador e Mesa)
        initCraftingSystems();
        this.craftingManager = new jogo.crafting.CraftingManager();

        // 4. Textos
        subtitleText = createText(ColorRGBA.Yellow, 1.2f);
        guiNode.attachChild(subtitleText);

        loadMenuText = createText(ColorRGBA.Green, 1.5f);
        loadMenuText.setText("SAVES DISPONIVEIS:\n(F1) save1\n(F2) save2");
        loadMenuNode.attachChild(loadMenuText);

        miniGameText = createText(ColorRGBA.Cyan, 1.0f);
        miniGameText.setLocalTranslation(10, 700, 0);
        guiNode.attachChild(miniGameText);

        highScoreText = createText(ColorRGBA.Yellow, 1.2f);
        leaderboardNode.attachChild(highScoreText);

        //Calcula as posições X/Y de tudo com base no tamanho da janela.
        refreshLayout();
        System.out.println("HudAppState initialized.");
    }

    // --- UI HELPERS ---

    // Metodo auxiliar para criar textos
    private BitmapText createText(ColorRGBA color, float sizeMult) {
        BitmapText txt = new BitmapText(guiFont, false);
        txt.setSize(guiFont.getCharSet().getRenderedSize() * sizeMult);
        txt.setColor(color);
        txt.setText("");
        return txt;
    }

    // Mostrar legenda no ecrã por um certo tempo.
    public void showSubtitle(String text, float duration) {
        if (subtitleText != null) {
            subtitleText.setText(text);
            subtitleTimer = duration;
            centerSubtitle();
        }
    }

    // Centralizar legenda no ecrã
    private void centerSubtitle() {
        SimpleApplication sapp = (SimpleApplication) getApplication();
        float w = sapp.getCamera().getWidth();
        float h = sapp.getCamera().getHeight();
        float x = (w - subtitleText.getLineWidth()) / 2f;
        float y = h * 0.75f;
        subtitleText.setLocalTranslation(x, y, 0);
    }

    // Atualizar informação do mini-jogo
    public void updateMiniGameInfo(float time, int hits, int total) {
        if (miniGameText != null) {
            miniGameText.setText(String.format("Tempo: %.1fs | Alvos: %d/%d", time, hits, total));
        }
    }

    // Mostrar menu de load
    public void showLoadMenu(List<String> saves) {
        StringBuilder sb = new StringBuilder("=== MENU DE LOAD ===\n\n");
        if (saves.isEmpty()) {
            sb.append("Nenhum save encontrado.\nJoga e carrega em 'M' para salvar.");
        } else {
            sb.append("Seleciona um mundo com F1, F2, F3...\n\n");
            for (int i = 0; i < saves.size(); i++) {
                if (i >= 9) { sb.append("... (mais saves ocultos)"); break; }
                sb.append("[F").append(i + 1).append("]  ").append(saves.get(i)).append("\n");
            }
        }
        loadMenuText.setText(sb.toString());
        SimpleApplication sapp = (SimpleApplication) getApplication();
        loadMenuText.setLocalTranslation(50, sapp.getCamera().getHeight() - 100, 0);
        guiNode.attachChild(loadMenuNode);
    }

    // Verificar se o menu de load está visível
    public boolean isLoadMenuVisible() { return loadMenuNode.getParent() != null; }
    // Esconder menu de load
    public void hideLoadMenu() { loadMenuNode.removeFromParent(); }

    // Mostrar leaderboard com scores
    public void showLeaderboard(List<HighScoreManager.ScoreEntry> scores, float duration, String headerMsg) {
        StringBuilder sb = new StringBuilder(headerMsg + "\n\n=== TOP SCORES ===\n");
        if (scores.isEmpty()) sb.append("(Ainda sem registos)");
        else {
            int pos = 1;
            for (HighScoreManager.ScoreEntry s : scores) {
                sb.append(pos++).append(". ").append(s.toString()).append("\n");
            }
        }
        highScoreText.setText(sb.toString());
        float w = getApplication().getCamera().getWidth();
        float h = getApplication().getCamera().getHeight();
        highScoreText.setLocalTranslation((w - highScoreText.getLineWidth()) / 2f, h * 0.75f, 0);

        if (leaderboardNode.getParent() == null) guiNode.attachChild(leaderboardNode);
        this.leaderboardTimer = duration;
        this.isLeaderboardVisible = true;
    }

    // Esconder leaderboard
    public void hideLeaderboard() {
        leaderboardNode.removeFromParent();
        isLeaderboardVisible = false;
        leaderboardTimer = 0f;
    }

    // Verificar se a leaderboard está visível
    public boolean isLeaderboardVisible() { return isLeaderboardVisible; }

    // --- INITIALIZATION ---

    // Inicializar hotbar
    private void initHotbar() {
        float slotWidth = HOTBAR_WIDTH / 9f;
        for (int i = 0; i < 9; i++) {
            createSlot(i, slotWidth, HOTBAR_HEIGHT, hotbarSlots, hotbarIcons, hotbarTexts, guiNode, true); // Cria a imagem e o texto
        }
        selector = new Picture("Selector");
        selector.setImage(assetManager, "Interface/selector.png", true);
        selector.setWidth(slotWidth);
        selector.setHeight(HOTBAR_HEIGHT);
        guiNode.attachChild(selector);
    }

    // Inicializar inventário principal
    private void initInventory() {
        float slotSize = HOTBAR_WIDTH / 9f;
        for (int i = 0; i < 27; i++) {
            createSlot(i, slotSize, slotSize, mainInvSlots, mainInvIcons, mainInvTexts, inventoryNode, false); // Cria a imagem e o texto
        }
    }

    // Metodo auxiliar para criar slots de inventário
    private void createSlot(int i, float w, float h, List<Picture> slots, List<Picture> icons, List<BitmapText> texts, Node parent, boolean isHotbar) {
        Picture slot = new Picture((isHotbar ? "HotbarSlot_" : "InvSlot_") + i);
        slot.setImage(assetManager, "Interface/hotbarsquare.png", true);
        slot.setWidth(w);
        slot.setHeight(h);
        parent.attachChild(slot);
        slots.add(slot);

        Picture icon = new Picture((isHotbar ? "HotbarIcon_" : "InvIcon_") + i);
        icon.setWidth(w * 0.6f);
        icon.setHeight(h * 0.6f);
        parent.attachChild(icon);
        icons.add(icon);

        BitmapText txt = new BitmapText(guiFont, false);
        txt.setSize(guiFont.getCharSet().getRenderedSize() * 0.8f);
        txt.setColor(ColorRGBA.White);
        parent.attachChild(txt);
        texts.add(txt);
    }

    // Inicializar corações de vida
    private void initHearts() {
        for (int i = 0; i < 10; i++) {
            Picture heart = new Picture("Heart_" + i);
            heart.setImage(assetManager, "Interface/heart_full.png", true);
            heart.setWidth(HEART_SIZE);
            heart.setHeight(HEART_SIZE);
            guiNode.attachChild(heart);
            hearts.add(heart);
        }
    }

    // Inicializar ícone do item preso ao cursor
    private void initCursorItem() {
        cursorItemIcon = new Picture("CursorItem");
        try { cursorItemIcon.setImage(assetManager, "Interface/CursorItem.png", true); }
        catch(Exception e){ System.out.println("Aviso: CursorItem.png em falta"); }
        cursorItemIcon.setWidth(HOTBAR_WIDTH / 9f * 0.6f);
        cursorItemIcon.setHeight(HOTBAR_WIDTH / 9f * 0.6f);
        cursorItemIcon.setCullHint(Node.CullHint.Always);
        guiNode.attachChild(cursorItemIcon);
    }

    // --- CRAFTING ---

    // Inicializar sistemas de crafting
    private void initCraftingSystems() {
        playerRecipes.clear();
        tableRecipes.clear();

        // Receitas do Jogador
        Recipe rPlanks = new Recipe("Planks", VoxelPalette.Wood_ID, 1, VoxelPalette.PLANKS_ID, 4);
        Recipe rSticks = new Recipe("Sticks", VoxelPalette.PLANKS_ID, 2, VoxelPalette.STICK_ID, 4);
        Recipe rTable = new Recipe("Table", VoxelPalette.PLANKS_ID, 4, VoxelPalette.CRAFTING_TABLE_ID, 1);

        // Adicionar receitas do jogador
        playerRecipes.add(rPlanks);
        playerRecipes.add(rSticks);
        playerRecipes.add(rTable);
        createRecipeIcons(playerRecipes, playerRecipeIcons, inventoryNode); // Cria os botões visuais

        // UI da Mesa
        initTableUI();

        // Receitas da Mesa
        tableRecipes.add(rPlanks);
        tableRecipes.add(rSticks);
        tableRecipes.add(rTable);

        // Ferramentas
        tableRecipes.add(new Recipe("WoodPick", VoxelPalette.WOOD_PICK_ID, 1, new ItemStack(VoxelPalette.PLANKS_ID, 3), new ItemStack(VoxelPalette.STICK_ID, 2)));
        tableRecipes.add(new Recipe("StonePick", VoxelPalette.STONE_PICK_ID, 1, new ItemStack(VoxelPalette.STONE_ID, 3), new ItemStack(VoxelPalette.STICK_ID, 2)));
        tableRecipes.add(new Recipe("IronPick", VoxelPalette.IRON_PICK_ID, 1, new ItemStack(VoxelPalette.IRON_MAT_ID, 3), new ItemStack(VoxelPalette.STICK_ID, 2)));

        // Outros
        tableRecipes.add(new Recipe("Lantern", VoxelPalette.LANTERN_OFF_ID, 1, new ItemStack(VoxelPalette.COAL_MAT_ID, 1), new ItemStack(VoxelPalette.IRON_MAT_ID, 4)));
        tableRecipes.add(new Recipe("SpikyPlanks", VoxelPalette.SPIKY_PLANKS_ID, 4, new ItemStack(VoxelPalette.SpikyWood_ID, 4)));
        tableRecipes.add(new Recipe("Sword", VoxelPalette.SWORD_ID, 1, new ItemStack(VoxelPalette.SPIKY_PLANKS_ID, 2), new ItemStack(VoxelPalette.STICK_ID, 1)));

        createRecipeIcons(tableRecipes, tableRecipeIcons, craftingTableNode);
    }

    // Inicializar UI da mesa de crafting
    private void initTableUI() {
        craftingBg = new Picture("CraftingBg");
        try { craftingBg.setImage(assetManager, "Interface/CraftingUI.png", true); }
        catch (Exception e) { craftingBg.setImage(assetManager, "Interface/hotbarsquare.png", true); }
        craftingBg.setWidth(TABLE_WIDTH);
        craftingBg.setHeight(PANEL_HEIGHT);
        craftingTableNode.attachChild(craftingBg);

        recipeBookBg = new Picture("RecipeBookBg");
        try { recipeBookBg.setImage(assetManager, "Interface/RecipeBook.png", true); }
        catch (Exception e) {
            recipeBookBg.setImage(assetManager, "Interface/hotbarsquare.png", true);
            recipeBookBg.getMaterial().setColor("Color", ColorRGBA.Brown);
        }
        recipeBookBg.setWidth(BOOK_WIDTH);
        recipeBookBg.setHeight(PANEL_HEIGHT);
        craftingTableNode.attachChild(recipeBookBg);

        float slotSize = 30f;
        for (int i = 0; i < 9; i++) {
            Picture gridIcon = new Picture("GridIcon_" + i);
            gridIcon.setWidth(slotSize);
            gridIcon.setHeight(slotSize);
            gridIcon.setCullHint(Node.CullHint.Always);
            craftingTableNode.attachChild(gridIcon);
            gridInputIcons.add(gridIcon);
        }

        resultIcon = new Picture("ResultIcon");
        resultIcon.setWidth(slotSize * 1.5f);
        resultIcon.setHeight(slotSize * 1.5f);
        resultIcon.setCullHint(Node.CullHint.Always);
        craftingTableNode.attachChild(resultIcon);
    }

    // Criar ícones de receitas
    private void createRecipeIcons(List<Recipe> rList, List<Picture> iList, Node parent) {
        float iconSize = 32f;
        for (Recipe r : rList) {
            Picture icon = new Picture("Rec_" + r.name);
            try {
                icon.setImage(assetManager, "Textures/" + getTextureNameById(r.outputId), true);
            } catch(Exception e){ System.out.println("Erro textura receita: " + r.name); }
            icon.setWidth(iconSize);
            icon.setHeight(iconSize);
            parent.attachChild(icon);
            iList.add(icon);

            BitmapText lbl = new BitmapText(guiFont, false);
            lbl.setSize(guiFont.getCharSet().getRenderedSize() * 0.7f);
            if (!r.inputs.isEmpty()) lbl.setText(r.inputs.get(0).getAmount() + "x");
            lbl.setColor(ColorRGBA.Yellow);
            parent.attachChild(lbl);
            icon.setUserData("label", lbl);
        }
    }

    // --- UPDATE ---

    //Sincroniza os dados lógicos do Player com as imagens no ecrã.
    @Override
    public void update(float tpf) {
        centerCrosshair();
        refreshLayout();

        // Gere temporizadores (se > 0, diminui. Se chegar a 0, esconde).
        if (leaderboardTimer > 0 && (leaderboardTimer -= tpf) <= 0) hideLeaderboard();
        if (subtitleTimer > 0 && (subtitleTimer -= tpf) <= 0) subtitleText.setText("");

        // Obtém dados do Jogador e Input
        InputAppState input = getState(InputAppState.class);
        PlayerAppState playerState = getState(PlayerAppState.class);

        if (playerState != null) {
            Player player = playerState.getPlayer();

            // 1. ATUALIZA VISUALMENTE O INVENTÁRIO
            updateInventoryDisplay(player);
            updateCursorItemVisual(player, getApplication().getInputManager().getCursorPosition());

            // 2. GERE CLIQUES DO RATO
            if (input != null && input.consumeUiClickRequested()) {
                Vector2f mousePos = getApplication().getInputManager().getCursorPosition();
                if (isInventoryVisible) {
                    handleInventoryClick(mousePos, player);
                    if (!isCraftingOpen) checkInstantCraftClick(mousePos, player, playerRecipes, playerRecipeIcons);
                }
                if (isCraftingOpen) handleTableInteraction(mousePos, player);
            }
        }
    }

    // --- LÓGICA DE INTERAÇÃO ---

    private void handleTableInteraction(Vector2f mouse, Player p) {
        for (int i = 0; i < tableRecipeIcons.size(); i++) {
            if (isMouseOver(tableRecipeIcons.get(i), mouse)) {
                selectedRecipe = tableRecipes.get(i);
                updateGridVisuals();
                return;
            }
        }
        if (isMouseOver(resultIcon, mouse) && selectedRecipe != null) {
            if (tryCraft(p, selectedRecipe)) {
                updateGridVisuals();
            }
        }
    }

    // Tentar craftar a receita ao verificar ingredientes e adicionar o item ao inventário.
    private boolean tryCraft(Player p, Recipe r) {
        // O HudAppState apenas "pede" ao gerente para fazer o trabalho
        boolean sucesso = craftingManager.craft(r, p);

        if (sucesso) {
            System.out.println("Crafting bem sucedido: " + r.name);
        }
        return sucesso;
    }

    // Atualizar visuais da grelha de crafting
    private void updateGridVisuals() {
        if (selectedRecipe == null) {
            gridInputIcons.forEach(p -> p.setCullHint(Node.CullHint.Always));
            resultIcon.setCullHint(Node.CullHint.Always);
            return;
        }
        int slotIndex = 0;
        for (ItemStack req : selectedRecipe.inputs) {
            for (int k = 0; k < req.getAmount(); k++) {
                if (slotIndex < 9) {
                    Picture icon = gridInputIcons.get(slotIndex++);
                    try {
                        icon.setImage(assetManager, "Textures/" + getTextureNameById(req.getId()), true);
                        icon.setCullHint(Node.CullHint.Never);
                    } catch (Exception e) {}
                }
            }
        }
        for (int i = slotIndex; i < 9; i++) gridInputIcons.get(i).setCullHint(Node.CullHint.Always);
        try {
            resultIcon.setImage(assetManager, "Textures/" + getTextureNameById(selectedRecipe.outputId), true);
            resultIcon.setCullHint(Node.CullHint.Never);
        } catch (Exception e) {}
    }

    // Verificar clique rápido em receitas do jogador
    private void checkInstantCraftClick(Vector2f mouse, Player p, List<Recipe> recipes, List<Picture> icons) {
        for (int i = 0; i < icons.size(); i++) {
            if (isMouseOver(icons.get(i), mouse)) {
                tryCraft(p, recipes.get(i));
                return;
            }
        }
    }

    // Lidar com clique no inventário
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

    // Lógica de clique num slot para mover itens
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

    // Verificar se o rato está sobre uma imagem
    private boolean isMouseOver(Picture pic, Vector2f mouse) {
        float x = pic.getWorldTranslation().x;
        float y = pic.getWorldTranslation().y;
        return mouse.x >= x && mouse.x <= x + pic.getWidth() && mouse.y >= y && mouse.y <= y + pic.getHeight();
    }

    // --- DISPLAY HELPERS ---

    // Atualizar display do inventário
    public void updateInventoryDisplay(Player player) {
        updateSlotList(player.getHotbar(), hotbarSlots, hotbarIcons, hotbarTexts);
        if (isInventoryVisible) updateSlotList(player.getMainInventory(), mainInvSlots, mainInvIcons, mainInvTexts);
    }

    // Atualizar visuais dos slots
    // Percorre a lista de Pictures (UI) e a lista de ItemStacks (Dados)
    // Se ItemStack tem item, define imagem e texto da quantidade.
    // Se ItemStack null define imagem "vazia" ou esconde.
    private void updateSlotList(ItemStack[] items, List<Picture> bgList, List<Picture> iconList, List<BitmapText> textList) {
        for (int i = 0; i < items.length; i++) {
            ItemStack stack = items[i];
            Picture icon = iconList.get(i);
            BitmapText text = textList.get(i);
            Picture bg = bgList.get(i);

            if (stack != null && stack.getAmount() > 0) {
                try {
                    icon.setImage(assetManager, "Textures/" + getTextureNameById(stack.getId()), true);
                    icon.setCullHint(Node.CullHint.Never);
                } catch (Exception e) { icon.setCullHint(Node.CullHint.Always); }

                float x = bg.getWorldTranslation().x + (bg.getWidth() - icon.getWidth()) / 2f;
                float y = bg.getWorldTranslation().y + (bg.getHeight() - icon.getHeight()) / 2f;
                icon.setPosition(x, y);

                text.setText(stack.getAmount() > 1 ? String.valueOf(stack.getAmount()) : "");
                text.setLocalTranslation(bg.getWorldTranslation().x + bg.getWidth() - text.getLineWidth() - 7f,
                        bg.getWorldTranslation().y + text.getLineHeight() + 5f, 1);
            } else {
                icon.setCullHint(Node.CullHint.Always);
                text.setText("");
            }
        }
    }

    // Atualizar visual do item preso ao cursor
    private void updateCursorItemVisual(Player player, Vector2f mousePos) {
        ItemStack stack = player.getCursorItem();
        if (stack != null && stack.getAmount() > 0) {
            try {
                cursorItemIcon.setImage(assetManager, "Textures/" + getTextureNameById(stack.getId()), true);
            } catch (Exception e) {}
            cursorItemIcon.setPosition(mousePos.x - cursorItemIcon.getWidth()/2, mousePos.y - cursorItemIcon.getHeight()/2);
            cursorItemIcon.setCullHint(Node.CullHint.Never);
        } else {
            cursorItemIcon.setCullHint(Node.CullHint.Always);
        }
    }

    // Atualizar cores dos ícones de receitas conforme disponibilidade de materiais
    private void updateRecipeColors(List<Recipe> rList, List<Picture> iList, Player p) {
        for (int i = 0; i < rList.size(); i++) {
            Recipe r = rList.get(i);
            Picture icon = iList.get(i);
            try {
                if (icon.getMaterial() != null) {
                    boolean hasAll = true;
                    for (ItemStack req : r.inputs) {
                        if (!p.hasItem(req.getId(), req.getAmount())) { hasAll = false; break; }
                    }
                    icon.getMaterial().setColor("Color", hasAll ? ColorRGBA.White : ColorRGBA.Gray);
                }
            } catch (Exception ignored) {}
        }
    }

    // Obter nome da textura pelo ID do voxel/item
    private String getTextureNameById(byte id) {
        return switch (id) {
            case VoxelPalette.DIRT_ID -> "DirtBlock.png";
            case VoxelPalette.GRASS_ID -> "GrassBlock.png";
            case VoxelPalette.STONE_ID -> "StoneBlock.png";
            case VoxelPalette.Wood_ID -> "WoodBlock.png";
            case VoxelPalette.Leaf_ID -> "LeafBlock.png";
            case VoxelPalette.SpikyWood_ID -> "SpikyWoodBlock.png";
            case VoxelPalette.COAL_ID -> "CoalBlock.png";
            case VoxelPalette.IRON_ID -> "IronBlock.png";
            case VoxelPalette.DIAMOND_ID -> "DiamondBlock.png";
            case VoxelPalette.PLANKS_ID -> "PlanksBlock.png";
            case VoxelPalette.STICK_ID -> "Stick.png";
            case VoxelPalette.CRAFTING_TABLE_ID -> "CraftingTableBlock.png";
            case VoxelPalette.SAND_ID -> "SandBlock.png";
            case VoxelPalette.WATER_ID -> "WaterBlock.png";
            case VoxelPalette.TARGET_ID -> "TargetBlock.png";
            case VoxelPalette.LANTERN_OFF_ID -> "LanternOff.png";
            case VoxelPalette.LANTERN_ON_ID -> "LanternOn.png";
            case VoxelPalette.WOOD_PICK_ID -> "WoodPick.png";
            case VoxelPalette.STONE_PICK_ID -> "StonePick.png";
            case VoxelPalette.IRON_PICK_ID -> "IronPick.png";
            case VoxelPalette.COAL_MAT_ID -> "CoalOre.png";
            case VoxelPalette.IRON_MAT_ID -> "IronOre.png";
            case VoxelPalette.SPIKY_PLANKS_ID -> "SpikyPlankBlock.png";
            case VoxelPalette.SWORD_ID -> "Sword.png";
            default -> "DirtBlock.png";
        };
    }

    // --- GESTÃO GERAL ---

    // Abrir mesa de crafting
    public void openCraftingTable() {
        if (isCraftingOpen) return;
        isCraftingOpen = true;
        guiNode.attachChild(craftingTableNode);
        setInventoryVisible(true);
        selectedRecipe = null;
        updateGridVisuals();
    }

    // Fechar mesa de crafting
    public void closeCraftingTable() {
        isCraftingOpen = false;
        craftingTableNode.removeFromParent();
        setInventoryVisible(false);
    }

    // Definir visibilidade do inventário chamado pelo InputAppState ao carregar em 'E' ou 'I'
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

    // Atualizar slot selecionado na hotbar
    public void updateSelector(int slotIndex) {
        this.currentSlotIndex = slotIndex;
        refreshLayout();
    }

    // Centralizar mira no ecrã
    private void centerCrosshair() {
        SimpleApplication sapp = (SimpleApplication) getApplication();
        int w = sapp.getCamera().getWidth();
        int h = sapp.getCamera().getHeight();
        crosshair.setLocalTranslation((w - crosshair.getLineWidth()) / 2f, (h + crosshair.getLineHeight()) / 2f, 0);
    }

    // Definir vida atual e atualizar corações
    public void setHealth(int currentHealth) {
        for (int i = 0; i < hearts.size(); i++) {
            int heartVal = (i + 1) * 10;
            int halfVal = heartVal - 5;
            try {
                if (currentHealth >= heartVal) hearts.get(i).setImage(assetManager, "Interface/heart_full.png", true);
                else if (currentHealth >= halfVal) hearts.get(i).setImage(assetManager, "Interface/heart_half.png", true);
                else hearts.get(i).setImage(assetManager, "Interface/heart_empty.png", true);
            } catch (Exception e) {}
        }
    }

    // Obter índice do slot selecionado na hotbar
    public int getSelectedSlotIndex() { return currentSlotIndex; }

    // --- LAYOUT ---

    // Atualizar layout dos elementos UI conforme tamanho da janela
    private void refreshLayout() {
        SimpleApplication sapp = (SimpleApplication) getApplication();
        float w = sapp.getCamera().getWidth();
        float h = sapp.getCamera().getHeight();
        float startX = (w / 2f) - (HOTBAR_WIDTH / 2f);
        float hotbarY = 10f;

        for (int i=0; i<9; i++) hotbarSlots.get(i).setPosition(startX + (i * (HOTBAR_WIDTH/9f)), hotbarY);
        selector.setPosition(startX + (currentSlotIndex * (HOTBAR_WIDTH/9f)), hotbarY);

        float heartsY = hotbarY + HOTBAR_HEIGHT + 10f;
        for (int i=0; i<hearts.size(); i++) hearts.get(i).setPosition(startX + (i * (HEART_SIZE + 2f)), heartsY);

        float invStartY = heartsY + 50f;
        if (isInventoryVisible) {
            for (int i = 0; i < mainInvSlots.size(); i++) {
                int row = i / 9; int col = i % 9;
                mainInvSlots.get(i).setPosition(startX + (col * (HOTBAR_WIDTH/9f)), invStartY + ((2 - row) * (HOTBAR_WIDTH/9f)));
            }

            boolean showPlayerRecipes = !isCraftingOpen;
            float craftX = startX - 60f;
            for (int i = 0; i < playerRecipeIcons.size(); i++) {
                Picture icon = playerRecipeIcons.get(i);
                if (showPlayerRecipes) {
                    float y = invStartY + 100f - (i * 50f);
                    icon.setPosition(craftX, y);
                    icon.setCullHint(Node.CullHint.Never);
                    BitmapText lbl = icon.getUserData("label");
                    if(lbl!=null) { lbl.setLocalTranslation(craftX+45f, y+25f, 1); lbl.setCullHint(Node.CullHint.Never); }
                    Player p = getState(PlayerAppState.class).getPlayer();
                    if(p!=null) updateRecipeColors(playerRecipes, playerRecipeIcons, p);
                } else {
                    icon.setCullHint(Node.CullHint.Always);
                    BitmapText lbl = icon.getUserData("label");
                    if(lbl!=null) lbl.setCullHint(Node.CullHint.Always);
                }
            }
        }

        if (isCraftingOpen) {
            float totalWidth = BOOK_WIDTH + TABLE_WIDTH - PANEL_OVERLAP;
            float groupStartX = (w - totalWidth) / 2f;
            float groupStartY = (h - PANEL_HEIGHT) / 2f + 40f;

            craftingTableNode.setLocalTranslation(groupStartX + BOOK_WIDTH - PANEL_OVERLAP, groupStartY, 0);
            craftingBg.setPosition(0, 0);
            recipeBookBg.setPosition(-BOOK_WIDTH + PANEL_OVERLAP, 0);

            float gridStartX = (TABLE_WIDTH / 2f) - 101f;
            float gridStartY = PANEL_HEIGHT - 113f;
            float gridSize = 30f;
            float gapX = 8f; float gapY = 14f;

            for (int i = 0; i < 9; i++) {
                int r = i / 3; int c = i % 3;
                gridInputIcons.get(i).setPosition(gridStartX + c * (gridSize + gapX), gridStartY - r * (gridSize + gapY));
            }
            resultIcon.setPosition(gridStartX + 3 * (gridSize + gapX) + 45f, gridStartY - (gridSize + gapY) - 15f);

            float currentGridX = (-BOOK_WIDTH + PANEL_OVERLAP) + GRID_START_X;
            float currentGridY = PANEL_HEIGHT - GRID_START_Y;

            for (int i = 0; i < tableRecipeIcons.size(); i++) {
                Picture icon = tableRecipeIcons.get(i);
                int col = i % GRID_COLS;
                int row = i / GRID_COLS;
                float x = currentGridX + (col * (SLOT_SIZE + SLOT_GAP));
                float y = currentGridY - (row * (SLOT_SIZE + SLOT_GAP));
                icon.setPosition(x + (SLOT_SIZE - icon.getWidth())/2f, y - (SLOT_SIZE - icon.getWidth())/2f);
                icon.setCullHint(Node.CullHint.Never);

                Player p = getState(PlayerAppState.class).getPlayer();
                if(p!=null) updateRecipeColors(tableRecipes, tableRecipeIcons, p);

                BitmapText lbl = icon.getUserData("label");
                if(lbl != null) lbl.setCullHint(Node.CullHint.Always);
            }
        }
    }

    // --- CLEANUP ---
    // Remover todos os elementos UI ao finalizar o AppState
    @Override
    protected void cleanup(Application app) {
        if(crosshair != null) crosshair.removeFromParent();
        hotbarSlots.forEach(Picture::removeFromParent);
        inventoryNode.removeFromParent();
        craftingTableNode.removeFromParent();
        if (subtitleText != null) subtitleText.removeFromParent();
        if (miniGameText != null) miniGameText.removeFromParent();
    }

    @Override protected void onEnable() {}
    @Override protected void onDisable() {}
}