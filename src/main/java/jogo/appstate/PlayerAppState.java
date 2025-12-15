package jogo.appstate;

import com.jme3.app.Application;
import com.jme3.app.state.BaseAppState;
import com.jme3.asset.AssetManager;
import com.jme3.audio.AudioData;
import com.jme3.audio.AudioNode;
import com.jme3.bullet.PhysicsSpace;
import com.jme3.bullet.control.BetterCharacterControl;
import com.jme3.light.PointLight;
import com.jme3.math.FastMath;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.scene.Node;
import jogo.gameobject.character.Player;
import jogo.gameobject.item.DroppedItem;
import jogo.gameobject.item.ItemStack;
import jogo.system.GameSaveData;
import jogo.system.HighScoreManager;
import jogo.system.SaveManager;
import jogo.voxel.VoxelPalette;
import jogo.voxel.VoxelWorld;

import java.util.ArrayList;
import java.util.List;

// AppState que gere o jogador: movimento, câmara, inventário, dano, respawn, etc.
public class PlayerAppState extends BaseAppState {

    // Referências aos sistemas principais
    private final Node rootNode;
    private final AssetManager assetManager;
    private final Camera cam; // câmera principal
    private final InputAppState input; // sistema de input
    private final PhysicsSpace physicsSpace; // espaço físico
    private final WorldAppState world; // estado do mundo

    //Entidades do jogador
    private Node playerNode; // nó principal do jogador
    private BetterCharacterControl characterControl; // controle físico do jogador
    private Player player; //Classe de lógica do jogador como a vida, inventário, etc.

    // view angles
    private float yaw = 0f; // horizontal rotation
    private float pitch = 0f; // vertical rotation

    private final float RESPAWN_PROTECTION_TIME = 3.0f;
    private float damageCooldown = 0f;
    private final float INVULNERABILITY_TIME = 1.0f;

    // tuning
    private float moveSpeed = 8.0f;
    private float sprintMultiplier = 1.7f;
    private float mouseSensitivity = 40f; // degrees per mouse analog unit
    private float eyeHeight = 1.7f;

    private Vector3f spawnPosition = new Vector3f(25.5f, 12f, 25.5f);
    private PointLight playerLight;

    private HudAppState hud;
    private boolean inventoryOpen = false;

    //Audios/Sound Effects
    private AudioNode audioHurt;

    private String currentSaveFileName;


    public Player getPlayer() {
        return player;
    }

    // Constructor
    public PlayerAppState(Node rootNode, AssetManager assetManager, Camera cam, InputAppState input, PhysicsSpace physicsSpace, WorldAppState world) {
        this.rootNode = rootNode;
        this.assetManager = assetManager;
        this.cam = cam;
        this.input = input;
        this.physicsSpace = physicsSpace;
        this.world = world;
        world.registerPlayerAppState(this);
    }

    // MÉTODOS PRINCIPAIS
    @Override
    protected void initialize(Application app) {

        if (world != null) {
            spawnPosition = world.getRecommendedSpawnPosition();
        }

        playerNode = new Node("Player");
        rootNode.attachChild(playerNode);

        // Lógica do Jogador
        player = new Player();

        // BetterCharacterControl(radius, height, mass)
        characterControl = new BetterCharacterControl(0.42f, 1.8f, 80f);
        characterControl.setGravity(new Vector3f(0, -24f, 0));
        characterControl.setJumpForce(new Vector3f(0, 500f, 0));
        playerNode.addControl(characterControl);
        physicsSpace.add(characterControl);

        // Configuração da Luz do Jogador
        playerLight = new PointLight();
        playerLight.setColor(new com.jme3.math.ColorRGBA(1.2f, 1.2f, 1.1f, 1f));
        playerLight.setRadius(30f);
        rootNode.addLight(playerLight);

        // --- Configuração do Som de Dano ---
        audioHurt = new AudioNode(assetManager, "Sounds/Hurt_Sound_Effect.wav", AudioData.DataType.Buffer);
        audioHurt.setPositional(false); // som 2D
        audioHurt.setLooping(false);
        audioHurt.setVolume(3.0f);
        playerNode.attachChild(audioHurt);

        this.currentSaveFileName = SaveManager.generateUniqueSaveName();// Novo jogo por defeito
        System.out.println("Novo jogo iniciado. Ficheiro de destino: " + this.currentSaveFileName);

        // Spawn inicial
        respawn();

        // initialize camera
        cam.setFrustumPerspective(60f, (float) cam.getWidth() / cam.getHeight(), 0.05f, 500f);
        this.pitch = -0.35f;
        applyViewToCamera();

        // Obter referência para o HUD para atualizar a vida
        hud = getState(HudAppState.class);

        // Definir vida máxima do jogador
        if(player != null) {
            player.setMaxHealth(100); // Isto define Max e Current para 100
        }
        updateHud();
    }

