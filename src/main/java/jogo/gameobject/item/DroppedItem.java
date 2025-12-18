package jogo.gameobject.item;

import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.scene.Node;

public class DroppedItem {
    private final Node node;
    private final RigidBodyControl physics;
    private final ItemStack stack;
    private float pickupDelay; // Tempo que o item espera até poder ser apanhado

    public DroppedItem(Node node, RigidBodyControl physics, ItemStack stack) {
        this.node = node;
        this.physics = physics;
        this.stack = stack;
        this.pickupDelay = 1.0f; // 1.0 segundo de espera inicial
    }

    public void update(float tpf) {
        if (pickupDelay > 0) {
            pickupDelay -= tpf;
        }
        node.rotate(0, tpf, 0);
    }

    //Metodo que o PlayerAppState chama para verificar se o item pode ser apanhado
    public boolean canBePickedUp() {
        return pickupDelay <= 0;
    }

    public ItemStack getStack() { return stack; }
    public Node getNode() { return node; }
    public RigidBodyControl getPhysics() { return physics; }
}