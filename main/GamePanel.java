package main;

import entity.Player;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.text.DecimalFormat;
import java.util.ArrayList;
import javax.swing.JPanel;
import tile.TileManager;

// Game Screen
public class GamePanel extends JPanel implements Runnable{

    // SCREEN SETTINGS
    final int originalTileSize = 16; // 16x16 tile
    final int scale = 3;

    public final int tileSize = originalTileSize * scale; // 48x48 tile

    // Spesifikasi Map
    public final int maxScreenCol = 14;
    public final int maxScreenRow = 10;
    public final int screenWidth = tileSize * maxScreenCol; // 672 pixels
    public final int screenHeight = tileSize * maxScreenRow; // 480 pixels

    // FPS
    public final int FPS = 60;

    // Game State
    public int gameState;
    public final int titleState = 0; // Main Menu
    public final int playState = 1;
    public final int gameOverState = 2; // (legacy, tidak dipakai di stage flow; dibiarkan agar tidak merusak kode lain)
    public final int instructionState = 3; // How to Play
    public final int stageSelectState = 4;
    public final int stagePreviewState = 5;
    public final int resultState = 6;

    // Stage config
    public StageConfig stageConfig;

    // Menu State
    public int commandNum = 0; // 0: Start Game, 1: How to Play, 2: Exit

    // Timer
    private long startTime;
    public long remainingTimeMillis;
    public long lastUpdateTime;

    // Tile
    public TileManager tileM = new TileManager(this);


    // Key Handler
    KeyHandler keyH = new KeyHandler();

    // Thread
    Thread gameThread;

    // Floor
    // floor items stored by tile coordinates (null = empty)
    public String[][] floorItem; // item key names, e.g. "chopped_meat", "tomato", etc.
    public java.awt.image.BufferedImage[][] floorItemImage;

    


    // Order
    public OrderManager orderManager;
    public java.util.ArrayList<ServingStation> servingStations = new java.util.ArrayList<>();
    public int score = 0; // total score
    public int ordersCompleted = 0;
    public int ordersFailed = 0;

    // Plate and Assembly and Trash
    public java.util.ArrayList<PlateStorage> plateStorages = new java.util.ArrayList<>();
    public java.util.ArrayList<AssemblyStation> assemblyStations = new java.util.ArrayList<>();
    public java.util.ArrayList<TrashStation> trashStations = new java.util.ArrayList<>();
    public java.util.ArrayList<WashingStation> washingStations = new java.util.ArrayList<>();

    // Collision
    public CollisionChecker cChecker = new CollisionChecker(this);

    // Player
    public Player[] players = new Player[2];
    public int activePlayerIndex = 0;

    // Stage system
    public StageManager stageManager = new StageManager();
    public StageMeta currentStage = null;
    public int selectedStageIndex = 0;

    // Cooking stations list
    public ArrayList<CookingStation> cookingStations = new ArrayList<>();
    //Ingredient stations list
    public ArrayList<IngredientStation> ingredientStations = new ArrayList<>();

    public GamePanel(){
        this.setPreferredSize(new Dimension(screenWidth, screenHeight));
        this.setBackground(Color.BLACK);
        this.setDoubleBuffered(true);
        this.addKeyListener(keyH);
        this.setFocusable(true);

        players[0] = new Player(this, keyH, "red");
        players[1] = new Player(this, keyH, "blue");

        players[1].x = 386;
        players[1].y = 239;
        
        players[0].syncPxPyToXY();
        players[1].syncPxPyToXY();

        // Stage/title flow versi baru
        gameState = titleState;
        stageConfig = null;

        startTime = System.currentTimeMillis();
        lastUpdateTime = System.currentTimeMillis();
        remainingTimeMillis = 0L; // belum ada stage berjalan

        // Build stations dari map awal (versi lama)
        rebuildStationsFromMap();

        // OrderManager sengaja TIDAK dibuat di constructor.
        // Ia dibuat saat startStage() supaya reward/penalty/maks order mengikuti stageConfig.
        orderManager = null;
    }

