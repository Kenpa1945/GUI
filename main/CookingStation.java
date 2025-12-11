package main;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;

import javax.imageio.ImageIO;

import entity.Player;

public class CookingStation {
    public int col, row;
    public int x, y; // pixel position (top-left)
    GamePanel gp;

    // Pan state
    public boolean panPresent = true; // every cooking station has a frying pan initially
    public int panOwner = -1; // -1 = pan at station, >=0 = carried by player index
    public String panItem = null; // null or "chopped_meat","cooked_meat","burned_meat"
    public int panTimer = 0; // frames elapsed since panItem started cooking (only increments while pan at station)
    public int COOKED_FRAMES;
    public int BURN_FRAMES;

    // images (shared)
    public static BufferedImage imgFryingPan = null;
    public static BufferedImage imgChoppedMeat = null;
    public static BufferedImage imgCookedMeat = null;
    public static BufferedImage imgBurnedMeat = null;

    public CookingStation(GamePanel gp, int col, int row) {
        this.gp = gp;
        this.col = col;
        this.row = row;
        this.x = col * gp.tileSize;
        this.y = row * gp.tileSize;

        COOKED_FRAMES = 12 * gp.FPS; // 12 seconds
        BURN_FRAMES = 24 * gp.FPS;   // 24 seconds

        loadImages();
    }

    private void loadImages() {
        if (imgFryingPan != null) return; // loaded already
        try{
            imgFryingPan = ImageIO.read(getClass().getResourceAsStream("/res/tiles/fryingpan.png"));
        }catch(IOException e){ e.printStackTrace(); }

        try{
            imgChoppedMeat = ImageIO.read(getClass().getResourceAsStream("/res/ingredient/chopped_meat.png"));
        }catch(IOException e){ /* may be null */ }

        try{
            imgCookedMeat = ImageIO.read(getClass().getResourceAsStream("/res/ingredient/cooked_meat.png"));
        }catch(IOException e){ /* may be null */ }

        try{
            imgBurnedMeat = ImageIO.read(getClass().getResourceAsStream("/res/ingredient/burned_meat.png"));
        }catch(IOException e){ /* may be null */ }
    }

    // Called every frame from GamePanel.update()
    public void update() {
        // If no pan or pan carried -> do nothing (cooking paused if carried)
        if (!panPresent) return;
        if (panOwner != -1) return; // paused while carried
        if (panItem == null) return;

        // Only progress timer until burned cap
        if (panTimer < BURN_FRAMES) {
            panTimer++;
            // transition from chopped -> cooked
            if (panTimer >= COOKED_FRAMES && panItem.equals("chopped_meat")) {
                panItem = "cooked_meat";
            }
            if (panTimer >= BURN_FRAMES) {
                panItem = "burned_meat";
            }
        }
    }

    /**
     * Player interaction with this cooking station.
     * @param playerIdx index of player in gp.players
     * @param player the Player object
     * @return true if any action occurred (so caller can consume the key)
     */
    public boolean interact(int playerIdx, Player player) {
        if (!panPresent) return false;

        // If player is carrying THIS station's pan -> place it back to station
        if (panOwner == playerIdx) {
            // place pan back (only allowed when player is adjacent to this station - caller ensures adjacency)
            panOwner = -1;
            // clear player's fryingpan marker if present
            if ("fryingpan".equals(player.heldItem)) {
                player.heldItem = null;
                player.heldItemImage = null;
            }
            // cooking (if panItem != null) will resume automatically in update()
            return true;
        }

        // If pan is at station (not carried)
        if (panOwner == -1) {
            // 1) Player wants to put chopped_meat on pan:
            //    require player to HOLD chopped_meat (and NOT hold any other item) and panItem==null
            if (player.heldItem != null && player.heldItem.equals("chopped_meat") && panItem == null) {
                panItem = "chopped_meat";
                panTimer = 0;
                // remove item from player
                player.heldItem = null;
                player.heldItemImage = null;
                return true;
            }

            // 2) Player wants to pick up pan (empty or with cooked/burned/chopped item)
            //    allow only if player currently holds NOTHING (single-item rule)
            if (player.heldItem == null) {
                // give pan to player: mark pan as carried by this player
                panOwner = playerIdx;
                // set player's heldItem to marker "fryingpan" so they cannot pick other items while carrying pan
                player.heldItem = "fryingpan";
                player.heldItemImage = imgFryingPan;
                // panItem and panTimer remain stored; update() will pause timer due to panOwner != -1
                return true;
            }

            // if player holds something else -> cannot pick up pan (enforce single-item)
            return false;
        }

        // Otherwise panOwner belongs to someone else -> nothing happens
        return false;
    }

