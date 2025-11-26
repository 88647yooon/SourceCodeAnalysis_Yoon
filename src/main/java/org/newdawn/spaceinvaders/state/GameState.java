package org.newdawn.spaceinvaders.state;

public interface GameState {
    // 상태 진입시 한번 호출
    void onEnter();
    // 상태 탈출시 한번 호출
    void onExit();
    // 매 프레임 호출
    void update(long delta);
}
