// java
package jogo.appstate;

import com.jme3.app.Application;
import com.jme3.app.state.BaseAppState;
import com.jme3.bullet.PhysicsSpace;
import com.jme3.bullet.control.BetterCharacterControl;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import jogo.engine.GameRegistry;
import jogo.gameobject.GameObject;
import jogo.gameobject.character.Player;
import jogo.gameobject.character.Sheep;
import com.jme3.asset.AssetManager;
import com.jme3.scene.Geometry;
import com.jme3.scene.shape.Sphere;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;


import java.util.HashMap;
import java.util.Map;

public class NpcAppState extends BaseAppState {

    private final Node rootNode;
    private final PhysicsSpace physicsSpace;
    private final GameRegistry registry;
    private final PlayerAppState playerState;

    // Guarda o controlo físico de cada ovelha
    private final Map<Sheep, BetterCharacterControl> npcControls = new HashMap<>();
    private final Node npcPhysicsNode = new Node("NpcPhysics");

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

        // --- DEBUG: Localizador GPS (Podes apagar depois) ---
        // Imprime a cada ~60 frames para não "entupir" a consola
        if (System.currentTimeMillis() % 1000 < 20) {
            Vector3f pPos = new Vector3f(
                    player.getPosition().x,
                    player.getPosition().y,
                    player.getPosition().z
            );
            System.out.println(">>> GPS JOGADOR: " + pPos);

            for (Sheep s : npcControls.keySet()) {
                System.out.println(">>> GPS OVELHA : " + s.getPosition().x + ", " + s.getPosition().y + ", " + s.getPosition().z);
            }
        }

        // 1. Verificar se há novas ovelhas no registo para criar física
        for (GameObject obj : registry.getAll()) {
            if (obj instanceof Sheep sheep && !npcControls.containsKey(sheep)) {
                createPhysicsFor(sheep);
            }
        }

        // 2. Atualizar inteligência de cada ovelha
        // Constrói Vector3f manualmente a partir do objecto de posição do Player
        Vector3f playerPos = new Vector3f(
                player.getPosition().x,
                player.getPosition().y,
                player.getPosition().z
        );

        for (Map.Entry<Sheep, BetterCharacterControl> entry : npcControls.entrySet()) {
            Sheep sheep = entry.getKey();
            BetterCharacterControl control = entry.getValue();

            // --- IA: Seguir Jogador ---
            Vector3f npcPos;
            if (control.getSpatial() != null) {
                npcPos = control.getSpatial().getWorldTranslation();
            } else {
                // Spatial ainda não associado; usar posição zero temporária
                npcPos = new Vector3f();
            }

            Vector3f dir = playerPos.subtract(npcPos);
            float distance = dir.length();
            dir.y = 0; // Não voar, apenas andar no plano

            if (distance > 2.0f && distance < 20.0f) { // Segue se estiver perto (mas não colado)
                dir.normalizeLocal().multLocal(3.0f); // Velocidade 3
                control.setWalkDirection(dir);
                control.setViewDirection(dir);
            } else {
                control.setWalkDirection(Vector3f.ZERO); // Parar
            }

            // --- IMPORTANTE: Sincronizar Posição Lógica ---
            // Atualizamos o objeto Sheep para o RenderAppState saber onde desenhar
            sheep.setPosition(npcPos.x, npcPos.y, npcPos.z);
        }
    }

    private void createPhysicsFor(Sheep sheep) {
        System.out.println("--- DEBUG NPC ---");
        System.out.println("Nome: " + sheep.getName());
        System.out.println("Posição Lógica (Antes da Física): " + sheep.getPosition().x + ", " + sheep.getPosition().y + ", " + sheep.getPosition().z);

        // Evitar duplicados por segurança
        if (npcControls.containsKey(sheep)) {
            System.out.println("createPhysicsFor: control já existe para " + sheep.getName());
            return;
        }

        float radius = 0.4f;
        float desiredHeight = 1.0f; // deve ser maior que 2 * radius (0.8f)
        float minHeight = 2f * radius + 0.01f;
        float height = Math.max(desiredHeight, minHeight);

        System.out.println(String.format("Criando BetterCharacterControl para %s — radius=%.3f, height=%.3f (mínimo=%.3f)",
                sheep.getName(), radius, height, minHeight));


        // Criar corpo físico
        BetterCharacterControl control;
        try {
            control = new BetterCharacterControl(radius, height, 30f);
        } catch (IllegalArgumentException e) {
            // fallback seguro: ajusta height e tenta outra vez
            height = minHeight;
            System.out.println("Fallback: ajustando height para " + height + " devido a: " + e.getMessage());
            control = new BetterCharacterControl(radius, height, 30f);
        }
        control.setGravity(new Vector3f(0, -20f, 0));

        // Adicionar o controlo ao Node
        Node node = new Node(sheep.getName() + "_Phys");
        node.addControl(control);
        npcPhysicsNode.attachChild(node);

        // IMPORTANTE: Adicionar ao PhysicsSpace ANTES de fazer warp
        physicsSpace.add(control);

        // Agora sim, forçar a posição
        control.warp(new Vector3f(sheep.getPosition().x, sheep.getPosition().y, sheep.getPosition().z));

        npcControls.put(sheep, control);
        System.out.println("Física criada e Warped!");
        System.out.println("-----------------");
    }

    @Override
    protected void cleanup(Application app) {
        npcPhysicsNode.removeFromParent();
        for (BetterCharacterControl c : npcControls.values()) {
            physicsSpace.remove(c);
        }
        npcControls.clear();
    }

    @Override protected void onEnable() {}
    @Override protected void onDisable() {}
}