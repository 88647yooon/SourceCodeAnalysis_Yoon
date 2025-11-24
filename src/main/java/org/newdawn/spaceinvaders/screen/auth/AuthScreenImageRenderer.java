package org.newdawn.spaceinvaders.screen.auth;

import java.awt.*;
import java.util.logging.Logger;

public class AuthScreenImageRenderer {
    private final Logger logger = Logger.getLogger(AuthScreenImageRenderer.class.getName());
    private final AuthFormState form;


    public AuthScreenImageRenderer(AuthFormState form) {
        this.form = form;
    }
    public void drawBackground(Graphics2D g) {
        int w = 800;
        int h = 600;
        Image backgroundImage;backgroundImage = loadImage();
        g.drawImage(backgroundImage, 0, 0, w, h, null);
    }


    public void drawAuthCard(Graphics2D g) {
        final String DIALOG = "Dialog";
        int cardX = 90;
        int cardY = 80;
        int cardW = 360;
        int cardH = 380;

        // 카드 배경
        g.setColor(new Color(255, 255, 255, 240));
        g.fillRoundRect(cardX, cardY, cardW, cardH, 18, 18);

        // 테두리
        g.setColor(new Color(0, 0, 0, 30));
        g.drawRoundRect(cardX, cardY, cardW, cardH, 18, 18);

        // 안쪽 패딩
        int baseX = cardX + 32;

        // 제목
        g.setColor(Color.black);
        g.setFont(new Font(DIALOG, Font.BOLD, 26));
        String title = form.isSignupMode() ? "회원가입" : "로그인";
        g.drawString(title, baseX, cardY + 60);

        // 필드들
        g.setFont(new Font(DIALOG, Font.PLAIN, 18));
        int y = cardY + 120;
        g.drawString("이메일: " + form.getEmail() +
                (form.getFieldIndex() == 0 ? "_" : ""), baseX, y);

        y += 40;
        g.drawString("비밀번호: " + mask(form.getPassword()) +
                (form.getFieldIndex() == 1 ? "_" : ""), baseX, y);

        if (form.isSignupMode()) {
            y += 40;
            g.drawString("비밀번호 확인: " + mask(form.getPassword2()) +
                    (form.getFieldIndex() == 2 ? "_" : ""), baseX, y);
        }

        // 도움말
        g.setFont(new Font(DIALOG, Font.PLAIN, 14));
        g.setColor(new Color(80, 80, 80));
        g.drawString("[Enter] 확인 | [Tab] 로그인/회원가입 전환 | ↑↓ 이동 | [ESC] 종료",
                cardX + 16, cardY + cardH - 40);

        // 메시지
        if (!form.getMessage().isEmpty()) {
            g.setColor(new Color(255, 160, 0));
            g.drawString(form.getMessage(), baseX, cardY + cardH - 70);
        }
    }

    private Image loadImage() {
        try {
            java.net.URL url = getClass().getResource("/sprites/AuthScreenImage.png");
            if (url == null) {
                logger.warning("배경 이미지 로드 실패");
                return null;
            }
            return new javax.swing.ImageIcon(url).getImage();
        } catch (Exception e) {
            return null;
        }
    }

    private String mask(String pw) {
        if (pw == null) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < pw.length(); i++) {
            sb.append('*');
        }
        return sb.toString();
    }
}
