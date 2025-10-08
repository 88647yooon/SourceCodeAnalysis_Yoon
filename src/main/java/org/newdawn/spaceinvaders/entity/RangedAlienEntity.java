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

        long effectiveCooldown = Math.max(200, (long)(baseCooldownMs / fireRateMul));
        long effectiveJitter   = Math.max(0,   (long)(cooldownJitterMs / fireRateMul));
        long nextWindow = lastShotAt
                + effectiveCooldown
                + ThreadLocalRandom.current().nextLong(effectiveJitter + 1);

        if (now < nextWindow || player == null) return;

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

        double speed = bulletSpeed * bulletSpeedMul;

        // Ranged 적은 항상 '아래로 직선' 발사 (유도처럼 보이지 않게)
        double vx = 0.0;
        double vy = speed;

        game.addEntity(new EnemyShotEntity(
                game,
                "sprites/enemy_bullet.png",
                sx, sy,
                vx, vy,                 // 절대속도(px/s) — 항상 아래로
                speed
        ));
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