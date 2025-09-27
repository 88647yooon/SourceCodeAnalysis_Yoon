package org.newdawn.spaceinvaders;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.InputStream;

class BackgroundRenderer {
    private BufferedImage[] backgrounds;

    public BackgroundRenderer() {
        // 리소스 경로는 클래스패스 기준. 실제 파일은 src/main/resources/sprites/ 아래에 있어야 함.
        backgrounds = new BufferedImage[] {
                load("/sprites/BackGround1.png"),
                load("/sprites/BackGround2.png"),
                load("/sprites/BackGround3.png")
        };

        // 하나도 못 읽었을 때는 null 배열이 되지 않게 유지(렌더에서 폴백 처리)
        boolean any = false;
        for (BufferedImage bg : backgrounds) if (bg != null) { any = true; break; }
        if (!any) {
            System.err.println("⚠️ Background images not found on classpath. Using gradient fallback.");
        }
    }

    /** 클래스패스에서 안전하게 로드 (null 허용) */
    private BufferedImage load(String path) {
        try (InputStream is = getClass().getResourceAsStream(path)) {
            if (is == null) {
                System.err.println("⚠️ Missing resource: " + path);
                return null;
            }
            return ImageIO.read(is);
        } catch (Exception e) {
            System.err.println("⚠️ Failed to load " + path + " : " + e.getMessage());
            return null;
        }
    }

    public void render(Graphics2D g, int waveCount, int width, int height) {
        // 1) 배경 이미지가 있으면 그리기
        BufferedImage bg = null;
        if (backgrounds != null && backgrounds.length > 0) {
            int idx = Math.floorMod((waveCount / 10), backgrounds.length);
            bg = backgrounds[idx];
        }

        if (bg != null) {
            g.drawImage(bg, 0, 0, width, height, null);
            return;
        }

        // 2) 폴백: 그라디언트로 채우기 (이미지 못 찾았을 때도 게임이 계속 되도록)
        Paint old = g.getPaint();
        g.setPaint(new GradientPaint(0, 0, new Color(12, 16, 28),
                0, height, new Color(8, 40, 84)));
        g.fillRect(0, 0, width, height);
        g.setPaint(old);
    }

    // 필요 시 화면 흔들기 효과
    @SuppressWarnings("unused")
    private static void applyShake(Graphics2D g) {
        int offsetX = (int) (Math.random() * 10 - 5);
        int offsetY = (int) (Math.random() * 10 - 5);
        g.translate(offsetX, offsetY);
    }
}