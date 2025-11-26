package org.newdawn.spaceinvaders.entity.enemy;

import org.newdawn.spaceinvaders.graphics.Sprite;
import org.newdawn.spaceinvaders.Game;
import org.newdawn.spaceinvaders.graphics.SpriteStore;
import org.newdawn.spaceinvaders.entity.base.Entity;
import org.newdawn.spaceinvaders.entity.player.ShipEntity;
import org.newdawn.spaceinvaders.entity.projectile.EnemyShotEntity;


public class RangedAlienEntity extends AlienEntity {
    private final ShipEntity player;

    private double bulletSpeed = 220; // px/s

    private final Sprite sprite;

    public RangedAlienEntity(Game game, int startX, int startY, ShipEntity player) {
        super(game,"sprites/enemy_gunner.png", startX, startY);
        this.player = player;
        this.sprite = SpriteStore.get().getSprite("sprites/enemy_gunner.png");

        //리팩토링
        //부모 클래스에 발사 주기 설정
        setFireCooldown(1500,600);
    }

    @Override
    public void move(long delta) {
        super.move(delta); // 기존 AlienEntity의 패턴 유지(좌우 이동 등)
    }

    /**
     * [리팩토링 - 신규]
     * 부모 클래스(AlienEntity)의 템플릿 메소드인 checkFire()에 의해 호출되는
     * "실제 발사" 로직 (Hook 메소드 구현)
     */
    @Override
    protected void performFire() {
        if (player == null) return;

        // 발사 원점
        double sx = this.getX() + this.getWidth()  * 0.5;
        double sy = this.getY() + this.getHeight() * 0.5;

        // 플레이어 현재 중심을 향해 "직선"
        // (참고: 아래 계산은 남겨두지만 실제 발사 방향은 '항상 아래로' 고정함)
        double px = player.getX() + player.getWidth()  * 0.5;
        double py = player.getY() + player.getHeight() * 0.5;

        double tx = px - sx;
        double ty = py - sy;
        double len = Math.hypot(tx, ty);
        if (len < 1e-3) {
            // 너무 가까우면 그냥 아래로 쏴 버림
            len = 1.0;
        }

        // [수정] 부모로부터 난이도 배수를 가져옴
        double speed = bulletSpeed * getBulletSpeedMultiplier();

        // Ranged 적은 항상 '아래로 직선' 발사 (유도처럼 보이지 않게)
        double vx = 0.0;
        double vy = speed;


        game.getEntityManager().addEntity(new EnemyShotEntity(
                game,
                "sprites/enemy_bullet.png",
                sx, sy,
                vx, vy
        ));
    }


    @Override
    public void collidedWith(Entity other) {
        //부모의 collideWith/wasHitBy가 처리함
    }
}