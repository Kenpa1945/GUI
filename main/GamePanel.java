package main;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Font;
import java.text.DecimalFormat;
import java.util.ArrayList;

import javax.swing.JPanel;

import entity.Player;
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

    // Menu State
    public int commandNum = 0; // 0: Start Game, 1: How to Play, 2: Exit

    // Timer
    private final int GAME_DURATION_SECONDS = 1 * 60; // 3 menit
    private long startTime;
    private long remainingTimeMillis;

    // Tile
    public TileManager tileM = new TileManager(this);

    // Key Handler
    KeyHandler keyH = new KeyHandler();

    // Thread
    Thread gameThread;

    // Collision
    public CollisionChecker cChecker = new CollisionChecker(this);

    //Player
    public Player[] players = new Player[2];
    public int acivePlayerIndex = 0;

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

        gameState = titleState; 
        startTime = System.currentTimeMillis(); 
        remainingTimeMillis = (long)GAME_DURATION_SECONDS * 1000;

        // create cooking stations by scanning map tiles (tile number 3)
        for (int col = 0; col < maxScreenCol; col++) {
            for (int row = 0; row < maxScreenRow; row++) {
                if (tileM.mapTileNum[col][row] == 3) {
                    cookingStations.add(new CookingStation(this, col, row));
                }
            }
        }
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
        if(keyH.upPressed){
            commandNum--;
            if(commandNum < 0){
                commandNum = 2; // Loop ke "Exit"
            }
            keyH.upPressed = false;
        }
        if(keyH.downPressed){
            commandNum++;
            if(commandNum > 2){
                commandNum = 0; // Loop ke "Start Game"
            }
            keyH.downPressed = false;
        } 
        
        if(keyH.enterPressed){
            if(commandNum == 0){ // Start Game
                // Reset/setup ulang nilai game sebelum mulai
                players[0].setDefaultValues(); 
                players[1].setDefaultValues();
                players[1].x = 386; // Posisi Blue Player 
                players[1].y = 239;
                
                // Reset Timer
                startTime = System.currentTimeMillis(); 
                remainingTimeMillis = (long)GAME_DURATION_SECONDS * 1000;

                // Reset cooking stations: place frying pans back and clear items
                for (CookingStation cs : cookingStations) {
                    cs.panPresent = true;
                    cs.panOwner = -1;
                    cs.panItem = null;
                    cs.panTimer = 0;
                }

                gameState = playState;
            } else if (commandNum == 1){ // How to Play
                gameState = instructionState;
            } else if (commandNum == 2){ // Exit
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

    public void update(){
        if(gameState == titleState){
            updateTitleState();
        }
        else if(gameState == playState) {
            
            // --- Cek Timer ---
            long elapsedTime = System.currentTimeMillis() - startTime;
            remainingTimeMillis = ((long)GAME_DURATION_SECONDS * 1000) - elapsedTime;

            if (remainingTimeMillis <= 0) {
                remainingTimeMillis = 0; // Pastikan tidak negatif
                gameState = gameOverState; // Pindah ke state Game Over
                System.out.println("Time's Up!!!"); // Output konsol untuk testing
            }
            // --- End Cek Timer ---

            if(keyH.switchPressed == true){
                acivePlayerIndex = (acivePlayerIndex + 1) % players.length;
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
        
        if (gameState == titleState) {
            drawTitleScreen(g2);
        } else if (gameState == playState) {
            // DRAW TILES
            tileM.draw(g2);

            // DRAW cooking stations (pan at station)
            for (CookingStation cs : cookingStations) {
                cs.drawAtStation(g2, this);
            }

            // DRAW PLAYERS (players should draw carried pans above head)
            for(int i = 0; i < players.length; i++){
                players[i].draw(g2);
                // after drawing player, draw any pan they carry (so pan icon overlays properly)
                for (CookingStation cs : cookingStations) {
                    cs.drawIfCarriedByPlayer(g2, this, i, players[i]);
                }
            }
            
            // DRAW UI / TIMER
            long seconds = remainingTimeMillis / 1000;
            long minutes = seconds / 60;
            seconds = seconds % 60;
            
            DecimalFormat dFormat = new DecimalFormat("00");
            String timeText = dFormat.format(minutes) + ":" + dFormat.format(seconds);

            drawText(g2, "Time: " + timeText, 10, 20, new Font("Arial", Font.BOLD, 20), Color.WHITE);
            drawText(g2, "Player Aktif: " + (acivePlayerIndex == 0 ? "Merah" : "Biru"), 10, 45, new Font("Arial", Font.PLAIN, 16), Color.WHITE);
            String holdingText = "Holding: " + (players[acivePlayerIndex].heldItem == null ? "None" : players[acivePlayerIndex].heldItem);
            drawText(g2, holdingText, 10, 70, new Font("Arial", Font.PLAIN, 16), Color.WHITE);

        }
        // --- DRAW GAME OVER SCREEN ---
        else if (gameState == gameOverState) {
            // Lapisan hitam transparan
            g2.setColor(new Color(0, 0, 0, 150));
            g2.fillRect(0, 0, screenWidth, screenHeight);
            
            // Teks Game Over
            String endText = "STAGE OVER!";
            Font endFont = new Font("Arial", Font.BOLD, 60);
            g2.setFont(endFont);
            g2.setColor(Color.RED);
            
            // Hitung posisi tengah
            int x = getXforCenteredText(g2, endText);
            int y = screenHeight / 2;
            
            g2.drawString(endText, x, y);
            
            // Teks "Time's Up!!!"
            String reasonText = "TIME'S UP!!!";
            Font reasonFont = new Font("Arial", Font.PLAIN, 30);
            g2.setFont(reasonFont);
            g2.setColor(Color.WHITE);
            x = getXforCenteredText(g2, reasonText);
            g2.drawString(reasonText, x, y + 50);

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
}
