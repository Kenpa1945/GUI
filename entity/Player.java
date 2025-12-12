package entity;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import javax.imageio.ImageIO;
import main.AssemblyStation;
import main.CookingStation;
import main.GamePanel;
import main.KeyHandler;
import main.PlateStorage;
import main.ServingStation;
import main.TrashStation;
import main.WashingStation;

public class Player extends Entity{
    GamePanel gp;
    KeyHandler keyH; 
    String color;

    // Ingredients / images
    BufferedImage imgBun, imgMeat, imgCheese, imgLettuce, imgTomato;
    BufferedImage imgChoppedMeat, imgChoppedCheese, imgChoppedLettuce, imgChoppedTomato;
    // fryingpan image might be handled in CookingStation

    // plate contents when player holds a plate
    public ArrayList<String> plateStack;

    // pending picking
    private String pendingItem = null;
    private BufferedImage pendingItemImage = null;

    // cutting fields
    public boolean isCutting = false;
    public int cutCounter = 0;
    public int CUT_DURATION_SECONDS = 3;
    public int CUT_DURATION_FRAMES = 0;

    // jumlah piring kotor yang dipegang (jika player.heldItem == "dirty_plate")
    public int dirtyPlateCount = 0;


    public Player(GamePanel gp, KeyHandler keyH, String color){
        this.gp = gp;
        this.keyH = keyH;
        this.color = color;

        solidArea = new Rectangle();
        solidArea.x = 8;
        solidArea.y = 16;
        solidArea.width = 32;
        solidArea.height = 32;

        plateStack = new ArrayList<>();
        setDefaultValues();
        getPlayerImage();
        loadIngredientImages();

        CUT_DURATION_FRAMES = CUT_DURATION_SECONDS * gp.FPS;
    }

