package org.newdawn.spaceinvaders.entity.projectile.movement;

import org.newdawn.spaceinvaders.entity.projectile.EnemyShotEntity;

public class LinearMovement implements ProjectileMovementStrategy {
    @Override
    public void updateMovement(EnemyShotEntity shot, long delta) {
        // 기본 Entity.move()가 dx, dy를 사용해 이동을 처리하므로
        // 추가적인 간섭을 하지 않습니다.
    }
}