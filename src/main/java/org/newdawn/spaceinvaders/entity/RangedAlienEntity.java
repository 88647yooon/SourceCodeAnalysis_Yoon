package org.newdawn.spaceinvaders.entity;

import org.newdawn.spaceinvaders.Sprite;
import org.newdawn.spaceinvaders.Game;
import org.newdawn.spaceinvaders.SpriteStore;

import java.util.concurrent.ThreadLocalRandom;

public class RangedAlienEntity extends AlienEntity {
    private final Game game;
    private final ShipEntity player;

    private long lastShotAt = 0L;
    private long baseCooldownMs = 1500; // 기본 쿨다운
    private long cooldownJitterMs = 600; // 랜덤 지터

    private double bulletSpeed = 220; // px/s
    private double leadFactor = 0.25;

    //난이도 증가 필드
    private double fireRateMul = 1.0;     // 높을수록 더 자주 쏨 (쿨다운을 나눔)
    private double bulletSpeedMul = 1.0;  // 탄속 배수// 수평 예측(원하면 0으로)

    private final Sprite sprite;

    public RangedAlienEntity(Game game, int startX, int startY, ShipEntity player) {
        super(game,"sprites/enemy_gunner.png", startX, startY);
        this.game = game;
        this.player = player;
        this.sprite = SpriteStore.get().getSprite("sprites/enemy_gunner.png");


    }


    @Override
    public void move(long delta) {
        super.move(delta); // 기존 AlienEntity의 패턴 유지(좌우 이동 등)

        maybeFire();
    }
    private void maybeFire() {
        long now = System.currentTimeMillis();

        // 배수 반영한 유효 쿨다운/지터 (연사 배수↑ => 더 자주 쏨)
        long effectiveCooldown = Math.max(200, (long)(baseCooldownMs / fireRateMul));
        long effectiveJitter   = (long)(cooldownJitterMs / fireRateMul);

        long nextWindow = lastShotAt
                + effectiveCooldown
                + ThreadLocalRandom.current().nextLong(effectiveJitter + 1);

        //플레이어 존재 체크
        if (now < nextWindow || player == null) return;
        // 발사 타이밍 도달 + 화면 안에 플레이어 존재 확인
        // 간단 조준(현재 위치를 향한 단위벡터)
        double sx = this.getX() + this.getWidth()  * 0.5;
        double sy = this.getY() + this.getHeight() * 0.5;
        double px = player.getX() + player.getWidth()  * 0.5;
        double py = player.getY() + player.getHeight() * 0.5;

        double tx = px - sx;
        double ty = py - sy;
        double len = Math.sqrt(tx*tx + ty*ty);
        if (len < 1) return;

        double dirX = 0.0;
        double dirY = 1.0;

        double effectiveBulletSpeed = bulletSpeed * bulletSpeedMul;

        EnemyShotEntity bullet = new EnemyShotEntity(
                game,
                "sprites/enemy_bullet.png",
                sx, sy,
                dirX, dirY,
                effectiveBulletSpeed
        );
        game.addEntity(bullet);
        lastShotAt = now;
    }

    //연사속도 증가 메소드
    public void setFireRateMultiplier(double mul) {
        this.fireRateMul = Math.max(0.25, mul); // 하한 보호
    }

    //탄속 증가 메소드
    public void setBulletSpeedMultiplier(double mul) {
        this.bulletSpeedMul = Math.max(0.5, mul);
    }
    @Override
    public void collidedWith(Entity other) {

    }
}