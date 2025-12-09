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
    }

    public void setDefaultValues(){

        x = 290;
        y = 95;
        speed = 1;
        direction = "down";
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

    public void update(){
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
            // CATATAN: Karena checkTile yang sebelumnya Anda miliki hanya melihat KeyHandler, 
            // Anda perlu memastikan CollisionChecker membandingkan goalX/goalY dengan tile.
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
                switch (direction) {
                    case "up":
                        direction = "up";
                        break;
                    case "down":
                        direction = "down";
                        break;
                    case "left":
                        direction = "left";
                        break;
                    case "right":
                        direction = "right";
                        break;
                // TIDAK perlu mengubah x/y karena tabrakan mencegah inisiasi pergerakan
                }
            }
        }
    }

    public void draw(Graphics2D g2){
        //g2.setColor(Color.black);
        //g2.fillRect(x, y, gp.tileSize, gp.tileSize);

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
    }
}
