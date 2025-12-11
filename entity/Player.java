package entity;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.IOException;

import javax.imageio.ImageIO;

import java.awt.Color;

import main.GamePanel;
import main.KeyHandler;
import main.CookingStation;

public class Player extends Entity{
    GamePanel gp;
    KeyHandler keyH; 
    String color;

    // Ingredient images (preload)
    BufferedImage imgBun, imgMeat, imgCheese, imgLettuce, imgTomato;
    // Chopped variants
    BufferedImage imgChoppedMeat, imgChoppedCheese, imgChoppedLettuce, imgChoppedTomato;

    // temporary pending item while interacting (from picking)
    private String pendingItem = null;
    private BufferedImage pendingItemImage = null;

    // Cutting fields
    public boolean isCutting = false;
    public int cutCounter = 0;
    public int CUT_DURATION_SECONDS = 3;
    public int CUT_DURATION_FRAMES = 0;

    public Player(GamePanel gp, KeyHandler keyH, String color){
        this.gp = gp;
        this.keyH = keyH;
        this.color = color;

        solidArea = new Rectangle();
        solidArea.x = 8;
        solidArea.y = 16;
        solidArea.width = 32;
        solidArea.height = 32;

        setDefaultValues();
        getPlayerImage();
        loadIngredientImages();

        CUT_DURATION_FRAMES = CUT_DURATION_SECONDS * gp.FPS;
    }

    public void setDefaultValues(){

        x = 290;
        y = 95;
        speed = 1;
        direction = "down";

        // reset holding/interact/cutting
        heldItem = null;
        heldItemImage = null;
        isInteracting = false;
        interactCounter = 0;

        isCutting = false;
        cutCounter = 0;
        pendingItem = null;
        pendingItemImage = null;
    }

    public void getPlayerImage(){

        try{

            up1 = ImageIO.read(getClass().getResourceAsStream("/res/player/"+ color + "_up_1.png"));
            up2 = ImageIO.read(getClass().getResourceAsStream("/res/player/"+ color + "_up_2.png"));
            down1 = ImageIO.read(getClass().getResourceAsStream("/res/player/"+ color + "_down_1.png"));
            down2 = ImageIO.read(getClass().getResourceAsStream("/res/player/"+ color + "_down_2.png"));
            left1 = ImageIO.read(getClass().getResourceAsStream("/res/player/"+ color + "_left_1.png"));
            left2 = ImageIO.read(getClass().getResourceAsStream("/res/player/"+ color + "_left_2.png"));
            right1 = ImageIO.read(getClass().getResourceAsStream("/res/player/"+ color + "_right_1.png"));
            right2 = ImageIO.read(getClass().getResourceAsStream("/res/player/"+ color + "_right_2.png"));

        }catch(IOException e){
            e.printStackTrace();
        }
    }

    private void loadIngredientImages(){
        try{
            imgBun = ImageIO.read(getClass().getResourceAsStream("/res/ingredient/bun.png"));
            imgMeat = ImageIO.read(getClass().getResourceAsStream("/res/ingredient/meat.png"));
            imgCheese = ImageIO.read(getClass().getResourceAsStream("/res/ingredient/cheese.png"));
            imgLettuce = ImageIO.read(getClass().getResourceAsStream("/res/ingredient/lettuce.png"));
            imgTomato = ImageIO.read(getClass().getResourceAsStream("/res/ingredient/tomato.png"));

            imgChoppedMeat = ImageIO.read(getClass().getResourceAsStream("/res/ingredient/chopped_meat.png"));
            imgChoppedCheese = ImageIO.read(getClass().getResourceAsStream("/res/ingredient/chopped_cheese.png"));
            imgChoppedLettuce = ImageIO.read(getClass().getResourceAsStream("/res/ingredient/chopped_lettuce.png"));
            imgChoppedTomato = ImageIO.read(getClass().getResourceAsStream("/res/ingredient/chopped_tomato.png"));

        }catch(IOException e){
            e.printStackTrace();
        }
    }

