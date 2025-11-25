package org.newdawn.spaceinvaders.screen;

import org.newdawn.spaceinvaders.Game;
import org.newdawn.spaceinvaders.screen.MenuScreen;
import org.newdawn.spaceinvaders.screen.Screen;

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
        drawPressAnyKeyMessage(g);

    }

    private void drawPressAnyKeyMessage(Graphics2D g) {
        if (!game.isWaitingForKeyPress()) {
            return;
        }

        Font uiFont = g.getFont(); // 이미 설정된 폰트 재사용하거나, 새로 만들어도 됨
        g.setColor(Color.white);
        g.setFont(uiFont);

        String winMsg = game.getMessage();    // 게임에서 세팅해 둔 메인 메시지 (예: "Well done! You Win!")
        String msg    = "아무 키나 누르세요";

        int screenWidth = 800;

        if (winMsg != null && !winMsg.isEmpty()) {
            int winX = (screenWidth - g.getFontMetrics().stringWidth(winMsg)) / 2;
            g.drawString(winMsg, winX, 260);
        }

        int msgX = (screenWidth - g.getFontMetrics().stringWidth(msg)) / 2;
        g.drawString(msg, msgX, 300);
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
            if (sel == 0) { // 재도전
                game.restartLastMode();
            } else if (sel == 1) {
                game.setScreen(new MenuScreen(game));
            }
        }
    }

    @Override
    public void handleKeyRelease(int keyCode) {
        // 필요 없으면 비워두기
    }
}