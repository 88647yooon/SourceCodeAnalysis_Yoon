package org.newdawn.spaceinvaders;

import java.awt.*;

public interface Screen {
    //화면 그리기
    void render(Graphics2D g);

    //프레임 단위 갱신
    void update(long delta);
    //키 입력 처리
    void handleKeyPress(int keyCode);

    void handleKeyRelease(int keyCode);
}
