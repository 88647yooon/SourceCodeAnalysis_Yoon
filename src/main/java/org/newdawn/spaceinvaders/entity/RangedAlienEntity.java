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
    private double leadFactor = 0.25;     // 수평 예측(원하면 0으로)

    private final Sprite sprite;

    public RangedAlienEntity(Game game, int startX, int startY, ShipEntity player) {
        super(game,"sprites/enemy_gunner.png", startX, startY);
        this.game = game;
        this.player = player;
        this.sprite = SpriteStore.get().getSprite("sprites/enemy_gunner.png");

        // 난이도/웨이브 보정(있으면)
        int wave = game.getWaveCount();
        bulletSpeed += Math.min(200, wave * 12); // 웨이브마다 탄속 소폭 증가
        baseCooldownMs = Math.max(700, baseCooldownMs - wave * 30); // 웨이브 올라갈수록 빠르게
    }


    @Override
    public void move(long delta) {
        super.move(delta); // 기존 AlienEntity의 패턴 유지(좌우 이동 등)

        maybeFire();
    }
    private void maybeFire() {
        long now = System.currentTimeMillis();
        long nextWindow = lastShotAt + baseCooldownMs + (long)(Math.random() * cooldownJitterMs);
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

        EnemyShotEntity bullet = new EnemyShotEntity(
                game,
                "sprites/enemy_bullet.png",
                sx, sy,
                dirX, dirY,
                bulletSpeed
        );
        game.addEntity(bullet);
        lastShotAt = now;
    }
    @Override
    public void collidedWith(Entity other) {

    }
}