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

// --- IMPORTS PARA O BRILHO (BLOOM) ---
import com.jme3.post.FilterPostProcessor;
import com.jme3.post.filters.BloomFilter;
// -------------------------------------

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

        // --- ADICIONAR EFEITO DE BRILHO (BLOOM) AQUI ---
        FilterPostProcessor fpp = new FilterPostProcessor(assetManager);

        // GlowMode.Objects faz com que apenas materiais com "GlowMap" ou "GlowColor" brilhem
        BloomFilter bloom = new BloomFilter(BloomFilter.GlowMode.Objects);
        bloom.setBloomIntensity(2.0f); // Força do brilho
        bloom.setExposurePower(5.0f);  // O quão longe a luz se espalha

        fpp.addFilter(bloom);
        app.getViewPort().addProcessor(fpp);
        // -----------------------------------------------
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
            // --- NOVO: Verificar se está morto ---
            if (obj instanceof jogo.gameobject.character.Character c) {
                if (c.isDead()) {
                    // Se estiver morto, esconde o modelo
                    s.setCullHint(Spatial.CullHint.Always);
                } else {
                    // Se estiver vivo, mostra o modelo
                    s.setCullHint(Spatial.CullHint.Inherit);
                }
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
            // Criar um NÓ para a ovelha
            Node sheepNode = new Node("SheepVisual");

            try {
                Spatial model = assetManager.loadModel("Models/SheepNPC/source/sheep.gltf");
                model.setName(obj.getName());
                model.setLocalScale(1.0f);
                sheepNode.attachChild(model);
            } catch (Exception e) {
                System.out.println("Erro ao carregar modelo: " + e.getMessage());
                Geometry g = new Geometry(obj.getName(), new Box(0.3f, 0.3f, 0.5f));
                g.setMaterial(colored(ColorRGBA.White));
                sheepNode.attachChild(g);
            }
            return sheepNode;

        } else if (obj instanceof jogo.gameobject.character.Zombie) {
            Node zombieNode = new Node("ZombieVisual");
            try {
                Spatial model = assetManager.loadModel("Models/zombieNPC/source/model.gltf");
                model.setName(obj.getName());
                model.setLocalScale(1.0f);
                zombieNode.attachChild(model);
            } catch (Exception e) {
                System.out.println("Erro ao carregar Zombie: " + e.getMessage());
                Geometry g = new Geometry(obj.getName(), new Box(0.4f, 0.9f, 0.4f));
                g.setMaterial(colored(ColorRGBA.Green));
                zombieNode.attachChild(g);
            }
            return zombieNode;
        } else if (obj instanceof jogo.gameobject.character.Wolf) {
            Node wolfNode = new Node("WolfVisual");
            try {
                Spatial model = assetManager.loadModel("Models/wolfNPC/wolf.gltf");
                model.setName(obj.getName());
                model.setLocalScale(0.075f);
                model.setLocalTranslation(0, 0.6f, 0);
                wolfNode.attachChild(model);
            } catch (Exception e) {
                System.out.println("Erro ao carregar Lobo: " + e.getMessage());
                Geometry g = new Geometry(obj.getName(), new Box(0.4f, 0.4f, 0.8f));
                g.setMaterial(colored(ColorRGBA.Gray));
                wolfNode.attachChild(g);
            }
            return wolfNode;
        } else if (obj instanceof jogo.gameobject.character.Trader) {
            Node traderNode = new Node("TraderVisual");
            try {
                Spatial model = assetManager.loadModel("Models/traderNPC/trader.gltf");
                model.setName(obj.getName());
                model.setLocalScale(1.3f);
                traderNode.attachChild(model);
            } catch (Exception e) {
                System.out.println("Erro Trader: " + e.getMessage());
                Geometry g = new Geometry(obj.getName(), new Box(0.4f, 0.9f, 0.4f));
                g.setMaterial(colored(ColorRGBA.Blue));
                traderNode.attachChild(g);
            }
            return traderNode;
        }
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