    public boolean panAtStation() {
        return panPresent && panOwner == -1;
    }

    /**
     * Draw pan when it sits on the station (not carried).
     * This draws frying pan + content + progress bar (if panItem != null).
     */
    public void drawAtStation(Graphics2D g2, GamePanel gp) {
        if (!panPresent) return;
        if (panOwner != -1) return; // carried -> don't draw at station

        int drawX = x;
        int drawY = y;

        if (imgFryingPan != null) {
            g2.drawImage(imgFryingPan, drawX, drawY, gp.tileSize, gp.tileSize, null);
        }

        if (panItem != null) {
            BufferedImage contentImg = null;
            switch (panItem) {
                case "chopped_meat": contentImg = imgChoppedMeat; break;
                case "cooked_meat": contentImg = imgCookedMeat; break;
                case "burned_meat": contentImg = imgBurnedMeat; break;
            }
            if (contentImg != null) {
                int iconW = gp.tileSize / 2;
                int iconH = gp.tileSize / 2;
                int iconX = drawX + (gp.tileSize - iconW) / 2;
                int iconY = drawY + (gp.tileSize - iconH) / 2;
                g2.drawImage(contentImg, iconX, iconY, iconW, iconH, null);
            }
            // draw progress bar above pan (persist if paused)
            double ratio = Math.min(1.0, (double)panTimer / (double)BURN_FRAMES);
            int barW = gp.tileSize / 2;
            int barH = 6;
            int bx = drawX + (gp.tileSize - barW) / 2;
            int by = drawY - barH - 6;
            g2.setColor(new java.awt.Color(40,40,40,220));
            g2.fillRect(bx, by, barW, barH);
            if (panTimer < COOKED_FRAMES) {
                g2.setColor(new java.awt.Color(255,165,0)); // orange
            } else if (panTimer < BURN_FRAMES) {
                g2.setColor(new java.awt.Color(50,205,50)); // green
            } else {
                g2.setColor(new java.awt.Color(220,20,60)); // red
            }
            g2.fillRect(bx, by, (int)(barW * ratio), barH);
            g2.setColor(java.awt.Color.WHITE);
            g2.drawRect(bx, by, barW, barH);
        }
    }

    /**
     * Draw pan when it is carried by a player.
     * IMPORTANT: progress bar is intentionally NOT drawn while carried (so it is hidden during carry).
     */
    public void drawIfCarriedByPlayer(Graphics2D g2, GamePanel gp, int playerIdx, Player player) {
        if (!panPresent) return;
        if (panOwner != playerIdx) return;

        int iconW = gp.tileSize / 2;
        int iconH = gp.tileSize / 2;
        int iconX = player.x + (gp.tileSize - iconW) / 2;
        int iconY = player.y - iconH - 4;
        if (imgFryingPan != null) {
            g2.drawImage(imgFryingPan, iconX, iconY, iconW, iconH, null);
        }
        // Draw *only* the content icon (if exists) but do NOT draw progress bar while carried.
        if (panItem != null) {
            BufferedImage contentImg = null;
            switch (panItem) {
                case "chopped_meat": contentImg = imgChoppedMeat; break;
                case "cooked_meat": contentImg = imgCookedMeat; break;
                case "burned_meat": contentImg = imgBurnedMeat; break;
            }
            if (contentImg != null) {
                int ciW = iconW * 3/4;
                int ciH = iconH * 3/4;
                int ciX = iconX + (iconW - ciW)/2;
                int ciY = iconY + (iconH - ciH)/2;
                g2.drawImage(contentImg, ciX, ciY, ciW, ciH, null);
            }
            // Note: Intentionally NOT drawing the progress bar here (hidden while carried).
        }
    }
}
