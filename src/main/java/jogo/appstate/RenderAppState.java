package jogo.appstate;

import com.jme3.app.Application;
import com.jme3.app.state.BaseAppState;
import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Box;
import com.jme3.scene.shape.Cylinder;
import jogo.engine.GameRegistry;
import jogo.engine.RenderIndex;
import jogo.framework.math.Vec3;
import jogo.gameobject.GameObject;
import jogo.gameobject.character.Player;
import jogo.gameobject.item.Item;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class RenderAppState extends BaseAppState {

    private final Node rootNode;
    private final AssetManager assetManager;
    private final GameRegistry registry;
    private final RenderIndex renderIndex;

    private Node gameNode;
    private final Map<GameObject, Spatial> instances = new HashMap<>();

    public RenderAppState(Node rootNode, AssetManager assetManager, GameRegistry registry, RenderIndex renderIndex) {
        this.rootNode = rootNode;
        this.assetManager = assetManager;
        this.registry = registry;
        this.renderIndex = renderIndex;
    }

    @Override
    protected void initialize(Application app) {
        gameNode = new Node("GameObjects");
        rootNode.attachChild(gameNode);
    }

    @Override
    public void update(float tpf) {
        // Ensure each registered object has a spatial and sync position
        var current = registry.getAll();
        Set<GameObject> alive = new HashSet<>(current);

        for (GameObject obj : current) {
            Spatial s = instances.get(obj);
            if (s == null) {
                s = createSpatialFor(obj);
                if (s != null) {
                    gameNode.attachChild(s);
                    instances.put(obj, s);
                    renderIndex.register(s, obj);
                }
            }
            if (s != null) {
                Vec3 p = obj.getPosition();
                s.setLocalTranslation(new Vector3f(p.x, p.y, p.z));
            }
        }

        // Cleanup: remove spatials for objects no longer in registry
        var it = instances.entrySet().iterator();
        while (it.hasNext()) {
            var e = it.next();
            if (!alive.contains(e.getKey())) {
                Spatial s = e.getValue();
                renderIndex.unregister(s);
                if (s.getParent() != null) s.removeFromParent();
                it.remove();
            }
        }
    }

    private Spatial createSpatialFor(GameObject obj) {
        //TODO This could be set inside each GameObject!
        if (obj instanceof Player) {
            Geometry g = new Geometry(obj.getName(), new Cylinder(16, 16, 0.35f, 1.4f, true));
            g.setMaterial(colored(ColorRGBA.Green));
            return g;
        } else if (obj instanceof Item) {
            Geometry g = new Geometry(obj.getName(), new Box(0.3f, 0.3f, 0.3f));
            g.setMaterial(colored(ColorRGBA.Yellow));
            return g;
            // --- CÓDIGO NOVO PARA O MODELO ---
        } else if (obj instanceof jogo.gameobject.character.Sheep) {
            // Criar um NÓ para podermos ter o modelo + debug juntos
            Node sheepNode = new Node("SheepVisual");

            try {
                // --- 1. Tenta carregar o modelo (CONFIRMA O CAMINHO) ---
                // Nota: Windows costuma ignorar maiúsculas, mas Java/Linux não!
                // Verifica se é "Sheep.gltf" ou "sheep.gltf" na pasta.
                Spatial model = assetManager.loadModel("Models/SheepNPC/source/sheep.gltf");

                // --- 2. Tenta várias escalas (Descomenta uma se não vires) ---
                model.setLocalScale(1.5f); // Tenta aumentar um pouco
                // model.setLocalScale(10.0f); // Tenta muito grande
                // model.setLocalScale(0.1f);  // Tenta muito pequeno

                sheepNode.attachChild(model);
                System.out.println("SUCESSO: Modelo da ovelha carregado!");

            } catch (Exception e) {
                System.out.println("ERRO CRÍTICO: Não encontrei o ficheiro! " + e.getMessage());
                e.printStackTrace(); // Mostra o erro todo na consola
            }

            // --- 3. CAIXA DE DEBUG (Vermelha) ---
            // Isto vai aparecer SEMPRE, mesmo que o modelo falhe.
            // Se vires isto a mexer, a lógica funciona.
            Geometry debugBox = new Geometry("DebugBox", new Box(0.2f, 0.8f, 0.2f));
            Material mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
            mat.setColor("Color", ColorRGBA.Red);
            debugBox.setMaterial(mat);
            debugBox.setLocalTranslation(0, 1.0f, 0); // Um pouco acima do chão

            sheepNode.attachChild(debugBox);
            // ------------------------------------

            return sheepNode;
        }

        // ---------------------------------

        return null;
    }

    private Material colored(ColorRGBA color) {
        Material m = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
        m.setBoolean("UseMaterialColors", true);
        m.setColor("Diffuse", color.clone());
        m.setColor("Specular", ColorRGBA.White.mult(0.1f));
        m.setFloat("Shininess", 8f);
        return m;
    }

    @Override
    protected void cleanup(Application app) {
        if (gameNode != null) {
            gameNode.removeFromParent();
            gameNode = null;
        }
        instances.clear();
    }

    @Override
    protected void onEnable() { }

    @Override
    protected void onDisable() { }
}
