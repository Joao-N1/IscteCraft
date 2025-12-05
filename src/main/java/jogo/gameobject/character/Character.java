package jogo.gameobject.character;

import jogo.gameobject.GameObject;

public abstract class Character extends GameObject {

    protected int maxHealth;
    protected int health;
    protected boolean isDead = false;

    protected Character(String name) {
        super(name);
        this.maxHealth = 100; // Padrão
        this.health = 100;
    }

    // ... métodos existentes ...

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
        this.health = max; // Começa com vida cheia
    }

    public int getHealth() { return health; }

    public void takeDamage(int damage) {
        if (isDead) return;

        this.health -= damage;
        System.out.println(name + " sofreu " + damage + " dano. Vida: " + health);

        if (this.health <= 0) {
            this.health = 0;
            die();
        }
    }

    public void heal(int amount) {
        if (isDead) return;
        this.health += amount;
        if (health > maxHealth) health = maxHealth;
    }

    // Método para reviver (Respawn)
    public void respawn() {
        this.health = maxHealth;
        this.isDead = false;
        System.out.println(name + " renasceu!");
    }

    protected void die() {
        isDead = true;
        System.out.println(name + " morreu!");
    }

    public boolean isDead() {
        return isDead;
    }
}