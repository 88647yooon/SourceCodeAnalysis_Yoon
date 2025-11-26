package org.newdawn.spaceinvaders.state;

import org.newdawn.spaceinvaders.Game;
import org.newdawn.spaceinvaders.screen.ScreenNavigator;

public class StageSelectState implements GameState{
    private final Game game;

    public StageSelectState(Game game) {
        this.game = game;
    }
    @Override
    public void onEnter(){
        ScreenNavigator navigator = game.getScreenNavigator();
        navigator.showStageSelect();

        game.setWaitingForKeyPress(false);
        game.setMessage("");
    }
    @Override
    public void onExit(){
        //필요 시 정리 로직
    }

    @Override
    public void update(long delta){
        game.stepIdle(delta);
    }
}
