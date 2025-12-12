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
import main.IngredientStation;
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

    // === image cache untuk item (letakkan di field area Player) ===
    private static java.util.Map<String, java.awt.image.BufferedImage> ITEM_IMAGE_CACHE = new java.util.HashMap<>();

    private static java.awt.image.BufferedImage loadItemImage(String path) {
        try {
            return javax.imageio.ImageIO.read(Player.class.getResourceAsStream(path));
        } catch (Exception e) {
            // file missing -> return null silently
            return null;
        }
    }

    // plate contents when player holds a plate
    public ArrayList<String> plateStack;

    // pending picking
    private String pendingItem = null;
    private BufferedImage pendingItemImage = null;

    // cutting fields
    public boolean isCutting = false;
    public int cutCounter = 0;
    public int CUT_DURATION_SECONDS = 18;
    public int CUT_DURATION_FRAMES = 0;

    // jumlah piring kotor yang dipegang (jika player.heldItem == "dirty_plate")
    public int dirtyPlateCount = 0;

    // Debounce untuk tombol drop (Q) — track apakah Q sudah diproses / sedang ditekan di frame sebelumnya
    private boolean prevQPressed = false;



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

        // handle drop key Q
        if (keyH.qPressed) {
            boolean acted = false;
            // priority: if player has fryingpan with content, drop content first
            acted = tryDropPanContentToFront();
            if (!acted) {
                // else if player holds ingredient, drop it
                acted = tryDropToFront();
            }
            
            // *** PERBAIKAN DITEMPATKAN DI SINI ***
            // Konsumsi input Q, terlepas dari apakah aksi berhasil.
            // Ini mencegah penekanan yang gagal memicu aksi di frame berikutnya.
            keyH.qPressed = false;
            
            // Jika aksi berhasil, kita bisa menghentikan frame ini untuk mencegah
            // tindakan lain yang mungkin terjadi dalam fungsi update() yang sama.
            if (acted) {
                return; // exit update so no immediate other actions this frame
            }
            
            // Jika tidak acted, kita telah mengkonsumsi input dan update() berlanjut
        }

        // at very end of update (or after input handling) remember to update prevQPressed:


        // Handle E interactions with priority order:
        // 1) PlateStorage (if adjacent and player empty -> pick plate)
        // 2) AssemblyStation (complex behaviors & special cooked_meat fetch)
        // 3) CookingStation (existing behavior)
        // 4) Storage (ingredients)
        // ----------------- REPLACE THE WHOLE if (keyH.ePressed) { ... } BLOCK WITH THIS -----------------
