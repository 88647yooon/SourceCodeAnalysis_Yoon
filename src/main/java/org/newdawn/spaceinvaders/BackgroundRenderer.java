package org.newdawn.spaceinvaders;

import java.awt.*;


class BackgroundRenderer {

    static void draw(Graphics2D g, Game game) {
        if (game.getAlienCount() < 20) {
            g.setColor(Color.red);
        } else {
            g.setColor(Color.black);
        }
        g.fillRect(0, 0, 800, 600);

        if (game.isDangerMode()) {
            applyShake(g);
            }
        }

    private static void applyShake(Graphics2D g) {
        int offsetX = (int)(Math.random() * 10 - 5);
        int offsetY = (int)(Math.random() * 10 - 5);
        g.translate(offsetX, offsetY);
    }
}

