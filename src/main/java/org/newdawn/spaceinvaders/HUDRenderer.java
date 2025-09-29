package org.newdawn.spaceinvaders;

import org.newdawn.spaceinvaders.entity.ShipEntity;
import java.awt.*;

public class HUDRenderer {
    private final Game game;

    public HUDRenderer(Game game){
        this.game = game;
    }

    public void render(Graphics2D g){
        ShipEntity player = game.getPlayerShip();
        if (player == null) return;

        // ===== 하트(HP) — 좌상단 유지 =====
        int x = 20, y = 20;
        int heartSize = 20;
        int heartGap  = 10;

        Image heart      = SpriteStore.get().getSprite("sprites/heart.png").getImage();
        Image emptyHeart = SpriteStore.get().getSprite("sprites/emptyheart.png").getImage();

        for (int i = 0; i < player.getMaxHP(); i++) {
            Image img = (i < player.getCurrentHP()) ? heart : emptyHeart;
            g.drawImage(img, x + i * (heartSize + heartGap), y, heartSize, heartSize, null);
        }

        //남은 시간(스테이지 모드일때만 표시)
        int timeLeftMs = game.getStageTimeLimitMs();
        if(timeLeftMs > 0 || game.isStageMode()){
            int seconds = timeLeftMs / 1000;
            int min = seconds / 60;
            int sec = seconds % 60;

            String timeText = String.format("%02d:%02d", min, sec);
            g.setFont(new Font("Arial", Font.BOLD, 20));
            g.setColor(Color.WHITE);
            g.drawString(timeText, game.getWidth() - 100 ,40);
        }


        // ===== XP 세그먼트 바 — 화면 하단 전체 폭으로 =====
        int screenW = (game.getWidth()  > 0) ? game.getWidth()  : 800;
        int screenH = (game.getHeight() > 0) ? game.getHeight() : 600;

        int marginX = 24;
        int marginY = 20;
        int barW = screenW - marginX * 2;
        int barH = 16;
        int barX = marginX;
        int barY = screenH - marginY - barH;

        drawXpBarSegmented(g, player, barX, barY, barW, barH, 10);

        // 레벨 텍스트(바 위에 살짝)
        g.setColor(Color.WHITE);
        g.drawString("Lv." + player.getLevel(), barX, barY - 6);


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

        int pct = p.getXpPercent();           // 0~100
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
        String xpTxt = p.getXpIntoLevel() + " / " + p.getXpToNextLevel();
        g.drawString(xpTxt, x, y + height + 14);
    }
}