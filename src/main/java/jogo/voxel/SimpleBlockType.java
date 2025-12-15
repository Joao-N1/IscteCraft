package jogo.voxel;

import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.texture.Texture2D;

// Classe para blocos simples com textura, dureza, drop e dano ao contato
public class SimpleBlockType extends VoxelBlockType {

    private final String texturePath;
    private final float hardness;
    private final byte dropId; // O ID do item que dropa (0 = dropa ele próprio)
    private final int damage;  // Dano ao tocar (para os Spiky blocks)

    // Construtor Básico (Blocos normais: Terra, Pedra, etc.)
    public SimpleBlockType(String name, String textureName, float hardness) {
        this(name, textureName, hardness, (byte)0, 0);
    }

    // Construtor para Minérios (Drop diferente)
    public SimpleBlockType(String name, String textureName, float hardness, byte dropId) {
        this(name, textureName, hardness, dropId, 0);
    }

    // Construtor Completo (com Dano)
    public SimpleBlockType(String name, String textureName, float hardness, byte dropId, int damage) {
        super(name);
        // Garante que o caminho tem "Textures/"
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
}