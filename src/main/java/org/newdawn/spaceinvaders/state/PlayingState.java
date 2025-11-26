package org.newdawn.spaceinvaders.state;

import org.newdawn.spaceinvaders.Game;
import org.newdawn.spaceinvaders.manager.SoundManager;
import org.newdawn.spaceinvaders.screen.GamePlayScreen;
import org.newdawn.spaceinvaders.screen.ScreenNavigator;

public class PlayingState implements GameState{
    private final Game game;
    public PlayingState(Game game) {
        this.game = game;
    }

    @Override
    public void onEnter() {
        ScreenNavigator navigator = game.getScreenNavigator();
        navigator.showGamePlay();

        if (game.isStageMode() && game.getCurrentStageId() == 5) {
            SoundManager.getSound().play(SoundManager.Bgm.BOSS);
        } else {
            SoundManager.getSound().play(SoundManager.Bgm.STAGE);
        }

        game.setWaitingForKeyPress(false);
        game.setMessage("");
    }

    @Override
    public void onExit() {
        SoundManager.getSound().stop();
        game.setMessage("");
    }

    @Override
    public void update(long delta) {
        if (game.getCurrentScreen() instanceof GamePlayScreen) {
            GamePlayScreen screen = (GamePlayScreen) game.getCurrentScreen();

            // 일시정지(ESC) 상태이거나 레벨업 팝업이 떠있으면 게임 로직(이동/충돌) 정지
            if (screen.isPaused() || screen.getLevelUpActive()) {
                return;
            }
        }
        // 한 프레임 게임 진행
        game.stepGamePlay(delta);
    }
}
