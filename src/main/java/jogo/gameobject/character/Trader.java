package jogo.gameobject.character;

public class Trader extends Character {
    public Trader() {
        super("Trader");
        setMaxHealth(45);
    }

    @Override
    public String getModelPath() {
        return "Models/traderNPC/trader.gltf";
    }

    @Override
    public float getModelScale() {
        return 1.3f;
    }

}
