package main;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;


public class KeyHandler implements KeyListener{

    public boolean upPressed, leftPressed,
                   rightPressed, downPressed, switchPressed, enterPressed;
    public boolean ePressed = false; // E untuk mengambil bahan (existing)
    public boolean pPressed = false; // P untuk cutting

    @Override
    public void keyTyped(KeyEvent e){
    } 

    @Override
    public void keyPressed(KeyEvent e){

        int code = e.getKeyCode();

        if(code == KeyEvent.VK_W){
            upPressed = true;
        }
        if(code == KeyEvent.VK_A){
            leftPressed = true;
        }
        if(code == KeyEvent.VK_S){
            downPressed = true;
        }
        if(code == KeyEvent.VK_D){
            rightPressed = true;
        }
        if(code == KeyEvent.VK_SPACE){
            switchPressed = true;
        }
        if(code == KeyEvent.VK_ENTER){ // Tambahkan input ENTER
            enterPressed = true;
        }
        if(code == KeyEvent.VK_E){ // Tambahkan tombol E
            ePressed = true;
        }
        if(code == KeyEvent.VK_P){ // Tambahkan tombol P (cutting)
            pPressed = true;
        }

    }

    @Override
    public void keyReleased(KeyEvent e){

        int code = e.getKeyCode();

        if(code == KeyEvent.VK_W){
            upPressed = false;
        }
        if(code == KeyEvent.VK_A){
            leftPressed = false;
        }
        if(code == KeyEvent.VK_S){
            downPressed = false;
        }
        if(code == KeyEvent.VK_D){
            rightPressed = false;
        }
        if(code == KeyEvent.VK_SPACE){
            switchPressed = false;
        }
        if(code == KeyEvent.VK_ENTER){ // Tambahkan input ENTER
            enterPressed = false;
        }
        if(code == KeyEvent.VK_E){
            ePressed = false;
        }
        if(code == KeyEvent.VK_P){
            pPressed = false;
        }
    }
}