    // Atualização por frame
    @Override
    public void update(float tpf) {

        if (input.consumeLeaderboardRequest()) {
            if (hud != null) {
                if (hud.isLeaderboardVisible()) {
                    // Se já estiver visível, esconde
                    hud.hideLeaderboard();
                } else {
                    // Se estiver escondido, carrega e mostra (sem tempo limite = 0f)
                    // Criamos uma instância temporária só para ler os scores globais
                    HighScoreManager tempManager = new HighScoreManager();
                    hud.showLeaderboard(tempManager.getTopScores(), 0f, "--- CLASSIFICACAO GLOBAL ---");
                }
            }
        }

        // --- LÓGICA DE SAVE (Tecla M) ---
        if (input.consumeSaveRequest()) {
            performSave(this.currentSaveFileName); // Por agora salvamos sempre no "save1"
        }

        // --- LÓGICA DE CARREGAR (Tecla L) ---
        if (input.consumeLoadMenuRequest()) {
            if (hud != null) {
                // LÓGICA DE TOGGLE
                if (hud.isLoadMenuVisible()) {
                    hud.hideLoadMenu();
                } else {
                    hud.showLoadMenu(jogo.system.SaveManager.getSaveList());
                }
            }
        }

        // --- LÓGICA DE CARREGAR SAVE ESPECÍFICO (F1, F2...) ---
        int saveIndex = input.consumeLoadSelection();
        if (saveIndex != -1) {
            List<String> saves = SaveManager.getSaveList();
            if (saveIndex < saves.size()) {
                performLoad(saves.get(saveIndex));
                if (hud != null) hud.hideLoadMenu();
            }
        }
        // respawn on request
        if (input.consumeRespawnRequested()) {
            if (world != null) spawnPosition = world.getRecommendedSpawnPosition();
            respawn();
        }

        // Atualizar posição do jogador na classe Player
        if (playerNode != null && player != null) {
            Vector3f physPos = playerNode.getWorldTranslation();
            player.setPosition(physPos.x, physPos.y, physPos.z);
        }
        // --- 1. ATIRAR ITEM (Tecla G) ---
        if (input.consumeDropRequested()) {
            // Ver o que temos na mão
            byte heldId = player.getHeldItem();
            if (heldId != 0) {
                // Criar um stack de 1 unidade desse item
                ItemStack dropStack = new ItemStack(heldId, 1);

                // Calcular posição (à frente dos olhos) e força do lançamento
                Vector3f camPos = cam.getLocation();
                Vector3f launchDir = cam.getDirection().mult(8.0f); // Força 8

                // Chamar o spawn no WorldAppState
                if (world != null) {
                    world.spawnDroppedItem(camPos, launchDir, dropStack);
                    player.consumeHeldItem(); // Remove 1 do inventário
                }
            }
        }

        // --- 2. APANHAR ITENS (Colisão) ---
        if (world != null) {
            // Percorrer lista de itens no chão
            List<DroppedItem> items = new ArrayList<>(world.getDroppedItems());
            Vector3f playerPos = playerNode.getWorldTranslation().add(0, 1f, 0); // Centro do corpo

            for (DroppedItem item : items) {
                if (!item.canBePickedUp()) continue;

                // Verificar distância (1.5 metros)
                if (playerPos.distance(item.getNode().getWorldTranslation()) < 1.5f) {
                    // Tentar adicionar ao inventário
                    if (player.addItem(item.getStack().getId(), item.getStack().getAmount())) {
                        // Se coube, remover do mundo
                        world.removeDroppedItem(item);
                        System.out.println("Apanhaste item!");
                    }
                }
            }
        }

        // 1. Verificar Inventário ('I')
        if (input.consumeInventoryRequest()) {
            inventoryOpen = !inventoryOpen;
            if (hud != null) hud.setInventoryVisible(inventoryOpen);
            input.setMouseCaptured(!inventoryOpen);
        }

        // 2. Verificar Hotbar (1-9)
        int requestedSlot = input.consumeHotbarRequest();
        if (requestedSlot != -1) {
            player.setSelectedSlot(requestedSlot);
            if (hud != null) {
                hud.updateSelector(player.getSelectedSlot());
            }
        }

        // Pause controls if mouse not captured
        if (!input.isMouseCaptured()) {
            characterControl.setWalkDirection(Vector3f.ZERO);
            if (playerLight != null) playerLight.setPosition(playerNode.getWorldTranslation().add(0, eyeHeight, 0));
            applyViewToCamera();
            return;
        }

        // Mouse Look
        Vector2f md = input.consumeMouseDelta();
        if (md.lengthSquared() != 0f) {
            float degX = md.x * mouseSensitivity;
            float degY = md.y * mouseSensitivity;
            yaw -= degX * FastMath.DEG_TO_RAD;
            pitch -= degY * FastMath.DEG_TO_RAD;
            //Limitar angulo vertical
            pitch = FastMath.clamp(pitch, -FastMath.HALF_PI * 0.99f, FastMath.HALF_PI * 0.99f);
        }
        // --- LÓGICA DE TERRENO ---
        float currentSpeed = 8.0f; // Velocidade base normal

        if (playerNode != null && world != null) {
            Vector3f pos = playerNode.getWorldTranslation();
            VoxelWorld vw = world.getVoxelWorld();
            if (vw != null) {
                // Verificar bloco debaixo dos pés (y - 1)
                int x = (int) Math.floor(pos.x);
                int y = (int) Math.floor(pos.y - 0.5f);
                int z = (int) Math.floor(pos.z);

                byte blockId = vw.getBlock(x, y, z);

                if (blockId == VoxelPalette.SAND_ID) {
                    currentSpeed = 4.0f; // Reduzir velocidade na areia
                }
            }
        }

        this.moveSpeed = currentSpeed; // Atualizar a variável da classe

        // Movement
        Vector3f wish = input.getMovementXZ();
        Vector3f dir = Vector3f.ZERO;
        if (wish.lengthSquared() > 0f) {
            dir = computeWorldMove(wish).normalizeLocal();
        }
        float speed = moveSpeed * (input.isSprinting() ? sprintMultiplier : 1f); // aplicar sprint
        characterControl.setWalkDirection(dir.mult(speed)); // definir direção de movimento

        // Jump
        if (input.consumeJumpRequested() && characterControl.isOnGround()) {
            characterControl.jump();
        }

        // Camera & Light
        applyViewToCamera();
        if (playerLight != null) playerLight.setPosition(playerNode.getWorldTranslation().add(0, eyeHeight, 0));

        // Dano Ambiental
        checkEnvironmentalDamage(tpf);
    }

