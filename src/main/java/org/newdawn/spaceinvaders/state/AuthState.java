package org.newdawn.spaceinvaders.state;

import org.newdawn.spaceinvaders.Game;
import org.newdawn.spaceinvaders.manager.SoundManager;
import org.newdawn.spaceinvaders.screen.auth.AuthScreen;

public class AuthState implements GameState{
    private final Game game;
    public AuthState(Game game){
        this.game = game;
    }

    @Override
    public void onEnter(){
        game.setScreen(new AuthScreen(game));
        SoundManager.getSound().play(SoundManager.Bgm.MENU);
    }

    @Override
    public void onExit(){
        //필요시 정리 로직
    }
    @Override
    public void update(long delta){
        game.stepIdle(delta);
    }
}
