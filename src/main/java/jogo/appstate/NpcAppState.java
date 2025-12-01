package jogo.appstate;

import com.jme3.app.Application;
import com.jme3.app.state.BaseAppState;
import com.jme3.bullet.PhysicsSpace;
import com.jme3.bullet.control.BetterCharacterControl;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import jogo.engine.GameRegistry;
import jogo.gameobject.GameObject;
import jogo.gameobject.character.Character;
import jogo.gameobject.character.Player;
import jogo.gameobject.character.Sheep;
import jogo.gameobject.character.Zombie;
import jogo.gameobject.character.Wolf;
import jogo.gameobject.character.Trader;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class NpcAppState extends BaseAppState {

    private final Node rootNode;
    private final PhysicsSpace physicsSpace;
    private final GameRegistry registry;
    private final PlayerAppState playerState;

    private final Map<Character, BetterCharacterControl> npcControls = new HashMap<>();
    private final Node npcPhysicsNode = new Node("NpcPhysics");

    // Mapa para guardar cooldowns de ataque dos inimigos (NPC -> Tempo restante)
    private final Map<Character, Float> attackCooldowns = new HashMap<>();

    // Mapa para gerir respawn (NPC -> Tempo até nascer)
    private final Map<Character, Float> respawnTimers = new HashMap<>();
    private final float RESPAWN_TIME = 30.0f;

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

        // --- 0. GESTÃO DE RESPAWN (Lógica de Renascer) ---
        Iterator<Map.Entry<Character, Float>> it = respawnTimers.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Character, Float> entry = it.next();
            Character npc = entry.getKey();
            float timeLeft = entry.getValue() - tpf;

            if (timeLeft <= 0) {
                // HORA DE RENASCER!
                npc.respawn();

                // Reativar física e visual
                if (npcControls.containsKey(npc)) {
                    BetterCharacterControl ctrl = npcControls.get(npc);
                    ctrl.setEnabled(true);
                    ctrl.getSpatial().setCullHint(Spatial.CullHint.Inherit); // Mostrar

                    ctrl.setGravity(new Vector3f(0, -20f, 0));

                    // Teleportar para o ar para não ficar preso
                    ctrl.warp(new Vector3f(npc.getPosition().x, 26, npc.getPosition().z));
                }
                it.remove(); // Remove do timer
            } else {
                entry.setValue(timeLeft); // Atualiza tempo
            }
        }

        // --- SISTEMA DE GPS (A CADA 5 SEGUNDOS) ---
        gpsTimer += tpf;
        if (gpsTimer >= 5.0f) {
            gpsTimer = 0f;
            var pPos = player.getPosition();
            System.out.println("\n===== GPS =====");
            System.out.println("JOGADOR: " + String.format("%.1f, %.1f, %.1f", pPos.x, pPos.y, pPos.z));
            // Podes descomentar o loop abaixo para ver NPCs também
            for (Character npc : npcControls.keySet()) {
                 if(!npc.isDead()) System.out.println(npc.getName() + ": " + npc.getPosition().x + ", " + npc.getPosition().y + ", " + npc.getPosition().z);
            }
        }

        // 1. Detetar NOVOS NPCs
        for (GameObject obj : registry.getAll()) {
            if (obj instanceof Character npc && !(obj instanceof Player)) {
                if (!npcControls.containsKey(npc)) {
                    createPhysicsFor(npc);
                }
            }
        }

        // 2. Atualizar IA de TODOS os NPCs
        // Usar new Vector3f() para converter o Vec3 do jogador para JME Vector3f
        Vector3f playerPos = new Vector3f(player.getPosition().x, player.getPosition().y, player.getPosition().z);

        for (Map.Entry<Character, BetterCharacterControl> entry : npcControls.entrySet()) {
            Character npc = entry.getKey();
            BetterCharacterControl control = entry.getValue();

            // --- VERIFICAÇÃO DE MORTE ---
            // Se estiver morto, ignoramos movimento e escondemos
            if (npc.isDead()) {
                if (!respawnTimers.containsKey(npc)) {
                    // Morreu neste frame
                    respawnTimers.put(npc, RESPAWN_TIME);
                    control.setEnabled(false); // Desliga física
                    control.getSpatial().setCullHint(Spatial.CullHint.Always); // Esconde visual
                }
                continue; // Salta para o próximo NPC
            }

            // --- LÓGICA DE VIDA (IA) ---
            Vector3f npcPos = control.getSpatial().getWorldTranslation();

            // Atualizar cooldown de ataque
            float atkCd = attackCooldowns.getOrDefault(npc, 0f);
            if (atkCd > 0) attackCooldowns.put(npc, atkCd - tpf);

            // TRADER (Passivo e Parado)
            if (npc instanceof Trader) {
                control.setWalkDirection(Vector3f.ZERO);
                npc.setPosition(npcPos.x, npcPos.y, npcPos.z);
                continue;
            }

            Vector3f dir = playerPos.subtract(npcPos);
            float distance = dir.length();
            dir.y = 0;

            // INIMIGOS (Zombie e Lobo) - Atacam se estiverem perto
            boolean isHostile = (npc instanceof Zombie || npc instanceof Wolf);

            // --- LÓGICA DE ATAQUE ---
            if (isHostile && distance < 1.5f) {
                // Perto do jogador: Para e morde
                control.setWalkDirection(Vector3f.ZERO);
                control.setViewDirection(dir);

                if (atkCd <= 0) {
                    // DANO AO JOGADOR
                    playerState.takeDamage(10);

                    attackCooldowns.put(npc, 1.5f); // Espera 1.5s
                    System.out.println("Cuidado! " + npc.getName() + " atacou-te!");
                }
            }
            // --- LÓGICA DE SEGUIR ---
            else if (distance > 1.5f && distance < 25.0f) {
                dir.normalizeLocal();

                // AUTO-JUMP
                Vector3f scanOrigin = npcPos.add(0, 0.5f, 0);
                Vector3f scanTarget = scanOrigin.add(dir.mult(1.0f)); // Ajustado para 1.0f
                var results = physicsSpace.rayTest(scanOrigin, scanTarget);

                // Só salta se estiver longe do jogador (> 4m) para não saltar no ataque
                if (results.size() > 0 && control.isOnGround() && distance > 4.0f) {
                    control.jump();
                }

                float speed = 1.8f;
                if (npc instanceof Zombie) speed = 2.2f;
                if (npc instanceof Wolf) speed = 3.5f;

                control.setWalkDirection(dir.mult(speed));
                control.setViewDirection(dir);

            } else {
                // Longe demais ou Trader: Parar
                control.setWalkDirection(Vector3f.ZERO);
            }

            // Sincronizar Posição Lógica
            npc.setPosition(npcPos.x, npcPos.y, npcPos.z);
        }
    }

    private void createPhysicsFor(Character npc) {
        System.out.println("Criando física para NPC: " + npc.getName());

        float radius = 0.4f;
        float height = 0.9f;
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