    public void startGameThread(){
        gameThread = new Thread(this);
        gameThread.start();
    }

    public void drawText(Graphics2D g2, String text, int x, int y, Font font, Color color) {
        g2.setFont(font);
        g2.setColor(color);
        g2.drawString(text, x, y);
    }

    @Override
    public void run(){

        // delta method
        double drawInterval = 100000000/FPS;
        double delta = 0;
        long lastTime = System.nanoTime();
        long currentTime;

        while(gameThread != null){

            currentTime = System.nanoTime();
            delta += (currentTime - lastTime) / drawInterval;
            lastTime = currentTime;

            if(delta >= 1){
                update();
                repaint();
                delta--;
            }
        }
    }

    // -----------------------
    // UI STATE UPDATES (baru)
    // -----------------------

    public void updateTitleState() {
        if (keyH.upPressed) {
            commandNum--;
            if (commandNum < 0) commandNum = 2;
            keyH.upPressed = false;
        }
        if (keyH.downPressed) {
            commandNum++;
            if (commandNum > 2) commandNum = 0;
            keyH.downPressed = false;
        }

        if (keyH.enterPressed) {
            if (commandNum == 0) {  // START GAME → Stage Select
                gameState = stageSelectState;
            }
            else if (commandNum == 1) { // HOW TO PLAY
                gameState = instructionState;
            }
            else if (commandNum == 2) { // EXIT
                System.exit(0);
            }
            keyH.enterPressed = false;
        }
    }

    public void updateInstructionState() {
        if(keyH.enterPressed){
            gameState = titleState;
            keyH.enterPressed = false;
        }
    }

    public void updateStageSelect() {
        if (keyH.upPressed) {
            selectedStageIndex--;
            if (selectedStageIndex < 0)
                selectedStageIndex = stageManager.stages.size() - 1;
            keyH.upPressed = false;
        }

        if (keyH.downPressed) {
            selectedStageIndex++;
            if (selectedStageIndex >= stageManager.stages.size())
                selectedStageIndex = 0;
            keyH.downPressed = false;
        }

        if (keyH.enterPressed) {
            StageMeta s = stageManager.stages.get(selectedStageIndex);
            if (s.isUnlocked) {
                currentStage = s;
                gameState = stagePreviewState;
            }
            keyH.enterPressed = false;
        }

        if (keyH.switchPressed) { // back
            gameState = titleState;
            keyH.switchPressed = false;
        }
    }

    public void updateStagePreview() {
        if (keyH.enterPressed) {
            startStage(currentStage);
            keyH.enterPressed = false;
        }

        if (keyH.switchPressed) {
            gameState = stageSelectState;
            keyH.switchPressed = false;
        }
    }

    public void updateResultState() {
        if (keyH.enterPressed) {
            gameState = stageSelectState;
            keyH.enterPressed = false;
        }

        if (keyH.switchPressed) {
            startStage(currentStage);
            keyH.switchPressed = false;
        }
    }

    // -----------------------
    // STAGE START (baru)
    // -----------------------

    public void startStage(StageMeta stage) {

        // Apply stage config
        stageConfig = StageConfig.forStage(selectedStageIndex + 1);

        // Reset players (tanpa mengubah movement/collision; hanya reset posisi/default seperti sebelumnya)
        players[0].setDefaultValues();
        players[1].setDefaultValues();
        players[1].x = 386;
        players[1].y = 239;

        activePlayerIndex = 0;

        // Reset score stats
        score = 0;
        ordersCompleted = 0;
        ordersFailed = 0;

        // Load map stage & rebuild stations
        tileM.loadMap(stage.mapPath);
        rebuildStationsFromMap();
        floorItem = new String[maxScreenCol][maxScreenRow];
        floorItemImage = new java.awt.image.BufferedImage[maxScreenCol][maxScreenRow];

        // Reset timer stage
        startTime = System.currentTimeMillis();
        remainingTimeMillis = stageConfig.gameDurationSeconds * 1000L;

        // (fungsionalitas lama) reset cooking stations pan state
        for (CookingStation cs : cookingStations) {
            cs.panPresent = true;
            cs.panOwner = -1;
            cs.panItem = null;
            cs.panTimer = 0;
        }

        // Create order manager AFTER stageConfig exists
        orderManager = new OrderManager(this);
        orderManager.activeOrders.clear();
        orderManager.resetSequence();
        orderManager.trySpawnInitial();

        gameState = playState;
    }

