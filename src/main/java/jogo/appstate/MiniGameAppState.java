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

// AppState que gere o minijogo de acertar nos blocos de alvo
public class MiniGameAppState extends BaseAppState {

    private WorldAppState worldState;
    private HudAppState hudState;
    private HighScoreManager scoreManager;

    // Estado do minijogo
    private boolean gameRunning = false; // Se o minijogo está a decorrer
    private float timer = 0f;  // Tempo decorrido
    private int targetsHit = 0; // Alvos acertados
    private final int TOTAL_TARGETS = 20; // Total de alvos a acertar

    @Override
    protected void initialize(Application app) {
        this.worldState = getState(WorldAppState.class);
        this.hudState = getState(HudAppState.class);

        // Cria o gestor de High Scores
        this.scoreManager = new HighScoreManager();

        // Começa o jogo automaticamente
        startGame();
    }

    public void startGame() {
        gameRunning = true;
        timer = 0f;
        targetsHit = 0;
        System.out.println("Desafio Iniciado!");
    }

    @Override
    public void update(float tpf) {
        if (!gameRunning) return; // Se o jogo acabou ou pausou, não faz nada

        timer += tpf; // Aumenta o cronómetro (tpf = tempo desde o último frame)

        // Atualizar HUD (vamos criar este método já a seguir)
        if (hudState != null) {
            hudState.updateMiniGameInfo(timer, targetsHit, TOTAL_TARGETS);
        }

        // Verificar colisões entre itens largados e alvos
        checkForHits();

        // Verificar Vitória
        if (targetsHit >= TOTAL_TARGETS) {
            finishGame();
        }
    }

    // Verificar colisões entre itens largados e alvos
    private void checkForHits() {
        VoxelWorld vw = worldState.getVoxelWorld();
        List<DroppedItem> items = worldState.getDroppedItems(); // Lista de todos os itens no chão/ar
        List<DroppedItem> toRemove = new ArrayList<>(); // Lista temporária para apagar itens usados

        for (DroppedItem item : items) {
            Vector3f pos = item.getNode().getWorldTranslation(); // Posição do item no mundo

            // Verificar o bloco onde o item está, e os vizinhos imediatos,
            // para facilitar a colisão dado que o item pode estar "dentro" ou "ao lado"
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

    // Verifica se há um alvo na posição dada e o quebra se houver
    private boolean checkAndBreakTarget(VoxelWorld vw, float fx, float fy, float fz) {
        // Converte coordenadas float (mundo) para coordenadas int (grelha de vóxeis)
        int x = (int) Math.floor(fx);
        int y = (int) Math.floor(fy);
        int z = (int) Math.floor(fz);

        // Verifica se o bloco nessa posição é um alvo
        byte id = vw.getBlock(x, y, z);
        if (id == VoxelPalette.TARGET_ID) {
            // Acertou!
            vw.setBlock(x, y, z, VoxelPalette.AIR_ID); // Remove o alvo
            vw.rebuildDirtyChunks(worldState.getPhysicsSpace());
            targetsHit++; // Incrementa o contador de alvos acertados
            System.out.println("Alvo destruído! " + targetsHit + "/" + TOTAL_TARGETS);
            return true;
        }
        return false;
    }


    private void finishGame() {
        gameRunning = false;
        System.out.println("JOGO TERMINADO! Tempo: " + timer);

        // Guardar High Score
        scoreManager.addScore("Player", timer);

        // Mostrar High Scores no HUD por 15 segundos
        if (hudState != null) {
            String msg = "DESAFIO COMPLETADO!\nTempo Final: " + String.format("%.2f", timer) + "s";
            // Passamos 15.0f como duração
            hudState.showLeaderboard(scoreManager.getTopScores(), 15.0f, msg);
        }
    }

    // Chamado pelo PlayerAppState quando é feito Save
    public void saveStateToData(jogo.system.GameSaveData data) {
        data.miniGameTimer = this.timer;
        data.miniGameTargetsHit = this.targetsHit;
        data.miniGameRunning = this.gameRunning;
    }

    // Chamado quando é feito Load
    public void loadStateFromData(jogo.system.GameSaveData data) {
        this.timer = data.miniGameTimer;
        this.targetsHit = data.miniGameTargetsHit;
        this.gameRunning = data.miniGameRunning;

        // Se o jogo estava a correr ou terminado, atualiza o HUD
        // para não mostrar valores incorretamente após o load
        if (hudState != null) {
            hudState.updateMiniGameInfo(timer, targetsHit, TOTAL_TARGETS);
        }

        System.out.println("Minijogo carregado: " + targetsHit + " alvos, " + timer + "s");
    }

    @Override protected void cleanup(Application app) {}
    @Override protected void onEnable() {}
    @Override protected void onDisable() {}
}