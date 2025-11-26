package org.newdawn.spaceinvaders.screen;

import org.newdawn.spaceinvaders.Game;

import java.awt.*;
//ScreenNavigator는 화면 라우터만 하는거로 변경
public class ScreenNavigator {
    private final Game game;
    private Screen currentScreen;

    public ScreenNavigator(Game game) {
        this.game = game;
    }

    public Screen getCurrentScreen() {
        return currentScreen;
    }

    public void setScreen(Screen screen) { this.currentScreen = screen; }

    // 화면 전환 메서들
    public void showMenu() { setScreen(new MenuScreen(game)); }

    public void showStageSelect() {
        setScreen(new StageSelectScreen(game));
    }

    public void showGamePlay() {
        setScreen(new GamePlayScreen(game));
    }

    public void showScoreboard() {
        setScreen(new ScoreboardScreen(game));
    }

    public void showGameOver() {
        setScreen(new GameOverScreen(game));
    }

    //GameLoop에서 호출하는 래퍼들

    public void update(long delta) {
        if (currentScreen != null) {
            currentScreen.update(delta);
        }
    }

    public void render(Graphics2D g) {
        if (currentScreen != null) {
            currentScreen.render(g);
        }
    }

    public void handleKeyPress(int keyCode) {
        if(currentScreen != null) {
            currentScreen.handleKeyPress(keyCode);
        }
    }

    public void handleKeyRelease(int keyCode){
        if(currentScreen != null) {
            currentScreen.handleKeyPress(keyCode);
        }
    }
}