    public void update(){
        if (gp.gameState != gp.playState) {
            return;
        }

        boolean isActive = (this == gp.players[gp.acivePlayerIndex]);

        // 1) Advance picking interaction if in progress (only the player who started it)
        if (isInteracting) {
            interactCounter++;
            if (interactCounter >= INTERACT_DURATION) {
                this.heldItem = pendingItem;
                this.heldItemImage = pendingItemImage;
                pendingItem = null;
                pendingItemImage = null;
                isInteracting = false;
                interactCounter = 0;
            }
            // block movement for active player while picking
            if (isActive) return;
            // if not active, continue so cutting/cooking updates still work
        }

        // 2) Cutting logic (runs for all players each frame)
        if (isCutting) {
            if (isAtCuttingStation()) {
                cutCounter++;
                if (cutCounter >= CUT_DURATION_FRAMES) {
                    applyChoppedVariant();
                    isCutting = false;
                    cutCounter = 0;
                }
            } else {
                // paused - do not reset cutCounter
            }
        }

        // 3) Cooking interactions and other inputs — only processed by active player
        if (!isActive) {
            // nothing more to do for non-active players (their cutting progress already updated)
            return;
        }

        // --- Handle P (cutting toggle) ---
        if (keyH.pPressed) {
            // Toggle cutting only if holding an item that is cuttable
            if (heldItem != null && isCuttable(heldItem)) {
                // If currently not cutting, start cutting (will progress only when at station)
                isCutting = !isCutting; // toggle: start/resume or pause
            }
            keyH.pPressed = false; // consume
        }

        // --- Handle E (cooking/storage interactions) ---
        if (keyH.ePressed) {
            // First try cooking station interaction if adjacent
            int csIndex = getAdjacentCookingStationIndex();
            if (csIndex != -1) {
                CookingStation cs = gp.cookingStations.get(csIndex);
                int myIndex = getMyPlayerIndex();
                if (myIndex != -1) {
                    boolean acted = cs.interact(myIndex, this); // PASS player index (fix bug)
                    if (acted) {
                        keyH.ePressed = false;
                        return;
                    }
                }
            }

            // If no cooking action happened, fallback to storage interaction
            if (this.heldItem == null) {
                int storageTileNum = getAdjacentStorageTile();
                if (storageTileNum != -1) {
                    pendingItem = tileNumToItemName(storageTileNum);
                    pendingItemImage = tileNumToImage(storageTileNum);
                    if (pendingItem != null && pendingItemImage != null) {
                        isInteracting = true;
                        interactCounter = 0;
                        keyH.ePressed = false;
                        return;
                    }
                }
            } else {
                // already carrying something -> can't pick another
                keyH.ePressed = false;
            }
        }

        // 4) Movement input (tile-based)
        if (!isMoving) {
            if(keyH.upPressed == true){
                direction = "up";
                isMoving = true;
                goalX = x;
                goalY = y - gp.tileSize;
            }
            else if(keyH.downPressed == true){
                direction = "down";
                isMoving = true;
                goalX = x;
                goalY = y + gp.tileSize;
            }
            else if(keyH.leftPressed == true){
                direction = "left";
                isMoving = true;
                goalX = x - gp.tileSize;
                goalY = y;
            }else if(keyH.rightPressed == true){
                direction = "right";
                isMoving = true;
                goalX = x + gp.tileSize;
                goalY = y;
            }

            spriteCounter++;
            if(spriteCounter > 200){
                if(spriteNum == 1){
                    spriteNum = 2;
                }
                else if(spriteNum == 2){
                    spriteNum = 1;
                }
                spriteCounter = 0;
            }
        }

        // Movement execution
        if (isMoving) {
            collisionOn = false;
            gp.cChecker.checkTile(this);
            gp.cChecker.checkPlayer(this, gp.players, gp.acivePlayerIndex);

            if (collisionOn == false) {
                switch (direction) {
                    case "up":
                        y -= speed;
                        if (y <= goalY) {
                            y = goalY;
                            isMoving = false;
                            direction = "up";
                        }
                        break;
                    case "down":
                        y += speed;
                        if (y >= goalY) {
                            y = goalY;
                            isMoving = false;
                            direction = "down";
                        }
                        break;
                    case "left":
                        x -= speed;
                        if (x <= goalX) {
                            x = goalX;
                            isMoving = false;
                            direction = "left";
                        }
                        break;
                    case "right":
                        x += speed;
                        if (x >= goalX) {
                            x = goalX;
                            isMoving = false;
                            direction = "right";
                        }
                        break;
                }
            } else {
                isMoving = false;
            }
        }
    }

