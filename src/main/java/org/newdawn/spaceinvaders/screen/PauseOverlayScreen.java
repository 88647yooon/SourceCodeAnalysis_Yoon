package org.newdawn.spaceinvaders.screen;

import org.newdawn.spaceinvaders.Game;
import org.newdawn.spaceinvaders.entity.player.PlayerSkills;
import org.newdawn.spaceinvaders.entity.player.ShipEntity;
import org.newdawn.spaceinvaders.manager.StageProgressManager;

import java.awt.*;
import java.awt.event.KeyEvent;

public class PauseOverlayScreen {
    private final GamePlayScreen gamePlayScreen;
    private final Game game;

    private static final String FONT_NAME = "Dialog";

    private final String[] OPTIONS = {"게임 재개", "재시작", "메인 화면"};
    private int selectionIndex = 0;

    public PauseOverlayScreen(GamePlayScreen gamePlayScreen) {
        this.gamePlayScreen = gamePlayScreen;
        this.game = gamePlayScreen.getGame();
    }

    public void draw(Graphics2D g) {
        // 1. 반투명 배경
        g.setColor(new Color(0, 0, 0, 180));
        g.fillRect(0, 0, 800, 600);

        // 2. 패널 박스 (가로를 넓혀서 좌우 배치 공간 확보)
        int w = 520;
        int h = 420;
        int x = (800 - w) / 2;
        int y = (600 - h) / 2;

        g.setColor(new Color(30, 30, 40));
        g.fillRoundRect(x, y, w, h, 20, 20);
        g.setColor(Color.WHITE);
        g.drawRoundRect(x, y, w, h, 20, 20);

        // 3. 상단 공통 정보 (타이틀 + 스테이지 기록)
        drawTopSection(g, x, y, w);

        // 4. 구분선 (중간 가로선)
        g.setColor(new Color(80, 80, 80));
        g.drawLine(x + 20, y + 150, x + w - 20, y + 150);

        // 5. 하단 분할 영역 (좌: 메뉴 / 우: 스탯)
        drawBottomSplitSection(g, x, y + 180, w);
    }

    private void drawTopSection(Graphics2D g, int x, int y, int w) {
        // 타이틀
        g.setFont(new Font(FONT_NAME, Font.BOLD, 32));
        g.setColor(Color.WHITE);
        drawCenteredString(g, "PAUSED", x, w, y + 50);

        g.setColor(Color.LIGHT_GRAY);

        // 스테이지/별 정보 OR 점수
        if (game.isStageMode()) {
            int stageId = game.getCurrentStageId();
            int savedStars = game.getStageStars(stageId);

            // 스테이지 제목
            g.setFont(new Font(FONT_NAME, Font.BOLD, 22));
            g.setColor(new Color(255, 220, 100)); // 금색 느낌
            drawCenteredString(g, "Stage " + stageId + " 기록", x, w, y + 90);

            // 별 그리기
            g.setFont(new Font(FONT_NAME, Font.PLAIN, 24));
            g.setColor(Color.YELLOW);
            StringBuilder starStr = new StringBuilder();
            for (int i = 0; i < 3; i++) {
                starStr.append(i < savedStars ? "★ " : "☆ ");
            }
            drawCenteredString(g, starStr.toString(), x, w, y + 120);

            // 간단 설명 (폰트 작게)
            g.setFont(new Font(FONT_NAME, Font.PLAIN, 12));
            g.setColor(new Color(180, 180, 180));
            drawCenteredString(g, "1★:클리어  2★:타임어택  3★:완벽승리", x, w, y + 140);

        } else {
            // 무한 모드 점수
            g.setFont(new Font(FONT_NAME, Font.BOLD, 24));
            g.setColor(Color.WHITE);
            drawCenteredString(g, "현재 점수: " + game.getScore(), x, w, y + 110);
        }
    }