    // -----------------------
    // REBUILD STATIONS (dipakai)
    // -----------------------

    public void rebuildStationsFromMap() {
        cookingStations.clear();
        assemblyStations.clear();
        trashStations.clear();
        plateStorages.clear();
        washingStations.clear();
        servingStations.clear();
        ingredientStations.clear();


        for (int col = 0; col < maxScreenCol; col++) {
            for (int row = 0; row < maxScreenRow; row++) {

                int t = tileM.mapTileNum[col][row];

                if (t == 3) cookingStations.add(new CookingStation(this, col, row));
                else if (t == 4) assemblyStations.add(new AssemblyStation(this, col, row));
                else if (t == 8) plateStorages.add(new PlateStorage(this, col, row, 5));
                else if (t == 9) trashStations.add(new TrashStation(this, col, row));
                else if (t == 5) servingStations.add(new ServingStation(this, col, row));
                else if (t == 6) washingStations.add(new WashingStation(this, col, row));
                if (t == 7) ingredientStations.add(
                    new IngredientStation(this, col, row, "bun", "/res/ingredient/bun.png")
                );
                else if (t == 10) ingredientStations.add(
                    new IngredientStation(this, col, row, "meat", "/res/ingredient/meat.png")
                );
                else if (t == 11) ingredientStations.add(
                    new IngredientStation(this, col, row, "cheese", "/res/ingredient/cheese.png")
                );
                else if (t == 12) ingredientStations.add(
                    new IngredientStation(this, col, row, "lettuce", "/res/ingredient/lettuce.png")
                );
                else if (t == 13) ingredientStations.add(
                    new IngredientStation(this, col, row, "tomato", "/res/ingredient/tomato.png")
                );
            }
        }

        // pair neighboring washing stations horizontally (left=washer, right=output)
        for (WashingStation ws : washingStations) {
            for (WashingStation ws2 : washingStations) {
                if (ws != ws2 && ws.row == ws2.row && ws2.col == ws.col + 1) {
                    ws.isWasher = true;
                    ws2.isWasher = false;
                    ws.linkedOutput = ws2;
                }
            }
        }
    }

    // -----------------------
    // MAIN UPDATE (gabungan)
    // -----------------------

    public void update() {

        long now = System.currentTimeMillis();
        long delta = now - lastUpdateTime;
        lastUpdateTime = now;

        // UI states: tidak menyentuh timer/stageConfig/order/station timers
        if (gameState == titleState) {
            updateTitleState();
            return;
        }
        if (gameState == instructionState) {
            updateInstructionState();
            return;
        }
        if (gameState == stageSelectState) {
            updateStageSelect();
            return;
        }
        if (gameState == stagePreviewState) {
            updateStagePreview();
            return;
        }
        if (gameState == resultState) {
            updateResultState();
            return;
        }

        // playState
        if (gameState == playState) {

            // Stage timer (baru)
            long elapsedTime = System.currentTimeMillis() - startTime;
            remainingTimeMillis = (stageConfig.gameDurationSeconds * 1000L) - elapsedTime;

            if (remainingTimeMillis <= 0) {
                remainingTimeMillis = 0;

                boolean pass = score >= currentStage.targetScore;

                if (pass) {
                    currentStage.isCleared = true;
                    int idx = stageManager.stages.indexOf(currentStage);
                    stageManager.unlockNext(idx);
                }

                gameState = resultState;
                return; // jangan update order/station setelah stage selesai
            }

            // Switch player (tidak ubah movement; hanya pilih aktif)
            if(keyH.switchPressed){
                activePlayerIndex  = (activePlayerIndex + 1) % players.length;
                keyH.switchPressed = false;
            }

            // ===== fungsionalitas lama yang harus berjalan setiap frame =====

            // 1) Update order timers (agar countdown jalan)
            if (orderManager != null) {
                orderManager.update(delta);
            }

            // 2) Update dirty plate timers
            for (PlateStorage ps : plateStorages) {
                ps.update(delta);
            }

            // 3) Update washing timers
            for (WashingStation ws : washingStations) {
                ws.update(delta);
            }

            // Update cooking stations
            for (CookingStation cs : cookingStations) {
                cs.update();
            }

            // Update all players
            for (int i = 0; i < players.length; i++) {
                if (players[i] != null) {
                    players[i].update();
                }
            }
        }
        // gameOverState dibiarkan (tidak dipakai dalam stage flow)
    }

