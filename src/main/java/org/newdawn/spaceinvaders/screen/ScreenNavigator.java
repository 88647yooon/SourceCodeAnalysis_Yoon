package org.newdawn.spaceinvaders.screen;

import org.newdawn.spaceinvaders.Game;
import org.newdawn.spaceinvaders.manager.SoundManager;

import java.awt.*;

public class ScreenNavigator {
    private final Game game;
    private Screen currentScreen;

    public ScreenNavigator(Game game) {
        this.game = game;
    }

    public Screen getCurrentScreen() {
        return currentScreen;
    }

    public void setScreen(Screen screen) {
        this.currentScreen = screen;
        updateBGMForContext();
    }

    public void showMenu() {
        setScreen(new MenuScreen(game));
    }

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


    private void updateBGMForContext() {
        SoundManager sm = SoundManager.getSound();

        if (currentScreen instanceof MenuScreen) {
            sm.play(SoundManager.Bgm.MENU);
            return;
        }

        if (currentScreen instanceof GamePlayScreen) {
            // stage 5 보스 여부는 game에서 물어볼 수도 있음
            if (game.isStageMode() && game.getCurrentStageId() == 5) {
                sm.play(SoundManager.Bgm.BOSS);
            } else {
                sm.play(SoundManager.Bgm.STAGE);
            }
            return;
        }

        sm.play(SoundManager.Bgm.MENU);
    }
}
