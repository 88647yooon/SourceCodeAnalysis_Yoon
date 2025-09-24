package org.newdawn.spaceinvaders;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Objects;


class BackgroundRenderer {
    private BufferedImage[] backgrounds;

    public BackgroundRenderer() {
        try{
            backgrounds = new BufferedImage[3];
            backgrounds[0] = ImageIO.read(getClass().getResource("/sprites/BackGround1.png"));
            backgrounds[1] = ImageIO.read(getClass().getResource("/sprites/BackGround2.png"));
            backgrounds[2] = ImageIO.read(getClass().getResource("/sprites/BackGround3.png"));

        } catch(Exception e){
            e.printStackTrace();
        }

    }
    public void render(Graphics2D g, int waveCount, int width, int height) {
        if (backgrounds == null || backgrounds.length == 0) return;

        int bgIndex =  (waveCount / 10) % backgrounds.length;

        BufferedImage bg = backgrounds[bgIndex];
        if (bg == null) {
            g.setColor(Color.black); // fallback
            g.fillRect(0, 0, width, height);
            return;
        }
        g.drawImage(bg, 0 ,0, width, height, null);
    }

    private static void applyShake(Graphics2D g) {
        int offsetX = (int)(Math.random() * 10 - 5);
        int offsetY = (int)(Math.random() * 10 - 5);
        g.translate(offsetX, offsetY);
    }
}

