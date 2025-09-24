package org.newdawn.spaceinvaders;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;

public class MenuScreen implements Screen {
    private Game game;
    private BufferedImage background;
    private String[] menuItems = {"스테이지 모드", "무한 모드", "스코어보드", "게임 종료"};
    private int menuIndex = 0;

    public MenuScreen(Game game) {
        this.game = game;
        try {
            background = ImageIO.read(getClass().getResource("/sprites/Menu.png"));
        } catch (Exception e) {
            System.err.println("메뉴 배경 로드 실패.." +e.getMessage());
        }
    }

    @Override
    public void render(Graphics2D g) {

        // 1.배경
        if(background != null) {
            g.drawImage(background, 0, 0, 800,600, null);
        }
        else {
            g.setColor(Color.black);
            g.fillRect(0, 0, 800, 600);
        }

        //2.타이틀
        g.setColor(Color.white);
        g.setFont(new Font("Dialog", Font.BOLD, 28));
        String title = "Space Invaders";
        g.drawString(title, (800 - g.getFontMetrics().stringWidth(title)) / 2, 150);

        // 3. 메뉴 항목
        g.setFont(new Font("Dialog", Font.PLAIN, 22));
        for (int i = 0; i < menuItems.length; i++) {
            String item = (i == menuIndex ? "> " : "  ") + menuItems[i];
            int x = (800 - g.getFontMetrics().stringWidth(item)) / 2;
            int y = 250 + i * 40;
            g.drawString(item, x, y);
        }
        // 4. 도움말
        g.setFont(new Font("Dialog", Font.ITALIC, 16));
        String help = "↑/↓: 이동, ENTER: 선택, ESC: 종료";
        g.drawString(help, (800 - g.getFontMetrics().stringWidth(help))/ 2, 500);
    }

    @Override
    public void update(long delta) {
        // 메뉴는 특별히 업데이트할 게 없음
    }

    @Override
    public void handleKeyPress(int keyCode) {
        if (keyCode == KeyEvent.VK_UP) {
            menuIndex = (menuIndex - 1 + menuItems.length) % menuItems.length;
        } else if (keyCode == KeyEvent.VK_DOWN) {
            menuIndex = (menuIndex + 1) % menuItems.length;
        } else if (keyCode == KeyEvent.VK_ENTER) {
            switch (menuIndex) {
                case 0: game.startStageMode(); break;
                case 1: game.startInfiniteMode(); break;
                //case 2: game.showScoreboard(); break;
                case 3: System.exit(0);
            }
        }
    }

    @Override
    public void handleKeyRelease(int keyCode) {
        // 메뉴는 특별히 release 동작 없음
    }
}
