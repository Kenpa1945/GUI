package entity;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;

public class Entity {
    public int x,y;
    public int speed;
    public BufferedImage up1, up2, down1, down2, left1, left2, right1, right2;
    public Rectangle solidArea;
    public boolean collisionOn = false;
    public String direction;
    public boolean isMoving = false;
    public int goalX, goalY;
    public int spriteCounter = 0;
    public int spriteNum = 1;
}
