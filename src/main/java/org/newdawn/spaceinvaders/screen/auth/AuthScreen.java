package org.newdawn.spaceinvaders.screen.auth;

import org.newdawn.spaceinvaders.Game;
import org.newdawn.spaceinvaders.input.LoginCharTypedHandler;
import org.newdawn.spaceinvaders.input.LoginFlowCoordinator;
import org.newdawn.spaceinvaders.screen.Screen;

import java.awt.*;
import java.awt.event.KeyEvent;

public class AuthScreen implements Screen {
    private final Game game;
    private final LoginFlowCoordinator loginFlowCoordinator;
    private final AuthFormState form;



    public AuthScreen(Game game) {
        this.game = game;
        form = new AuthFormState();
        loginFlowCoordinator = new LoginFlowCoordinator(game, form);

    }

    @Override
    public void render(Graphics2D g) {
        final String dialog = "Dialog";

        g.setColor(Color.black);
        g.fillRect(0, 0, 800, 600);

        g.setColor(Color.white);
        g.setFont(new Font(dialog , Font.BOLD, 28));
        String title = form.isSignupMode() ? "회원가입" : "로그인";
        g.drawString(title, (800 - g.getFontMetrics().stringWidth(title)) / 2, 100);

        g.setFont(new Font(dialog, Font.PLAIN, 20));
        g.drawString("이메일: " + form.getEmail() + (form.getFieldIndex() == 0 ? "_" : ""), 200, 200);
        g.drawString("비밀번호: " + mask(form.getPassword()) + (form.getFieldIndex() == 1 ? "_" : ""), 200, 240);

        if (form.isSignupMode()) {
            g.drawString("비밀번호 확인: " + mask(form.getPassword()) + (form.getFieldIndex() == 2 ? "_" : ""), 200, 280);
        }

        g.setFont(new Font(dialog, Font.PLAIN, 16));
        g.drawString("[Enter] 확인 | [Tab] 로그인/회원가입 전환 | ↑↓ 이동 | [ESC] 종료", 120, 360);

        if (!form.getEmail().isEmpty()) {
            g.setColor(Color.yellow);
            g.drawString(form.getMessage(), 200, 420);
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
    public void handleKeyPress(int keyCode) {
        if (keyCode == KeyEvent.VK_TAB) {
            form.toggleMode();
            return;
        }
        if (keyCode == KeyEvent.VK_ENTER) {
            loginFlowCoordinator.tryAuth();
            return;
        }
        if (keyCode == KeyEvent.VK_ESCAPE) {
            System.exit(0);
            return;
        }
        if (keyCode == KeyEvent.VK_UP) {
            form.moveFieldUp();
            return;
        }
        if (keyCode == KeyEvent.VK_DOWN) {
            form.moveFieldDown();
        }
    }


    @Override
    public void handleKeyRelease(int keyCode) {}

    @Override
    public void update(long delta) {}


    public void handleCharTyped(char c) {
        LoginCharTypedHandler l = new LoginCharTypedHandler(form);
        l.CharTypedHandler(c);
    }
}
