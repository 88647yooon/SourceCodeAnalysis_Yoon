package org.newdawn.spaceinvaders.entity.projectile.movement;

import org.newdawn.spaceinvaders.entity.base.Entity;
import org.newdawn.spaceinvaders.entity.projectile.EnemyShotEntity;

public class HomingMovement implements ProjectileMovementStrategy {
    private final Entity target;       // 추적할 대상
    private final double homingAccel;  // 유도 성능 (회전 속도)
    private final double maxSpeed;     // 최대 속도
    private final long delayMs;        // 유도 시작 전 대기 시간

    public HomingMovement(Entity target, double accel, double maxSpeed, long delayMs) {
        this.target = target;
        this.homingAccel = accel;
        this.maxSpeed = maxSpeed;
        this.delayMs = delayMs;
    }

    @Override
    public void updateMovement(EnemyShotEntity shot, long delta) {
        // 1. 유도 시작 조건 체크 (타겟 없음 or 대기 시간 미달)
        if (target == null || shot.getAge() < delayMs) return;

        // 2. 타겟 방향 벡터 계산
        double tx = target.getX() - shot.getX();
        double ty = target.getY() - shot.getY();
        double len = Math.sqrt(tx * tx + ty * ty);

        if (len > 1e-4) {
            tx /= len; // 단위 벡터 정규화
            ty /= len;

            // 3. 현재 속도 벡터
            double vx = shot.getHorizontalMovement();
            double vy = shot.getVerticalMovement();

            // 4. 목표 속도 벡터
            double targetVx = tx * maxSpeed;
            double targetVy = ty * maxSpeed;

            // 5. 현재 속도에서 목표 속도로 서서히 변경 (가속도 적용)
            vx += (targetVx - vx) * homingAccel;
            vy += (targetVy - vy) * homingAccel;

            // 6. 결과 적용
            shot.setHorizontalMovement(vx);
            shot.setVerticalMovement(vy);
        }
    }
}