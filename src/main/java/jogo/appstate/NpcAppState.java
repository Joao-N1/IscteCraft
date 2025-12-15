package jogo.appstate;

import com.jme3.app.Application;
import com.jme3.app.state.BaseAppState;
import com.jme3.bullet.PhysicsSpace;
import com.jme3.bullet.control.BetterCharacterControl;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import jogo.engine.GameRegistry;
import jogo.framework.math.Vec3;
import jogo.gameobject.GameObject;
import jogo.gameobject.character.*;
import jogo.gameobject.character.Character;
import jogo.voxel.VoxelPalette;
import jogo.voxel.VoxelWorld;

import java.util.*;

// AppState que gere a lógica dos NPCs (IA, combate, respawn, etc.) e cria corpos físicos para eles
public class NpcAppState extends BaseAppState {

    //Dependências físicas e lógicas
    private final Node rootNode; // Nó raiz
    private final PhysicsSpace physicsSpace; // Mundo da física (gravidade, colisões)
    private final GameRegistry registry; // Registo global de objetos do jogo
    private final PlayerAppState playerState; // Estado do jogador

    // Mapa que liga lógica a física
    //Ovelha (Lógica) para Cilindro (física)
    private final Map<Character, BetterCharacterControl> npcControls = new HashMap<>();
    private final Node npcPhysicsNode = new Node("NpcPhysics");

    // Combate e Respawn
    private final Map<Character, Float> attackCooldowns = new HashMap<>();
    private final Map<Character, Float> respawnTimers = new HashMap<>();
    private final float RESPAWN_TIME = 30.0f;

    // Vaguear (Wandering)
    private final Map<Character, Float> wanderTimers = new HashMap<>();
    private final Map<Character, Vector3f> wanderDirections = new HashMap<>();

    private float gpsTimer = 0f;

    // Construtor com injeção de dependências
    public NpcAppState(Node rootNode, PhysicsSpace physicsSpace, GameRegistry registry, PlayerAppState playerState) {
        this.rootNode = rootNode;
        this.physicsSpace = physicsSpace;
        this.registry = registry;
        this.playerState = playerState;
    }

    // Ciclo de vida do AppState
    @Override
    protected void initialize(Application app) {
        rootNode.attachChild(npcPhysicsNode);
    }

    @Override
    public void update(float tpf) {
        Player player = playerState.getPlayer();
        if (player == null) return;

        // --- 0. GESTÃO DE RESPAWN ---
        // Percorre a lista de NPCs mortos à espera de renascer
        Iterator<Map.Entry<Character, Float>> it = respawnTimers.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Character, Float> entry = it.next();
            Character npc = entry.getKey();
            float timeLeft = entry.getValue() - tpf; // Diminui o tempo

            if (timeLeft <= 0) {
                // Renasce o NPC
                npc.respawn(); // Restaura vida e estado
                if (npcControls.containsKey(npc)) {
                    // Reativa a física
                    BetterCharacterControl ctrl = npcControls.get(npc);
                    ctrl.setEnabled(true);
                    ctrl.getSpatial().setCullHint(Spatial.CullHint.Inherit);
                    ctrl.setGravity(new Vector3f(0, -20f, 0));
                    // Coloca o NPC no ar acima da sua posição original
                    Vec3 p = npc.getPosition();
                    ctrl.warp(new Vector3f(p.x, 50f, p.z));
                }
                // Remove da lista de respawn
                it.remove();
            } else {
                entry.setValue(timeLeft);
            }
        }

        // GPS (Debug)
        gpsTimer += tpf;
        if (gpsTimer >= 10.0f) {
            gpsTimer = 0f;
            Vec3 pp = player.getPosition();
            System.out.println("\n===== NPC GPS =====");
            System.out.println("JOGADOR: " + String.format("%.1f, %.1f, %.1f", pp.x, pp.y, pp.z));

            for (Character npc : npcControls.keySet()) {
                if (!npc.isDead()) {
                    Vec3 np = npc.getPosition();
                    BetterCharacterControl ctrl = npcControls.get(npc);
                    String status = (ctrl.getVelocity().length() > 0.1f) ? "A andar" : "Parado";
                    System.out.println(npc.getName().toUpperCase() + ": " +
                            String.format("%.1f, %.1f, %.1f", np.x, np.y, np.z) + " [" + status + "]");
                }
            }
        }

