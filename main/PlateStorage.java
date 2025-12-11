package main;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;

import javax.imageio.ImageIO;
import entity.Player;

public class PlateStorage {
    public int col, row;
    public int x, y;
    public int count = 5; // default jumlah piring di storage (ubah sesuai kebutuhan)
    GamePanel gp;

    public static BufferedImage imgCleanPlate = null;

    public PlateStorage(GamePanel gp, int col, int row, int initialCount) {
        this.gp = gp;
        this.col = col;
        this.row = row;
        this.x = col * gp.tileSize;
        this.y = row * gp.tileSize;
        this.count = initialCount;
        loadImage();
    }

    private void loadImage(){
        if (imgCleanPlate != null) return;
        try {
            imgCleanPlate = ImageIO.read(getClass().getResourceAsStream("/res/ingredient/clean_plate.png"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Interact: if player empty hand and count>0 -> give player a plate
    // return true if acted
    public boolean interact(Player player) {
        if (player.heldItem != null) return false; // must be empty to pick
        if (count <= 0) return false;
        // create plate on player: marker heldItem="plate", create empty plateStack
        player.heldItem = "plate";
        player.heldItemImage = imgCleanPlate;
        player.plateStack.clear(); // should be empty
        count--;
        return true;
    }

    public void draw(Graphics2D g2, GamePanel gp) {
        // draw the clean plate icon and count
        if (imgCleanPlate != null) {
            g2.drawImage(imgCleanPlate, x + gp.tileSize/4, y + gp.tileSize/4, gp.tileSize/2, gp.tileSize/2, null);
        }
        // draw count text (white)
        g2.setColor(java.awt.Color.WHITE);
        g2.setFont(new java.awt.Font("Arial", java.awt.Font.BOLD, 14));
        g2.drawString("x"+count, x + gp.tileSize - 28, y + gp.tileSize - 8);
    }
}
