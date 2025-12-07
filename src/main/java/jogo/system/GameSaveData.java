package jogo.system;

import jogo.gameobject.item.ItemStack;
import java.io.Serializable;
import java.util.ArrayList; // <--- Importante
import java.util.HashMap;
import java.util.List;      // <--- Importante
import java.util.Map;

public class GameSaveData implements Serializable {
    private static final long serialVersionUID = 1L;

    // --- DADOS DO MINIJOGO ---
    public float miniGameTimer = 0f;
    public int miniGameTargetsHit = 0;
    public boolean miniGameRunning = false;

    // --- Dados do Jogador ---
    public float playerX, playerY, playerZ;
    public float rotYaw, rotPitch;
    public int health;
    public ItemStack[] hotbar;
    public ItemStack[] mainInventory;

    // --- Dados do Mundo ---
    public Map<String, byte[][][]> modifiedChunks = new HashMap<>();

    // --- NOVO: Dados dos NPCs ---
    public List<NpcData> npcs = new ArrayList<>();

    // Pequena classe interna para guardar info de cada NPC individual
    public static class NpcData implements Serializable {
        public String type; // "Sheep", "Zombie", etc.
        public float x, y, z;
        public int health;

        public NpcData(String type, float x, float y, float z, int health) {
            this.type = type;
            this.x = x; this.y = y; this.z = z;
            this.health = health;
        }
    }
}