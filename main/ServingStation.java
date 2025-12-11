package main;

import java.awt.Graphics2D;

import entity.Player;

public class ServingStation {
    public int col, row;
    public int x, y;
    GamePanel gp;

    public ServingStation(GamePanel gp, int col, int row) {
        this.gp = gp;
        this.col = col;
        this.row = row;
        this.x = col * gp.tileSize;
        this.y = row * gp.tileSize;
    }

    /**
     * Player interacts with serving station (E).
     * Behavior:
     * - If player not holding plate -> cannot serve (return false)
     * - If player holds plate -> check OrderManager.matchPlateContents(plateStack)
     *    - if matched -> Order removed (earliest matching), score += reward, schedule dirty plate return in 10s
     *    - if not matched -> apply penalty, schedule dirty plate return in 10s (plate removed)
     * In both success/fail the player's plate is removed (consumed) and a dirty plate will be returned.
     */
    public boolean interact(Player player) {
        if (player == null) return false;
        if (!"plate".equals(player.heldItem)) {
            // nothing to serve
            return false;
        }
        // get plate contents (copy)
        java.util.List<String> contents = new java.util.ArrayList<>(player.plateStack);

        // try match
        Order matched = gp.orderManager.matchPlateContents(contents);
        if (matched != null) {
            // success
            gp.orderManager.completeOrder(matched);
            // schedule dirty plate to be added after 10 seconds
            gp.plateStorages.get(0).scheduleDirtyPlate(10); // place into the first PlateStorage (we will treat plate storage as a stack; we'll add helper to set top)
            // remove player's plate
            player.plateStack.clear();
            player.heldItem = null;
            player.heldItemImage = null;
            return true;
        } else {
             // wrong dish: apply penalty by failing the earliest active order if exists
            if (!gp.orderManager.activeOrders.isEmpty()) {
                Order earliest = gp.orderManager.activeOrders.get(0);
                gp.orderManager.failOrder(earliest); // this will add penalty and increment ordersFailed
            } else {
                // fallback penalty if no orders in list
                gp.score -= 50;
                gp.ordersFailed++;
            }
            // schedule dirty plate
            gp.plateStorages.get(0).scheduleDirtyPlate(10);
            // remove player's plate
            player.plateStack.clear();
            player.heldItem = null;
            player.heldItemImage = null;
            return true;
        }
    }

    public void draw(Graphics2D g2, GamePanel gp) {
        // optional overlay (tile already drawn by TileManager)
    }
}
