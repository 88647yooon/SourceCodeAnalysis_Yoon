package org.newdawn.spaceinvaders.entity;

import org.newdawn.spaceinvaders.Game;

/**
 * An entity representing a shot fired by the player's ship
 *
 * @author Kevin Glass
 */
public class ShotEntity extends Entity {
    /** The vertical speed at which the players shot moves */
    private final double moveSpeed = -300;
    /** The game in which this entity exists */
    private final Game game;
    /** True if this shot has been "used", i.e. its hit something */
    private boolean used = false;
    //기본 데미지
    private int damage = 1;

    /**
     * Create a new shot from the player
     *
     * @param game The game in which the shot has been created
     * @param sprite The sprite representing this shot
     * @param x The initial x location of the shot
     * @param y The initial y location of the shot
     */
    public ShotEntity(Game game,String sprite,int x,int y) {
        super(sprite,x,y);
        this.game = game;
        dy = moveSpeed;
    }

    /**
     * Request that this shot moved based on time elapsed
     *
     * @param delta The time that has elapsed since last move
     */
    public void move(long delta) {
        super.move(delta);
        if (y < -100) {
            game.removeEntity(this);
        }
    }

    @Override
    public boolean collidesWith(Entity other) {
        // 적 탄에 대해서만 축소 히트박스 사용
        if (other instanceof EnemyShotEntity) {
            final double scale = 0.45; // 크기 조절 가능
            int w = (int) (sprite.getWidth()  * scale);
            int h = (int) (sprite.getHeight() * scale);
            int sx = getX() + (sprite.getWidth()  - w) / 2;
            int sy = getY() + (sprite.getHeight() - h) / 2;

            java.awt.Rectangle me  = new java.awt.Rectangle(sx, sy, w, h);
            java.awt.Rectangle him = new java.awt.Rectangle(
                    other.getX(), other.getY(),
                    other.sprite.getWidth(), other.sprite.getHeight()
            );
            return me.intersects(him);
        }

        // 그 외(외계인 몸통 등)는 기존 전역 판정 유지
        return super.collidesWith(other);
    }


    /**
     * Notification that this shot has collided with another entity
     *
     * @param other The other entity with which we've collided
     */
    public void collidedWith(Entity other) {
        if (used) return;

        //충돌 대상이 AlienEntity인지 확인
        if (other instanceof AlienEntity) {


            // 탄은 소모
            used = true;
            game.removeEntity(this);

            //XP,점수, 외계인 제거 로직은 AlienEntity와 Game의 책임으로 이동
            AlienEntity alien = (AlienEntity) other;
            alien.wasHitBy(this);

            return;
        }
    }
    // 데미지 getter setter 추가
    public void setDamage(int damage) { this.damage = Math.max(1, damage); }
    public int getDamage() { return damage; }
}