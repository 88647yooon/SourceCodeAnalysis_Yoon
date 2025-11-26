package org.newdawn.spaceinvaders.entity.projectile;

import java.awt.Graphics;
import org.newdawn.spaceinvaders.Game;
import org.newdawn.spaceinvaders.graphics.Sprite;
import org.newdawn.spaceinvaders.graphics.SpriteStore;
import org.newdawn.spaceinvaders.entity.base.Entity;
import org.newdawn.spaceinvaders.entity.player.ShipEntity;
import org.newdawn.spaceinvaders.entity.projectile.movement.*;

public class EnemyShotEntity extends Entity {
    private final Game game;
    private long ageMs = 0;
    private long lifeTimeMs = 15000; // 15초 후 제거

    //모든 이동 관련 변수
    private ProjectileMovementStrategy movementStrategy;



    public EnemyShotEntity(Game game, String spriteRef, double x, double y, double dx, double dy) {
        super(spriteRef, (int)Math.round(x), (int)Math.round(y));
        this.game = game;

        // 초기 속도 설정
        setHorizontalMovement(dx);
        setVerticalMovement(dy);

        // 기본 전략: 직선 이동
        this.movementStrategy = new LinearMovement();
    }

    //외부에서 전략을 교체할 수 있는 메서드(보스 패턴 등에서 사용)
    public void setMovementStrategy(ProjectileMovementStrategy strategy) {
        this.movementStrategy = strategy;
    }

    public long getAge() {
        return ageMs;
    }

    @Override
    public void move(long delta) {
        ageMs += delta;

        // 1. 화면 밖/수명 체크 (기존 로직 유지)
        if (checkExpiredOrOutOfBounds()) {
            return;
        }

        // 2. [전략 패턴 적용] 현재 전략에 따라 움직임 계산 위임
        if (movementStrategy != null) {
            movementStrategy.updateMovement(this, delta);
        }

        // 3. 실제 물리 이동 (Entity 부모 클래스의 기능 사용)
        super.move(delta);
    }

    private boolean checkExpiredOrOutOfBounds() {
        boolean expired = ageMs > lifeTimeMs;
        // 여유분을 두고 화면 밖 체크
        boolean outOfBounds = y < -64 || y > game.getHeight() + 64 || x < -64 || x > game.getWidth() + 64;

        if (expired || outOfBounds) {
            game.removeEntity(this);
            return true;
        }
        return false;
    }

    @Override
    public void collidedWith(Entity other) {
        // 플레이어 충돌 처리 (기존 로직 유지)
        if (other instanceof ShipEntity) {
            ShipEntity ship = (ShipEntity) other;
            if (ship.isInvulnerable()) {
                game.removeEntity(this);
                return;
            }
            ship.damage(1);
            game.removeEntity(this);
        }
    }
}