package jogo.appstate;

import com.jme3.app.Application;
import com.jme3.app.state.BaseAppState;
import com.jme3.math.Vector3f;
import jogo.gameobject.item.DroppedItem;
import jogo.system.HighScoreManager;
import jogo.voxel.VoxelPalette;
import jogo.voxel.VoxelWorld;

import java.util.ArrayList;
import java.util.List;

public class MiniGameAppState extends BaseAppState {

    private WorldAppState worldState;
    private HudAppState hudState;
    private HighScoreManager scoreManager;

    private boolean gameRunning = false;
    private float timer = 0f;
    private int targetsHit = 0;
    private final int TOTAL_TARGETS = 20; // Tem de bater certo com a geração

    @Override
    protected void initialize(Application app) {
        this.worldState = getState(WorldAppState.class);
        this.hudState = getState(HudAppState.class);
        this.scoreManager = new HighScoreManager();

        // Começa o jogo automaticamente
        startGame();
    }

    public void startGame() {
        gameRunning = true;
        timer = 0f;
        targetsHit = 0;
        System.out.println("Desafio Iniciado! Acerta nos blocos vermelhos!");
    }

    @Override
    public void update(float tpf) {
        if (!gameRunning) return;

        timer += tpf;

        // Atualizar HUD (vamos criar este método já a seguir)
        if (hudState != null) {
            hudState.updateMiniGameInfo(timer, targetsHit, TOTAL_TARGETS);
        }

        checkForHits();

        // Verificar Vitória
        if (targetsHit >= TOTAL_TARGETS) {
            finishGame();
        }
    }

    private void checkForHits() {
        VoxelWorld vw = worldState.getVoxelWorld();
        List<DroppedItem> items = worldState.getDroppedItems();
        List<DroppedItem> toRemove = new ArrayList<>();

        for (DroppedItem item : items) {
            Vector3f pos = item.getNode().getWorldTranslation();

            // Verificar o bloco onde o item está, e os vizinhos imediatos
            // (para facilitar a colisão dado que o item pode estar "dentro" ou "ao lado")
            if (checkAndBreakTarget(vw, pos.x, pos.y, pos.z) ||
                    checkAndBreakTarget(vw, pos.x + 0.5f, pos.y, pos.z) ||
                    checkAndBreakTarget(vw, pos.x - 0.5f, pos.y, pos.z) ||
                    checkAndBreakTarget(vw, pos.x, pos.y + 0.5f, pos.z) ||
                    checkAndBreakTarget(vw, pos.x, pos.y - 0.5f, pos.z) ||
                    checkAndBreakTarget(vw, pos.x, pos.y, pos.z + 0.5f) ||
                    checkAndBreakTarget(vw, pos.x, pos.y, pos.z - 0.5f)) {

                // Se acertou, removemos o item também
                toRemove.add(item);
            }
        }

        // Limpar itens que acertaram
        for (DroppedItem i : toRemove) {
            worldState.removeDroppedItem(i);
        }
    }

    private boolean checkAndBreakTarget(VoxelWorld vw, float fx, float fy, float fz) {
        int x = (int) Math.floor(fx);
        int y = (int) Math.floor(fy);
        int z = (int) Math.floor(fz);

        byte id = vw.getBlock(x, y, z);
        if (id == VoxelPalette.TARGET_ID) {
            // Acertou!
            vw.setBlock(x, y, z, VoxelPalette.AIR_ID); // Remove o alvo
            vw.rebuildDirtyChunks(worldState.getPhysicsSpace());
            targetsHit++;
            System.out.println("Alvo destruído! " + targetsHit + "/" + TOTAL_TARGETS);
            return true;
        }
        return false;
    }

    // ... código existente ...

    private void finishGame() {
        gameRunning = false;
        System.out.println("JOGO TERMINADO! Tempo: " + timer);

        // Guardar High Score
        scoreManager.addScore("Player", timer);

        // Mostrar High Scores no HUD por 15 SEGUNDOS
        if (hudState != null) {
            String msg = "DESAFIO COMPLETADO!\nTempo Final: " + String.format("%.2f", timer) + "s";
            // Passamos 15.0f como duração
            hudState.showLeaderboard(scoreManager.getTopScores(), 15.0f, msg);
        }
    }

    // Chamado quando fazes Save
    public void saveStateToData(jogo.system.GameSaveData data) {
        data.miniGameTimer = this.timer;
        data.miniGameTargetsHit = this.targetsHit;
        data.miniGameRunning = this.gameRunning;
    }

    // Chamado quando fazes Load
    public void loadStateFromData(jogo.system.GameSaveData data) {
        this.timer = data.miniGameTimer;
        this.targetsHit = data.miniGameTargetsHit;
        this.gameRunning = data.miniGameRunning;

        // Se o jogo estava a correr (ou terminado), atualizamos o HUD imediatamente
        // para não mostrar "0/20" incorretamente por um frame
        if (hudState != null) {
            hudState.updateMiniGameInfo(timer, targetsHit, TOTAL_TARGETS);
        }

        System.out.println("Minijogo carregado: " + targetsHit + " alvos, " + timer + "s");
    }

    @Override protected void cleanup(Application app) {}
    @Override protected void onEnable() {}
    @Override protected void onDisable() {}
}