package jogo.gameobject.character;

public class Zombie extends Character {
    public Zombie() {
        super("Zombie");
        setMaxHealth(90);
    }

    @Override
    public String getModelPath() {
        return "Models/zombieNPC/source/model.gltf";
    }

}