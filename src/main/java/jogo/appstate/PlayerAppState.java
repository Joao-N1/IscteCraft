package jogo.appstate;

import com.jme3.app.Application;
import com.jme3.app.state.BaseAppState;
import com.jme3.asset.AssetManager;
import com.jme3.bullet.PhysicsSpace;
import com.jme3.bullet.control.BetterCharacterControl;
import com.jme3.light.PointLight;
import com.jme3.math.FastMath;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.scene.Node;
import jogo.gameobject.character.Player;
import jogo.voxel.VoxelPalette;
import jogo.voxel.VoxelWorld;
import com.jme3.audio.AudioNode;
import com.jme3.audio.AudioData;


public class PlayerAppState extends BaseAppState {

    private final Node rootNode;
    private final AssetManager assetManager;
    private final Camera cam;
    private final InputAppState input;
    private final PhysicsSpace physicsSpace;
    private final WorldAppState world;

    private Node playerNode;
    private BetterCharacterControl characterControl;
    private Player player;

    // view angles
    private float yaw = 0f;
    private float pitch = 0f;

    private final float RESPAWN_PROTECTION_TIME = 3.0f;
    private float damageCooldown = 0f;
    private final float INVULNERABILITY_TIME = 1.0f;

    // tuning
    private float moveSpeed = 8.0f; // m/s
    private float sprintMultiplier = 1.7f;
    private float mouseSensitivity = 40f; // degrees per mouse analog unit
    private float eyeHeight = 1.7f;

    private Vector3f spawnPosition = new Vector3f(25.5f, 12f, 25.5f);
    private PointLight playerLight;

    private HudAppState hud;
    private boolean inventoryOpen = false;

    //Audios/Sound Effects
    private AudioNode audioHurt;

    public Player getPlayer() {
        return player;
    }

    public PlayerAppState(Node rootNode, AssetManager assetManager, Camera cam, InputAppState input, PhysicsSpace physicsSpace, WorldAppState world) {
        this.rootNode = rootNode;
        this.assetManager = assetManager;
        this.cam = cam;
        this.input = input;
        this.physicsSpace = physicsSpace;
        this.world = world;
        world.registerPlayerAppState(this);

    }

    @Override
    protected void initialize(Application app) {
        // query world for recommended spawn now that it should be initialized
        if (world != null) {
            spawnPosition = world.getRecommendedSpawnPosition();
        }

        playerNode = new Node("Player");
        rootNode.attachChild(playerNode);

        // Engine-neutral player entity (no engine visuals here)
        player = new Player();

        // BetterCharacterControl(radius, height, mass)
        characterControl = new BetterCharacterControl(0.42f, 1.8f, 80f);
        characterControl.setGravity(new Vector3f(0, -24f, 0));
        characterControl.setJumpForce(new Vector3f(0, 500f, 0));
        playerNode.addControl(characterControl);
        physicsSpace.add(characterControl);

        // Local light source that follows the player's head
        playerLight = new PointLight();
        playerLight.setColor(new com.jme3.math.ColorRGBA(0.6f, 0.55f, 0.5f, 1f));
        playerLight.setRadius(12f);
        rootNode.addLight(playerLight);


        // --- Configuração do Som de Dano ---
        audioHurt = new AudioNode(assetManager, "Sounds/Hurt_Sound_Effect.wav", AudioData.DataType.Buffer);
        audioHurt.setPositional(false); // false = som ambiente/2D (ouve-se sempre no volume máximo)
        audioHurt.setLooping(false);    // Não repetir
        audioHurt.setVolume(3.0f);      // Ajusta o volume se necessário (2.0 é bem alto)
        playerNode.attachChild(audioHurt);

        // Spawn at recommended location
        respawn();

        // initialize camera
        cam.setFrustumPerspective(60f, (float) cam.getWidth() / cam.getHeight(), 0.05f, 500f);
        // Look slightly downward so ground is visible immediately
        this.pitch = -0.35f;
        applyViewToCamera();


        // Obter referência para o HUD para atualizar a vida
        hud = getState(HudAppState.class);

        // Assegurar que o player começa com vida cheia
        if(player != null) player.setHealth(100);
        updateHud();
    }

