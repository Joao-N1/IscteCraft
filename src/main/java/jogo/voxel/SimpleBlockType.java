package jogo.voxel;

import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.texture.Texture2D;
import jogo.appstate.PlayerAppState;
import jogo.appstate.WorldAppState;
import jogo.gameobject.item.ItemStack;
import jogo.voxel.VoxelWorld.Vector3i;

// Classe para representar um tipo simples de bloco com textura, dureza e drop configuráveis
public class SimpleBlockType extends VoxelBlockType {

    private final String texturePath;
    private final float hardness;
    private final byte dropId;
    private final int damage;

    // Construtores com diferentes níveis de configuração
    public SimpleBlockType(String name, String textureName, float hardness) {
        this(name, textureName, hardness, (byte)0, 0);
    }

    public SimpleBlockType(String name, String textureName, float hardness, byte dropId) {
        this(name, textureName, hardness, dropId, 0);
    }

    public SimpleBlockType(String name, String textureName, float hardness, byte dropId, int damage) {
        super(name);
        this.texturePath = textureName.contains("/") ? textureName : "Textures/" + textureName;
        this.hardness = hardness;
        this.dropId = dropId;
        this.damage = damage;
    }

    @Override
    public float getHardness() { return hardness; }

    @Override
    public byte getDropItem() { return dropId; }

    @Override
    public int getContactDamage() { return damage; }

    @Override
    public Material getMaterial(AssetManager assetManager) {
        Texture2D tex = (Texture2D) assetManager.loadTexture(texturePath);
        Material m = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
        m.setTexture("DiffuseMap", tex);
        m.setBoolean("UseMaterialColors", true);
        m.setColor("Diffuse", ColorRGBA.White);
        m.setColor("Specular", ColorRGBA.White.mult(0.05f));
        m.setFloat("Shininess", 16f);
        return m;
    }

    // --- AQUI ESTÁ A CORREÇÃO DO DROP ---
    @Override
    public void onBlockBreak(WorldAppState world, Vector3i pos, PlayerAppState player) {
        // 1. Guardar o ID do bloco ANTES de ele ser destruído (senão vira Ar = 0)
        byte originalBlockId = world.getVoxelWorld().getBlock(pos.x, pos.y, pos.z);

        // 2. Chamar o 'super' para partir o bloco fisicamente (VoxelBlockType remove do mundo)
        super.onBlockBreak(world, pos, player);

        System.out.println("SimpleBlockType: Parti " + getName());

        // 3. Lógica de Drop
        byte itemToDrop = dropId;

        // Se o dropId for 0, significa que dropa ele próprio
        if (itemToDrop == 0) {
            itemToDrop = originalBlockId;
        }

        // Se for 0 (Ar) não dropa nada
        if (itemToDrop != 0) {
            world.spawnDroppedItem(
                    new Vector3f(pos.x + 0.5f, pos.y + 0.5f, pos.z + 0.5f), // Posição
                    new Vector3f(0, 3f, 0), // Velocidade (salta para cima)
                    new ItemStack(itemToDrop, 1) // Item
            );
        }
    }
}