package org.newdawn.spaceinvaders;

import org.newdawn.spaceinvaders.entity.player.ShipEntity;

public class LevelUpOverlayScreen {
    private static final String DIALOG = "dialog";
    private GamePlayScreen gamePlayScreen;
    protected static final String[] LEVELUP_OPTIONS = {
            "공격력 +8%",
            "연사속도 +7%",
            "대시 쿨타임 -12%"
    };

    public LevelUpOverlayScreen(GamePlayScreen gamePlayScreen) {
        this.gamePlayScreen = gamePlayScreen;
    }


    public void drawLevelUpOverlay(java.awt.Graphics2D g2, ShipEntity ship) {
        // 반투명 배경
        g2.setColor(new java.awt.Color(0,0,0,160));
        g2.fillRect(0, 0, 800, 600); // 해상도에 맞게

        int panelW = 420;
        int panelH = 220;
        int px = (800 - panelW)/2;
        int py = (600 - panelH)/2;

        // 패널
        g2.setColor(new java.awt.Color(30, 30, 40, 230));
        g2.fillRoundRect(px, py, panelW, panelH, 16, 16);
        g2.setColor(new java.awt.Color(255, 215, 120));
        g2.drawRoundRect(px, py, panelW, panelH, 16, 16);

        // 제목
        g2.setFont(new java.awt.Font(DIALOG, java.awt.Font.BOLD, 20));
        g2.drawString("레벨 업! 강화할 스탯을 선택하세요", px + 24, py + 40);

        // 현재 레벨 표기(선택)
        g2.setFont(new java.awt.Font(DIALOG, java.awt.Font.PLAIN, 14));
        g2.drawString("남은 포인트: " + (ship.getStats().hasUnspentLevelUp()? "1+" : "0"), px + 24, py + 62);

        // 옵션 리스트
        int oy = py + 90;
        for (int i = 0; i < LEVELUP_OPTIONS.length; i++) {
            boolean sel = (i == gamePlayScreen.getLevelUpIndex());
            if (sel) {
                g2.setColor(new java.awt.Color(255,255,255,40));
                g2.fillRoundRect(px + 14, oy - 18, panelW - 28, 28, 10, 10);
            }
            g2.setColor(java.awt.Color.WHITE);
            g2.setFont(new java.awt.Font(DIALOG, sel? java.awt.Font.BOLD : java.awt.Font.PLAIN, 16));
            g2.drawString((i+1) + ". " + LEVELUP_OPTIONS[i], px + 24, oy);
            oy += 36;
        }

        g2.setFont(new java.awt.Font(DIALOG, java.awt.Font.PLAIN, 12));
        g2.setColor(new java.awt.Color(220,220,220));
        g2.drawString("↑/↓ 이동, Enter(또는 숫자 1~3)로 확정", px + 24, py + panelH - 18);
    }

    public void openLevelUpOverlay() {
        gamePlayScreen.setLevelUpActive(true);
        // 눌림 상태 초기화(선택)
        gamePlayScreen.getGame().setLeftPressed(false);
        gamePlayScreen.getGame().setRightPressed(false);
        gamePlayScreen.getGame().setUpPressed(false);
        gamePlayScreen.getGame().setDownPressed(false);
        gamePlayScreen.getGame().setFirePressed(false);
    }

    public  void closeLevelUpOverlay() {
        gamePlayScreen.setLevelUpActive(false);
    }

    public void handleKeyPress(int keyCode) {


        if (!gamePlayScreen.getLevelUpActive()) {
            return;
        }


        ShipEntity ship = gamePlayScreen.getShip();
        if (ship == null) {
            closeLevelUpOverlay();
            return;
        }


        if (keyCode == java.awt.event.KeyEvent.VK_UP) {
            int idx = gamePlayScreen.getLevelUpIndex();
            int len = LEVELUP_OPTIONS.length;
            gamePlayScreen.setLevelUpIndex((idx + len - 1) % len);
            return;
        }

        if (keyCode == java.awt.event.KeyEvent.VK_DOWN) {
            int idx = gamePlayScreen.getLevelUpIndex();
            int len = LEVELUP_OPTIONS.length;
            gamePlayScreen.setLevelUpIndex((idx + 1) % len);
            return;
        }


        if (keyCode == java.awt.event.KeyEvent.VK_ENTER
                || keyCode == java.awt.event.KeyEvent.VK_Z) {

            gamePlayScreen.applyLevelUpChoice(ship, gamePlayScreen.getLevelUpIndex());
            ship.getStats().spendLevelUpPoint();
            gamePlayScreen.finishOrStay(ship);
            return;
        }

        // 숫자 1~3 직접 선택
        handleNumericSelection(keyCode, ship);
    }

    private void handleNumericSelection(int keyCode, ShipEntity s) {
        int chosen;

        if (keyCode == java.awt.event.KeyEvent.VK_1) {
            chosen = 0;
        } else if (keyCode == java.awt.event.KeyEvent.VK_2) {
            chosen = 1;
        } else if (keyCode == java.awt.event.KeyEvent.VK_3) {
            chosen = 2;
        } else {
            return; // 숫자 1~3 외에는 무시
        }

        if (s == null) {
            return;
        }

        gamePlayScreen.setLevelUpIndex(chosen);
        s.getStats().spendLevelUpPoint();
        gamePlayScreen.finishOrStay(s);

    }
}
