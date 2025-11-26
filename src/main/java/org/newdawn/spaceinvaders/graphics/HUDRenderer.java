package org.newdawn.spaceinvaders.graphics;

import org.newdawn.spaceinvaders.Game;
import org.newdawn.spaceinvaders.entity.player.ShipEntity;

import java.awt.*;

public class HUDRenderer {
    private final Game game;

    public HUDRenderer(Game game){
        this.game = game;
    }

    public void render(Graphics2D g){
        ShipEntity player = game.getPlayerShip();
        if (player == null) return;

        int screenW = (game.getWidth() > 0) ? game.getWidth() : 800;
        int screenH = (game.getHeight() > 0) ? game.getHeight() : 600;

        // ===== 하트(HP) — 좌상단 유지 =====
        int x = 20, y = 20;
        int heartSize = 20;
        int heartGap  = 10;

        Image heart      = SpriteStore.get().getSprite("sprites/heart.png").getImage();
        Image emptyHeart = SpriteStore.get().getSprite("sprites/emptyheart.png").getImage();

        for (int i = 0; i < player.getStats().getMaxHP(); i++) {
            Image img = (i < player.getStats().getCurrentHP()) ? heart : emptyHeart;
            g.drawImage(img, x + i * (heartSize + heartGap), y, heartSize, heartSize, null);
        }

        g.setFont(new Font("Arial", Font.BOLD, 20));
        g.setColor(Color.WHITE);

        int rightMargin = 30; // 우측 여백

        if (game.isStageMode()) {
            // 1. 스테이지 모드: 타이머 표시 (기존 위치 y=40)
            int timeLeftMs = game.getStageTimeLimitMs();
            int seconds = timeLeftMs / 1000;
            int min = seconds / 60;
            int sec = seconds % 60;
            String timeText = String.format("%02d:%02d", min, sec);

            // 텍스트 너비에 맞춰 우측 정렬
            int timeW = g.getFontMetrics().stringWidth(timeText);
            g.drawString(timeText, screenW - timeW - rightMargin, 40);

            // 2. 스테이지 모드: 스테이지 명 표시 (타이머 아래 y=70)
            String stageText = "Stage " + game.getCurrentStageId();
            int stageW = g.getFontMetrics().stringWidth(stageText);
            g.drawString(stageText, screenW - stageW - rightMargin, 70);

        } else {
            // 3. 무한 모드: Wave 수 표시 (타이머 위치인 y=40 활용)
            String waveText = "Wave " + game.getWaveCount();
            int waveW = g.getFontMetrics().stringWidth(waveText);
            g.drawString(waveText, screenW - waveW - rightMargin, 40);
        }
        int marginX = 24;
        int marginY = 20;
        int barW = screenW - marginX * 2;
        int barH = 16;
        int barX = marginX;
        int barY = screenH - marginY - barH;

        drawXpBarSegmented(g, player, barX, barY, barW, barH, 10);

        // 레벨 텍스트
        g.setColor(Color.WHITE);
        g.drawString("Lv." + player.getStats().getLevel(), barX, barY - 6);


    }

    /** 초록 세그먼트 XP 바 (segments: 칸 수) — 하단 버전 */
    private void drawXpBarSegmented(Graphics2D g, ShipEntity p,
                                    int x, int y, int width, int height, int segments) {
        // 색상
        Color frameOuter = new Color(6, 24, 16);
        Color slotBg     = new Color(12, 28, 18, 200);
        Color filled     = new Color(58, 132, 84);
        Color empty      = new Color(26, 56, 36);
        Color gloss      = new Color(255, 255, 255, 35);

        // 프레임/바 바탕
        g.setColor(frameOuter);
        g.fillRoundRect(x - 2, y - 2, width + 4, height + 4, 8, 8);
        g.setColor(slotBg);
        g.fillRoundRect(x, y, width, height, 8, 8);

        // 세그먼트 계산
        final int gap = 3;
        int segW = (width - gap * (segments - 1)) / segments;

        int pct = p.getStats().getXpPercent();           // 0~100
        double unit = 100.0 / segments;
        int fullSegs = (int)Math.floor(pct / unit);
        double lastFrac = Math.max(0.0, (pct / unit) - fullSegs); // 0~1

        int sx = x;
        for (int i = 0; i < segments; i++) {
            boolean full = i < fullSegs;
            g.setColor(full ? filled : empty);
            g.fillRect(sx, y, segW, height);

            // 진행 중인 마지막 칸 부분 채움
            if (!full && i == fullSegs && lastFrac > 0) {
                g.setColor(filled);
                g.fillRect(sx, y, (int)Math.round(segW * lastFrac), height);
            }

            // 윗면 하이라이트
            g.setColor(gloss);
            g.fillRect(sx, y, segW, Math.max(1, height / 2));

            sx += segW + gap;
        }

        // XP 수치(바 바로 아래, 화면 아래쪽이라 가독성 좋음)
        g.setColor(Color.WHITE);
        String xpTxt = p.getStats().getXpIntoLevel() + " / " + p.getStats().getXpToNextLevel();
        g.drawString(xpTxt, x, y + height + 14);
    }
}