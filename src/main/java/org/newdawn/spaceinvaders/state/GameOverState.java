package org.newdawn.spaceinvaders.state;

import org.newdawn.spaceinvaders.Game;
import org.newdawn.spaceinvaders.manager.SoundManager;
import org.newdawn.spaceinvaders.screen.ScreenNavigator;
/**
 * 메인 메뉴 상태
 *  - 메뉴 화면을 표시하고, 메뉴용 BGM을 재생함.
 *  - 실제 메뉴 선택/키 처리 로직은 MenuScreen 이 담당하고,
 *    이 State는 "지금은 메뉴 단계"라는 것만 표현함.
 */
public class GameOverState implements GameState {
    private final Game game;

    public GameOverState(Game game) {
        this.game = game;
    }

    @Override
    public void onEnter() {
        ScreenNavigator navigator = game.getScreenNavigator();
        navigator.showGameOver();

        SoundManager.getSound().play(SoundManager.Bgm.MENU);

    }

    @Override
    public void onExit() {
        SoundManager.getSound().stop();
    }

    @Override
    public void update(long delta) {
        game.stepIdle(delta);
    }

}
