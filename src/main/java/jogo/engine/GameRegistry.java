package jogo.engine;

import jogo.gameobject.GameObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

// Registro centralizado para todos os objetos do jogo.
public class GameRegistry {
    // Lista de todos os objetos registrados
    private final List<GameObject> objects = new ArrayList<>();

    // Adiciona um objeto ao registro
    public synchronized void add(GameObject obj) {
        if (obj != null && !objects.contains(obj)) {
            objects.add(obj);
        }
    }

    // Remove um objeto do registro
    public synchronized void remove(GameObject obj) {
        objects.remove(obj);
    }

    // Retorna uma lista imutável de todos os objetos registrados
    public synchronized List<GameObject> getAll() {
        return Collections.unmodifiableList(new ArrayList<>(objects));
    }
}

