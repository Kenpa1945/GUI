package entity;

import java.awt.Rectangle;

public class Entity {
    public int x,y;
    public int speed;
    public Rectangle solidArea;
    public boolean collisionOn = false;
    public String direction = "stay";
    public boolean isMoving = false;
    public int goalX, goalY;
}

