package org.newdawn.spaceinvaders.screen;

import java.awt.*;

public class ScoreBoardScreenImageRenderer {
    private final Image backgroundImage;
    public ScoreBoardScreenImageRenderer() {

        this.backgroundImage = loadImage();
    }

    public Image loadImage() {
        try {
            java.net.URL url = getClass().getResource("/sprites/AuthScreenImage.png");
            if (url == null) {

                return null;
            }
            return new javax.swing.ImageIcon(url).getImage();
        } catch (Exception e) {
            return null;
        }
    }

    public void renderBackground(Graphics2D g) {
        if (backgroundImage != null) {
            // 화면 크기에 맞춰 이미지 그리기 (800x600)
            g.drawImage(backgroundImage, 0, 0, 800, 600, null);
        } else {
            // 이미지가 없을 경우 검은색 배경 (Fallback)
            g.setColor(Color.black);
            g.fillRect(0, 0, 800, 600);
        }
    }


}


