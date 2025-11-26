package jogo.appstate;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.BaseAppState;
import com.jme3.asset.AssetManager;
import com.jme3.font.BitmapFont;
import com.jme3.font.BitmapText;
import com.jme3.scene.Node;
import com.jme3.ui.Picture;

import java.util.ArrayList;
import java.util.List;

public class HudAppState extends BaseAppState {

    private final Node guiNode;
    private final AssetManager assetManager;

    //Elementos da interface do jogador
    private BitmapText crosshair;

    private List<Picture> hotbarSlots = new ArrayList<>();
    private Picture selector;
    private List<Picture> hearts = new ArrayList<>();

    // --- NOVO: Inventário ---
    private Node inventoryNode = new Node("InventoryNode");
    private List<Picture> mainInvSlots = new ArrayList<>();
    private boolean isInventoryVisible = false;
    private int currentSlotIndex = 0;

    // Tamanho dos elementos da interface
    private final float HOTBAR_WIDTH = 400f;
    private final float HOTBAR_HEIGHT = 44f;
    private final float HEART_SIZE = 20f;

    public HudAppState(Node guiNode, AssetManager assetManager) {
        this.guiNode = guiNode;
        this.assetManager = assetManager;
    }

    @Override
    protected void initialize(Application app) {
        BitmapFont font = assetManager.loadFont("Interface/Fonts/Default.fnt");
        crosshair = new BitmapText(font, false);
        crosshair.setText("+");
        crosshair.setSize(font.getCharSet().getRenderedSize() * 2f);
        guiNode.attachChild(crosshair);
        centerCrosshair();
        initHotbar(app);
        initHearts(app);
        initInventory(app);
        refreshLayout();
        System.out.println("HudAppState initialized: UI elements attached");
    }
    private void initHotbar(Application app) {
        // Calcular a largura de UM quadrado baseado na largura total desejada / 9
        float slotWidth = HOTBAR_WIDTH / 9f;

        // Criar os 9 slots individuais
        for (int i = 0; i < 9; i++) {
            Picture slot = new Picture("HotbarSlot_" + i);
            // AQUI: Certifica-te que tens a imagem 'hotbarsquare.png' na pasta Interface
            slot.setImage(assetManager, "Interface/hotbarsquare.png", true);

            slot.setWidth(slotWidth);
            slot.setHeight(HOTBAR_HEIGHT);

            guiNode.attachChild(slot);
            hotbarSlots.add(slot);
        }

        // Criar a imagem do Selector (o quadrado que se move)
        selector = new Picture("Selector");
        selector.setImage(assetManager, "Interface/selector.png", true);

        // O selector deve ter o mesmo tamanho que um slot individual
        selector.setWidth(slotWidth);
        selector.setHeight(HOTBAR_HEIGHT);

        // Adicionamos o selector DEPOIS dos slots para ele ficar "em cima" visualmente
        guiNode.attachChild(selector);
    }
    private void initHearts(Application app) {
        // Criar 10 corações
        for (int i = 0; i < 10; i++) {
            Picture heart = new Picture("Heart_" + i);
            heart.setImage(assetManager, "Interface/heart_full.png", true);
            heart.setWidth(HEART_SIZE);
            heart.setHeight(HEART_SIZE);

            guiNode.attachChild(heart);
            hearts.add(heart);
        }
    }

