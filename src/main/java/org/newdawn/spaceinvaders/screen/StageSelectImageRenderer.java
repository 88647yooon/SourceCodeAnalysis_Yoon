package org.newdawn.spaceinvaders.screen;

import org.newdawn.spaceinvaders.Game;
import java.util.logging.Logger;

import javax.swing.*;
import java.awt.*;

public class StageSelectImageRenderer {
    private Game game;
    private StageSelectScreen stageSelectScreen;
    private Logger logger = Logger.getLogger(StageSelectImageRenderer.class.getName());

    //별 이미지
    private final Image starFilled = loadImage("/sprites/star_filled.png");
    private final Image starEmpty  = loadImage("/sprites/star_empty.png");

    // 애니메이션용 오프셋
    private int selectedSize = 200;
    public StageSelectImageRenderer(Game game, StageSelectScreen screen) {
        this.game = game;
        this.stageSelectScreen = screen;
    }

    public void renderSingleStage(Graphics2D g, int stageIndex, int centerXForStage, int centerYForStage, int globalCenterX) {

        boolean selected = (Math.round(stageSelectScreen.getCurrentIndex()) == stageIndex);
        int size = selected ? selectedSize : 120;

        int drawX = centerXForStage - size / 2;
        int drawY = centerYForStage - size / 2;
        boolean unlocked = game.isStageUnlocked(stageIndex);

        // 1) 스테이지 이미지 또는 기본 원
        renderStageImageOrFallback(g, stageIndex, drawX, drawY, size);

        // 2) 선택된 스테이지 하이라이트/텍스트
        if (selected) {
            renderSelectedStageHighlightAndText(
                    g, stageIndex, drawX, drawY, size,
                    globalCenterX, centerYForStage, unlocked
            );
        }

        // 3) 별(★) 표시
        renderStageStars(g, stageIndex, centerXForStage, drawY, size);
    }

    private void renderStageImageOrFallback(Graphics2D g,
                                            int stageIndex,
                                            int drawX,
                                            int drawY,
                                            int size) {
        if (stageSelectScreen.stageImages[stageIndex] != null) {
            g.drawImage(stageSelectScreen.stageImages[stageIndex], drawX, drawY, size, size, null);
        } else {
            g.setColor(Color.RED);
            g.fillOval(drawX, drawY, size, size);
        }
    }

    private void renderStageStars(Graphics2D g, int stageIndex, int centerXForStage, int drawY, int size) {

        int stars = game.getStageStars(stageIndex + 1);
        int starSize = 30;
        int starSpacing = 36;
        int starY = drawY + size - 15;
        int starX = centerXForStage - (starSpacing * 3) / 2;

        for (int s = 0; s < 3; s++) {
            Image starImg = (s < stars) ? starFilled : starEmpty;
            if (starImg != null) {
                g.drawImage(starImg,
                        starX + s * starSpacing,
                        starY, starSize, starSize, null);
            }
        }
    }

    private void renderSelectedStageHighlightAndText(Graphics2D g, int stageIndex, int drawX, int drawY, int size,
                                                     int globalCenterX, int centerY, boolean unlocked) {
        // 테두리
        g.setStroke(new BasicStroke(4));
        g.setColor(Color.BLACK);
        g.drawOval(drawX - 4, drawY - 4, size + 8, size + 8);

        // "Stage N"
        g.setFont(new Font("Arial", Font.BOLD, 24));
        g.setColor(Color.WHITE);
        g.drawString("Stage " + (stageIndex + 1),
                globalCenterX - 40,
                centerY + selectedSize / 2 + 40);

        // 잠금 표시
        if (!unlocked) {
            g.setFont(new Font("Arial", Font.BOLD, 20));
            g.setColor(Color.RED);
            g.drawString("Locked",
                    globalCenterX - 40,
                    centerY + selectedSize / 2 + 70);
        }
    }


    /** 공통 이미지 로드 헬퍼 */
    private Image loadImage(String path) {
        java.net.URL url = getClass().getResource(path);
        if (url == null) {
            logger.warning("Image loading failed: ");
            return null;
        }
        return new ImageIcon(url).getImage();
    }
}
