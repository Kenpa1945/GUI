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
    public final int titleState = 0; // State baru untuk Main Menu
    public final int playState = 1;
    public final int gameOverState = 2;
    public final int instructionState = 3; // State baru untuk How to Play
    public final int stageSelectState = 4;
    public final int stagePreviewState = 5;
    public final int resultState = 6;
    public int selectedStage = 1;
    public StageConfig stageConfig;
    


    // Menu State
    public int commandNum = 0; // 0: Start Game, 1: How to Play, 2: Exit


    // Timer
    // private final int GAME_DURATION_SECONDS = 1 * 60; // 3 menit
    private long startTime;
    public long remainingTimeMillis;
    public long lastUpdateTime;


    // Tile
    public TileManager tileM = new TileManager(this);

    // Key Handler
    KeyHandler keyH = new KeyHandler();

    // Thread
    Thread gameThread;

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

    //Player
    public Player[] players = new Player[2];
    public int activePlayerIndex  = 0;

    // Stage system
    public StageManager stageManager = new StageManager();
    public StageMeta currentStage = null;
    public int selectedStageIndex = 0;


    // Cooking stations list
    public ArrayList<CookingStation> cookingStations = new ArrayList<>();

    public GamePanel(){
        this.setPreferredSize(new Dimension(screenWidth, screenHeight));
        this.setBackground(Color.BLACK);
        this.setDoubleBuffered(true); 
        this.addKeyListener(keyH);
        this.setFocusable(true);

        players[0] = new Player(this, keyH, "red");
        players[1] = new Player(this,keyH, "blue");

        players[1].x = 386;
        players[1].y = 239;

        gameState = titleState;     // BUKAN stageSelect
        stageConfig = null;         // akan dibuat di startStage()
        startTime = System.currentTimeMillis(); 
        lastUpdateTime = System.currentTimeMillis();
        remainingTimeMillis = 0L; // belum ada stage yang berjalan


        // create cooking stations by scanning map tiles (tile number 3)
        for (int col = 0; col < maxScreenCol; col++) {
            for (int row = 0; row < maxScreenRow; row++) {
                int t = tileM.mapTileNum[col][row];
                if (t == 3) {
                    cookingStations.add(new CookingStation(this, col, row));
                }
                else if (t == 4){
                    assemblyStations.add(new AssemblyStation(this, col, row));
                }
                else if (t == 8){
                    plateStorages.add(new PlateStorage(this, col, row, 5)); // default 5 plates
                }
                else if (t == 9){
                    trashStations.add(new TrashStation(this, col, row));
                }
                else if (t == 5){
                    servingStations.add(new ServingStation(this, col, row));
                }
                else if (t == 6){
                    washingStations.add(new WashingStation(this, col, row));
                }
            }
        }

        // pair neighboring washing stations horizontally (left=washer, right=output)
        for (WashingStation ws : washingStations) {
            for (WashingStation ws2 : washingStations) {
                if (ws != ws2 && ws.row == ws2.row && ws2.col == ws.col + 1) {
                    // ws left, ws2 right
                    ws.isWasher = true;
                    ws2.isWasher = false;
                    ws.linkedOutput = ws2;
        }
    }
}
        // Order manager

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
        long timer = 0;
        int drawCount = 0;

        while(gameThread != null){

            currentTime = System.nanoTime();
            delta += (currentTime - lastTime) / drawInterval;
            timer += (currentTime - lastTime);
            lastTime = currentTime;

            if(delta >= 1){
                update();
                repaint();
                delta--;
                drawCount++;
            }
        }
    }

    

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
            if (commandNum == 0) {  // START GAME → masuk Stage Select
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

        if (keyH.switchPressed) {
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

    public void startStage(StageMeta stage) {
        // Apply stage config
        stageConfig = StageConfig.forStage(selectedStageIndex + 1);
        orderManager = new OrderManager(this);
        players[0].setDefaultValues();
        players[1].setDefaultValues();
        players[1].x = 386;
        players[1].y = 239;

        score = 0;
        ordersCompleted = 0;
        ordersFailed = 0;

        tileM.loadMap(stage.mapPath);
        rebuildStationsFromMap();

        startTime = System.currentTimeMillis();
        remainingTimeMillis = stageConfig.gameDurationSeconds * 1000L;

        orderManager.activeOrders.clear();
        orderManager.resetSequence();
        orderManager.trySpawnInitial();

        gameState = playState;
    }

    public void rebuildStationsFromMap() {
        cookingStations.clear();
        assemblyStations.clear();
        trashStations.clear();
        plateStorages.clear();
        washingStations.clear();
        servingStations.clear();

        for (int col = 0; col < maxScreenCol; col++) {
            for (int row = 0; row < maxScreenRow; row++) {

                int t = tileM.mapTileNum[col][row];

                if (t == 3) cookingStations.add(new CookingStation(this, col, row));
                else if (t == 4) assemblyStations.add(new AssemblyStation(this, col, row));
                else if (t == 8) plateStorages.add(new PlateStorage(this, col, row, 5));
                else if (t == 9) trashStations.add(new TrashStation(this, col, row));
                else if (t == 5) servingStations.add(new ServingStation(this, col, row));
                else if (t == 6) washingStations.add(new WashingStation(this, col, row));
            }
        }

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




    public void update() {

        long now = System.currentTimeMillis();
        long delta = now - lastUpdateTime;
        lastUpdateTime = now;

        // Update UI states (mereka tidak boleh menyentuh timer / stageConfig)
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


        if(gameState == titleState){
            updateTitleState();
        }
        else if(gameState == playState) {
            
            // --- Cek Timer ---
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
            }

            if(keyH.switchPressed == true){
                activePlayerIndex  = (activePlayerIndex  + 1) % players.length;
                keyH.switchPressed = false;
            }

            // Update cooking stations first (they handle cooking timers)
            for (CookingStation cs : cookingStations) {
                cs.update();
            }

            // Update all players each frame so e.g. cutting/cooking timers linked to players continue correctly
            for (int i = 0; i < players.length; i++) {
                if (players[i] != null) {
                    players[i].update();
                }
            }
        } else if (gameState == gameOverState) {
            // Logika untuk Game Over (misalnya, menunggu input untuk restart)
        }
        else if (gameState == instructionState){
            updateInstructionState();
        }
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
            return;
        }

        if (gameState == stagePreviewState) {
            drawStagePreviewScreen(g2);
            return;
        }

        if (gameState == resultState) {
            drawResultScreen(g2);
            return;
        }

        if (gameState == titleState) {
            drawTitleScreen(g2);
        } else if (gameState == playState) {
            // DRAW TILES
            tileM.draw(g2);

            // DRAW trash stations (optional overlay)
            for (TrashStation ts : trashStations) {
                ts.draw(g2, this);
            }

            // DRAW cooking stations (pan at station)
            for (CookingStation cs : cookingStations) {
                cs.drawAtStation(g2, this);
            }

            // DRAW plate storages
            for (PlateStorage ps : plateStorages) ps.draw(g2, this);

            // DRAW assembly stations
            for (AssemblyStation a : assemblyStations) a.draw(g2, this);

            // Washing
            for (WashingStation ws : washingStations) ws.draw(g2, this);

            // DRAW PLAYERS (players should draw carried pans above head)
            for(int i = 0; i < players.length; i++){
                players[i].draw(g2);
                // after drawing player, draw any pan they carry (so pan icon overlays properly)
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
            drawText(g2, "Player Aktif: " + (activePlayerIndex  == 0 ? "Merah" : "Biru"), 10, 45, new Font("Arial", Font.PLAIN, 16), Color.WHITE);
            String holdingText = "Holding: " + (players[activePlayerIndex].heldItem == null ? "None" : players[activePlayerIndex].heldItem);
            drawText(g2, holdingText, 10, 70, new Font("Arial", Font.PLAIN, 16), Color.WHITE);

            // draw orders UI bottom-left
            drawOrdersUI(g2);

        }
        // --- DRAW GAME OVER SCREEN ---
        else if (gameState == gameOverState) {
            // Lapisan hitam transparan
    g2.setColor(new Color(0, 0, 0, 150));
    g2.fillRect(0, 0, screenWidth, screenHeight);

    // Teks Game Over (besar)
    String endText = "STAGE OVER!";
    Font endFont = new Font("Arial", Font.BOLD, 60);
    g2.setFont(endFont);
    g2.setColor(Color.RED);
    int endX = getXforCenteredText(g2, endText);
    int endY = screenHeight / 2 - 60; // sedikit ke atas dari tengah
    g2.drawString(endText, endX, endY);

    // Teks alasan / subjudul
    String reasonText = "TIME'S UP!!!";
    Font reasonFont = new Font("Arial", Font.PLAIN, 30);
    g2.setFont(reasonFont);
    g2.setColor(Color.WHITE);
    int reasonX = getXforCenteredText(g2, reasonText);
    int reasonY = endY + 50;
    g2.drawString(reasonText, reasonX, reasonY);

    // --- Statistik akhir ---
    // Hitung posisi vertikal dasar untuk menampilkan statistik
    int statsBaseY = reasonY + 60;
    g2.setFont(new Font("Arial", Font.PLAIN, 28));
    g2.setColor(Color.WHITE);

    // Total score
    String scoreText = "Total Score: " + score;
    int scoreX = getXforCenteredText(g2, scoreText);
    g2.drawString(scoreText, scoreX, statsBaseY);

    // Orders completed
    String completedText = "Orders Completed: " + ordersCompleted;
    int completedX = getXforCenteredText(g2, completedText);
    g2.setFont(new Font("Arial", Font.PLAIN, 22));
    g2.drawString(completedText, completedX, statsBaseY + 40);

    // Orders failed
    String failedText = "Orders Failed: " + ordersFailed;
    int failedX = getXforCenteredText(g2, failedText);
    g2.drawString(failedText, failedX, statsBaseY + 70);
        }
        else if (gameState == instructionState){
            drawInstructionScreen(g2);
        }

        g2.dispose();
    }

    // ... drawTitleScreen, drawInstructionScreen (unchanged from your original) ...
    public void drawTitleScreen(Graphics2D g2) {
        // original code...
        g2.setColor(new Color(0, 50, 0));
        g2.fillRect(0, 0, screenWidth, screenHeight);
        // (copy original code or keep as is)
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
        if(commandNum == 0) {
            g2.drawString(">", x - tileSize, y);
        }

        text = "HOW TO PLAY";
        x = getXforCenteredText(g2, text);
        y += tileSize;
        g2.drawString(text, x, y);
        if(commandNum == 1) {
            g2.drawString(">", x - tileSize, y);
        }

        text = "EXIT";
        x = getXforCenteredText(g2, text);
        y += tileSize;
        g2.drawString(text, x, y);
        if(commandNum == 2) {
            g2.drawString(">", x - tileSize, y);
        }
    }

    public void drawInstructionScreen(Graphics2D g2) {
        // original instruction screen (copy if you changed earlier)
        g2.setColor(new Color(0, 0, 50)); // Background biru gelap
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
        drawText(g2, "- Gerak: W (Atas), A (Kiri), S (Bawah), D (Kanan)", marginX, y, g2.getFont(), g2.getColor());
        y += tileSize * 0.7;
        drawText(g2, "- Switch Player: SPACE", marginX, y, g2.getFont(), g2.getColor());
        y += tileSize * 0.7;
        drawText(g2, "- Interaksi: E (ambil/taruh bahan / cooking station)", marginX, y, g2.getFont(), g2.getColor());
        
        y += tileSize * 2;
        g2.setColor(Color.YELLOW);
        drawText(g2, "Tekan ENTER untuk kembali ke Main Menu...", getXforCenteredText(g2, "Tekan ENTER untuk kembali ke Main Menu..."), y, g2.getFont(), g2.getColor());
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