    public void setDefaultValues(){
        x = 290; y = 95; speed = 1; direction = "down";

        heldItem = null;
        heldItemImage = null;
        plateStack.clear();

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
        }catch(IOException e){ e.printStackTrace(); }
    }

    private void loadIngredientImages(){
        try{
            imgBun = ImageIO.read(getClass().getResourceAsStream("/res/ingredient/bun.png"));
            imgMeat = ImageIO.read(getClass().getResourceAsStream("/res/ingredient/meat.png"));
            imgCheese = ImageIO.read(getClass().getResourceAsStream("/res/ingredient/cheese.png")); // avoid compile error if unused
        }catch(IOException e){}
        try { imgCheese = ImageIO.read(getClass().getResourceAsStream("/res/ingredient/cheese.png")); } catch(IOException e){}
        try { imgLettuce = ImageIO.read(getClass().getResourceAsStream("/res/ingredient/lettuce.png")); } catch(IOException e){}
        try { imgTomato = ImageIO.read(getClass().getResourceAsStream("/res/ingredient/tomato.png")); } catch(IOException e){}
        try { imgChoppedMeat = ImageIO.read(getClass().getResourceAsStream("/res/ingredient/chopped_meat.png")); } catch(IOException e){}
        try { imgChoppedCheese = ImageIO.read(getClass().getResourceAsStream("/res/ingredient/chopped_cheese.png")); } catch(IOException e){}
        try { imgChoppedLettuce = ImageIO.read(getClass().getResourceAsStream("/res/ingredient/chopped_lettuce.png")); } catch(IOException e){}
        try { imgChoppedTomato = ImageIO.read(getClass().getResourceAsStream("/res/ingredient/chopped_tomato.png")); } catch(IOException e){}
    }

    public void update(){
        if (gp.gameState != gp.playState) return;

        boolean isActive = (this == gp.players[gp.activePlayerIndex]);

        // advance picking
        if (isInteracting) {
            interactCounter++;
            if (interactCounter >= INTERACT_DURATION) {
                this.heldItem = pendingItem;
                this.heldItemImage = pendingItemImage;
                pendingItem = null; pendingItemImage = null;
                isInteracting = false; interactCounter = 0;
            }
            if (isActive) return;
        }

        // cutting progress
        if (isCutting) {
            if (isAtCuttingStation()) {
                cutCounter++;
                if (cutCounter >= CUT_DURATION_FRAMES) {
                    applyChoppedVariant();
                    isCutting = false; cutCounter = 0;
                }
            }
        }

        if (!isActive) return;

        // handle P for cutting toggle (if holding cuttable)
        if (keyH.pPressed) {
            if (heldItem != null && isCuttable(heldItem)) {
                isCutting = !isCutting;
            }
            keyH.pPressed = false;
        }

        // Handle E interactions with priority order:
        // 1) PlateStorage (if adjacent and player empty -> pick plate)
        // 2) AssemblyStation (complex behaviors & special cooked_meat fetch)
        // 3) CookingStation (existing behavior)
        // 4) Storage (ingredients)
        if (keyH.ePressed) {

            if ("plate".equals(this.heldItem)) {
                int centerX = x + solidArea.x + solidArea.width/2;
                int centerY = y + solidArea.y + solidArea.height/2;
                int col = centerX / gp.tileSize;
                int row = centerY / gp.tileSize;
        
                // loop semua cooking station dan cek adjacency manhattan <=1
                for (CookingStation cs : gp.cookingStations) {
                    if (Math.abs(cs.col - col) + Math.abs(cs.row - row) <= 1) {
                        if (cs.panAtStation() && "cooked_meat".equals(cs.panItem)) {
                            // transfer cooked_meat to player's plate
                            this.plateStack.add("cooked_meat");
                            // remove it from the frying pan and reset timer
                            cs.panItem = null;
                            cs.panTimer = 0;
                            // consume input and stop further interaction processing
                            keyH.ePressed = false;
                            return; // important: stop here so no other interact logic runs this frame
                        }
                    }
                }
            }

            // --- SERVING STATION (prioritas tinggi) ---
            int svIndex = getAdjacentServingStationIndex();
            if (svIndex != -1) {
                ServingStation sv = gp.servingStations.get(svIndex);
                boolean acted = sv.interact(this);
            if (acted) {
                keyH.ePressed = false;
                return; // consumed
                }
            }

            

            // 0) Trash station interaction (highest priority)
            int tsIndex = getAdjacentTrashStationIndex();
            if (tsIndex != -1) {
                TrashStation ts = gp.trashStations.get(tsIndex);
                boolean acted = ts.interact(this);
                if (acted) {
                    keyH.ePressed = false;
                    return; // consume the input and stop other interactions for this frame
                }
            }           

            // check washing station first
            int wsIndex = getAdjacentWashingStationIndex();
            if (wsIndex != -1) {
                WashingStation ws = gp.washingStations.get(wsIndex);
                int myIndex = getMyPlayerIndex();
                boolean acted = ws.interact(myIndex, this);
                if (acted) { keyH.ePressed = false; return; }
            }

            // 1) PlateStorage
            int psIndex = getAdjacentPlateStorageIndex();
            if (psIndex != -1) {
                PlateStorage ps = gp.plateStorages.get(psIndex);
                boolean acted = ps.interact(this);
                if (acted) { keyH.ePressed = false; return; }
            }

            // 2) AssemblyStation
            int asIndex = getAdjacentAssemblyStationIndex();
            if (asIndex != -1) {
                AssemblyStation as = gp.assemblyStations.get(asIndex);

                // Special: if player holds plate and assembly empty AND there is adjacent cooking station with cooked_meat,
                // transfer cooked_meat from that cooking station directly to player's plate.
                if ("plate".equals(this.heldItem) && as.plateStack == null && as.singleItem == null) {
                    boolean fetched = tryFetchCookedMeatToPlateNearby();
                    if (fetched) { keyH.ePressed = false; return; }
                }

                boolean acted = as.interact(this);
                if (acted) { keyH.ePressed = false; return; }
            }

            // 3) CookingStation (fallback existing behavior)
            int csIndex = getAdjacentCookingStationIndex();
            if (csIndex != -1) {
                CookingStation cs = gp.cookingStations.get(csIndex);
                int myIndex = getMyPlayerIndex();
                if (myIndex != -1) {
                    boolean acted = cs.interact(myIndex, this);
                    if (acted) { keyH.ePressed = false; return; }
                }
                else if ("plate".equals(this.heldItem) && cs.panAtStation() && "cooked_meat".equals(cs.panItem)){
                    // transfer cooked meat to player's plate
                    this.plateStack.add("cooked_meat");
                    // clear pan content and reset timer
                    cs.panItem = null;
                    cs.panTimer = 0;
                    keyH.ePressed = false; // consume input
                    return;
                }
            }

            // 4) Ingredient storage
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
                // holding something -> can't pick another
                keyH.ePressed = false;
            }
        }

        // movement input
        if (!isMoving) {
            if (keyH.upPressed) { direction = "up"; isMoving = true; goalX = x; goalY = y - gp.tileSize; }
            else if (keyH.downPressed) { direction = "down"; isMoving = true; goalX = x; goalY = y + gp.tileSize; }
            else if (keyH.leftPressed) { direction = "left"; isMoving = true; goalX = x - gp.tileSize; goalY = y; }
            else if (keyH.rightPressed) { direction = "right"; isMoving = true; goalX = x + gp.tileSize; goalY = y; }

            spriteCounter++;
            if (spriteCounter > 200) { spriteCounter = 0; spriteNum = (spriteNum==1?2:1); }
        }

        if (isMoving) {
            collisionOn = false;
            gp.cChecker.checkTile(this);
            gp.cChecker.checkPlayer(this, gp.players, gp.activePlayerIndex);
            if (!collisionOn) {
                switch(direction) {
                    case "up": y -= speed; if (y <= goalY) { y = goalY; isMoving = false; } break;
                    case "down": y += speed; if (y >= goalY) { y = goalY; isMoving = false; } break;
                    case "left": x -= speed; if (x <= goalX) { x = goalX; isMoving = false; } break;
                    case "right": x += speed; if (x >= goalX) { x = goalX; isMoving = false; } break;
                }
            } else { isMoving = false; }
        }
    }

    // Try to fetch cooked_meat from any adjacent cooking station into player's plate
    // Try to fetch cooked_meat from any adjacent cooking station into player's plate
