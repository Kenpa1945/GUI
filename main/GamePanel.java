package main;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;

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
    int FPS = 60;

    // Tile
    TileManager tileM = new TileManager(this);

    // Key Handler
    KeyHandler keyH = new KeyHandler();

    // Thread
    Thread gameThread;

    // Collision
    public CollisionChecker cChecker = new CollisionChecker(this);

    //Player
    public Player[] players = new Player[2];
    public int acivePlayerIndex = 0;

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

    }

    public void startGameThread(){
        gameThread = new Thread(this);
        gameThread.start();
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

    public void update(){
        if(keyH.switchPressed == true){
            acivePlayerIndex = (acivePlayerIndex + 1) % players.length;
            keyH.switchPressed = false;
        }
        players[acivePlayerIndex].update();
    }

    public void paintComponent(Graphics g){
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D)g;
        
        tileM.draw(g2);

        for(int i = 0; i < players.length; i++){
            players[i].draw(g2);
        }

        g2.dispose();
    }
}