    // --- SISTEMA DE SAVE/LOAD ---
    private void performSave(String saveName) {
        if (hud != null) hud.showSubtitle("A Gravar...", 2.0f);

        GameSaveData data = new GameSaveData(); // novo objeto de dados

        // 1. Jogador (posição, rotação, inventário)
        Vector3f pos = playerNode.getWorldTranslation();
        data.playerX = pos.x; data.playerY = pos.y; data.playerZ = pos.z;
        data.rotPitch = this.pitch; data.rotYaw = this.yaw;
        data.health = player.getHealth();
        data.hotbar = player.getHotbar();
        data.mainInventory = player.getMainInventory();

        // 2. Mundo
        if (world != null && world.getVoxelWorld() != null) {
            world.getVoxelWorld().saveChunksToData(data);
        }

        // 3. Salvar NPCs
        NpcAppState npcState = getState(NpcAppState.class);
        if (npcState != null) {
            npcState.saveNpcsToData(data);
        }

        // 4. MINIJOGO
        MiniGameAppState miniGame = getState(MiniGameAppState.class);
        if (miniGame != null) {
            miniGame.saveStateToData(data);
        }

        // Gravar no disco
        SaveManager.saveGame(saveName, data);
    }

    private void performLoad(String saveName) {
        System.out.println("A carregar save: " + saveName);

        GameSaveData data = SaveManager.loadGame(saveName);
        if (data == null) {
            if (hud != null) hud.showSubtitle("Erro ao carregar!", 2.0f);
            return;
        }

        this.currentSaveFileName = saveName;

        // 1. Jogador (Update Posição e Inventário) ...
        characterControl.warp(new Vector3f(data.playerX, data.playerY, data.playerZ));
        this.yaw = data.rotYaw;
        this.pitch = data.rotPitch;
        player.setHealth(data.health);
        if (hud != null) hud.setHealth(player.getHealth());

        if(data.hotbar != null) System.arraycopy(data.hotbar, 0, player.getHotbar(), 0, 9);
        if(data.mainInventory != null) System.arraycopy(data.mainInventory, 0, player.getMainInventory(), 0, 27);

        // 2. Mundo
        if (world != null && world.getVoxelWorld() != null) {
            world.getVoxelWorld().loadChunksFromData(data);
            world.getVoxelWorld().rebuildDirtyChunks(world.getPhysicsSpace());
        }

        // 3. NPCs
        NpcAppState npcState = getState(NpcAppState.class);
        if (npcState != null) npcState.loadNpcsFromData(data);

        // 4. MINIJOGO
        MiniGameAppState miniGame = getState(MiniGameAppState.class);
        if (miniGame != null) {
            miniGame.loadStateFromData(data);
        }

        if (hud != null) {
            hud.updateInventoryDisplay(player);
            hud.showSubtitle("Mundo Carregado: " + saveName, 3.0f);

            // Se o menu de load estiver aberto, fecha-o automaticamente após carregar
            if (hud.isLoadMenuVisible()) hud.hideLoadMenu();
        }
    }


