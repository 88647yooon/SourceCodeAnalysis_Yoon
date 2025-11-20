package org.newdawn.spaceinvaders;

import org.newdawn.spaceinvaders.Screen.Screen;

import java.awt.*;
import java.awt.event.KeyEvent;

public class AuthScreen implements Screen {
    private final Game game;
    private AuthController authController;

    private boolean signupMode = false; // false=로그인, true=회원가입
    private String email = "";
    private String password = "";
    private String password2 = ""; // 회원가입용
    private String message = "";
    private int fieldIndex = 0; // 0=email, 1=password, 2=password2 (회원가입 모드만)
    public AuthScreen(Game game) {
        this.game = game;
        authController = new AuthController(game, this);
    }
    public boolean getSignupMode() {
        return signupMode;
    }
    public String getEmail(){
        return email;
    }

    public String getPassword() {
        return password;
    }

    public String getPassword2() {
        return password2;
    }
    public void setMessage(String message) {
        this.message = message;
    }

    @Override
    public void render(Graphics2D g) {
        final String dialog = "Dialog";
        g.setColor(Color.black);
        g.fillRect(0, 0, 800, 600);

        g.setColor(Color.white);
        g.setFont(new Font(dialog , Font.BOLD, 28));
        String title = signupMode ? "회원가입" : "로그인";
        g.drawString(title, (800 - g.getFontMetrics().stringWidth(title)) / 2, 100);

        g.setFont(new Font(dialog, Font.PLAIN, 20));
        g.drawString("이메일: " + email + (fieldIndex == 0 ? "_" : ""), 200, 200);
        g.drawString("비밀번호: " + mask(password) + (fieldIndex == 1 ? "_" : ""), 200, 240);

        if (signupMode) {
            g.drawString("비밀번호 확인: " + mask(password2) + (fieldIndex == 2 ? "_" : ""), 200, 280);
        }

        g.setFont(new Font(dialog, Font.PLAIN, 16));
        g.drawString("[Enter] 확인 | [Tab] 로그인/회원가입 전환 | ↑↓ 이동 | [ESC] 종료", 120, 360);

        if (!message.isEmpty()) {
            g.setColor(Color.yellow);
            g.drawString(message, 200, 420);
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

    @Override
    public void update(long delta) {}

    @Override
    public void handleKeyPress(int keyCode) {
        if (keyCode == KeyEvent.VK_TAB) {
            signupMode = !signupMode;
            message = signupMode ? "회원가입 모드" : "로그인 모드";
            fieldIndex = 0;
            return;
        }
        if (keyCode == KeyEvent.VK_ENTER) {
            authController.tryAuth();
            return;
        }
        if (keyCode == KeyEvent.VK_ESCAPE) {
            System.exit(0);
            return;
        }
        if (keyCode == KeyEvent.VK_UP) {
            fieldIndex = Math.max(0, fieldIndex - 1);
            return;
        }
        if (keyCode == KeyEvent.VK_DOWN) {
            int max = signupMode ? 2 : 1;
            fieldIndex = Math.min(max, fieldIndex + 1);
        }
    }

    @Override
    public void handleKeyRelease(int keyCode) {}



    // 키보드 문자 입력 처리 (Game.KeyInputHandler에서 호출해줘야 함)
    public void handleCharTyped(char c) {
        if (c == '\b') { // 백스페이스
            if (fieldIndex == 0 && email.length() > 0) email = email.substring(0, email.length() - 1);
            if (fieldIndex == 1 && password.length() > 0) password = password.substring(0, password.length() - 1);
            if (fieldIndex == 2 && password2.length() > 0) password2 = password2.substring(0, password2.length() - 1);
            return;
        }
        if (Character.isISOControl(c)) return;

        if (fieldIndex == 0) email += c;
        if (fieldIndex == 1) password += c;
        if (fieldIndex == 2) password2 += c;
    }
}