    @Override
    public void update(float tpf) {
        // respawn on request
        if (input.consumeRespawnRequested()) {
            // refresh spawn from world in case terrain changed
            if (world != null) spawnPosition = world.getRecommendedSpawnPosition();
            respawn();

        }
        if (playerNode != null && player != null) {
            Vector3f physPos = playerNode.getWorldTranslation();
            player.setPosition(physPos.x, physPos.y, physPos.z);
        }

        // 1. Verificar se o jogador quer abrir/fechar inventário ('I')
        if (input.consumeInventoryRequest()) {
            inventoryOpen = !inventoryOpen;

            // Atualizar HUD
            if (hud != null) hud.setInventoryVisible(inventoryOpen);

            // Atualizar Rato:
            // Se inventário aberto -> Rato Solto (false no captured) para clicar
            // Se inventário fechado -> Rato Preso (true no captured) para olhar
            input.setMouseCaptured(!inventoryOpen);
        }

        // 2. Verificar input da Hotbar (Teclas 1-9)
        // Só permitimos mudar a hotbar se o inventário estiver FECHADO (opcional)
        // Mas geralmente em jogos podes mudar a seleção mesmo com ele aberto.
        int requestedSlot = input.consumeHotbarRequest();
        if (requestedSlot != -1) {
            System.out.println("Mudar para slot: " + requestedSlot);
            player.setSelectedSlot(requestedSlot);

            // Avisar o HUD para mover o quadrado
            if (hud != null) {
                hud.updateSelector(player.getSelectedSlot());
            }
        }

        // pause controls if mouse not captured
        if (!input.isMouseCaptured()) {
            characterControl.setWalkDirection(Vector3f.ZERO);
            // keep light with player even when paused
            if (playerLight != null) playerLight.setPosition(playerNode.getWorldTranslation().add(0, eyeHeight, 0));
            applyViewToCamera();
            return;
        }

        // handle mouse look
        Vector2f md = input.consumeMouseDelta();
        if (md.lengthSquared() != 0f) {
            float degX = md.x * mouseSensitivity;
            float degY = md.y * mouseSensitivity;
            yaw -= degX * FastMath.DEG_TO_RAD;
            pitch -= degY * FastMath.DEG_TO_RAD;
            pitch = FastMath.clamp(pitch, -FastMath.HALF_PI * 0.99f, FastMath.HALF_PI * 0.99f);
        }

        // movement input in XZ plane based on camera yaw
        Vector3f wish = input.getMovementXZ();
        Vector3f dir = Vector3f.ZERO;
        if (wish.lengthSquared() > 0f) {
            dir = computeWorldMove(wish).normalizeLocal();
        }
        float speed = moveSpeed * (input.isSprinting() ? sprintMultiplier : 1f);
        characterControl.setWalkDirection(dir.mult(speed));

        // Jump
        if (input.consumeJumpRequested() && characterControl.isOnGround()) {
            characterControl.jump();
        }

        // place camera at eye height above physics location
        applyViewToCamera();

        // update light to follow head
        if (playerLight != null) playerLight.setPosition(playerNode.getWorldTranslation().add(0, eyeHeight, 0));

        // --- LÓGICA DE DANO AMBIENTAL ---
        checkEnvironmentalDamage(tpf);
    }

