package jogo.appstate;

import com.jme3.app.Application;
import com.jme3.app.state.BaseAppState;
import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.post.FilterPostProcessor;
import com.jme3.post.filters.BloomFilter;
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
        // O Player continua a ser especial (cilindro simples)
        if (obj instanceof Player) {
            Geometry g = new Geometry(obj.getName(), new Cylinder(16, 16, 0.35f, 1.4f, true));
            g.setMaterial(colored(ColorRGBA.Green));
            return g;
        }
        // Itens simples
        else if (obj instanceof Item) {
            Geometry g = new Geometry(obj.getName(), new Box(0.3f, 0.3f, 0.3f));
            g.setMaterial(colored(ColorRGBA.Yellow));
            return g;
        }
        // LÓGICA GENÉRICA PARA TODOS OS PERSONAGENS (NPCs)
        else if (obj instanceof jogo.gameobject.character.Character c) {
            String path = c.getModelPath();

            if (path != null) {
                // Tenta carregar o modelo definido na classe do NPC
                Node npcNode = new Node(obj.getName() + "_Visual");
                try {
                    Spatial model = assetManager.loadModel(path);
                    model.setName(obj.getName());
                    model.setLocalScale(c.getModelScale());
                    model.setLocalTranslation(0, c.getModelOffsetY(), 0);
                    npcNode.attachChild(model);
                    return npcNode;
                } catch (Exception e) {
                    System.out.println("Erro ao carregar modelo para " + obj.getName() + ": " + e.getMessage());
                    // Fallback: desenha uma caixa se o modelo falhar
                    Geometry g = new Geometry(obj.getName(), new Box(0.4f, 0.9f, 0.4f));
                    g.setMaterial(colored(ColorRGBA.Red));
                    return g;
                }
            }
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