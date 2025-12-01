package jogo.appstate;

import com.jme3.app.Application;
import com.jme3.app.state.BaseAppState;
import com.jme3.bullet.PhysicsSpace;
import com.jme3.bullet.control.BetterCharacterControl;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import jogo.engine.GameRegistry;
import jogo.gameobject.GameObject;
import jogo.gameobject.character.Character;
import jogo.gameobject.character.Player;
import jogo.gameobject.character.Sheep;
import jogo.gameobject.character.Zombie;
import jogo.gameobject.character.Wolf;
import jogo.gameobject.character.Trader;

import java.util.HashMap;
import java.util.Map;

public class NpcAppState extends BaseAppState {

    private final Node rootNode;
    private final PhysicsSpace physicsSpace;
    private final GameRegistry registry;
    private final PlayerAppState playerState;

    private final Map<Character, BetterCharacterControl> npcControls = new HashMap<>();
    private final Node npcPhysicsNode = new Node("NpcPhysics");

    // Variável para o temporizador do GPS
    private float gpsTimer = 0f;

    public NpcAppState(Node rootNode, PhysicsSpace physicsSpace, GameRegistry registry, PlayerAppState playerState) {
        this.rootNode = rootNode;
        this.physicsSpace = physicsSpace;
        this.registry = registry;
        this.playerState = playerState;
    }

    @Override
    protected void initialize(Application app) {
        rootNode.attachChild(npcPhysicsNode);
    }

    @Override
    public void update(float tpf) {
        Player player = playerState.getPlayer();
        if (player == null) return;

        // --- SISTEMA DE GPS (A CADA 5 SEGUNDOS) ---
        gpsTimer += tpf;
        if (gpsTimer >= 5.0f) {
            gpsTimer = 0f; // Reiniciar temporizador

            System.out.println("\n===== GPS REPORT (" + String.format("%.2f", getApplication().getTimer().getTimeInSeconds()) + "s) =====");

            // Posição do Jogador
            var pPos = player.getPosition();
            System.out.println("JOGADOR : X=" + String.format("%.1f", pPos.x) +
                    " Y=" + String.format("%.1f", pPos.y) +
                    " Z=" + String.format("%.1f", pPos.z));

            // Posição de todos os NPCs (Ovelhas e Zombies)
            if (npcControls.isEmpty()) {
                System.out.println("NPCs    : Nenhum encontrado.");
            } else {
                for (Character npc : npcControls.keySet()) {
                    var nPos = npc.getPosition();
                    String nome = npc.getName();
                    // Ajustar espaçamento para ficar bonito no log
                    String padding = nome.length() < 7 ? " " : "";

                    System.out.println(nome.toUpperCase() + padding + " : X=" + String.format("%.1f", nPos.x) +
                            " Y=" + String.format("%.1f", nPos.y) +
                            " Z=" + String.format("%.1f", nPos.z));
                }
            }
            System.out.println("=====================================");
        }
        // ------------------------------------------

        // 1. Detetar NOVOS NPCs
        for (GameObject obj : registry.getAll()) {
            if (obj instanceof Character npc && !(obj instanceof Player)) {
                if (!npcControls.containsKey(npc)) {
                    createPhysicsFor(npc);
                }
            }
        }

        // 2. Atualizar IA de TODOS os NPCs
        var p = playerState.getPlayer().getPosition();
        Vector3f playerPos = new Vector3f(p.x, p.y, p.z);

        for (Map.Entry<Character, BetterCharacterControl> entry : npcControls.entrySet()) {
            Character npc = entry.getKey();
            BetterCharacterControl control = entry.getValue();

            // Obter posição física atual
            Vector3f npcPos = control.getSpatial().getWorldTranslation();
            // --- NOVO: Lógica Específica para o Trader ---
            if (npc instanceof Trader) {
                // Forçar a ficar parado
                control.setWalkDirection(Vector3f.ZERO);

                // (Opcional) Fazer com que olhe para o jogador, mas sem andar:
                // Vector3f lookDir = playerPos.subtract(npcPos);
                // lookDir.y = 0; lookDir.normalizeLocal();
                // control.setViewDirection(lookDir);

                // Sincronizar posição e saltar para o próximo NPC
                npc.setPosition(npcPos.x, npcPos.y, npcPos.z);
                continue;
            }

            Vector3f dir = playerPos.subtract(npcPos);
            float distance = dir.length();
            dir.y = 0;

            // Lógica de Seguir
            if (distance > 1.5f && distance < 25.0f) {
                dir.normalizeLocal();

                // AUTO-JUMP
                Vector3f scanOrigin = npcPos.add(0, 0.5f, 0);
                Vector3f scanTarget = scanOrigin.add(dir.mult(1.3f));
                var results = physicsSpace.rayTest(scanOrigin, scanTarget);

                if (results.size() > 0 && control.isOnGround() && distance > 3.0f) {
                    control.jump();
                }

                // Velocidade
                float speed = 1.8f;
                if (npc instanceof Zombie) speed = 2.2f;
                if (npc instanceof Wolf) speed = 3.5f;

                control.setWalkDirection(dir.mult(speed));
                control.setViewDirection(dir);

            } else {
                control.setWalkDirection(Vector3f.ZERO);
            }

            // Sincronizar Posição Lógica
            npc.setPosition(npcPos.x, npcPos.y, npcPos.z);
        }
    }

    private void createPhysicsFor(Character npc) {
        // (Este método mantém-se igual ao que tinhas, com a correção da altura)
        System.out.println("Criando física para NPC: " + npc.getName());

        float radius = 0.4f;
        float height = 0.9f; // Altura corrigida para evitar crash
        if (npc instanceof Zombie || npc instanceof Trader) {
            height = 1.8f;
        }


        BetterCharacterControl control = new BetterCharacterControl(radius, height, 30f);
        control.setGravity(new Vector3f(0, -20f, 0));

        Node node = new Node(npc.getName() + "_Phys");
        node.addControl(control);
        npcPhysicsNode.attachChild(node);

        physicsSpace.add(control);
        control.warp(new Vector3f(npc.getPosition().x, npc.getPosition().y, npc.getPosition().z));

        npcControls.put(npc, control);
    }

    @Override
    protected void cleanup(Application app) {
        npcPhysicsNode.removeFromParent();
        for (BetterCharacterControl c : npcControls.values()) physicsSpace.remove(c);
        npcControls.clear();
    }

    @Override protected void onEnable() {}
    @Override protected void onDisable() {}
}