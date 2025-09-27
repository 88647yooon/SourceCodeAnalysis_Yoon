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

    /**
     * Notification that this shot has collided with another entity
     *
     * @param other The other entity with which we've collided
     */
    public void collidedWith(Entity other) {
        if (used) return;

        if (other instanceof AlienEntity) {
            AlienEntity alien = (AlienEntity) other;

            // 탄은 소모
            used = true;
            game.removeEntity(this);

            // 데미지 1 주고 사망 여부 판정
            boolean dead = alien.takeDamage(1);
            if (dead) {
                // XP는 '실제로 내가 제거하는' 쪽에서만 1번 지급
                if (game.getEntities().contains(other)) {
                    // 종류별 XP
                    int xp = 5; // 기본: 일반
                    if (other instanceof RangedAlienEntity) {
                        xp = 8;
                    } else if (other instanceof DiagonalShooterAlienEntity) {
                        xp = 10;
                    }

                    // 플레이어에게 XP 지급
                    ShipEntity player = game.getPlayerShip();
                    if (player != null) {
                        player.addXp(xp);
                    }

                    // 제거 + 카운트
                    game.removeEntity(other);
                    game.notifyAlienKilled();
                }
            }
            return;
        }
    }
}