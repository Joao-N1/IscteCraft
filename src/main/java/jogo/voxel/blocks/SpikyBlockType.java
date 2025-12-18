package jogo.voxel.blocks;

import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.texture.Texture2D;
import jogo.appstate.HudAppState;
import jogo.appstate.PlayerAppState;
import jogo.appstate.WorldAppState;
import jogo.gameobject.item.ItemStack;
import jogo.voxel.VoxelBlockType;
import jogo.voxel.VoxelPalette;
import jogo.voxel.VoxelWorld;

// Classe para representar um bloco com espinhos que causa dano ao jogador ao contato e ao minerar sem ferramenta adequada
public class SpikyBlockType extends VoxelBlockType {

    private final String texturePath;
    private final float hardness;
    private final byte dropId;

    public SpikyBlockType(String name, String textureName, float hardness, byte dropId) {
        super(name);
        this.texturePath = textureName.contains("/") ? textureName : "Textures/" + textureName;
        this.hardness = hardness;
        this.dropId = dropId;
    }

    @Override
    public Material getMaterial(AssetManager assetManager) {
        Texture2D tex = (Texture2D) assetManager.loadTexture(texturePath);
        Material m = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
        m.setTexture("DiffuseMap", tex);
        m.setColor("Diffuse", ColorRGBA.White);
        return m;
    }

    @Override
    public float getHardness() {
        return hardness;
    }

    @Override
    public byte getDropItem() {
        return dropId;
    }

    // --- 2. DANO DE CONTACTO ---
    // Este metodo é chamado pelo PlayerAppState quando o player se encosta ao bloco
    @Override
    public int getContactDamage() {
        return 10;
    }

    // --- 3. DANO AO MINAR ---
    @Override
    public boolean processMining(WorldAppState world, PlayerAppState player, float tpf) {
        if (player != null) {
            // Verificar se o jogador está a usar uma ferramenta adequada
            HudAppState hud = world.getState(HudAppState.class);
            int slot = hud != null ? hud.getSelectedSlotIndex() : 0;
            ItemStack itemInHand = player.getPlayer().getHotbar()[slot];

            boolean isTool = false;
            if (itemInHand != null) {
                byte id = itemInHand.getId();
                // Picaretas ou Espada protegem do dano ao minar
                if (id == VoxelPalette.WOOD_PICK_ID ||
                        id == VoxelPalette.STONE_PICK_ID ||
                        id == VoxelPalette.IRON_PICK_ID ||
                        id == VoxelPalette.SWORD_ID) {
                    isTool = true;
                }
            }

            // Se nao tiver ferramenta, sofre dano a cada frame que minar

            if (!isTool) {
                player.takeDamage(5);
            }
        }
        return true; // Pode continuar a minar
    }

    // --- 4. LOGICA DE DROP ---
    @Override
    public void onBlockBreak(WorldAppState world, VoxelWorld.Vector3i pos, PlayerAppState player) {
        // 1. Guardar o ID do bloco antes de partir (para saber o que dropar se dropId for 0)
        byte selfId = world.getVoxelWorld().getBlock(pos.x, pos.y, pos.z);

        // 2. Partir o bloco (põe Ar no mundo)
        super.onBlockBreak(world, pos, player);

        // 3. Drop do Item
        byte itemToDrop = dropId;
        if (itemToDrop == 0) {
            itemToDrop = selfId; // Se for 0, dropa-se a si mesmo
        }

        if (itemToDrop != 0) {
            world.spawnDroppedItem(
                    new Vector3f(pos.x + 0.5f, pos.y + 0.5f, pos.z + 0.5f),
                    new Vector3f(0, 3f, 0),
                    new ItemStack(itemToDrop, 1)
            );
        }
    }
}