    //Verifica os blocos à volta do jogador (pés e cabeça) para ver se leva dano
    private void checkEnvironmentalDamage(float tpf) {
        // Se estiver em cooldown (invulnerável), reduz o timer e sai
        if (damageCooldown > 0) {
            damageCooldown -= tpf;
            return;
        }

        if (world == null || player == null) return;
        VoxelWorld voxelWorld = world.getVoxelWorld();
        if (voxelWorld == null) return;

        // Posição do jogador
        Vector3f pos = playerNode.getWorldTranslation();
        // Verificar uma pequena área à volta do jogador (bounding box simples)
        // O jogador tem ~0.8 de largura e 1.8 de altura. Vamos verificar blocos que ele possa estar a tocar.
        // Convertemos coordenadas do mundo para coordenadas inteiras (voxels)

        int minX = (int) Math.floor(pos.x - 0.5f);
        int maxX = (int) Math.floor(pos.x + 0.5f);
        int minZ = (int) Math.floor(pos.z - 0.5f);
        int maxZ = (int) Math.floor(pos.z + 0.5f);

        // Verifica nos pés (y) e no tronco (y+1)
        int feetY = (int) Math.floor(pos.y);
        int headY = (int) Math.floor(pos.y + 1f);
        int belowY = feetY - 1;                    // Bloco debaixo dos pés
        int aboveY = headY + 1;                    // Bloco acima da cabeça


        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {

                // Verifica Pés
                if (checkBlockDamage(voxelWorld, x, feetY, z)) {
                    return; // <--- PARAR IMEDIATAMENTE se levou dano
                }
                // Verifica Cabeça
                if (checkBlockDamage(voxelWorld, x, headY, z)) {
                    return; // <--- PARAR IMEDIATAMENTE se levou dano
                }
                // 2. Verifica chão (Pisar em cima)
                if (checkBlockDamage(voxelWorld, x, belowY, z))
                    return;

                // 3. Verifica teto (Bater com a cabeça)
                if (checkBlockDamage(voxelWorld, x, aboveY, z))
                    return;
            }
        }
    }

    //Vê se um bloco específico numa coordenada tem dano de contacto
    private boolean checkBlockDamage(VoxelWorld vw, int x, int y, int z) {
        byte id = vw.getBlock(x, y, z);
        if (id == VoxelPalette.AIR_ID) return false;

        var blockType = vw.getPalette().get(id);
        int damage = blockType.getContactDamage();

        if (damage > 0) {
            takeDamage(damage);
            return true;
        }
        return false;
    }

    //Retira vida, ativa invencibilidade temporária e verifica se morreu
    public void takeDamage(int amount) {
        if (damageCooldown > 0) return; // Segurança extra

        //Tocar o som
        if (audioHurt != null) {
            audioHurt.playInstance();
        }

        int currentHealth = player.getHealth();
        currentHealth -= amount;
        player.setHealth(currentHealth);

        System.out.println("Dano: " + amount + ". Vida atual: " + currentHealth);

        // Atualiza o HUD
        updateHud();

        // Verifica Morte
        if (currentHealth <= 0) {
            System.out.println("Jogador morreu!");
            respawn();
            // O respawn() está definido abaixo, vamos garantir que reseta a vida
        } else {
            // Ativa invencibilidade temporária
            damageCooldown = INVULNERABILITY_TIME;

            // Opcional: Efeito visual de "empurrão" (Knockback)
            // characterControl.applyCentralImpulse(...) // Requer ajustes na física
        }
    }

    private void updateHud() {
        if (hud != null && player != null) {
            hud.setHealth(player.getHealth());
        }
    }

    private void respawn() {
        characterControl.setWalkDirection(Vector3f.ZERO);
        characterControl.warp(spawnPosition);

        //Reseta a vida do jogador
        if (player != null) {
            player.setHealth(100);
        }

        //invencibilidade ao dar respawn
        damageCooldown = RESPAWN_PROTECTION_TIME;

        updateHud(); // Atualiza HUD para cheio
        damageCooldown = 0f; // Remove cooldown se houver

        // Reset look
        this.pitch = -0.35f;
        applyViewToCamera();
    }

    private Vector3f computeWorldMove(Vector3f inputXZ) {
        // Build forward and left unit vectors from yaw
        float sinY = FastMath.sin(yaw);
        float cosY = FastMath.cos(yaw);
        Vector3f forward = new Vector3f(-sinY, 0, -cosY); // -Z when yaw=0
        Vector3f left = new Vector3f(-cosY, 0, sinY);     // -X when yaw=0
        return left.mult(inputXZ.x).addLocal(forward.mult(inputXZ.z));
    }

    private void applyViewToCamera() {
        // Character world location (spatial is synced by control)
        Vector3f loc = playerNode.getWorldTranslation().add(0, eyeHeight, 0);
        cam.setLocation(loc);
        cam.setRotation(new com.jme3.math.Quaternion().fromAngles(pitch, yaw, 0f));
    }

    @Override
    protected void cleanup(Application app) {
        if (playerNode != null) {
            if (characterControl != null) {
                physicsSpace.remove(characterControl);
                playerNode.removeControl(characterControl);
                characterControl = null;
            }
            playerNode.removeFromParent();
            playerNode = null;
        }
        if (playerLight != null) {
            rootNode.removeLight(playerLight);
            playerLight = null;
        }
    }

    @Override
    protected void onEnable() { }

    @Override
    protected void onDisable() { }

    public void refreshPhysics() {
        if (characterControl != null) {
            physicsSpace.remove(characterControl);
            physicsSpace.add(characterControl);
        }
    }
}
