package jogo.voxel.blocks;

import com.jme3.asset.AssetManager;
import com.jme3.audio.AudioNode;
import com.jme3.audio.AudioSource;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.texture.Texture2D;
import jogo.appstate.PlayerAppState;
import jogo.appstate.WorldAppState;
import jogo.voxel.VoxelBlockType;

public class TheRockBlockType extends VoxelBlockType {

    private AudioNode rockMusic;

    public TheRockBlockType() {
        super("The Rock");
    }

    @Override
    public Material getMaterial(AssetManager assetManager) {
        // Carrega a textura do The Rock
        Texture2D tex = (Texture2D) assetManager.loadTexture("Textures/TheRock.png");
        Material m = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
        m.setTexture("DiffuseMap", tex);
        m.setBoolean("UseMaterialColors", true);
        m.setColor("Diffuse", ColorRGBA.White);
        return m;
    }

    @Override
    public boolean processMining(WorldAppState world, PlayerAppState player, float tpf) {
        // Inicializa a música se ainda não existir
        if (rockMusic == null) {
            try {
                rockMusic = new AudioNode(world.getApplication().getAssetManager(), "Sounds/TheRockMusic.wav", false);
                rockMusic.setPositional(false);
                rockMusic.setLooping(false);
                rockMusic.setVolume(2.0f);
                // Anexa ao rootNode do mundo para ser ouvido
                world.getRootNode().attachChild(rockMusic);
            } catch (Exception e) {
                System.out.println("Erro ao carregar som do Rock: " + e.getMessage());
            }
        }

        // Toca a música se não estiver a tocar
        if (rockMusic != null && rockMusic.getStatus() != AudioSource.Status.Playing) {
            rockMusic.play();
            System.out.println("Can't stop the Rock!");
        }

        // Retorna false para impedir que o timer de quebra avance
        return false;
    }
}