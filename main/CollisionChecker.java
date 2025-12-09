package main;

import entity.Entity;
import entity.Player;
import java.awt.Rectangle;

public class CollisionChecker {
    GamePanel gp;

    public CollisionChecker(GamePanel gp){
        this.gp = gp;
    }

    public void checkTile(Entity entity){

        if (!entity.isMoving) return; // Hanya cek jika player sedang mencoba bergerak

        // Ambil goal yang sudah dihitung Player
        int goalX = entity.goalX; 
        int goalY = entity.goalY;

        // Hitung sudut solidArea di posisi goal
        int entityLeftGoalX = goalX + entity.solidArea.x;
        int entityRightGoalX = goalX + entity.solidArea.x + entity.solidArea.width;
        int entityTopGoalY = goalY + entity.solidArea.y;
        int entityBottomGoalY = goalY + entity.solidArea.y + entity.solidArea.height;

        // Konversi ke koordinat Tile (Col/Row)
        int entityLeftCol = entityLeftGoalX / gp.tileSize;
        int entityRightCol = entityRightGoalX / gp.tileSize;
        int entityTopRow = entityTopGoalY / gp.tileSize;
        int entityBottomRow = entityBottomGoalY / gp.tileSize;

        // Pastikan indeks tidak keluar dari batas peta
        if (entityLeftCol < 0 || entityRightCol >= gp.maxScreenCol ||
            entityTopRow < 0 || entityBottomRow >= gp.maxScreenRow) {
                entity.collisionOn = true;
                return;
        }

        // Tentukan dua tile yang mungkin disentuh oleh solidArea di posisi goal
        int tileNum1, tileNum2;

        switch (entity.direction) {
            case "up":
            case "down":
                // Jika bergerak vertikal, cek kolom yang sama di baris tujuan
                tileNum1 = gp.tileM.mapTileNum[entityLeftCol][entityTopRow]; // Sudut kiri atas/bawah
                tileNum2 = gp.tileM.mapTileNum[entityRightCol][entityTopRow]; // Sudut kanan atas/bawah
                break;
            case "left":
            case "right":
                // Jika bergerak horizontal, cek baris yang sama di kolom tujuan
                tileNum1 = gp.tileM.mapTileNum[entityLeftCol][entityTopRow]; // Sudut kiri atas/bawah
                tileNum2 = gp.tileM.mapTileNum[entityLeftCol][entityBottomRow]; // Sudut kanan atas/bawah
                break;
            default:
                // Tidak seharusnya terjadi jika isMoving=true
                return;
        }

        // Final check
        if (gp.tileM.tile[tileNum1].collision == true || gp.tileM.tile[tileNum2].collision == true) {
            entity.collisionOn = true;
        }
    }

    public void checkPlayer(Entity entity, Player[] players, int activePlayerIndex) {

    // Goal posisi yang sudah dihitung dari Player.update()
    int goalX = entity.goalX;
    int goalY = entity.goalY;

    // Hitung rectangle solid area di goal
    int entityLeft = goalX + entity.solidArea.x;
    int entityRight = goalX + entity.solidArea.x + entity.solidArea.width;
    int entityTop = goalY + entity.solidArea.y;
    int entityBottom = goalY + entity.solidArea.y + entity.solidArea.height;

    Rectangle entityGoalArea = new Rectangle(
        entityLeft,
        entityTop,
        entity.solidArea.width,
        entity.solidArea.height
    );

    // Loop semua pemain lain
    for(int i = 0; i < players.length; i++){
        if(i == activePlayerIndex) continue; // Skip diri sendiri

        Player target = players[i];

        Rectangle targetArea = new Rectangle(
            target.x + target.solidArea.x,
            target.y + target.solidArea.y,
            target.solidArea.width,
            target.solidArea.height
        );

        if(entityGoalArea.intersects(targetArea)){
            entity.collisionOn = true;
            return;
        }
    }
}

}
