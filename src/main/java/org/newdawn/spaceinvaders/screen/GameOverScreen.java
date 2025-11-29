package org.newdawn.spaceinvaders.screen;

import org.newdawn.spaceinvaders.Game;
import org.newdawn.spaceinvaders.input.InputState;

import java.awt.*;
import java.awt.event.KeyEvent;


public class GameOverScreen implements Screen {
    private final Game game;
    private int sel = 0;
    private final String[] items = {"재도전", "타이틀로"};
    private static final String TITLE = "GAME OVER";

    public GameOverScreen(Game game) {
        this.game = game;
    }





    @Override
    public void render(Graphics2D g) {
        g.setColor(Color.white);
        g.drawString(TITLE, (800 - g.getFontMetrics().stringWidth(TITLE)) / 2, 220);

        for (int i = 0; i < items.length; i++) {
            String line = (i == sel ? "> " : "  ") + items[i];
            g.drawString(line, (800 - g.getFontMetrics().stringWidth(line)) / 2, 280 + i * 28);
        }
        String help = "↑/↓ 이동  Enter 선택  Esc 타이틀";
        g.drawString(help, (800 - g.getFontMetrics().stringWidth(help)) / 2, 360);


    }

    @Override
    public void update(long delta) {
        // 게임오버 화면에서 애니메이션/타이머 없으면 비워둬도 OK
    }

    @Override
    public void handleKeyPress(int keyCode) {

        if (keyCode == KeyEvent.VK_UP) {
            sel = (sel + items.length - 1) % items.length;
        } else if (keyCode == KeyEvent.VK_DOWN) {
            sel = (sel + 1) % items.length;
        } else if (keyCode == KeyEvent.VK_ENTER) {
            if (sel == 0) { // Game에게 재시작해줘라고 요청만 함
                game.restartLastMode();
            } else if (sel == 1) {
                game.setScreen(new MenuScreen(game));
            }
        }
    }

    @Override
    public void handleKeyRelease(int keyCode) {
        InputState input = game.getPlayerController().getInputState();

        input.setLeft(false);
        input.setRight(false);
        input.setUp(false);
        input.setDown(false);
        input.setFire(false);

    }
}