        // 1. Detetar NOVOS NPCs
        // Percorrer todos os objetos do registo
        for (GameObject obj : registry.getAll()) {
            // Se for um NPC e ainda não tiver física, cria
            if (obj instanceof Character npc && !(obj instanceof Player)) {
                if (!npcControls.containsKey(npc)) {
                    createPhysicsFor(npc);
                }
            }
        }

        // 2. Atualizar IA
        Vec3 pp = player.getPosition();
        Vector3f playerPos = new Vector3f(pp.x, pp.y, pp.z);

        for (Map.Entry<Character, BetterCharacterControl> entry : npcControls.entrySet()) {
            // Se morto, ignora
            Character npc = entry.getKey();
            BetterCharacterControl control = entry.getValue();

            if (npc.isDead()) {
                if (!respawnTimers.containsKey(npc)) {
                    respawnTimers.put(npc, RESPAWN_TIME);
                    control.setEnabled(false);
                    control.getSpatial().setCullHint(Spatial.CullHint.Always);
                }
                continue;
            }

            Vector3f npcPos = control.getSpatial().getWorldTranslation();

            float atkCd = attackCooldowns.getOrDefault(npc, 0f);
            if (atkCd > 0) attackCooldowns.put(npc, atkCd - tpf);

            // TRADER (Sempre Parado)
            if (npc instanceof Trader) {
                control.setWalkDirection(Vector3f.ZERO);
                npc.setPosition(npcPos.x, npcPos.y, npcPos.z);
                continue;
            }

            // Cálculo da direção e distância ao jogador
            Vector3f dirToPlayer = playerPos.subtract(npcPos);
            float distance = dirToPlayer.length();
            dirToPlayer.y = 0;

            boolean isHostile = (npc instanceof Zombie || npc instanceof Wolf);

            // Velocidade Base
            float runSpeed = 1.8f;
            if (npc instanceof Zombie) runSpeed = 2.2f;
            if (npc instanceof Wolf) runSpeed = 3.5f;

            // Lógica de Areia (Lentidão)
            if (playerState.getWorld() != null) {
                VoxelWorld vw = playerState.getWorld().getVoxelWorld();
                if (vw != null) {
                    int nx = (int) npcPos.x;
                    int ny = (int) (npcPos.y - 0.5f);
                    int nz = (int) npcPos.z;
                    if (vw.getBlock(nx, ny, nz) == VoxelPalette.SAND_ID) {
                        runSpeed *= 0.5f;
                    }
                }
            }

            // --- ESTADO 1: ATACAR ---
            if (isHostile && distance < 1.5f) {
                // Para de andar e ataca ao olhar para o jogador
                control.setWalkDirection(Vector3f.ZERO);
                control.setViewDirection(dirToPlayer);

                if (atkCd <= 0) {
                    playerState.takeDamage(10);
                    attackCooldowns.put(npc, 1.5f);
                    System.out.println("Cuidado! " + npc.getName() + " atacou-te!");
                }
            }
            // --- ESTADO 2: PERSEGUIR ---
            else if (distance > 1.5f && distance < 25.0f) {
                // Corre atrás do jogador
                dirToPlayer.normalizeLocal();
                handleAutoJump(control, npcPos, dirToPlayer, distance);// Verifica se deve saltar
                control.setWalkDirection(dirToPlayer.mult(runSpeed));
                control.setViewDirection(dirToPlayer);

                wanderTimers.put(npc, 0f); // Reset vaguear
            }
            // --- ESTADO 3: WANDERING ---
            else {
                float wTimer = wanderTimers.getOrDefault(npc, 0f);
                wTimer -= tpf;

                if (wTimer <= 0) {
                    // Novo destino a cada 2-7 segundos
                    wTimer = 2f + FastMath.nextRandomFloat() * 5f;

                    if (FastMath.nextRandomFloat() < 0.4f) {
                        wanderDirections.put(npc, Vector3f.ZERO); // Parar
                    } else {
                        float angle = FastMath.nextRandomFloat() * FastMath.TWO_PI;
                        Vector3f randomDir = new Vector3f(FastMath.cos(angle), 0, FastMath.sin(angle));
                        wanderDirections.put(npc, randomDir);
                    }
                    wanderTimers.put(npc, wTimer);
                } else {
                    wanderTimers.put(npc, wTimer);
                }

                Vector3f wDir = wanderDirections.getOrDefault(npc, Vector3f.ZERO);

                if (wDir != Vector3f.ZERO) {
                    handleAutoJump(control, npcPos, wDir, 100f); // 100f = longe do player, pode saltar sempre
                    control.setWalkDirection(wDir.mult(runSpeed * 0.5f)); // Vagueia devagar
                    control.setViewDirection(wDir);
                } else {
                    control.setWalkDirection(Vector3f.ZERO);
                }
            }
            // Atualiza posição lógica do NPC
            npc.setPosition(npcPos.x, npcPos.y, npcPos.z);
        }
    }

    // Verifica se o NPC deve saltar automaticamente
    private void handleAutoJump(BetterCharacterControl control, Vector3f position, Vector3f direction, float distanceToPlayer) {
        Vector3f scanOrigin = position.add(0, 0.5f, 0);
        Vector3f scanTarget = scanOrigin.add(direction.mult(1.2f));

        var results = physicsSpace.rayTest(scanOrigin, scanTarget);

        // Só salta se houver obstáculo E estiver longe do jogador (> 4m)
        if (results.size() > 0 && control.isOnGround() && distanceToPlayer > 4.0f) {
            control.jump();
        }
    }

    // Cria a física para um NPC
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

        Vec3 p = npc.getPosition();
        control.warp(new Vector3f(p.x, p.y, p.z));

        npcControls.put(npc, control);
    }

    // SISTEMA DE SAVE/LOAD DE NPCs

    public void saveNpcsToData(jogo.system.GameSaveData data) {
        data.npcs.clear();
        // Percorrer todos os NPCs controlados por este estado
        for (Character npc : npcControls.keySet()) {
            if (npc.isDead()) continue; // Não guardamos mortos

            String type = "";
            if (npc instanceof Sheep) type = "Sheep";
            else if (npc instanceof Zombie) type = "Zombie";
            else if (npc instanceof Wolf) type = "Wolf";
            else if (npc instanceof Trader) type = "Trader";

            if (!type.isEmpty()) {
                Vec3 p = npc.getPosition();
                data.npcs.add(new jogo.system.GameSaveData.NpcData(
                        type, p.x, p.y, p.z, npc.getHealth()
                ));
            }
        }
    }

    public void loadNpcsFromData(jogo.system.GameSaveData data) {
        // 1. Limpar NPCs atuais
        // Criar cópia da lista para evitar erros de concorrência ao remover
        List<Character> toRemove = new ArrayList<>(npcControls.keySet());
        for (Character npc : toRemove) {
            // Remover da física
            BetterCharacterControl ctrl = npcControls.get(npc);
            if (ctrl != null) physicsSpace.remove(ctrl);

            // Remover visual e do registo do jogo
            registry.remove(npc);
        }
        npcControls.clear();
        attackCooldowns.clear();
        wanderTimers.clear();

        // 2. Recriar NPCs a partir do save
        if (data.npcs == null) return;

        for (jogo.system.GameSaveData.NpcData npcData : data.npcs) {
            Character newNpc = null;

            switch (npcData.type) {
                case "Sheep" -> newNpc = new Sheep();
                case "Zombie" -> newNpc = new Zombie();
                case "Wolf" -> newNpc = new Wolf();
                case "Trader" -> newNpc = new Trader();
            }

            if (newNpc != null) {
                newNpc.setPosition(npcData.x, npcData.y, npcData.z);
                newNpc.setHealth(npcData.health);

                // Adicionar ao registo (o NpcAppState vai detetar no próximo update e criar física)
                registry.add(newNpc);

                // Forçar criação imediata de física para não cair no vazio no primeiro frame
                createPhysicsFor(newNpc);
            }
        }
    }

    // Limpeza ao desligar o AppState
    @Override
    protected void cleanup(Application app) {
        npcPhysicsNode.removeFromParent();
        for (BetterCharacterControl c : npcControls.values()) physicsSpace.remove(c);
        npcControls.clear();
        wanderTimers.clear();
        wanderDirections.clear();
        respawnTimers.clear();
    }

    @Override protected void onEnable() {}
    @Override protected void onDisable() {}
}