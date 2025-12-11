package main;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.imageio.ImageIO;

import entity.Player;

public class PlateStorage {
    public int col, row;
    public int x, y;
    public int cleanCount = 5; // jumlah piring bersih
    public int dirtyCount = 0; // piring kotor sudah tersedia di storage (tidak boleh diambil)
    GamePanel gp;

    public static BufferedImage imgCleanPlate = null;
    public static BufferedImage imgDirtyPlate = null;

    // pending dirty plates scheduled to arrive after some millis
    private static class DirtyTimer {
        long remainingMillis;
        public DirtyTimer(long ms){ remainingMillis = ms; }
    }
    private List<DirtyTimer> pendingDirty = new ArrayList<>();

    public PlateStorage(GamePanel gp, int col, int row, int initialCount) {
        this.gp = gp;
        this.col = col;
        this.row = row;
        this.x = col * gp.tileSize;
        this.y = row * gp.tileSize;
        this.cleanCount = initialCount;
        loadImage();
    }

    private void loadImage(){
        if (imgCleanPlate == null) {
            try {
                imgCleanPlate = ImageIO.read(getClass().getResourceAsStream("/res/ingredient/clean_plate.png"));
            } catch (IOException e) {}
        }
        if (imgDirtyPlate == null) {
            try {
                imgDirtyPlate = ImageIO.read(getClass().getResourceAsStream("/res/ingredient/dirty_plate.png"));
            } catch (IOException e) {}
        }
    }

    // Interact: if player empty hand and cleanCount>0 -> give player a clean plate
    // return true if acted
    public boolean interact(Player player) {
        if (player.heldItem != null) return false; // must be empty to pick
        if (cleanCount <= 0) return false;
        player.heldItem = "plate";
        player.heldItemImage = imgCleanPlate;
        player.plateStack.clear();
        cleanCount--;
        return true;
    }

    // schedule a dirty plate to be added after seconds secs
    public void scheduleDirtyPlate(int seconds) {
        pendingDirty.add(new DirtyTimer(seconds * 1000L));
    }

    // called every frame from GamePanel.update(delta)
    public void update(long deltaMillis) {
        if (pendingDirty.isEmpty()) return;
        Iterator<DirtyTimer> it = pendingDirty.iterator();
        while (it.hasNext()) {
            DirtyTimer dt = it.next();
            dt.remainingMillis -= deltaMillis;
            if (dt.remainingMillis <= 0) {
                // add to top: count dirty increases (we keep cleanCount separate)
                dirtyCount++;
                it.remove();
            }
        }
    }

    public void draw(Graphics2D g2, GamePanel gp) {
        if (imgCleanPlate != null) {
            g2.drawImage(imgCleanPlate, x + gp.tileSize/4, y + gp.tileSize/4, gp.tileSize/2, gp.tileSize/2, null);
        }
        g2.setColor(java.awt.Color.WHITE);
        g2.setFont(new java.awt.Font("Arial", java.awt.Font.BOLD, 14));
        g2.drawString("C:"+cleanCount, x + 4, y + gp.tileSize - 18);
        g2.drawString("D:"+dirtyCount, x + 4, y + gp.tileSize - 4);
        // optionally show pending timer count
        if (!pendingDirty.isEmpty()) {
            g2.drawString("p:"+pendingDirty.size(), x + gp.tileSize - 38, y + gp.tileSize - 4);
        }
    }

    // If you want players to be able to pick dirty plate later (e.g. for cleaning), you can add method.
    // For now behavior is: interact() only gives clean plates. Dirty plates are just tracked visually.
}
