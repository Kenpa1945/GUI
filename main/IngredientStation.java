package main;

import entity.Player;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.imageio.ImageIO;

public class IngredientStation {

    public int col, row;
    public int x, y;
    GamePanel gp;

    // ingredient type produced by this station (e.g. "bun","meat","cheese")
    private final String ingredientKey;
    private BufferedImage ingredientImage;

    // surface state (SAMA seperti AssemblyStation)
    public String singleItem = null;
    public BufferedImage singleItemImage = null;

    public ArrayList<String> plateStack = null;
    public static BufferedImage imgCleanPlate = null;

    // allowed stacking
    private static final List<String> ALLOWED_STACK = Arrays.asList(
        "bun","chopped_cheese","chopped_lettuce","chopped_tomato","cooked_meat"
    );

    public IngredientStation(GamePanel gp, int col, int row, String ingredientKey, String imgPath) {
        this.gp = gp;
        this.col = col;
        this.row = row;
        this.x = col * gp.tileSize;
        this.y = row * gp.tileSize;
        this.ingredientKey = ingredientKey;
        loadImages(imgPath);
    }

    private void loadImages(String imgPath) {
        try {
            ingredientImage = ImageIO.read(getClass().getResourceAsStream(imgPath));
        } catch (IOException e) {}

        if (imgCleanPlate == null) {
            try {
                imgCleanPlate = ImageIO.read(
                    getClass().getResourceAsStream("/res/ingredient/clean_plate.png")
                );
            } catch (IOException e) {}
        }
    }

    /**
     * INTERACTION LOGIC
     * - If surface empty → source mode
     * - If surface occupied → assembly mode
     */
    public boolean interact(Player player) {

        // =========================
        // MODE 1: SURFACE MODE
        // =========================
        if (singleItem != null || plateStack != null) {

            // player empty → pick up
            if (player.heldItem == null) {
                if (plateStack != null) {
                    player.heldItem = "plate";
                    player.heldItemImage = imgCleanPlate;
                    player.plateStack = new ArrayList<>(plateStack);
                    plateStack = null;
                    return true;
                }
                if (singleItem != null) {
                    player.heldItem = singleItem;
                    player.heldItemImage = singleItemImage;
                    singleItem = null;
                    singleItemImage = null;
                    return true;
                }
            }

            // player holds plate
            if ("plate".equals(player.heldItem)) {

                if (singleItem != null) {
                    if (!ALLOWED_STACK.contains(singleItem)) return false;
                    player.plateStack.add(singleItem);
                    singleItem = null;
                    singleItemImage = null;
                    return true;
                }

                // cannot merge plate + plate
                return false;
            }

            // player holds ingredient
            if (player.heldItem != null) {
                if (plateStack != null && ALLOWED_STACK.contains(player.heldItem)) {
                    plateStack.add(player.heldItem);
                    player.heldItem = null;
                    player.heldItemImage = null;
                    return true;
                }
                return false;
            }

            return false;
        }

        // =========================
        // MODE 2: SOURCE MODE
        // =========================

        // player empty → take ingredient
        if (player.heldItem == null) {
            player.heldItem = ingredientKey;
            player.heldItemImage = ingredientImage;
            return true;
        }

        // player holds plate → place plate on surface
        if ("plate".equals(player.heldItem)) {
            plateStack = new ArrayList<>(player.plateStack);
            player.plateStack.clear();
            player.heldItem = null;
            player.heldItemImage = null;
            return true;
        }

        // player holds ingredient → place as single item
        if (player.heldItem != null) {
            singleItem = player.heldItem;
            singleItemImage = player.heldItemImage;
            player.heldItem = null;
            player.heldItemImage = null;
            return true;
        }

        return false;
    }

    public void draw(Graphics2D g2, GamePanel gp) {

        // draw base ingredient icon ONLY if surface empty
        boolean surfaceEmpty = (singleItem == null && plateStack == null);
        if (surfaceEmpty && ingredientImage != null) {
            g2.drawImage(
                ingredientImage,
                x + gp.tileSize/4,
                y + gp.tileSize/4,
                gp.tileSize/2,
                gp.tileSize/2,
                null
            );
        }


        // draw surface content (same visual semantics as Assembly)
        if (plateStack != null) {
            g2.drawImage(
                imgCleanPlate,
                x + gp.tileSize/4,
                y + gp.tileSize/8,
                gp.tileSize/2,
                gp.tileSize/2,
                null
            );
        }
        else if (singleItem != null && singleItemImage != null) {
            g2.drawImage(
                singleItemImage,
                x + gp.tileSize/4,
                y + gp.tileSize/4,
                gp.tileSize/2,
                gp.tileSize/2,
                null
            );
        }
    }
}
