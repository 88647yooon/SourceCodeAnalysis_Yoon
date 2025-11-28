package org.newdawn.spaceinvaders.input;

import org.newdawn.spaceinvaders.Game;
import org.newdawn.spaceinvaders.screen.Screen;
import org.newdawn.spaceinvaders.screen.auth.AuthScreen;

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

/*
    게임에서 키 입력 처리를 담당하는 클래스
 */
public class GameKeyInputHandler extends KeyAdapter {
    private final Game game;
    public GameKeyInputHandler(Game game) {
        this.game = game;
    }

    @Override
    public void keyPressed(KeyEvent e) {
        Screen screen = game.getCurrentScreen();
        if (screen != null) {
            screen.handleKeyPress(e.getKeyCode());
        }
        if (game.isWaitingForKeyPress()) {
            game.setWaitingForKeyPress(false);
            game.goToStageSelectScreen();
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        Screen screen = game.getCurrentScreen();
        if (screen != null) {
            screen.handleKeyRelease(e.getKeyCode());
        }
    }

    @Override
    public void keyTyped(KeyEvent e) {
        if (game.getCurrentScreen() instanceof AuthScreen) {
            ((AuthScreen) game.getCurrentScreen()).handleCharTyped(e.getKeyChar());
        }
    }
}
