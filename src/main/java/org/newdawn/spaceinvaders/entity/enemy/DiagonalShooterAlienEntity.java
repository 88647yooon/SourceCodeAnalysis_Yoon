package org.newdawn.spaceinvaders.entity.enemy;

import org.newdawn.spaceinvaders.Game;
import org.newdawn.spaceinvaders.entity.Entity;
import org.newdawn.spaceinvaders.entity.projectile.EnemyShotEntity;

public class DiagonalShooterAlienEntity extends AlienEntity{
    private double bulletSpeed = 240;     // px/s

    public DiagonalShooterAlienEntity(Game game, int x, int y) {
        super(game, "sprites/enemy_diagonal.png", x, y); // 스프라이트 하나 준비해 두면 좋아요
        // 부모 클래스에 발사 주기 설정
        setFireCooldown(1600, 500); // (기존 baseCooldownMs, cooldownJitterMs 값)
        // --- [리팩토링 끝] ---
    }

    @Override
    public void move(long delta) {
        super.move(delta);  // AlienEntity의 이동/도로직 유지
    }
    /**
     * [리팩토링 - 신규]
     * 부모 클래스(AlienEntity)의 템플릿 메소드인 checkFire()에 의해 호출되는
     * "실제 발사" 로직 (Hook 메소드 구현)
     */
    @Override
    protected void performFire() {
        double sx = getX() + getWidth() * 0.5;
        double sy = getY() + getHeight();

        // 양쪽 대각선 단위벡터
        final double inv = 1.0 / Math.sqrt(2.0);
        double dlx = -inv, dly = inv; // down-left
        double drx =  inv, dry = inv; // down-right

        // [수정] 부모로부터 난이도 배수를 가져옴
        double speed = bulletSpeed * getBulletSpeedMultiplier();

        //  절대속도(px/s)로 변환해서 넘긴다
        double vx1 = dlx * speed, vy1 = dly * speed;
        double vx2 = drx * speed, vy2 = dry * speed;

        // [수정] 'game'은 부모의 protected 필드를 사용
        game.addEntity(new EnemyShotEntity(game, "sprites/enemy_bullet.png", sx, sy, vx1, vy1, speed));
        game.addEntity(new EnemyShotEntity(game, "sprites/enemy_bullet.png", sx, sy, vx2, vy2, speed));

    }

    @Override
    public void collidedWith(Entity other) {
        // 처치/카운트는 ShotEntity 쪽에서만 처리 (중복 방지)
    }
}
