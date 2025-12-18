package jogo.appstate;

import com.jme3.app.Application;
import com.jme3.app.state.BaseAppState;
import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Quaternion;
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

// AppState responsável por renderizar os GameObjects na cena (lê o GameRegistry)
public class RenderAppState extends BaseAppState {

    private final Node rootNode; // A raiz do mundo 3D
    private final AssetManager assetManager; // Para carregar modelos e materiais
    private final GameRegistry registry; // Onde os GameObjects estão registados
    private final RenderIndex renderIndex; // Para gerir a renderização

    private Node gameNode; // Nó onde os GameObjects são anexados
    private final Map<GameObject, Spatial> instances = new HashMap<>(); // Map de objetos para os seus spatials (Geometry/Node)

    public RenderAppState(Node rootNode, AssetManager assetManager, GameRegistry registry, RenderIndex renderIndex) {
        this.rootNode = rootNode;
        this.assetManager = assetManager;
        this.registry = registry;
        this.renderIndex = renderIndex;
    }


    @Override
    protected void initialize(Application app) {
        // Criar o nó principal para os objetos do jogo
        gameNode = new Node("GameObjects");
        rootNode.attachChild(gameNode);

        // --- ADICIONAR EFEITO DE BRILHO ---
        FilterPostProcessor fpp = new FilterPostProcessor(assetManager);

        // GlowMode.Objects faz com que apenas materiais com "GlowMap" ou "GlowColor" brilhem
        BloomFilter bloom = new BloomFilter(BloomFilter.GlowMode.Objects);
        bloom.setBloomIntensity(3.0f); // Força do brilho
        bloom.setExposurePower(5.0f);  // O quão longe a luz se espalha

        fpp.addFilter(bloom);
        app.getViewPort().addProcessor(fpp); // Adiciona o processador de filtros à camera
    }

    @Override
    public void update(float tpf) {
        // Atualizar ou criar spatials para cada GameObject no registry
        var current = registry.getAll();
        Set<GameObject> alive = new HashSet<>(current); // Copiar para verificar

        for (GameObject obj : current) {
            // Verificar se já existe um Spatial para este objeto
            Spatial s = instances.get(obj);

            // Se não existir, criar um novo
            if (s == null) {
                s = createSpatialFor(obj);
                if (s != null) {
                    gameNode.attachChild(s);
                    instances.put(obj, s);
                    renderIndex.register(s, obj);
                }
            }

            // Atualizar a posição do Spatial para corresponder ao GameObject
            if (s != null) {
                Vec3 p = obj.getPosition();
                s.setLocalTranslation(new Vector3f(p.x, p.y, p.z));

                // --- Rotação dos NPCs ---
                // Verifica se é um Personagem (Character) para aplicar rotação
                if (obj instanceof jogo.gameobject.character.Character c) {

                    // 1. Atualizar visibilidade (Morto ou Vivo)
                    if (c.isDead()) {
                        s.setCullHint(Spatial.CullHint.Always);
                    } else {
                        s.setCullHint(Spatial.CullHint.Inherit);
                    }

                    // 2. Aplicar a Rotação (Yaw)
                    Quaternion rotation = new Quaternion();
                    rotation.fromAngleAxis(c.getYaw(), Vector3f.UNIT_Y);
                    s.setLocalRotation(rotation);
                }
            }
        }

        // Remover spatials de objetos que já não existem
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

    // Cria um Spatial (Geometry ou Node) para o GameObject dado
    private Spatial createSpatialFor(GameObject obj) {
        // O Player continua a ser um cilindro simples
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
            // Obter o caminho do modelo 3D definido na classe do NPC
            String path = c.getModelPath();

            if (path != null) {
                // Tenta carregar o modelo definido na classe do NPC
                Node npcNode = new Node(obj.getName() + "_Visual");
                try {
                    Spatial model = assetManager.loadModel(path);
                    model.setName(obj.getName());
                    // Ajustar escala e posição conforme definido na classe do NPC
                    model.setLocalScale(c.getModelScale());
                    model.setLocalTranslation(0, c.getModelOffsetY(), 0);
                    npcNode.attachChild(model);
                    return npcNode;
                } catch (Exception e) {
                    System.out.println("Erro ao carregar modelo para " + obj.getName() + ": " + e.getMessage());
                    // Desenha uma caixa se o modelo falhar
                    Geometry g = new Geometry(obj.getName(), new Box(0.4f, 0.9f, 0.4f));
                    g.setMaterial(colored(ColorRGBA.Red));
                    return g;
                }
            }
        }

        return null;
    }

    // Cria um material colorido simples
    private Material colored(ColorRGBA color) {
        Material m = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
        m.setBoolean("UseMaterialColors", true);
        m.setColor("Diffuse", color.clone());
        m.setColor("Specular", ColorRGBA.White.mult(0.1f));
        m.setFloat("Shininess", 8f);
        return m;
    }

    //Limpa tudo do AppState
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