    // DANO AMBIENTAL
    // Verifica se o jogador está em contacto com blocos que causam dano
    private void checkEnvironmentalDamage(float tpf) {
        if (damageCooldown > 0) { // invulnerabilidade temporária
            damageCooldown -= tpf;
            return;
        }

        if (world == null || player == null) return;
        VoxelWorld voxelWorld = world.getVoxelWorld();
        if (voxelWorld == null) return;

        Vector3f pos = playerNode.getWorldTranslation();

        int minX = (int) Math.floor(pos.x - 0.5f);
        int maxX = (int) Math.floor(pos.x + 0.5f);
        int minZ = (int) Math.floor(pos.z - 0.5f);
        int maxZ = (int) Math.floor(pos.z + 0.5f);

        int feetY = (int) Math.floor(pos.y);
        int headY = (int) Math.floor(pos.y + 1f);
        int belowY = feetY - 1;
        int aboveY = headY + 1;

        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                if (checkBlockDamage(voxelWorld, x, feetY, z)) return;
                if (checkBlockDamage(voxelWorld, x, headY, z)) return;
                if (checkBlockDamage(voxelWorld, x, belowY, z)) return;
                if (checkBlockDamage(voxelWorld, x, aboveY, z)) return;
            }
        }
    }

    // Verifica se o bloco na posição dada causa dano ao jogador
    private boolean checkBlockDamage(VoxelWorld vw, int x, int y, int z) {
        byte id = vw.getBlock(x, y, z);
        if (id == VoxelPalette.AIR_ID) return false;


        // Agora vai buscar o dano real (ex: 10 na SpikyWood, 0 na Terra)
        int damage = vw.getPalette().get(id).getContactDamage();

        if (damage > 0) {
            takeDamage(damage);
            return true;
        }
        return false;
    }

    // --- SISTEMA DE DANO ---

    public void takeDamage(int amount) {
        if (damageCooldown > 0) return; // invulnerabilidade temporária

        if (audioHurt != null) {
            audioHurt.playInstance(); // Toca o som de dano
        }

        //Reduz vida
        player.takeDamage(amount);

        System.out.println("Dano: " + amount + ". Vida atual: " + player.getHealth());

        updateHud(); // Atualiza os corações no HUD

        if (player.isDead()) {
            System.out.println("Jogador morreu!");
            respawn();
        } else {
            damageCooldown = INVULNERABILITY_TIME;
        }
    }

    private void updateHud() {
        if (hud != null && player != null) {
            hud.setHealth(player.getHealth());
        }
    }

    // --- SISTEMA DE RESPAWN ---
    private void respawn() {
        characterControl.setWalkDirection(Vector3f.ZERO);
        characterControl.warp(spawnPosition);

        if (player != null) {
            player.respawn();
        }

        damageCooldown = RESPAWN_PROTECTION_TIME;
        updateHud();
        damageCooldown = RESPAWN_PROTECTION_TIME;

        // reset camera angles
        this.pitch = -0.35f;
        applyViewToCamera();
    }


    private Vector3f computeWorldMove(Vector3f inputXZ) {
        // Garante que os movimentos são relacionados à direção da câmara e não ao eixo fixo do mundo
        float sinY = FastMath.sin(yaw);
        float cosY = FastMath.cos(yaw);
        Vector3f forward = new Vector3f(-sinY, 0, -cosY);
        Vector3f left = new Vector3f(-cosY, 0, sinY);
        return left.mult(inputXZ.x).addLocal(forward.mult(inputXZ.z));
    }

    // Aplica a posição e rotação do jogador à câmara
    private void applyViewToCamera() {
        Vector3f loc = playerNode.getWorldTranslation().add(0, eyeHeight, 0);
        cam.setLocation(loc);
        cam.setRotation(new com.jme3.math.Quaternion().fromAngles(pitch, yaw, 0f));
    }


    public WorldAppState getWorld() {
        return world;
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