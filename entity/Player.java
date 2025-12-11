package entity;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.IOException;

import javax.imageio.ImageIO;

import java.awt.Color;

import main.GamePanel;
import main.KeyHandler;

public class Player extends Entity{
    GamePanel gp;
    KeyHandler keyH; 
    String color;

    // Ingredient images (preload)
    BufferedImage imgBun, imgMeat, imgCheese, imgLettuce, imgTomato;

    // temporary pending item while interacting
    private String pendingItem = null;
    private BufferedImage pendingItemImage = null;

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
    }

    public void setDefaultValues(){

        x = 290;
        y = 95;
        speed = 1;
        direction = "down";

        // reset holding/interact
        heldItem = null;
        heldItemImage = null;
        isInteracting = false;
        interactCounter = 0;
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
        }catch(IOException e){
            // Jika resource tidak ditemukan, print stack trace tapi jangan crash program.
            e.printStackTrace();
        }
    }

    public void update(){
        if (gp.gameState != gp.playState) {
            // Player tidak boleh bergerak atau menerima input jika bukan playState
            return;
        }

        // Jika sedang berinteraksi (mengambil bahan), blok input lain sampai selesai
        if (isInteracting) {
            interactCounter++;
            if (interactCounter >= INTERACT_DURATION) {
                // selesai mengambil -> set held item
                this.heldItem = pendingItem;
                this.heldItemImage = pendingItemImage;

                // reset pending & interaction
                pendingItem = null;
                pendingItemImage = null;
                isInteracting = false;
                interactCounter = 0;
            }
            // selama interact, tidak memproses input bergerak
            return;
        }

        // Interaksi: tekan E
        if (keyH.ePressed) {
            // hanya mulai interaksi jika player saat ini tidak membawa barang
            if (this.heldItem == null) {
                // cek apakah ada storage di tile saat ini atau tile tetangga (adjacent)
                int storageTileNum = getAdjacentStorageTile();
                if (storageTileNum != -1) {
                    // mulai proses mengambil
                    pendingItem = tileNumToItemName(storageTileNum);
                    pendingItemImage = tileNumToImage(storageTileNum);
                    if (pendingItem != null && pendingItemImage != null) {
                        isInteracting = true;
                        interactCounter = 0;
                        // cegah multi-trigger (tahan 1 kali tekan)
                        keyH.ePressed = false;
                        return;
                    }
                }
            } else {
                // sudah membawa item -> tidak bisa mengambil lain
                // optional: Anda bisa menambahkan pesan, suara, atau flash
                keyH.ePressed = false;
            }
        }

        if (!isMoving) {
            // Cek input hanya jika player tidak sedang bergerak
            if(keyH.upPressed == true){
                direction = "up";
                isMoving = true;
                goalX = x;
                goalY = y - gp.tileSize; // Target y adalah 1 tile ke atas
            }
            else if(keyH.downPressed == true){
                direction = "down";
                isMoving = true;
                goalX = x;
                goalY = y + gp.tileSize; // Target y adalah 1 tile ke bawah
            }
            else if(keyH.leftPressed == true){
                direction = "left";
                isMoving = true;
                goalX = x - gp.tileSize; // Target x adalah 1 tile ke kiri
                goalY = y;
            }else if(keyH.rightPressed == true){
                direction = "right";
                isMoving = true;
                goalX = x + gp.tileSize; // Target x adalah 1 tile ke kanan
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

        // === Bagian 2: Melakukan Pergerakan ke Tujuan ===
        if (isMoving) {

            // Cek Tabrakan di tile target SEBELUM bergerak
            collisionOn = false;
            gp.cChecker.checkTile(this); // Asumsi checkTile() diperbarui untuk mengecek goalX/goalY
            gp.cChecker.checkPlayer(this, gp.players, gp.acivePlayerIndex);

            // Jika TIDAK ada tabrakan, lakukan pergerakan
            if (collisionOn == false) {

                // Pergerakan satu langkah (speed) menuju goal
                switch (direction) {
                    case "up":
                        y -= speed;
                        if (y <= goalY) { // Cek jika sudah mencapai atau melewati target
                            y = goalY;
                            isMoving = false; // Berhenti bergerak
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
                // Jika ada tabrakan, batalkan pergerakan dan reset status
                isMoving = false; 
                // tidak mengubah x/y karena tidak memulai pergerakan
            }
        }
    }

    // Mengembalikan nomor tile storage adjacent (player tile atau tetangga atas/bawah/kiri/kanan), -1 jika tidak ada
    private int getAdjacentStorageTile() {
        int centerX = x + solidArea.x + solidArea.width/2;
        int centerY = y + solidArea.y + solidArea.height/2;

        int col = centerX / gp.tileSize;
        int row = centerY / gp.tileSize;

        // Cek tile center
        if (isStorageTile(col, row)) return gp.tileM.mapTileNum[col][row];

        // atas
        if (row - 1 >= 0 && isStorageTile(col, row - 1)) return gp.tileM.mapTileNum[col][row - 1];
        // bawah
        if (row + 1 < gp.maxScreenRow && isStorageTile(col, row + 1)) return gp.tileM.mapTileNum[col][row + 1];
        // kiri
        if (col - 1 >= 0 && isStorageTile(col - 1, row)) return gp.tileM.mapTileNum[col - 1][row];
        // kanan
        if (col + 1 < gp.maxScreenCol && isStorageTile(col + 1, row)) return gp.tileM.mapTileNum[col + 1][row];

        return -1;
    }

    private boolean isStorageTile(int col, int row) {
        int t = gp.tileM.mapTileNum[col][row];
        return t == 7 || t == 10 || t == 11 || t == 12 || t == 13;
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
                }
                if(spriteNum == 2){
                    image = up2;
                }
                break;
            case "down":
                if(spriteNum == 1){
                    image = down1;
                }
                if(spriteNum == 2){
                    image = down2;
                }
                break;
            case "left":
                if(spriteNum == 1){
                    image = left1;
                }
                if(spriteNum == 2){
                    image = left2;
                }
                break;
            case "right":
                if(spriteNum == 1){
                    image = right1;
                }
                if(spriteNum == 2){
                    image = right2;
                }
                break;
        }

        g2.drawImage(image, x, y, gp.tileSize, gp.tileSize, null);

        // Gambar held item di atas kepala pemain (jika ada)
        if (heldItemImage != null) {
            int iconW = gp.tileSize / 2;
            int iconH = gp.tileSize / 2;
            int iconX = x + (gp.tileSize - iconW) / 2;
            int iconY = y - iconH - 4; // sedikit jarak di atas kepala
            g2.drawImage(heldItemImage, iconX, iconY, iconW, iconH, null);
        }

        // Jika sedang berinteraksi, gambar indikator kecil (mis. kotak progress sederhana)
        if (isInteracting) {
            // progress ratio
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
    }
}