    public int getXforCenteredText(Graphics2D g2, String text) {
        int length = (int)g2.getFontMetrics().getStringBounds(text, g2).getWidth();
        int x = screenWidth / 2 - length / 2;
        return x;
    }

    public void paintComponent(Graphics g){
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D)g;

        if (gameState == stageSelectState) {
            drawStageSelectScreen(g2);
            g2.dispose();
            return;
        }

        if (gameState == stagePreviewState) {
            drawStagePreviewScreen(g2);
            g2.dispose();
            return;
        }

        if (gameState == resultState) {
            drawResultScreen(g2);
            g2.dispose();
            return;
        }

        if (gameState == titleState) {
            drawTitleScreen(g2);
        } else if (gameState == playState) {

            // DRAW TILES
            tileM.draw(g2);

            // DRAW floor items (items dropped on floor)
            if (floorItem != null && floorItemImage != null) {
                for (int col = 0; col < maxScreenCol; col++) {
                    for (int row = 0; row < maxScreenRow; row++) {
                        String key = floorItem[col][row];
                        if (key != null) {
                            java.awt.image.BufferedImage img = floorItemImage[col][row];
                            if (img != null) {
                                int x = col * tileSize;
                                int y = row * tileSize;
                                int w = tileSize / 2;
                                int h = tileSize / 2;
                                int ix = x + (tileSize - w) / 2;
                                int iy = y + (tileSize - h) / 2;
                                g2.drawImage(img, ix, iy, w, h, null);
                            } else {
                                g2.setColor(Color.WHITE);
                                g2.setFont(new Font("Arial", Font.PLAIN, 12));
                                g2.drawString(
                                    key,
                                    col * tileSize + 8,
                                    row * tileSize + tileSize / 2
                                );
                            }
                        }
                    }
                }
            }


            // DRAW INGREDIENT STATIONS (WAJIB DI SINI)
            for (IngredientStation is : ingredientStations) {
                is.draw(g2, this);
            }


            // DRAW trash stations (optional overlay)
            for (TrashStation ts : trashStations) {
                ts.draw(g2, this);
            }

            // DRAW cooking stations
            for (CookingStation cs : cookingStations) {
                cs.drawAtStation(g2, this);
            }

            // DRAW plate storages
            for (PlateStorage ps : plateStorages) ps.draw(g2, this);

            // DRAW assembly stations
            for (AssemblyStation a : assemblyStations) a.draw(g2, this);

            // Washing
            for (WashingStation ws : washingStations) ws.draw(g2, this);

            // DRAW PLAYERS + carried pan overlay (legacy behavior tetap)
            for(int i = 0; i < players.length; i++){
                players[i].draw(g2);

                for (CookingStation cs : cookingStations) {
                    cs.drawIfCarriedByPlayer(g2, this, i, players[i]);
                }

                for (AssemblyStation a : assemblyStations) a.draw(g2, this);
                for (PlateStorage ps : plateStorages) ps.draw(g2, this);
            }

            // DRAW UI / TIMER
            long seconds = remainingTimeMillis / 1000;
            long minutes = seconds / 60;
            seconds = seconds % 60;

            DecimalFormat dFormat = new DecimalFormat("00");
            String timeText = dFormat.format(minutes) + ":" + dFormat.format(seconds);

            drawText(g2, "Time: " + timeText, 10, 20, new Font("Arial", Font.BOLD, 20), Color.WHITE);
            drawText(g2, "Player Aktif: " + (activePlayerIndex  == 0 ? "Merah" : "Biru"),
                    10, 45, new Font("Arial", Font.PLAIN, 16), Color.WHITE);

            String holdingText = "Holding: " + (players[activePlayerIndex].heldItem == null ? "None" : players[activePlayerIndex].heldItem);
            drawText(g2, holdingText, 10, 70, new Font("Arial", Font.PLAIN, 16), Color.WHITE);

            // draw orders UI bottom-left
            if (orderManager != null) {
                drawOrdersUI(g2);
            }

        } else if (gameState == instructionState){
            drawInstructionScreen(g2);
        }

