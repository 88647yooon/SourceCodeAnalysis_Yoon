package org.newdawn.spaceinvaders.entity.projectile;

import java.awt.Graphics;
import org.newdawn.spaceinvaders.Game;
import org.newdawn.spaceinvaders.Sprite;
import org.newdawn.spaceinvaders.SpriteStore;
import org.newdawn.spaceinvaders.entity.base.Entity;
import org.newdawn.spaceinvaders.entity.player.ShipEntity;

public class EnemyShotEntity extends Entity {
    private final Game game;
    private final double speed;      // 기본 속도
    private double friction = 0.0;   // 매 프레임 감속 (선택)

    // 수명 관리
    private long ageMs = 0;
    private long lifeTimeMs = 15000; // 15초 후 제거

    // 호밍 관련
    private boolean homing = false;
    private long homingDelayMs = 0;
    private double homingAccel = 0;
    private double maxHomingSpeed = 280;
    private ShipEntity target;

    // 직선 강제 스위치
    private boolean forceNoHoming = false;

    private final Sprite sprite;

    public EnemyShotEntity(Game game, String spriteRef, double x, double y, double dx, double dy, double speed) {
        super(spriteRef, (int)Math.round(x), (int)Math.round(y));
        this.game = game;
        this.sprite = SpriteStore.get().getSprite(spriteRef);
        this.speed = speed;
        setHorizontalMovement(dx);
        setVerticalMovement(dy);
    }


    /** 호밍 활성화 (보스 전용) */
    public EnemyShotEntity enableHoming(ShipEntity target, long delayMs, double accel, double maxSpeed) {
        this.homing = true;
        this.target = target;
        this.homingDelayMs = Math.max(0, delayMs);
        this.homingAccel = accel;
        this.maxHomingSpeed = maxSpeed;
        return this;
    }

    @Override
    public void move(long delta) {
        ageMs += delta;

        // 1. 제거 조건 체크 (수명 다함 or 화면 밖)
        if (checkExpiredOrOutOfBounds()) {
            return; // 제거되었으면 이동 로직 수행 안 함
        }

        // 2. 유도탄(호밍) 로직 처리
        updateHomingLogic();

        // 3. 마찰력(감속) 처리
        updateFrictionLogic(delta);

        // 4. 실제 이동 적용
        super.move(delta);
    }
    // 제거 조건 체크 및 처리
    private boolean checkExpiredOrOutOfBounds() {
        boolean expired = ageMs > lifeTimeMs;
        boolean outOfBounds = y < -64 || y > game.getHeight() + 64 || x < -64 || x > game.getWidth() + 64;

        if (expired || outOfBounds) {
            game.removeEntity(this);
            return true; // 제거됨
        }
        return false; // 살아있음
    }

    // 호밍 계산 로직
    private void updateHomingLogic() {
        if (forceNoHoming || !homing || ageMs < homingDelayMs || target == null) {
            return;
        }

        double tx = target.getX() - this.x;
        double ty = target.getY() - this.y;
        double len = Math.sqrt(tx * tx + ty * ty);

        if (len > 1e-4) {
            tx /= len;
            ty /= len;

            double vx = getHorizontalMovement();
            double vy = getVerticalMovement();

            double targetVx = tx * maxHomingSpeed;
            double targetVy = ty * maxHomingSpeed;

            vx = vx + (targetVx - vx) * homingAccel;
            vy = vy + (targetVy - vy) * homingAccel;

            setHorizontalMovement(vx);
            setVerticalMovement(vy);
        }
    }

    // 마찰력 계산 로직
    private void updateFrictionLogic(long delta) {
        if (friction <= 0) return;

        double vx = getHorizontalMovement();
        double vy = getVerticalMovement();
        double v = Math.sqrt(vx * vx + vy * vy);

        if (v > 0) {
            double dec = friction * (delta / 1000.0);
            v = Math.max(0, v - dec);

            if (v == 0) {
                setHorizontalMovement(0);
                setVerticalMovement(0);
            } else {
                // 방향 유지하며 속도만 줄임
                double nx = vx / Math.sqrt(vx * vx + vy * vy); // 현재 속도가 0이 아님을 위에서 확인했으므로 안전
                double ny = vy / Math.sqrt(vx * vx + vy * vy);
                setHorizontalMovement(nx * v);
                setVerticalMovement(ny * v);
            }
        }
    }

    @Override
    public void draw(Graphics g) {
        sprite.draw(g, (int)x, (int)y);
    }

    @Override
    public void collidedWith(Entity other) {
        if (other instanceof ShipEntity) {
            ShipEntity ship = (ShipEntity) other;
            // 대시 무적 중이면 피해 없이 탄만 제거
            if (ship.isInvulnerable()) {
                game.removeEntity(this);
                return;
            }
            // 평소처럼 피해 적용
            ship.damage(1);
            game.removeEntity(this);
            ((ShipEntity) other).damage(1);
        }
        // 적탄은 적과 충돌 무시
    }
}