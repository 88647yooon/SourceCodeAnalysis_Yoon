package org.newdawn.spaceinvaders.entity;

import org.newdawn.spaceinvaders.Game;

public class DiagonalShooterAlienEntity extends AlienEntity{
    private final Game game;

    private long lastShotAt = 0L;
    private long baseCooldownMs = 1600;   // 기본 쿨다운(양쪽 2발이라 약간 길게)
    private long cooldownJitterMs = 500;  // 랜덤 지터
    private double bulletSpeed = 240;     // px/s

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
        long nextWindow = lastShotAt + baseCooldownMs + (long) (Math.random() * cooldownJitterMs);
        if (now < nextWindow) return;

        // 내 총구 위치 (아래쪽 중앙)
        double sx = getX() + getWidth() * 0.5;
        double sy = getY() + getHeight();

        // 양쪽 대각선 단위벡터 (아래-왼쪽, 아래-오른쪽)
        final double inv = 1.0 / Math.sqrt(2.0);
        double dlx = -inv, dly = inv; // down-left
        double drx =  inv, dry = inv; // down-right

        // 두 발 생성
        game.addEntity(new EnemyShotEntity(game, "sprites/enemy_bullet.png", sx, sy, dlx, dly, bulletSpeed));
        game.addEntity(new EnemyShotEntity(game, "sprites/enemy_bullet.png", sx, sy, drx, dry, bulletSpeed));

        lastShotAt = now;
    }

    @Override
    public void collidedWith(Entity other) {
        // 처치/카운트는 ShotEntity 쪽에서만 처리 (중복 방지)
    }
}