    // Find this player's index in gp.players (returns -1 if not found)
    private int getMyPlayerIndex() {
        for (int i = 0; i < gp.players.length; i++) {
            if (gp.players[i] == this) return i;
        }
        return -1;
    }

    private void applyChoppedVariant() {
        if (heldItem == null) return;

        switch (heldItem) {
            case "meat":
                heldItem = "chopped_meat";
                heldItemImage = imgChoppedMeat;
                break;
            case "cheese":
                heldItem = "chopped_cheese";
                heldItemImage = imgChoppedCheese;
                break;
            case "lettuce":
                heldItem = "chopped_lettuce";
                heldItemImage = imgChoppedLettuce;
                break;
            case "tomato":
                heldItem = "chopped_tomato";
                heldItemImage = imgChoppedTomato;
                break;
            default:
                break;
        }
    }

    private boolean isCuttable(String itemName) {
        if (itemName == null) return false;
        return itemName.equals("meat") || itemName.equals("cheese") || itemName.equals("lettuce") || itemName.equals("tomato");
    }

    private int getAdjacentCookingStationIndex() {
        int centerX = x + solidArea.x + solidArea.width/2;
        int centerY = y + solidArea.y + solidArea.height/2;

        int col = centerX / gp.tileSize;
        int row = centerY / gp.tileSize;

        for (int i = 0; i < gp.cookingStations.size(); i++) {
            CookingStation cs = gp.cookingStations.get(i);
            if (cs.col == col && cs.row == row) return i;
            if (cs.col == col && cs.row == row - 1) return i;
            if (cs.col == col && cs.row == row + 1) return i;
            if (cs.col == col - 1 && cs.row == row) return i;
            if (cs.col == col + 1 && cs.row == row) return i;
        }
        return -1;
    }

    private int getAdjacentStorageTile() {
        int centerX = x + solidArea.x + solidArea.width/2;
        int centerY = y + solidArea.y + solidArea.height/2;

        int col = centerX / gp.tileSize;
        int row = centerY / gp.tileSize;

        if (isStorageTile(col, row)) return gp.tileM.mapTileNum[col][row];
        if (row - 1 >= 0 && isStorageTile(col, row - 1)) return gp.tileM.mapTileNum[col][row - 1];
        if (row + 1 < gp.maxScreenRow && isStorageTile(col, row + 1)) return gp.tileM.mapTileNum[col][row + 1];
        if (col - 1 >= 0 && isStorageTile(col - 1, row)) return gp.tileM.mapTileNum[col - 1][row];
        if (col + 1 < gp.maxScreenCol && isStorageTile(col + 1, row)) return gp.tileM.mapTileNum[col + 1][row];

        return -1;
    }

    private boolean isStorageTile(int col, int row) {
        int t = gp.tileM.mapTileNum[col][row];
        return t == 7 || t == 10 || t == 11 || t == 12 || t == 13;
    }