    private void drawBottomSplitSection(Graphics2D g, int panelX, int startY, int panelW) {
        int centerX = panelX + panelW / 2;

        // [좌측 영역] 메뉴 옵션
        drawMenuOptions(g, panelX, startY, panelW / 2);

        // [우측 영역] 스탯 정보
        drawStats(g, centerX, startY);

        // 중앙 세로 구분선 (선택 사항)
        g.setColor(new Color(60, 60, 70));
        g.drawLine(centerX, startY, centerX, startY + 180);
    }

    private void drawMenuOptions(Graphics2D g, int areaX, int startY, int areaW) {
        g.setFont(new Font(FONT_NAME, Font.BOLD, 20));

        for (int i = 0; i < OPTIONS.length; i++) {
            String label = OPTIONS[i];

            // 영역 내 중앙 정렬
            int textWidth = g.getFontMetrics().stringWidth(label);
            int textX = areaX + (areaW - textWidth) / 2;
            int textY = startY + 40 + (i * 50); // 간격 넓힘

            if (i == selectionIndex) {
                g.setColor(Color.YELLOW);
                g.drawString(label, textX, textY);
                g.drawString(">", textX - 20, textY);
            } else {
                g.setColor(Color.WHITE);
                g.drawString(label, textX, textY);
            }
        }

        // 조작 도움말
        g.setFont(new Font(FONT_NAME, Font.PLAIN, 12));
        g.setColor(Color.GRAY);
        drawCenteredString(g, "↑/↓ 이동, Enter 선택", areaX, areaW, startY + 210);
    }

    private void drawStats(Graphics2D g, int startX, int startY) {
        ShipEntity player = game.getPlayerShip();
        if (player == null) return;

        PlayerSkills skills = player.getStats().getSkills();
        int indent = 30; // 우측 영역 내부 들여쓰기

        // 제목
        g.setFont(new Font(FONT_NAME, Font.BOLD, 18));
        g.setColor(new Color(100, 200, 255)); // 하늘색
        g.drawString("[ 스탯 정보 ]", startX + indent, startY + 30);

        // 내용
        g.setFont(new Font("Monospaced", Font.BOLD, 15));
        g.setColor(Color.LIGHT_GRAY);

        int lineGap = 35;
        int y = startY + 70;

        g.drawString(String.format("공격력 : Lv.%d", skills.atkLv), startX + indent, y);
        g.drawString(String.format("연사력 : Lv.%d", skills.rofLv), startX + indent, y + lineGap);
        g.drawString(String.format("대시   : Lv.%d", skills.dashLv), startX + indent, y + lineGap * 2);

        // 현재 체력 정보 추가 (선택)
        g.setColor(new Color(255, 100, 100));
        g.drawString(String.format("HP     : %d/%d",
                player.getStats().getCurrentHP(),
                player.getStats().getMaxHP()), startX + indent, y + lineGap * 3);
    }

    // [헬퍼] 텍스트 중앙 정렬
    private void drawCenteredString(Graphics2D g, String text, int rectX, int rectW, int y) {
        int textWidth = g.getFontMetrics().stringWidth(text);
        g.drawString(text, rectX + (rectW - textWidth) / 2, y);
    }

    public void handleKeyPress(int keyCode) {
        if (keyCode == KeyEvent.VK_UP) {
            selectionIndex = (selectionIndex - 1 + OPTIONS.length) % OPTIONS.length;
        } else if (keyCode == KeyEvent.VK_DOWN) {
            selectionIndex = (selectionIndex + 1) % OPTIONS.length;
        } else if (keyCode == KeyEvent.VK_ENTER) {
            executeSelection();
        } else if (keyCode == KeyEvent.VK_ESCAPE) {
            gamePlayScreen.setPaused(false);
        }
    }

    private void executeSelection() {
        switch (selectionIndex) {
            case 0: // 재개
                gamePlayScreen.setPaused(false);
                break;
            case 1: // 재시작
                gamePlayScreen.setPaused(false);
                game.restartLastMode();
                break;
            case 2: // 메인으로
                gamePlayScreen.setPaused(false);
                if (game.getScore() > 0) {
                    game.uploadScoreIfLoggedIn();
                }
                game.goToMenuScreen();
                break;

            default:
                //예상치 못한 인덱스가 들어왔을 때 안전하게 무시
                break;
        }
    }
}