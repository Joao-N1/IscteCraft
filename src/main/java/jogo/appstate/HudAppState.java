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

    //Interface do jogador
    private BitmapText crosshair;
    private Picture hotbar;
    private Picture selector;
    private List<Picture> hearts = new ArrayList<>();

    // Tamanho dos elementos da interface
    private final float HOTBAR_WIDTH = 400f;
    private final float HOTBAR_HEIGHT = 50f;
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
        Hotbar(app);
        Hearts(app);
        refreshLayout();
        System.out.println("HudAppState initialized: UI elements attached");
    }
    private void Hotbar(Application app) {
        // Criar a imagem da Hotbar
        hotbar = new Picture("Hotbar");
        // Carregar a textura (assume que o ficheiro existe)
        hotbar.setImage(assetManager, "Interface/hotbar.png", true);
        hotbar.setWidth(HOTBAR_WIDTH);
        hotbar.setHeight(HOTBAR_HEIGHT);
        guiNode.attachChild(hotbar);

        // Criar a imagem do Selector (o quadrado que se move)
        selector = new Picture("Selector");
        selector.setImage(assetManager, "Interface/selector.png", true);
        // O selector é 1/9 da largura da hotbar (assumindo 9 slots)
        float slotSize = HOTBAR_WIDTH / 9f;
        selector.setWidth(slotSize);
        selector.setHeight(HOTBAR_HEIGHT); // Ou ligeiramente maior para destaque
        guiNode.attachChild(selector);
    }
    private void Hearts(Application app) {
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

        // 2. Posicionar Hotbar (Em baixo, ao centro)
        float hotbarX = (screenW / 2f) - (HOTBAR_WIDTH / 2f);
        float hotbarY = 10f; // Margem do fundo
        hotbar.setPosition(hotbarX, hotbarY);

        // Posicionar Selector (por defeito no primeiro slot)
        // Futuramente podes mudar isto com base numa variável 'currentSlot'
        selector.setPosition(hotbarX, hotbarY);

        // 3. Posicionar Corações (Em cima da hotbar)
        float heartsStartX = hotbarX;
        float heartsY = hotbarY + HOTBAR_HEIGHT + 10f; // 10px acima da hotbar

        for (int i = 0; i < hearts.size(); i++) {
            Picture heart = hearts.get(i);
            // Espaçamento simples entre corações
            float x = heartsStartX + (i * (HEART_SIZE + 2f));
            heart.setPosition(x, heartsY);
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
        if (hotbar != null) hotbar.removeFromParent();
        if (selector != null) selector.removeFromParent();
        for (Picture p : hearts) {
            p.removeFromParent();
        }
        hearts.clear();
    }

    @Override
    protected void onEnable() { }

    @Override
    protected void onDisable() { }
}

