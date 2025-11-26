package org.newdawn.spaceinvaders.entity.projectile.movement;

import org.newdawn.spaceinvaders.entity.projectile.EnemyShotEntity;
public interface ProjectileMovementStrategy {
    /**
     * 탄환의 움직임을 계산하여 적용합니다.
     * @param shot 움직일 탄환 객체 (위치, 속도 정보 접근용)
     * @param delta 지난 프레임으로부터 경과된 시간 (ms)
     */
    void updateMovement(EnemyShotEntity shot, long delta);
}