if (keyH.ePressed) {
    if (gp.floorItem == null || gp.floorItemImage == null) {
        keyH.ePressed = false;
        return;
    }
    // compute front tile once
    int[] coord = getFrontTileCoord(); // returns [col,row] or null
    if (coord == null) {
        keyH.ePressed = false;
        return;
    }
    int fc = (coord != null) ? coord[0] : -1;
    int fr = (coord != null) ? coord[1] : -1;
    String floorKey = (coord != null) ? gp.floorItem[fc][fr] : null;

    // allowed items to stack on a plate
    java.util.List<String> ALLOWED_ON_PLATE = java.util.Arrays.asList(
        "bun", "chopped_cheese", "chopped_lettuce", "chopped_tomato", "cooked_meat"
    );

    // 0) If player holds a plate -> try pick floor->plate (highest priority)
    if ("plate".equals(this.heldItem) && floorKey != null) {
        if (ALLOWED_ON_PLATE.contains(floorKey)) {
            this.plateStack.add(floorKey);
            gp.floorItem[fc][fr] = null;
            gp.floorItemImage[fc][fr] = null;
            keyH.ePressed = false;
            return;
        }
        // if floor item exists but not allowed on plate, continue to other interactions
    }

    // 0.1) If player holds a plate and adjacent to a cooking station with cooked_meat -> take from pan
    if ("plate".equals(this.heldItem)) {
        int centerX = x + solidArea.x + solidArea.width/2;
        int centerY = y + solidArea.y + solidArea.height/2;
        int col = centerX / gp.tileSize;
        int row = centerY / gp.tileSize;

        for (CookingStation cs : gp.cookingStations) {
            if (Math.abs(cs.col - col) + Math.abs(cs.row - row) <= 1) {
                if (cs.panAtStation() && "cooked_meat".equals(cs.panItem)) {
                    this.plateStack.add("cooked_meat");
                    cs.panItem = null;
                    cs.panTimer = 0;
                    keyH.ePressed = false;
                    return;
                }
            }
        }
    }

    // 1) If player empty handed -> try pick floor->hand
    if (this.heldItem == null && floorKey != null) {
        // pick up any floor item (ingredients or dirty_plate)
        this.heldItem = floorKey;
        this.heldItemImage = mapItemToImageForDraw(floorKey);
        if ("dirty_plate".equals(floorKey)) {
            // if floor supports count, adjust; here assume 1
            this.dirtyPlateCount = 1;
        }
        gp.floorItem[fc][fr] = null;
        gp.floorItemImage[fc][fr] = null;
        keyH.ePressed = false;
        return;
    }

    // 2) Washing Station (allow washer to accept dirty plates or start washing)
    int wsIndex = getAdjacentWashingStationIndex();
    if (wsIndex != -1) {
        WashingStation ws = gp.washingStations.get(wsIndex);
        int myIndex = getMyPlayerIndex();
        boolean acted = ws.interact(myIndex, this);
        if (acted) { keyH.ePressed = false; return; }
    }

    // 3) Serving Station
    int svIndex = getAdjacentServingStationIndex();
    if (svIndex != -1) {
        ServingStation sv = gp.servingStations.get(svIndex);
        boolean acted = sv.interact(this);
        if (acted) { keyH.ePressed = false; return; }
    }

    // 4) Trash station
    int tsIndex = getAdjacentTrashStationIndex();
    if (tsIndex != -1) {
        TrashStation ts = gp.trashStations.get(tsIndex);
        boolean acted = ts.interact(this);
        if (acted) { keyH.ePressed = false; return; }
    }

    // 5) Plate Storage
    int psIndex = getAdjacentPlateStorageIndex();
    if (psIndex != -1) {
        PlateStorage ps = gp.plateStorages.get(psIndex);
        boolean acted = ps.interact(this);
        if (acted) { keyH.ePressed = false; return; }
    }

    // 5.5) INGREDIENT STATION (OBJECT, BUKAN TILE)
    int centerX = x + solidArea.x + solidArea.width / 2;
    int centerY = y + solidArea.y + solidArea.height / 2;
    int col = centerX / gp.tileSize;
    int row = centerY / gp.tileSize;

    for (IngredientStation is : gp.ingredientStations) {
        if (Math.abs(is.col - col) + Math.abs(is.row - row) <= 1) {
            if (is.interact(this)) {
                keyH.ePressed = false;
                return;
            }
        }
    }

    // 6) Assembly Station (with special: transfer cooked_meat nearby to plate if empty)
    int asIndex = getAdjacentAssemblyStationIndex();
    if (asIndex != -1) {
        AssemblyStation as = gp.assemblyStations.get(asIndex);

        if ("plate".equals(this.heldItem) && as.plateStack == null && as.singleItem == null) {
            boolean fetched = tryFetchCookedMeatToPlateNearby();
            if (fetched) { keyH.ePressed = false; return; }
        }

        boolean acted = as.interact(this);
        if (acted) { keyH.ePressed = false; return; }
    }

    // 7) Cooking Station (interact / pick cooked meat if pan at station and player holds plate)
    int csIndex = getAdjacentCookingStationIndex();
    if (csIndex != -1) {
        CookingStation cs = gp.cookingStations.get(csIndex);
        int myIndex = getMyPlayerIndex();
        if (myIndex != -1) {
            boolean acted = cs.interact(myIndex, this);
            if (acted) { keyH.ePressed = false; return; }
        } else if ("plate".equals(this.heldItem) && cs.panAtStation() && "cooked_meat".equals(cs.panItem)) {
            this.plateStack.add("cooked_meat");
            cs.panItem = null;
            cs.panTimer = 0;
            keyH.ePressed = false;
            return;
        }
    } else {
        // holding something -> can't start storage pickup; consume input to avoid repeats
        keyH.ePressed = false;
    }
}
// ----------------- END REPLACEMENT BLOCK -----------------


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

    private java.awt.image.BufferedImage mapItemToImageForDraw(String key) {
        if (key == null) return null;
        if (ITEM_IMAGE_CACHE.containsKey(key)) return ITEM_IMAGE_CACHE.get(key);
    
        java.awt.image.BufferedImage img = null;
        switch (key) {
            case "bun": img = loadItemImage("/res/ingredient/bun.png"); break;
            case "meat": img = loadItemImage("/res/ingredient/meat.png"); break;
            case "chopped_meat": img = loadItemImage("/res/ingredient/chopped_meat.png"); break;
            case "chopped_cheese": img = loadItemImage("/res/ingredient/chopped_cheese.png"); break;
            case "chopped_lettuce": img = loadItemImage("/res/ingredient/chopped_lettuce.png"); break;
            case "chopped_tomato": img = loadItemImage("/res/ingredient/chopped_tomato.png"); break;
            case "cooked_meat": img = loadItemImage("/res/ingredient/cooked_meat.png"); break;
            case "burned_meat": img = loadItemImage("/res/ingredient/burned_meat.png"); break;
            case "cheese": img = loadItemImage("/res/ingredient/cheese.png"); break;
            case "lettuce": img = loadItemImage("/res/ingredient/lettuce.png"); break;
            case "tomato": img = loadItemImage("/res/ingredient/tomato.png"); break;
            case "clean_plate": img = loadItemImage("/res/ingredient/clean_plate.png"); break;
            case "dirty_plate": img = loadItemImage("/res/ingredient/dirty_plate.png"); break;
            default:
                // try generic path: /res/ingredient/<key>.png
                img = loadItemImage("/res/ingredient/" + key + ".png");
                break;
        }
    
        ITEM_IMAGE_CACHE.put(key, img); // may be null if missing
        return img;
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

    // returns [col,row] of tile in front of player (one tile ahead), or null if out of bounds
    private int[] getFrontTileCoord() {
        int centerX = x + solidArea.x + solidArea.width/2;
        int centerY = y + solidArea.y + solidArea.height/2;
        int col = centerX / gp.tileSize;
        int row = centerY / gp.tileSize;
        switch (direction) {
            case "up": row = row - 1; break;
            case "down": row = row + 1; break;
            case "left": col = col - 1; break;
            case "right": col = col + 1; break;
        }
        if (col < 0 || col >= gp.maxScreenCol || row < 0 || row >= gp.maxScreenRow) return null;
        return new int[]{col, row};
    }

    // attempt to drop currently held ingredient to front tile
    // returns true if drop happened
    private boolean tryDropToFront() {
        if (heldItem == null) return false;
        if (heldItem.equals("plate") || heldItem.equals("fryingpan") || heldItem.equals("dirty_plate")) return false;
    
        int[] coord = getFrontTileCoord();
        if (coord == null) return false;
        int col = coord[0], row = coord[1];
    
        int tileNum = gp.tileM.mapTileNum[col][row];
        if (tileNum != 0) return false;
        if (gp.floorItem[col][row] != null) return false;
    
        // Drop ingredient from player's hand to floor
        gp.floorItem[col][row] = heldItem;
        gp.floorItemImage[col][row] = mapItemToImageForDraw(heldItem); // <-- set image here
        // clear player's held item
        heldItem = null;
        heldItemImage = null;
        return true;
    }

    // If player is carrying a fryingpan (panOwner), drop the pan's item to the front tile (if allowed)
    private boolean tryDropPanContentToFront() {
        // find CookingStation whose panOwner is this player
        int myIdx = getMyPlayerIndex();
        if (myIdx == -1) return false;
        CookingStation ownerCS = null;
        for (CookingStation cs : gp.cookingStations) {
            if (cs.panOwner == myIdx) { ownerCS = cs; break; }
        }
        if (ownerCS == null) return false;
        if (ownerCS.panItem == null) return false; // nothing to drop

        // check front tile
        int[] coord = getFrontTileCoord();
        if (coord == null) return false;
        int col = coord[0], row = coord[1];
        int tileNum = gp.tileM.mapTileNum[col][row];
        if (tileNum != 0) return false;
        if (gp.floorItem[col][row] != null) return false;

        // drop panItem to floor
        String itemToDrop = ownerCS.panItem;
        ownerCS.panItem = null;
        ownerCS.panTimer = 0;
        gp.floorItem[col][row] = itemToDrop;
        gp.floorItemImage[col][row] = mapItemToImageForDraw(itemToDrop);
        return true;
    }

    



}