    // --- NOVO: Criar os 27 slots do inventário ---
    private void initInventory(Application app) {
        float slotSize = HOTBAR_WIDTH / 9f; // Mesmo tamanho da hotbar

        // 3 linhas, 9 colunas
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                Picture slot = new Picture("InvSlot_" + row + "_" + col);
                slot.setImage(assetManager, "Interface/hotbarsquare.png", true);
                slot.setWidth(slotSize);
                slot.setHeight(slotSize); // Quadrado

                inventoryNode.attachChild(slot);
                mainInvSlots.add(slot);
            }
        }
        // Nota: Não fazemos guiNode.attachChild(inventoryNode) aqui.
        // Só fazemos quando o jogador abrir o inventário.
    }

    public void setInventoryVisible(boolean visible) {
        this.isInventoryVisible = visible;
        if (visible) {
            guiNode.attachChild(inventoryNode);
            // Esconder a mira quando inventário está aberto
            crosshair.removeFromParent();
        } else {
            inventoryNode.removeFromParent();
            // Mostrar a mira de volta
            guiNode.attachChild(crosshair);
        }
    }

    // --- CORREÇÃO: Atualizar a variável e forçar refresh ---
    public void updateSelector(int slotIndex) {
        this.currentSlotIndex = slotIndex; // Guardar o novo índice
        refreshLayout(); // Atualizar visual imediatamente
    }

    /**
     * Recalcula as posições baseado no tamanho atual da janela.
     * Importante se o jogador redimensionar a janela.
     */
    private void refreshLayout() {
        SimpleApplication sapp = (SimpleApplication) getApplication();
        float screenW = sapp.getCamera().getWidth();
        float screenH = sapp.getCamera().getHeight();

        // 1. Centrar Mira
        float chX = (screenW - crosshair.getLineWidth()) / 2f;
        float chY = (screenH + crosshair.getLineHeight()) / 2f;
        crosshair.setLocalTranslation(chX, chY, 0);

        // --- Posicionamento da Hotbar ---
        // Largura de cada slot individual
        float slotWidth = HOTBAR_WIDTH / 9f;

        // Ponto de partida X (para ficar centrado no ecrã)
        float startX = (screenW / 2f) - (HOTBAR_WIDTH / 2f);
        float hotbarY = 10f; // Margem do fundo

        // Atualizar posição de cada quadrado da hotbar
        for (int i = 0; i < hotbarSlots.size(); i++) {
            Picture slot = hotbarSlots.get(i);
            // A posição X é o inicio + (índice * largura).
            // Como não adicionamos margem extra, eles ficam colados.
            float x = startX + (i * slotWidth);
            slot.setPosition(x, hotbarY);
        }

        // --- CORREÇÃO: Usar a variável currentSlotIndex ---
        float selectorX = startX + (currentSlotIndex * slotWidth);
        selector.setPosition(selectorX, hotbarY);
        // ----

        // --- Posicionamento dos Corações ---
        float heartsStartX = startX;
        float heartsY = hotbarY + HOTBAR_HEIGHT + 10f; // 10px acima da hotbar

        for (int i = 0; i < hearts.size(); i++) {
            Picture heart = hearts.get(i);
            float x = heartsStartX + (i * (HEART_SIZE + 2f)); // Pequeno espaço entre corações
            heart.setPosition(x, heartsY);
        }
        // --- Layout do Inventário (Centrado na tela, acima da hotbar) ---
        float invStartY = heartsY + 50f; // Um pouco acima dos corações

        // O array mainInvSlots foi preenchido linha a linha.
        // Linha 0 é a de baixo do inventário principal
        for (int i = 0; i < mainInvSlots.size(); i++) {
            int row = i / 9; // 0, 1 ou 2
            int col = i % 9; // 0 a 8

            Picture slot = mainInvSlots.get(i);
            float x = startX + (col * slotWidth);
            // Inverter row para desenhar de baixo para cima ou ajustar conforme preferência
            // Vamos desenhar: Linha 2 (topo), Linha 1 (meio), Linha 0 (baixo)
            float y = invStartY + ((2 - row) * slotWidth);

            slot.setPosition(x, y);
        }
    }

    private void centerCrosshair() {
        SimpleApplication sapp = (SimpleApplication) getApplication();
        int w = sapp.getCamera().getWidth();
        int h = sapp.getCamera().getHeight();
        float x = (w - crosshair.getLineWidth()) / 2f;
        float y = (h + crosshair.getLineHeight()) / 2f;
        crosshair.setLocalTranslation(x, y, 0);
    }

    @Override
    public void update(float tpf) {
        // keep centered (cheap)
        centerCrosshair();
        refreshLayout();
    }

    @Override
    protected void cleanup(Application app) {
        if (crosshair != null) crosshair.removeFromParent();

        // Remover todos os slots
        for (Picture slot : hotbarSlots) {
            slot.removeFromParent();
        }
        hotbarSlots.clear();

        if (selector != null) selector.removeFromParent();

        // Remover corações
        for (Picture p : hearts) {
            p.removeFromParent();
        }
        hearts.clear();
        inventoryNode.removeFromParent();
        inventoryNode.detachAllChildren();
    }



    @Override
    protected void onEnable() { }

    @Override
    protected void onDisable() { }
}