        g2.dispose();
    }

    // -----------------------
    // DRAW SCREENS (baru)
    // -----------------------

    public void drawTitleScreen(Graphics2D g2) {
        g2.setColor(new Color(0, 50, 0));
        g2.fillRect(0, 0, screenWidth, screenHeight);

        g2.setFont(new Font("Arial", Font.BOLD, 70));
        g2.setColor(Color.WHITE);
        String text = "NimonsCooked";
        int x = getXforCenteredText(g2, text);
        int y = tileSize * 2;
        g2.drawString(text, x, y);

        g2.setFont(new Font("Arial", Font.PLAIN, 32));
        g2.setColor(Color.YELLOW);

        text = "START GAME";
        x = getXforCenteredText(g2, text);
        y += tileSize * 3;
        g2.drawString(text, x, y);
        if(commandNum == 0) g2.drawString(">", x - tileSize, y);

        text = "HOW TO PLAY";
        x = getXforCenteredText(g2, text);
        y += tileSize;
        g2.drawString(text, x, y);
        if(commandNum == 1) g2.drawString(">", x - tileSize, y);

        text = "EXIT";
        x = getXforCenteredText(g2, text);
        y += tileSize;
        g2.drawString(text, x, y);
        if(commandNum == 2) g2.drawString(">", x - tileSize, y);
    }

    public void drawInstructionScreen(Graphics2D g2) {
        g2.setColor(new Color(0, 0, 50));
        g2.fillRect(0, 0, screenWidth, screenHeight);

        g2.setFont(new Font("Arial", Font.BOLD, 50));
        g2.setColor(Color.WHITE);
        String title = "HOW TO PLAY";
        int x = getXforCenteredText(g2, title);
        int y = tileSize * 1;
        g2.drawString(title, x, y);

        g2.setFont(new Font("Arial", Font.PLAIN, 24));
        g2.setColor(Color.LIGHT_GRAY);

        int marginX = tileSize;
        y += tileSize * 1.5;

        drawText(g2, "Kontrol Pemain:", marginX, y, g2.getFont(), g2.getColor());
        y += tileSize * 0.7;
        drawText(g2, "- Movement: W (Atas), A (Kiri), S (Bawah), D (Kanan)", marginX, y, g2.getFont(), g2.getColor());
        y += tileSize * 0.7;
        drawText(g2, "- Switch Player: SPACE", marginX, y, g2.getFont(), g2.getColor());
        y += tileSize * 0.7;
        drawText(g2, "- Interaksi: E (ambil/taruh bahan / cooking station)", marginX, y, g2.getFont(), g2.getColor());
        y += tileSize * 0.7;
        drawText(g2, "- Interaksi: P (memotong bahan / cutting station)", marginX, y, g2.getFont(), g2.getColor());
        y += tileSize * 0.7;
        drawText(g2, "- Interaksi: Q (drop/jatuhkan bahan)", marginX, y, g2.getFont(), g2.getColor());
        y += tileSize * 0.7;
        drawText(g2, "", marginX, y, g2.getFont(), g2.getColor());
        y += tileSize * 0.7;
        drawText(g2, "Selesaikan semua order sesuai dengan waktunya!", marginX, y, g2.getFont(), g2.getColor());

        y += tileSize * 2;
        g2.setColor(Color.YELLOW);
        String back = "Tekan ENTER untuk kembali ke Main Menu...";
        drawText(g2, back, getXforCenteredText(g2, back), y, g2.getFont(), g2.getColor());
    }

    public void drawOrdersUI(Graphics2D g2) {
        int startX = 10;
        int startY = screenHeight - 10;
        g2.setFont(new java.awt.Font("Arial", java.awt.Font.PLAIN, 14));
        g2.setColor(java.awt.Color.WHITE);
        int gap = 18;
        int drawn = 0;

        for (int i = 0; i < orderManager.activeOrders.size() && drawn < 3; i++) {
            Order o = orderManager.activeOrders.get(i);
            String timePart;
            if (i == 0) {
                timePart = " (" + o.getRemainingSeconds() + "s)";
            } else {
                timePart = " (waiting)";
            }
            String text = "[" + o.position + "] " + o.recipeName + timePart;
            g2.drawString(text, startX, startY - (drawn * gap));
            drawn++;
        }
    }

    public void drawStageSelectScreen(Graphics2D g2) {
        g2.setColor(Color.BLACK);
        g2.fillRect(0, 0, screenWidth, screenHeight);

        g2.setFont(new Font("Arial", Font.BOLD, 36));
        g2.setColor(Color.WHITE);
        g2.drawString("Stage Select", 240, 60);

        g2.setFont(new Font("Arial", Font.PLAIN, 24));

        int y = 140;

        for (int i = 0; i < stageManager.stages.size(); i++) {

            StageMeta s = stageManager.stages.get(i);

            if (i == selectedStageIndex) g2.setColor(Color.YELLOW);
            else if (!s.isUnlocked) g2.setColor(Color.GRAY);
            else if (s.isCleared) g2.setColor(Color.GREEN);
            else g2.setColor(Color.WHITE);

            g2.drawString(s.name + "  (Target: " + s.targetScore + ")", 80, y);
            y += 40;
        }

        g2.setFont(new Font("Arial", Font.ITALIC, 18));
        g2.setColor(Color.LIGHT_GRAY);
        g2.drawString("ENTER: Preview | SPACE: Back", 200, screenHeight - 40);
    }

    public void drawStagePreviewScreen(Graphics2D g2) {
        g2.setColor(new Color(20,20,20));
        g2.fillRect(0, 0, screenWidth, screenHeight);

        g2.setFont(new Font("Arial", Font.BOLD, 32));
        g2.setColor(Color.WHITE);
        g2.drawString("Preview: " + currentStage.name, 140, 60);

        g2.setFont(new Font("Arial", Font.PLAIN, 24));
        g2.drawString("Target Score: " + currentStage.targetScore, 200, 120);

        g2.setFont(new Font("Arial", Font.PLAIN, 20));
        g2.setColor(Color.LIGHT_GRAY);
        g2.drawString("ENTER: Start | SPACE: Back", 200, screenHeight - 50);

        tileM.draw(g2);
    }

    public void drawResultScreen(Graphics2D g2) {
        g2.setColor(new Color(0, 0, 0, 180));
        g2.fillRect(0, 0, screenWidth, screenHeight);

        boolean pass = score >= currentStage.targetScore;

        g2.setFont(new Font("Arial", Font.BOLD, 48));
        g2.setColor(pass ? Color.GREEN : Color.RED);
        g2.drawString(pass ? "STAGE CLEARED!" : "STAGE FAILED!", 140, 120);

        g2.setFont(new Font("Arial", Font.PLAIN, 28));
        g2.setColor(Color.WHITE);
        g2.drawString("Score: " + score, 220, 190);
        g2.drawString("Target: " + currentStage.targetScore, 220, 230);
        g2.drawString("Orders Success: " + ordersCompleted, 220, 270);
        g2.drawString("Orders Failed: " + ordersFailed, 220, 310);

        g2.setFont(new Font("Arial", Font.PLAIN, 20));
        g2.setColor(Color.YELLOW);
        g2.drawString("ENTER: Back to Stage Select", 180, 380);
        g2.drawString("SPACE: Replay Stage", 220, 410);
    }
}
