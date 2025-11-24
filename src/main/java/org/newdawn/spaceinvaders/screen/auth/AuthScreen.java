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
    private final AuthScreenImageRenderer renderer;


    public AuthScreen(Game game) {
        form = new AuthFormState();
        loginFlowCoordinator = new LoginFlowCoordinator(game, form);
        renderer = new AuthScreenImageRenderer(form);
        this.game = game;

    }

    @Override
    public void render(Graphics2D g) {

        // 안티앨리어싱
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);

        // 1) 배경 먼저
        renderer.drawBackground(g);

        // 2) 로그인 카드 그리기
        renderer.drawAuthCard(g);
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
            game.requestExit();
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
    public void handleKeyRelease(int keyCode) {
        //여기서는 필요 없음
    }

    @Override
    public void update(long delta) {
        //여기서는 필요 없음
    }


    public void handleCharTyped(char c) {
        LoginCharTypedHandler l = new LoginCharTypedHandler(form);
        l.CharTypedHandler(c);
    }
}
