package main;

import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
    
        // find earliest matching order index
        int matchedIndex = -1;
        for (int i = 0; i < gp.orderManager.activeOrders.size(); i++) {
            Order o = gp.orderManager.activeOrders.get(i);
            if (gp.orderManager.multisetEquals(o.requiredItems, contents)) {
                matchedIndex = i;
                break; // earliest match
            }
        }
    
        if (matchedIndex != -1) {
            // success: complete that order by index
            gp.orderManager.completeOrderAtIndex(matchedIndex);
    
            // schedule dirty plate to nearest plate storage (I use index 0; change if you want nearest)
            if (!gp.plateStorages.isEmpty()) gp.plateStorages.get(0).scheduleDirtyPlate(10);
    
            // consume player's plate
            player.plateStack.clear();
            player.heldItem = null;
            player.heldItemImage = null;
            return true;
        } else {
            // wrong dish: apply penalty to the front-most active order (index 0) OR generic penalty
            if (!gp.orderManager.activeOrders.isEmpty()) {
                // fail the earliest order (index 0)
                gp.orderManager.failOrderAtIndex(0);
            } else {
                // no active orders, apply generic penalty
                gp.score -= 50;
                gp.ordersFailed++;
            }
    
            // schedule dirty plate
            if (!gp.plateStorages.isEmpty()) gp.plateStorages.get(0).scheduleDirtyPlate(10);
    
            // consume player's plate
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
