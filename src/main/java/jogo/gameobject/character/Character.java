package jogo.gameobject.character;

import jogo.gameobject.GameObject;

// Classe base para personagens no jogo (Jogadores, NPCs, Inimigos, etc.)
public abstract class Character extends GameObject {

    protected int maxHealth;
    protected int health;
    protected boolean isDead = false;

    protected Character(String name) {
        super(name);
        this.maxHealth = 100; // Padrão
        this.health = 100;
    }

    // Define a vida atual do personagem
    public void setHealth(int health) {
        this.health = health;
        if (this.health <= 0) {
            this.health = 0;
            this.isDead = true;
        } else {
            this.isDead = false;
        }
    }

    // Define a vida máxima do personagem
    public void setMaxHealth(int max) {
        this.maxHealth = max;
        this.health = max; // Começa com vida cheia
    }

    public int getHealth() { return health; }

    // Método para receber dano
    public void takeDamage(int damage) {
        if (isDead) return;

        this.health -= damage;
        System.out.println(name + " sofreu " + damage + " dano. Vida: " + health);

        if (this.health <= 0) {
            this.health = 0;
            die();
        }
    }

    // Metodo para reviver (Respawn)
    public void respawn() {
        this.health = maxHealth;
        this.isDead = false;
        System.out.println(name + " renasceu!");
    }

    // Metodo chamado quando a vida chega a zero
    protected void die() {
        isDead = true;
        System.out.println(name + " morreu!");
    }

    // Verifica se o personagem está morto
    public boolean isDead() {
        return isDead;
    }

    // Métodos para definir o modelo 3D do personagem
    public String getModelPath() {
        return null; // Se retornar null, o RenderAppState pode usar um cubo genérico
    }

    public float getModelScale() {
        return 1.0f;
    }

    public float getModelOffsetY() {
        return 0.0f; // Ajuste de altura
    }
}