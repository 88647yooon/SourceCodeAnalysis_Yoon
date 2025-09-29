package org.newdawn.spaceinvaders.entity;

import org.newdawn.spaceinvaders.Game;

import java.util.concurrent.ThreadLocalRandom;

public class DiagonalShooterAlienEntity extends AlienEntity{
    private final Game game;

    private long lastShotAt = 0L;
    private long baseCooldownMs = 1600;   // 기본 쿨다운(양쪽 2발이라 약간 길게)
    private long cooldownJitterMs = 500;  // 랜덤 지터
    private double bulletSpeed = 240;     // px/s

    //난이도 조절관련 필드
    private double fireRateMul = 1.0;
    private double bulletSpeedMul = 1.0;

    public DiagonalShooterAlienEntity(Game game, int x, int y) {
        super(game, "sprites/enemy_diagonal.png", x, y); // 스프라이트 하나 준비해 두면 좋아요
        this.game = game;
        // 필요하면 크기 통일
        // int targetW = 40;
        // setScale(targetW / (double) getSprite().getWidth()); // getSprite()가 없으면 생략
    }

    @Override
    public void move(long delta) {
        super.move(delta);  // AlienEntity의 이동/도로직 유지
        maybeFire();
    }

    private void maybeFire() {
        long now = System.currentTimeMillis();

        long effectiveCooldown = Math.max(250, (long) (baseCooldownMs / fireRateMul));
        long effectiveJitter   = Math.max(1,   (long) (cooldownJitterMs / fireRateMul));
        long nextWindow = lastShotAt
                + effectiveCooldown
                + ThreadLocalRandom.current().nextLong(effectiveJitter + 1);
        if (now < nextWindow) return;

        double sx = getX() + getWidth() * 0.5;
        double sy = getY() + getHeight();

        // 양쪽 대각선 단위벡터
        final double inv = 1.0 / Math.sqrt(2.0);
        double dlx = -inv, dly = inv; // down-left
        double drx =  inv, dry = inv; // down-right

        double speed = bulletSpeed * bulletSpeedMul;

        //  절대속도(px/s)로 변환해서 넘긴다
        double vx1 = dlx * speed, vy1 = dly * speed;
        double vx2 = drx * speed, vy2 = dry * speed;

        game.addEntity(new EnemyShotEntity(game, "sprites/enemy_bullet.png", sx, sy, vx1, vy1, speed));
        game.addEntity(new EnemyShotEntity(game, "sprites/enemy_bullet.png", sx, sy, vx2, vy2, speed));

        lastShotAt = now;
    }

    //난이도 조절용 메소드
    public void setFireRateMultiplier(double mul) { this.fireRateMul = Math.max(0.25, mul); }
    public void setBulletSpeedMultiplier(double mul) { this.bulletSpeedMul = Math.max(0.5, mul); }

    @Override
    public void collidedWith(Entity other) {
        // 처치/카운트는 ShotEntity 쪽에서만 처리 (중복 방지)
    }
}
