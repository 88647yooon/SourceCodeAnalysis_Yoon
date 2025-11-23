package org.newdawn.spaceinvaders.input;

    // 키 입력 상태만 보관하는 VO 클래스
public class InputState {

    private boolean left;
    private boolean right;
    private boolean up;
    private boolean down;
    private boolean fire;

    public boolean isLeft()   { return left; }
    public boolean isRight()  { return right; }
    public boolean isUp()     { return up; }
    public boolean isDown()   { return down; }
    public boolean isFire()   { return fire; }

    public void setLeft(boolean left)     { this.left = left; }
    public void setRight(boolean right)   { this.right = right; }
    public void setUp(boolean up)         { this.up = up; }
    public void setDown(boolean down)     { this.down = down; }
    public void setFire(boolean fire)     { this.fire = fire; }

    public void reset() {
        left = right = up = down = fire = false;
    }

}
