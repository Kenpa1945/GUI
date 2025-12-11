package main;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.imageio.ImageIO;

import entity.Player;

public class AssemblyStation {
    public int col, row;
    public int x,y;
    GamePanel gp;

    // Assembly can hold either:
    // - singleItem (String) + image, or
    // - plateStack (ArrayList<String>) representing a plate placed on station
    public String singleItem = null;
    public BufferedImage singleItemImage = null;

    public ArrayList<String> plateStack = null; // if not null => means a plate sits here (may be empty)
    public static BufferedImage imgCleanPlate = null;

    // images for many item types
    public static BufferedImage imgBun = null;
    public static BufferedImage imgMeat = null;
    public static BufferedImage imgChoppedMeat = null;
    public static BufferedImage imgChoppedCheese = null;
    public static BufferedImage imgChoppedLettuce = null;
    public static BufferedImage imgChoppedTomato = null;
    public static BufferedImage imgCookedMeat = null;
    public static BufferedImage imgBurnedMeat = null;
    public static BufferedImage imgCheese = null;
    public static BufferedImage imgLettuce = null;
    public static BufferedImage imgTomato = null;

    // allowed to stack onto plate
    private static final List<String> ALLOWED_STACK = Arrays.asList(
        "bun","chopped_cheese","chopped_lettuce","chopped_tomato","cooked_meat"
    );

    public AssemblyStation(GamePanel gp, int col, int row) {
        this.gp = gp;
        this.col = col;
        this.row = row;
        this.x = col * gp.tileSize;
        this.y = row * gp.tileSize;
        loadImages();
    }

    private void loadImages() {
        if (imgCleanPlate != null) return;
        try { imgCleanPlate = ImageIO.read(getClass().getResourceAsStream("/res/ingredient/clean_plate.png")); } catch(IOException e){}
        try { imgBun = ImageIO.read(getClass().getResourceAsStream("/res/ingredient/bun.png")); } catch(IOException e){}
        try { imgMeat = ImageIO.read(getClass().getResourceAsStream("/res/ingredient/meat.png")); } catch(IOException e){}
        try { imgChoppedMeat = ImageIO.read(getClass().getResourceAsStream("/res/ingredient/chopped_meat.png")); } catch(IOException e){}
        try { imgChoppedCheese = ImageIO.read(getClass().getResourceAsStream("/res/ingredient/chopped_cheese.png")); } catch(IOException e){}
        try { imgChoppedLettuce = ImageIO.read(getClass().getResourceAsStream("/res/ingredient/chopped_lettuce.png")); } catch(IOException e){}
        try { imgChoppedTomato = ImageIO.read(getClass().getResourceAsStream("/res/ingredient/chopped_tomato.png")); } catch(IOException e){}
        try { imgCookedMeat = ImageIO.read(getClass().getResourceAsStream("/res/ingredient/cooked_meat.png")); } catch(IOException e){}
        try { imgBurnedMeat = ImageIO.read(getClass().getResourceAsStream("/res/ingredient/burned_meat.png")); } catch(IOException e){}
        try { imgCheese = ImageIO.read(getClass().getResourceAsStream("/res/ingredient/cheese.png")); } catch(IOException e){}
        try { imgLettuce = ImageIO.read(getClass().getResourceAsStream("/res/ingredient/lettuce.png")); } catch(IOException e){}
        try { imgTomato = ImageIO.read(getClass().getResourceAsStream("/res/ingredient/tomato.png")); } catch(IOException e){}
    }

    /**
     * Player interacts (E) with this assembly station.
     *
     * Rules enforced here:
     * - Player cannot place a fryingpan on assembly (reject if player.heldItem == "fryingpan")
     * - If player is holding a fryingpan that contains cooked_meat, and assembly accepts that transfer,
     *   we move cooked_meat from that fryingpan to assembly/plate and remove it from that fryingpan.
     */
    public boolean interact(Player player) {
        // disallow placing fryingpan on assembly
        if ("fryingpan".equals(player.heldItem)) {
            // special case: allow transferring cooked_meat FROM the fryingpan to assembly/plate
            // find cooking station whose panOwner == this player (if any)
            CookingStation playerPanCS = findPanOwnedByPlayer(player);
            if (playerPanCS != null && playerPanCS.panItem != null && playerPanCS.panItem.equals("cooked_meat")) {
                // if player holds a plate -> transfer cooked_meat into player's plate
                if ("plate".equals(player.heldItem)) {
                    player.plateStack.add("cooked_meat");
                    playerPanCS.panItem = null;
                    playerPanCS.panTimer = 0;
                    return true;
                }
                // if assembly has plate -> transfer to assembly plate
                if (plateStack != null) {
                    plateStack.add("cooked_meat");
                    playerPanCS.panItem = null;
                    playerPanCS.panTimer = 0;
                    return true;
                }
                // if assembly empty and singleItem empty -> put cooked_meat as single item
                if (singleItem == null && plateStack == null) {
                    singleItem = "cooked_meat";
                    singleItemImage = mapItemToImage(singleItem);
                    playerPanCS.panItem = null;
                    playerPanCS.panTimer = 0;
                    return true;
                }
            }
            return false; // otherwise placing fryingpan on assembly is not allowed
        }

        // If player holds nothing:
        if (player.heldItem == null) {
            // If assembly has a plate -> pick up plate
            if (plateStack != null) {
                player.heldItem = "plate";
                player.heldItemImage = imgCleanPlate;
                player.plateStack = new ArrayList<>(plateStack);
                plateStack = null;
                return true;
            }
            // If assembly has single item -> pick it up
            if (singleItem != null) {
                player.heldItem = singleItem;
                player.heldItemImage = mapItemToImage(singleItem);
                singleItem = null;
                singleItemImage = null;
                return true;
            }
            return false;
        }

        // If player holds a plate
        if ("plate".equals(player.heldItem)) {
            // If assembly empty -> place player's plate here
            if (plateStack == null && singleItem == null) {
                plateStack = new ArrayList<>(player.plateStack);
                player.plateStack.clear();
                player.heldItem = null;
                player.heldItemImage = null;
                return true;
            }
            // If assembly has single item -> try to transfer that item onto player's plate (if allowed)
            if (singleItem != null) {
                if (ALLOWED_STACK.contains(singleItem)) {
                    player.plateStack.add(singleItem);
                    singleItem = null;
                    singleItemImage = null;
                    return true;
                } else {
                    return false;
                }
            }
            // If assembly has a plate already -> disallow merging two plates
            return false;
        }

        // If player holds a non-plate item:
        // If assembly empty, place player's item as singleItem
        if (player.heldItem != null && !"plate".equals(player.heldItem)) {
            // Special case: player might be holding cooked_meat inside a fryingpan? That should be "fryingpan" handled above.
            if (singleItem == null && plateStack == null) {
                singleItem = player.heldItem;
                singleItemImage = mapItemToImage(singleItem);
                player.heldItem = null;
                player.heldItemImage = null;
                return true;
            }
            // If assembly has plate and item is allowed -> stack onto plate
            if (plateStack != null) {
                if (ALLOWED_STACK.contains(player.heldItem)) {
                    plateStack.add(player.heldItem);
                    player.heldItem = null;
                    player.heldItemImage = null;
                    return true;
                } else {
                    return false;
                }
            }
            // otherwise cannot place
            return false;
        }

        return false;
    }