    private boolean isAtCuttingStation() {
        int centerX = x + solidArea.x + solidArea.width/2;
        int centerY = y + solidArea.y + solidArea.height/2;

        int col = centerX / gp.tileSize;
        int row = centerY / gp.tileSize;

        if (col >= 0 && col < gp.maxScreenCol && row >= 0 && row < gp.maxScreenRow) {
            if (gp.tileM.mapTileNum[col][row] == 2) return true;
        }
        if (row - 1 >= 0 && gp.tileM.mapTileNum[col][row - 1] == 2) return true;
        if (row + 1 < gp.maxScreenRow && gp.tileM.mapTileNum[col][row + 1] == 2) return true;
        if (col - 1 >= 0 && gp.tileM.mapTileNum[col - 1][row] == 2) return true;
        if (col + 1 < gp.maxScreenCol && gp.tileM.mapTileNum[col + 1][row] == 2) return true;

        return false;
    }

    private String tileNumToItemName(int tileNum) {
        switch (tileNum) {
            case 7: return "bun";
            case 10: return "meat";
            case 11: return "cheese";
            case 12: return "lettuce";
            case 13: return "tomato";
            default: return null;
        }
    }

    private BufferedImage tileNumToImage(int tileNum) {
        switch (tileNum) {
            case 7: return imgBun;
            case 10: return imgMeat;
            case 11: return imgCheese;
            case 12: return imgLettuce;
            case 13: return imgTomato;
            default: return null;
        }
    }

    public void draw(Graphics2D g2){
        BufferedImage image = null;

        switch(direction){
            case "up":
                if(spriteNum == 1){
                    image = up1;
                } else {
                    image = up2;
                }
                break;
            case "down":
                if(spriteNum == 1){
                    image = down1;
                } else {
                    image = down2;
                }
                break;
            case "left":
                if(spriteNum == 1){
                    image = left1;
                } else {
                    image = left2;
                }
                break;
            case "right":
                if(spriteNum == 1){
                    image = right1;
                } else {
                    image = right2;
                }
                break;
        }

        g2.drawImage(image, x, y, gp.tileSize, gp.tileSize, null);

        // Gambar held item di atas kepala pemain (jika ada dan bukan pan)
        if (heldItemImage != null) {
            int iconW = gp.tileSize / 2;
            int iconH = gp.tileSize / 2;
            int iconX = x + (gp.tileSize - iconW) / 2;
            int iconY = y - iconH - 4; // sedikit jarak di atas kepala
            g2.drawImage(heldItemImage, iconX, iconY, iconW, iconH, null);
        }

        // Jika sedang berinteraksi picking, gambar progress bar kecil (seperti sebelumnya)
        if (isInteracting) {
            double ratio = (double)interactCounter / (double)INTERACT_DURATION;
            int barW = gp.tileSize / 2;
            int barH = 6;
            int bx = x + (gp.tileSize - barW) / 2;
            int by = y - gp.tileSize / 2 - barH - 8;
            g2.setColor(Color.DARK_GRAY);
            g2.fillRect(bx, by, barW, barH);
            g2.setColor(Color.GREEN);
            g2.fillRect(bx, by, (int)(barW * ratio), barH);
            g2.setColor(Color.WHITE);
            g2.drawRect(bx, by, barW, barH);
        }

        // Jika ada progress chopping (cutCounter > 0 but not finished), tampilkan bar kecil
        if (cutCounter > 0 && cutCounter < CUT_DURATION_FRAMES) {
            double ratio = (double)cutCounter / (double)CUT_DURATION_FRAMES;
            int barW = gp.tileSize / 2;
            int barH = 6;
            int bx = x + (gp.tileSize - barW) / 2;
            int by = y - gp.tileSize / 2 - barH - 24;
            g2.setColor(new Color(40, 40, 40, 200));
            g2.fillRect(bx, by, barW, barH);
            g2.setColor(Color.ORANGE);
            g2.fillRect(bx, by, (int)(barW * ratio), barH);
            g2.setColor(Color.WHITE);
            g2.drawRect(bx, by, barW, barH);
        }

        // Note: drawing of carried pans is done by GamePanel (it checks cookingStations)
    }
}
