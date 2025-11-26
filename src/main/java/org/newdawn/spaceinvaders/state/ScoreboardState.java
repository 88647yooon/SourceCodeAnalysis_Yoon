package org.newdawn.spaceinvaders.state;

import org.newdawn.spaceinvaders.Game;

public class ScoreboardState implements GameState{
    private final Game game;
    public ScoreboardState(Game game) {
        this.game = game;
    }

    @Override
    public void onEnter(){
        game.getScreenNavigator().showScoreboard();
    }
    @Override
    public void onExit(){
        //필요시 정리
    }

    @Override
    public void update(long delta){
        game.stepIdle(delta);
    }
}
