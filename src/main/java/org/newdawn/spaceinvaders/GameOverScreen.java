package org.newdawn.spaceinvaders;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.event.KeyEvent;

public class GameOverScreen implements Screen {
    private final Game game;
    private int sel = 0;
    private final String[] items = {"재도전", "타이틀로"};

    public GameOverScreen(Game game) {
        this.game = game;
    }

    @Override
    public void render(Graphics2D g) {
        g.setColor(Color.white);
        String title = "GAME OVER";
        g.drawString(title, (800 - g.getFontMetrics().stringWidth(title)) / 2, 220);

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
            if (sel == 0) { // 재도전
                // 이전 모드로 재시작 (리플렉션 사용: Game에 isInfiniteMode() 게터 없을 때)
                try {
                    java.lang.reflect.Field f = Game.class.getDeclaredField("infiniteMode");
                    f.setAccessible(true);
                    boolean wasInfinite = f.getBoolean(game);
                    if (wasInfinite) game.startInfiniteMode(); else game.startStageMode();
                } catch (Exception ignore) {
                    game.startInfiniteMode(); // 실패하면 무한모드로 재시작
                }
            } else if (sel == 1) { // 타이틀로
                game.setScreen(new MenuScreen(game));
            }
        }
    }

    @Override
    public void handleKeyRelease(int keyCode) {
        // 필요 없으면 비워두기
    }
}