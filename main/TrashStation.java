package main;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;

import javax.imageio.ImageIO;

import entity.Player;

public class TrashStation {
    public int col, row;
    public int x, y;
    GamePanel gp;

    // use same trash tile image you already have, else fallback null
    public static BufferedImage imgTrash = null;

    public TrashStation(GamePanel gp, int col, int row) {
        this.gp = gp;
        this.col = col;
        this.row = row;
        this.x = col * gp.tileSize;
        this.y = row * gp.tileSize;
        loadImage();
    }

    private void loadImage() {
        if (imgTrash != null) return;
        try {
            // reuse tile image if present
            imgTrash = ImageIO.read(getClass().getResourceAsStream("/res/tiles/trash.png"));
        } catch (IOException e) {
            // ignore if missing
        }
    }

    /**
     * Interact: player presses E while adjacent to trash station.
     * Returns true if action occurred (so Player can consume the key).
     */
    public boolean interact(Player player) {
        if (player == null) return false;

        // 1) Player holds nothing -> nothing to do
        if (player.heldItem == null) return false;

        // 2) If player holds plate -> clear its contents but keep plate
        if ("plate".equals(player.heldItem)) {
            if (player.plateStack != null && !player.plateStack.isEmpty()) {
                player.plateStack.clear();
            }
            // do not remove the plate itself
            return true;
        }

        // 3) If player holds fryingpan -> remove contents from the corresponding CookingStation
        if ("fryingpan".equals(player.heldItem)) {
            // find cooking station owned by this player (panOwner == playerIdx)
            int myIdx = -1;
            for (int i = 0; i < gp.players.length; i++) {
                if (gp.players[i] == player) { myIdx = i; break; }
            }
            if (myIdx != -1) {
                for (CookingStation cs : gp.cookingStations) {
                    if (cs.panOwner == myIdx) {
                        // remove its content (if any) but keep pan
                        cs.panItem = null;
                        cs.panTimer = 0;
                        return true;
                    }
                }
            }
            // If we can't find a pan owned by player, still return false (nothing to trash)
            return false;
        }

        // 4) Otherwise player holds a normal item (ingredient/processed) -> discard it
        // This covers raw ingredient, chopped_X, cooked_meat, burned_meat, etc.
        player.heldItem = null;
        player.heldItemImage = null;
        // If player had any plateStack (shouldn't while holding non-plate) ignore.
        return true;
    }

    public void draw(Graphics2D g2, GamePanel gp) {
        // Optional: draw additional trash marker over tile if available
        if (imgTrash != null) {
            g2.drawImage(imgTrash, x, y, gp.tileSize, gp.tileSize, null);
        }
    }
}
