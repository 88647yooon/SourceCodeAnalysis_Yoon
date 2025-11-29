package org.newdawn.spaceinvaders.entity.base;

import java.awt.Graphics;
import java.awt.Rectangle;

import org.newdawn.spaceinvaders.graphics.Sprite;
import org.newdawn.spaceinvaders.graphics.SpriteStore;

/**
 * 게임 내 모든 객체의 기본이 되는 추상 클래스.
 * 위치(x,y), 이동(dx,dy), 충돌, 그리기를 담당합니다.
 * * @author Kevin Glass
 */
public abstract class Entity {
    // 상속받은 자식들이 접근해야 하므로 protected 유지
    protected double x;
    protected double y;
    protected Sprite sprite;
    protected double dx;
    protected double dy;

    // 충돌 계산용 사각형 (매번 new 하지 않기 위해 재사용)
    private final Rectangle me = new Rectangle();
    private final Rectangle otherRect = new Rectangle();

    public Entity(String ref, int x, int y) {
        this.sprite = SpriteStore.get().getSprite(ref);
        this.x = x;
        this.y = y;
    }

    /** 시간(delta)에 따른 좌표 이동 */
    public void move(long delta) {
        x += (delta * dx) / 1000;
        y += (delta * dy) / 1000;
    }

    public void setHorizontalMovement(double dx) { this.dx = dx; }
    public void setVerticalMovement(double dy)   { this.dy = dy; }
    public double getHorizontalMovement() { return dx; }
    public double getVerticalMovement()   { return dy; }

    public void draw(Graphics g) {
        sprite.draw(g, (int) x, (int) y);
    }

    /** 추가 로직이 필요할 때 오버라이드 (Hook method) */
    public void doLogic() {}

    public int getX() { return (int) x; }
    public int getY() { return (int) y; }

    public int getWidth() { return sprite.getWidth(); }
    public int getHeight() { return sprite.getHeight(); }

    /** 충돌 여부 확인 */
    public boolean collidesWith(Entity other) {
        me.setBounds((int) x, (int) y, sprite.getWidth(), sprite.getHeight());
        otherRect.setBounds((int) other.x, (int) other.y, other.sprite.getWidth(), other.sprite.getHeight());

        return me.intersects(otherRect);
    }

    /** 강제 위치 이동 및 정지 */
    public void teleportTo(double x, double y) {
        this.x = x;
        this.y = y;
        this.dx = 0;
        this.dy = 0;
    }

    /** 충돌 발생 시 호출되는 추상 메소드 (자식이 구현) */
    public abstract void collidedWith(Entity other);
}