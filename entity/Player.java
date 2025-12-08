package entity;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Color;

import main.GamePanel;
import main.KeyHandler;

public class Player extends Entity{
    GamePanel gp;
    KeyHandler keyH; 

    public Player(GamePanel gp, KeyHandler keyH){
        this.gp = gp;
        this.keyH = keyH;

        solidArea = new Rectangle();
        solidArea.x = 8;
        solidArea.y = 16;
        solidArea.width = 32;
        solidArea.height = 32;

        setDefaultValues();
    }

    public void setDefaultValues(){

        x = 290;
        y = 95;
        speed = 1;
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
        }
        
        // === Bagian 2: Melakukan Pergerakan ke Tujuan ===
        if (isMoving) {
            
            // Cek Tabrakan di tile target SEBELUM bergerak
            collisionOn = false;
            gp.cChecker.checkTile(this); // Asumsi checkTile() diperbarui untuk mengecek goalX/goalY
            // CATATAN: Karena checkTile yang sebelumnya Anda miliki hanya melihat KeyHandler, 
            // Anda perlu memastikan CollisionChecker membandingkan goalX/goalY dengan tile.
            
            // Jika TIDAK ada tabrakan, lakukan pergerakan
            if (collisionOn == false) {
                
                // Pergerakan satu langkah (speed) menuju goal
                switch (direction) {
                    case "up":
                        y -= speed;
                        if (y <= goalY) { // Cek jika sudah mencapai atau melewati target
                            y = goalY;
                            isMoving = false; // Berhenti bergerak
                            direction = "stay";
                        }
                        break;
                    case "down":
                        y += speed;
                        if (y >= goalY) {
                            y = goalY;
                            isMoving = false;
                            direction = "stay";
                        }
                        break;
                    case "left":
                        x -= speed;
                        if (x <= goalX) {
                            x = goalX;
                            isMoving = false;
                            direction = "stay";
                        }
                        break;
                    case "right":
                        x += speed;
                        if (x >= goalX) {
                            x = goalX;
                            isMoving = false;
                            direction = "stay";
                        }
                        break;
                }
            } else {
                // Jika ada tabrakan, batalkan pergerakan dan reset status
                isMoving = false; 
                direction = "stay";
                // TIDAK perlu mengubah x/y karena tabrakan mencegah inisiasi pergerakan
            }
        }
    }

    public void draw(Graphics2D g2){
        g2.setColor(Color.black);
        g2.fillRect(x, y, gp.tileSize, gp.tileSize);
    }
}
