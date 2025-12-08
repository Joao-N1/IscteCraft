package jogo.gameobject.character;

public class Sheep extends Character {

    public Sheep() {
        super("Sheep");
        setMaxHealth(40);
        // Podes definir vida ou velocidade aqui no futuro
    }

    @Override
    public String getModelPath() {
        return "Models/SheepNPC/source/sheep.gltf";
    }
}
