package main;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Queue;

import javax.imageio.ImageIO;

import entity.Player;

public class WashingStation {
    public int col, row;
    public int x, y;
    GamePanel gp;

    // type: washer (true) or output (false)
    public boolean isWasher = true;

    // washer fields
    private Queue<Integer> queue = new ArrayDeque<>(); // each int represents a single dirty plate to wash
    public int processingPlayer = -1; // playerIndex currently doing the washing (or -1)
    public long currentPlateRemainingMillis = 0L;
    public final long WASH_MILLIS = 3000L; // 3 seconds per plate

    // output fields (for isWasher==false)
    public int outputCleanCount = 0;

    // images (optional)
    public static BufferedImage imgWasher = null;
    public static BufferedImage imgOutput = null;
    public static BufferedImage imgDirtyPlate = null;
    public static BufferedImage imgCleanPlate = null;

    // linked output (for washers): reference to the output station where cleaned plates are deposited
    public WashingStation linkedOutput = null;

    public WashingStation(GamePanel gp, int col, int row) {
        this.gp = gp;
        this.col = col;
        this.row = row;
        this.x = col * gp.tileSize;
        this.y = row * gp.tileSize;
        loadImages();
    }

    private void loadImages() {
        if (imgWasher != null && imgOutput != null) return;
        try { imgWasher = ImageIO.read(getClass().getResourceAsStream("/res/tiles/wash.png")); } catch(IOException e){}
        try { imgOutput = ImageIO.read(getClass().getResourceAsStream("/res/tiles/plate.png")); } catch(IOException e){}
        try { imgDirtyPlate = ImageIO.read(getClass().getResourceAsStream("/res/ingredient/dirty_plate.png")); } catch(IOException e){}
        try { imgCleanPlate = ImageIO.read(getClass().getResourceAsStream("/res/ingredient/clean_plate.png")); } catch(IOException e){}
    }

    // Called when player interacts (E) with this station
    // Behavior:
    // - If this is OUTPUT: if player empty -> give 1 clean plate from outputCleanCount (if any)
    // - If this is WASHER: 
    //    * if player holds dirty_plate -> transfer all player's dirty plates to this.queue (return true)
    //    * else if queue>0 and player empty -> start/resume washing by setting processingPlayer to this player's index (washing will tick while that player remains adjacent)
    public boolean interact(int playerIdx, Player player) {
        if (isOutput()) {
            // output station: give a clean plate if available and player empty hand
            if (player.heldItem != null) return false;
            if (outputCleanCount <= 0) return false;
            // give one clean plate to player
            player.heldItem = "plate";
            player.heldItemImage = imgCleanPlate;
            player.plateStack.clear();
            outputCleanCount--;
            return true;
        } else {
            // washer behavior
            // if player carrying dirty plates, transfer all to washer.queue
            if ("dirty_plate".equals(player.heldItem) && player.dirtyPlateCount > 0) {
                for (int i = 0; i < player.dirtyPlateCount; i++) queue.add(1); // add each plate
                player.dirtyPlateCount = 0;
                player.heldItem = null;
                player.heldItemImage = null;
                // do NOT auto-start washing here; player must press E again to start washing (or we can start immediately)
                // We'll start immediately by setting processingPlayer to this playerIdx so washing begins if they stay.
                processingPlayer = playerIdx;
                if (currentPlateRemainingMillis <= 0 && !queue.isEmpty()) {
                    currentPlateRemainingMillis = WASH_MILLIS;
                }
                return true;
            }

            // if player empty and there's queue -> player can start/resume washing
            if (player.heldItem == null && !queue.isEmpty()) {
                processingPlayer = playerIdx;
                if (currentPlateRemainingMillis <= 0) currentPlateRemainingMillis = WASH_MILLIS;
                return true;
            }

            // nothing to do
            return false;
        }
    }

    // Should be called every frame (GamePanel.update with deltaMillis)
    public void update(long deltaMillis) {
        if (isOutput()) return;

        // If no queued plates or no processing player assigned -> do nothing
        if (queue.isEmpty()) return;
        if (processingPlayer < 0 || processingPlayer >= gp.players.length) return;

        // Ensure the processing player is still adjacent to this washer. If not, pause.
        Player p = gp.players[processingPlayer];
        if (!isPlayerAdjacent(p)) {
            // pause: don't decrement currentPlateRemainingMillis
            return;
        }

        // decrement timer
        currentPlateRemainingMillis -= deltaMillis;
        if (currentPlateRemainingMillis <= 0) {
            // finished one plate
            queue.poll(); // remove one dirty plate from queue

            // deposit cleaned plate to linkedOutput if exists, otherwise to nearest PlateStorage
            if (linkedOutput != null) {
                linkedOutput.outputCleanCount++;
            } else {
                // fallback: deposit to nearest PlateStorage (index 0 if none nearby)
                if (!gp.plateStorages.isEmpty()) gp.plateStorages.get(0).cleanCount++;
            }

            // prepare next plate if any
            if (!queue.isEmpty()) {
                currentPlateRemainingMillis = WASH_MILLIS; // start next (will continue only if player stays)
            } else {
                currentPlateRemainingMillis = 0;
                // optionally release processingPlayer so anyone else can start later
                processingPlayer = -1;
            }
        }
    }

    // helper: check adjacency (Manhattan <= 1) between player center and station tile
    private boolean isPlayerAdjacent(Player p) {
        int centerX = p.x + p.solidArea.x + p.solidArea.width/2;
        int centerY = p.y + p.solidArea.y + p.solidArea.height/2;
        int pcol = centerX / gp.tileSize;
        int prow = centerY / gp.tileSize;
        int dist = Math.abs(pcol - this.col) + Math.abs(prow - this.row);
        return dist <= 1;
    }

    public boolean isOutput() { return !isWasher; }

    // draw (optional progress)
    public void draw(Graphics2D g2, GamePanel gp) {
        if (isOutput()) {
            if (imgOutput != null) {
                g2.drawImage(imgOutput, x, y, gp.tileSize, gp.tileSize, null);
            }
            // draw count of ready clean plates
            g2.setColor(java.awt.Color.WHITE);
            g2.setFont(new java.awt.Font("Arial", java.awt.Font.BOLD, 14));
            g2.drawString("C:"+outputCleanCount, x + 4, y + gp.tileSize - 6);
        } else {
            if (imgWasher != null) {
                g2.drawImage(imgWasher, x, y, gp.tileSize, gp.tileSize, null);
            }
            // show queue size and progress if any
            g2.setColor(java.awt.Color.WHITE);
            g2.setFont(new java.awt.Font("Arial", java.awt.Font.BOLD, 12));
            g2.drawString("Q:" + queue.size(), x + 4, y + gp.tileSize - 22);

            if (currentPlateRemainingMillis > 0) {
                double ratio = 1.0 - (double)currentPlateRemainingMillis / (double)WASH_MILLIS;
                int barW = gp.tileSize / 2;
                int barH = 6;
                int bx = x + (gp.tileSize - barW) / 2;
                int by = y - barH - 6;
                g2.setColor(new java.awt.Color(40,40,40,220));
                g2.fillRect(bx, by, barW, barH);
                g2.setColor(java.awt.Color.CYAN);
                g2.fillRect(bx, by, (int)(barW * ratio), barH);
                g2.setColor(java.awt.Color.WHITE);
                g2.drawRect(bx, by, barW, barH);
            }
        }
    }
}