private boolean tryFetchCookedMeatToPlateNearby() {
    for (CookingStation cs : gp.cookingStations) {
        // check adjacency (manhattan distance <= 1)
        int centerX = x + solidArea.x + solidArea.width/2;
        int centerY = y + solidArea.y + solidArea.height/2;
        int col = centerX / gp.tileSize;
        int row = centerY / gp.tileSize;
        if (Math.abs(cs.col - col) + Math.abs(cs.row - row) <= 1) {
            // only if pan at station and panItem == cooked_meat
            if (cs.panAtStation() && "cooked_meat".equals(cs.panItem)) {
                // transfer cooked meat to player's plate
                this.plateStack.add("cooked_meat");
                // remove cooked meat from pan
                cs.panItem = null;
                cs.panTimer = 0;
                return true;
            }
            // ALSO handle case where pan is carried by someone else? no — only panAtStation allowed here
        }
    }
    return false;
}


    private int getAdjacentPlateStorageIndex() {
        int centerX = x + solidArea.x + solidArea.width/2;
        int centerY = y + solidArea.y + solidArea.height/2;
        int col = centerX / gp.tileSize;
        int row = centerY / gp.tileSize;
        for (int i=0;i<gp.plateStorages.size();i++){
            PlateStorage ps = gp.plateStorages.get(i);
            if (ps.col == col && ps.row == row) return i;
            if (ps.col == col && ps.row == row-1) return i;
            if (ps.col == col && ps.row == row+1) return i;
            if (ps.col == col-1 && ps.row == row) return i;
            if (ps.col == col+1 && ps.row == row) return i;
        }
        return -1;
    }

    private int getAdjacentAssemblyStationIndex() {
        int centerX = x + solidArea.x + solidArea.width/2;
        int centerY = y + solidArea.y + solidArea.height/2;
        int col = centerX / gp.tileSize;
        int row = centerY / gp.tileSize;
        for (int i=0;i<gp.assemblyStations.size();i++){
            AssemblyStation as = gp.assemblyStations.get(i);
            if (as.col == col && as.row == row) return i;
            if (as.col == col && as.row == row-1) return i;
            if (as.col == col && as.row == row+1) return i;
            if (as.col == col-1 && as.row == row) return i;
            if (as.col == col+1 && as.row == row) return i;
        }
        return -1;
    }

    // (other helpers reused from previous Player implementations)
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

    private int getMyPlayerIndex() {
        for (int i = 0; i < gp.players.length; i++) if (gp.players[i] == this) return i;
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

    private void applyChoppedVariant() {
        if (heldItem == null) return;
        switch (heldItem) {
            case "meat": heldItem = "chopped_meat"; heldItemImage = imgChoppedMeat; break;
            case "cheese": heldItem = "chopped_cheese"; heldItemImage = imgChoppedCheese; break;
            case "lettuce": heldItem = "chopped_lettuce"; heldItemImage = imgChoppedLettuce; break;
            case "tomato": heldItem = "chopped_tomato"; heldItemImage = imgChoppedTomato; break;
            default: break;
        }
    }

    private boolean isCuttable(String itemName) {
        if (itemName == null) return false;
        return itemName.equals("meat") || itemName.equals("cheese") || itemName.equals("lettuce") || itemName.equals("tomato");
    }

    public void draw(Graphics2D g2){
        BufferedImage image = null;
        switch(direction){
            case "up": image = (spriteNum==1?up1:up2); break;
            case "down": image = (spriteNum==1?down1:down2); break;
            case "left": image = (spriteNum==1?left1:left2); break;
            case "right": image = (spriteNum==1?right1:right2); break;
        }
        g2.drawImage(image, x, y, gp.tileSize, gp.tileSize, null);

        // draw held item (if not a plate; if plate, we draw plate contents separately)
        if (heldItem != null && !"plate".equals(heldItem)) {
            int iconW = gp.tileSize/2;
            int iconH = gp.tileSize/2;
            int iconX = x + (gp.tileSize - iconW)/2;
            int iconY = y - iconH - 4;
            if (heldItemImage != null) g2.drawImage(heldItemImage, iconX, iconY, iconW, iconH, null);
            else {
                g2.setColor(java.awt.Color.WHITE);
                g2.drawString(heldItem, iconX, iconY);
            }
        }

        // draw plate on player (if player holds plate)
        if ("plate".equals(heldItem)) {
            if (PlateStorage.imgCleanPlate != null) {
                int iconW = gp.tileSize/2;
                int iconH = gp.tileSize/2;
                int iconX = x + (gp.tileSize - iconW)/2;
                int iconY = y - iconH - 4;
                g2.drawImage(PlateStorage.imgCleanPlate, iconX, iconY, iconW, iconH, null);
        
                // vertical stack on player's plate: draw from bottom to top
                int maxShow = Math.min(5, plateStack.size());
                int baseX = iconX + iconW/2;
                int baseY = iconY + iconH - (gp.tileSize/8); // slightly above bottom of plate icon
                int layerH = iconH / 4;
                for (int i = 0; i < maxShow; i++) {
                    BufferedImage ii = mapItemToImageForDraw(plateStack.get(i));
                    if (ii != null) {
                        int w = iconW * 2/3;
                        int h = layerH;
                        int ix = baseX - w/2;
                        int iy = baseY - i * (layerH - 2) - h;
                        g2.drawImage(ii, ix, iy, w, h, null);
                    }
                }
                if (plateStack.size() > maxShow) {
                    int more = plateStack.size() - maxShow;
                    g2.setColor(java.awt.Color.WHITE);
                    g2.setFont(new java.awt.Font("Arial", java.awt.Font.BOLD, 12));
                    g2.drawString("+" + more, iconX + iconW - 18, iconY + 8);
                }
            }
        }

        // draw dirty plates on player (if player holds dirty_plate)
        else if ("dirty_plate".equals(heldItem)) {
            if (PlateStorage.imgDirtyPlate != null) {
                int iconW = gp.tileSize/2;
                int iconH = gp.tileSize/2;
                int iconX = x + (gp.tileSize - iconW)/2;
                int iconY = y - iconH - 4;
                g2.drawImage(PlateStorage.imgDirtyPlate, iconX, iconY, iconW, iconH, null);

        // draw count
                g2.setColor(java.awt.Color.WHITE);
                g2.setFont(new java.awt.Font("Arial", java.awt.Font.BOLD, 12));
                g2.drawString("x"+dirtyPlateCount, iconX + iconW - 18, iconY + 10);
    }
}

        // draw interaction bars (pick, cut) same as previous implementations
        if (isInteracting) {
            double ratio = (double)interactCounter / (double)INTERACT_DURATION;
            int barW = gp.tileSize / 2; int barH = 6;
            int bx = x + (gp.tileSize - barW) / 2; int by = y - gp.tileSize/2 - barH - 8;
            g2.setColor(Color.DARK_GRAY); g2.fillRect(bx, by, barW, barH);
            g2.setColor(Color.GREEN); g2.fillRect(bx, by, (int)(barW * ratio), barH);
            g2.setColor(Color.WHITE); g2.drawRect(bx, by, barW, barH);
        }
        if (cutCounter > 0 && cutCounter < CUT_DURATION_FRAMES) {
            double ratio = (double)cutCounter / (double)CUT_DURATION_FRAMES;
            int barW = gp.tileSize / 2; int barH = 6;
            int bx = x + (gp.tileSize - barW) / 2; int by = y - gp.tileSize/2 - barH - 24;
            g2.setColor(new Color(40,40,40,200)); g2.fillRect(bx, by, barW, barH);
            g2.setColor(Color.ORANGE); g2.fillRect(bx, by, (int)(barW * ratio), barH);
            g2.setColor(Color.WHITE); g2.drawRect(bx, by, barW, barH);
        }
    }

    private BufferedImage mapItemToImageForDraw(String key) {
        if (key == null) return null;
        switch(key) {
            case "bun": return imgBun;
            case "chopped_cheese": return imgChoppedCheese;
            case "chopped_lettuce": return imgChoppedLettuce;
            case "chopped_tomato": return imgChoppedTomato;
            case "cooked_meat": return CookingStation.imgCookedMeat;
            default: return null;
        }
    }

    private int getAdjacentTrashStationIndex() {
        int centerX = x + solidArea.x + solidArea.width/2;
        int centerY = y + solidArea.y + solidArea.height/2;
        int col = centerX / gp.tileSize;
        int row = centerY / gp.tileSize;
        for (int i = 0; i < gp.trashStations.size(); i++) {
            TrashStation ts = gp.trashStations.get(i);
            if (ts.col == col && ts.row == row) return i;
            if (ts.col == col && ts.row == row - 1) return i;
            if (ts.col == col && ts.row == row + 1) return i;
            if (ts.col == col - 1 && ts.row == row) return i;
            if (ts.col == col + 1 && ts.row == row) return i;
        }
        return -1;
    }

    private int getAdjacentServingStationIndex() {
        int centerX = x + solidArea.x + solidArea.width/2;
        int centerY = y + solidArea.y + solidArea.height/2;
        int col = centerX / gp.tileSize;
        int row = centerY / gp.tileSize;
        for (int i=0;i<gp.servingStations.size();i++){
            ServingStation s = gp.servingStations.get(i);
            if (s.col == col && s.row == row) return i;
            if (s.col == col && s.row == row-1) return i;
            if (s.col == col && s.row == row+1) return i;
            if (s.col == col-1 && s.row == row) return i;
            if (s.col == col+1 && s.row == row) return i;
        }
        return -1;
    }    

    private int getAdjacentWashingStationIndex() {
        int centerX = x + solidArea.x + solidArea.width/2;
        int centerY = y + solidArea.y + solidArea.height/2;
        int col = centerX / gp.tileSize;
        int row = centerY / gp.tileSize;
        for (int i = 0; i < gp.washingStations.size(); i++) {
            WashingStation ws = gp.washingStations.get(i);
            if (ws.col == col && ws.row == row) return i;
            if (ws.col == col && ws.row == row - 1) return i;
            if (ws.col == col && ws.row == row + 1) return i;
            if (ws.col == col - 1 && ws.row == row) return i;
            if (ws.col == col + 1 && ws.row == row) return i;
        }
        return -1;
    }


}
