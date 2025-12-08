package jogo.gameobject.character;

public class Wolf extends Character {
    public Wolf() {
        super("Wolf");
        setMaxHealth(50);
    }

    @Override
    public String getModelPath() {
        return "Models/wolfNPC/wolf.gltf";
    }

    @Override
    public float getModelScale() {
        return 0.075f;
    }

    @Override
    public float getModelOffsetY() {
        return 0.6f;
    }
}