package jogo.engine;

import com.jme3.scene.Spatial;
import jogo.gameobject.GameObject;

import java.util.Map;
import java.util.WeakHashMap;

// Índice para mapear Spatials para GameObjects correspondentes
public class RenderIndex {
    // Usamos WeakHashMap para permitir que Spatials sejam coletados pelo Garbage Collector quando não forem mais referenciados
    private final Map<Spatial, GameObject> bySpatial = new WeakHashMap<>();

    // Regista um Spatial com o seu GameObject correspondente
    public synchronized void register(Spatial spatial, GameObject obj) {
        if (spatial != null && obj != null) bySpatial.put(spatial, obj);
    }

    // Remove o registo de um Spatial
    public synchronized void unregister(Spatial spatial) {
        if (spatial != null) bySpatial.remove(spatial);
    }

    // Procura o GameObject correspondente a um Spatial
    public synchronized GameObject lookup(Spatial spatial) {
        return bySpatial.get(spatial);
    }
}