    // find the cooking station whose pan is currently carried by this player (if any)
    private CookingStation findPanOwnedByPlayer(Player player) {
        int playerIdx = -1;
        for (int i = 0; i < gp.players.length; i++) {
            if (gp.players[i] == player) { playerIdx = i; break; }
        }
        if (playerIdx == -1) return null;
        for (CookingStation cs : gp.cookingStations) {
            if (cs.panOwner == playerIdx) return cs;
        }
        return null;
    }

    private BufferedImage mapItemToImage(String key) {
        if (key == null) return null;
        switch (key) {
            case "bun": return imgBun;
            case "meat": return imgMeat;
            case "chopped_meat": return imgChoppedMeat;
            case "chopped_cheese": return imgChoppedCheese;
            case "chopped_lettuce": return imgChoppedLettuce;
            case "chopped_tomato": return imgChoppedTomato;
            case "cooked_meat": return imgCookedMeat;
            case "burned_meat": return imgBurnedMeat;
            case "cheese": return imgCheese;
            case "lettuce": return imgLettuce;
            case "tomato": return imgTomato;
            default: return null;
        }
    }

    // Draw assembly: show either single item or a plate with small stack icons + count
    // Draw assembly: show either single item or a plate with vertical stack icons + count
public void draw(Graphics2D g2, GamePanel gp) {
    int drawX = x;
    int drawY = y;

    if (plateStack != null) {
        if (imgCleanPlate != null) {
            g2.drawImage(imgCleanPlate, drawX + gp.tileSize/4, drawY + gp.tileSize/8, gp.tileSize/2, gp.tileSize/2, null);
        }
        // draw vertical stack: bottom item at plate center, next item slightly higher, etc.
        int maxShow = Math.min(5, plateStack.size()); // show up to 5 layers
        int baseX = drawX + gp.tileSize/2; // center
        int baseY = drawY + gp.tileSize/2 + gp.tileSize/8; // slightly lower to center on plate
        int layerH = gp.tileSize / 5; // height of each layer icon
        for (int i = 0; i < maxShow; i++) {
            // draw from bottom (oldest, index 0) to top (index size-1)
            int idx = i; // bottom-first
            BufferedImage ii = mapItemToImage(plateStack.get(idx));
            if (ii != null) {
                int w = gp.tileSize / 3;
                int h = layerH;
                int ix = baseX - w/2;
                int iy = baseY - i * (layerH - 2) - h; // stack upward
                g2.drawImage(ii, ix, iy, w, h, null);
            }
        }
        // if more than maxShow, draw "+n" indicator above
        if (plateStack.size() > maxShow) {
            int more = plateStack.size() - maxShow;
            g2.setColor(java.awt.Color.WHITE);
            g2.setFont(new java.awt.Font("Arial", java.awt.Font.BOLD, 12));
            g2.drawString("+" + more, drawX + gp.tileSize/2 + gp.tileSize/6, drawY + gp.tileSize/8);
        }

        // draw count text
        g2.setColor(java.awt.Color.WHITE);
        g2.setFont(new java.awt.Font("Arial", java.awt.Font.BOLD, 12));
        g2.drawString("x"+plateStack.size(), drawX + gp.tileSize - 26, drawY + gp.tileSize - 6);
    } else if (singleItem != null) {
        BufferedImage ii = singleItemImage != null ? singleItemImage : mapItemToImage(singleItem);
        if (ii != null) {
            g2.drawImage(ii, drawX + gp.tileSize/4, drawY + gp.tileSize/4, gp.tileSize/2, gp.tileSize/2, null);
        } else {
            g2.setColor(java.awt.Color.WHITE);
            g2.drawString(singleItem, drawX + 8, drawY + gp.tileSize/2);
        }
    } else {
        // empty station - optional marker
    }
}

}
