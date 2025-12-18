package jogo.gameobject.character;

import jogo.gameobject.GameObject;

// Classe abstrata Character que estende GameObject
public abstract class Character extends GameObject {

    protected int maxHealth;
    protected int health;
    protected boolean isDead = false;

    // Variável para guardar a rotação (em radianos)
    protected float yaw = 0f;

    protected Character(String name) {
        super(name);
        this.maxHealth = 100;
        this.health = 100;
    }


    public void setHealth(int health) {
        this.health = health;
        if (this.health <= 0) {
            this.health = 0;
            this.isDead = true;
        } else {
            this.isDead = false;
        }
    }

    public void setMaxHealth(int max) {
        this.maxHealth = max;
        this.health = max;
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

    public void respawn() {
        this.health = maxHealth;
        this.isDead = false;
        System.out.println(name + " renasceu!");
    }

    protected void die() {
        isDead = true;
        System.out.println(name + " morreu!");
    }

    public boolean isDead() { return isDead; }

    public String getModelPath() { return null; }
    public float getModelScale() { return 1.0f; }
    public float getModelOffsetY() { return 0.0f; }

    // --- Getters e Setters para a Rotação ---
    public void setYaw(float yaw) {
        this.yaw = yaw;
    }

    public float getYaw() {
        return yaw;
    }
}