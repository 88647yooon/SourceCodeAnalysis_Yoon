package org.newdawn.spaceinvaders;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;

public class StageSelectScreen implements Screen{
    private Game game;
    private int SelectIndex = 0;
    private final int MaxStage = 5;

    //스테이지 이미지
    private final Image[] stageImages = new Image[MaxStage];

    // 애니메이션용 오프셋
    private int BaseCardWidth = 180;
    private int BaseCardHeight = 260;
    private int spacing = 40;

    private float currentOffsetX = 0;  // 현재 오프셋
    private float targetOffsetX = 0;   // 목표 오프셋
    private float slideSpeed = 0.2f; //슬라이드 이동 속도

    public StageSelectScreen(Game game) {
        this.game = game;

        // 스테이지 이미지 로드 (없으면 기본 사각형)
        for (int i = 0; i < MaxStage; i++) {
            String path = "/sprites/stage" + (i + 1) + ".png";
            java.net.URL url = getClass().getResource(path);
            if (url != null) {
                stageImages[i] = new ImageIcon(url).getImage();
                System.out.println("✅ 로드 성공: " + path);
            } else {
                System.out.println("⚠️ 로드 실패: " + path);
                stageImages[i] = null;
            }
        }
    }

    @Override
    public void render(Graphics2D g) {
        g.setColor(Color.black);
        g.fillRect(0, 0, 800, 600);

        // 부드러운 이동
        currentOffsetX += (targetOffsetX - currentOffsetX) * slideSpeed;

        int iconSize = 150;
        int spacing = 50;

        // 전체 카드 폭 계산
        int totalWidth = MaxStage * (iconSize + spacing) - spacing;

        // 화면 중앙 정렬 좌표
        int startX = (800 - totalWidth) / 2;
        int y = (600 - iconSize) / 2;  // 세로도 중앙

        for (int i = 0; i < MaxStage; i++) {
            int x = startX + i * (iconSize + spacing);

            if (stageImages[i] != null) {
                g.drawImage(stageImages[i], x, y, iconSize, iconSize, null);
            } else {
                g.setColor(Color.RED);
                g.fillOval(x, y, iconSize, iconSize); // fallback
            }

            // 선택된 아이콘 강조 (노란 원 테두리)
            if (i == SelectIndex) {
                g.setColor(Color.YELLOW);
                g.setStroke(new BasicStroke(4));
                g.drawOval(x - 2, y - 2, iconSize + 4, iconSize + 4);

                g.setFont(new Font("Arial", Font.BOLD, 20));
                g.setColor(Color.WHITE);
                g.drawString("Stage " + (i + 1),
                        x + iconSize / 2 - 30,
                        y + iconSize + 30);
            }
        }

        // 안내 텍스트
        g.setFont(new Font("Dialog", Font.BOLD, 18));
        g.setColor(Color.LIGHT_GRAY);
        g.drawString("← → 키로 선택, ENTER로 확정, ESC로 메뉴로 돌아가기", 160, 550);
    }

    @Override
    public void update(long delta) {

    }

    @Override
    public void handleKeyPress(int keyCode) {
        if (keyCode == KeyEvent.VK_LEFT) {
            if (SelectIndex > 0) {
                SelectIndex--;
                targetOffsetX += (BaseCardWidth + spacing);
            }
        }
        if (keyCode == KeyEvent.VK_RIGHT) {
            if (SelectIndex < MaxStage - 1) {
                SelectIndex++;
                targetOffsetX -= (BaseCardWidth + spacing);
            }
        }
        if (keyCode == KeyEvent.VK_ENTER) {
            int stage = SelectIndex + 1;
            game.startStageMode(stage);
        }
        if (keyCode == KeyEvent.VK_ESCAPE) {
            game.setScreen(new MenuScreen(game));
        }
    }

    @Override
    public void handleKeyRelease(int keyCode) {}


}
