package org.newdawn.spaceinvaders;

import org.newdawn.spaceinvaders.entity.Entity;

import java.awt.*;
import java.awt.event.KeyEvent;

public class GamePlayScreen implements Screen{
    private Game game;
    private BackgroundRenderer backgroundRenderer;

    public GamePlayScreen(Game game) {
        this.game = game;
        this.backgroundRenderer = new BackgroundRenderer();
    }

    @Override
    public void render(Graphics2D g){
        // 웨이브별 배경 (10 웨이브마다 교체)
        backgroundRenderer.render(g, game.waveCount, 800, 600);

        // 엔티티 그리기
        for (Entity e : game.getEntities()) {
            e.draw(g);
        }

        // "Press any key" 상태 표시
        if (game.isWaitingForKeyPress()) {
            g.setColor(Color.white);
            String msg = "Press any key";
            g.drawString(msg, (800 - g.getFontMetrics().stringWidth(msg)) / 2, 300);
        }
    }

    @Override
    public void update(long delta) {
        game.updateEntities(delta); // Game 내부 메서드로 엔티티 이동/충돌 처리
    }

    @Override
    public void handleKeyPress(int keyCode) {
        if (keyCode == KeyEvent.VK_LEFT) {
            game.setLeftPressed(true);
        }
        if (keyCode == KeyEvent.VK_RIGHT) {
            game.setRightPressed(true);
        }
        if(keyCode == KeyEvent.VK_UP) {
            game.setUpPressed(true);
        }
        if(keyCode == KeyEvent.VK_DOWN) {
            game.setDownPressed(true);
        }
        if (keyCode == KeyEvent.VK_SPACE) {
            game.setFirePressed(true);
        }
    }

    @Override
    public void handleKeyRelease(int keyCode) {
        if (keyCode == KeyEvent.VK_LEFT) {
            game.setLeftPressed(false);
        }
        if (keyCode == KeyEvent.VK_RIGHT) {
            game.setRightPressed(false);
        }
        if(keyCode == KeyEvent.VK_UP) {
            game.setUpPressed(false);
        }
        if (keyCode == KeyEvent.VK_DOWN) {
            game.setDownPressed(false);
        }
        if (keyCode == KeyEvent.VK_SPACE) {
            game.setFirePressed(false);
        }
    }


}
