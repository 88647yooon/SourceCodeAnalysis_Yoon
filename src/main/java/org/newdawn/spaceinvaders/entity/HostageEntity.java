package org.newdawn.spaceinvaders.entity;

import org.newdawn.spaceinvaders.Game;
import org.newdawn.spaceinvaders.SpriteStore;

import java.awt.Graphics;

public class HostageEntity extends Entity {
    private final Game game;

    public HostageEntity(Game game, int x, int y) {
        super("sprites/hostage.png", x, y); // 스프라이트는 따로 준비 필요
        this.game = game;
    }
    @Override
    public void move(long delta) {
        // 움직이지 않음
    }
    @Override
    public void collidedWith(Entity other) {
        if (other instanceof ShotEntity) {
            // 인질이 총에 맞았다! → 페널티 부여
            game.removeEntity(this);
            game.removeEntity(other);

            // 예: 게임 오버 처리
            game.notifyDeath();

            // 또는 점수 차감만 하고 계속 진행할 수도 있음
            // game.addScore(-500);
        }
        // 적 탄/외계인 충돌은 무시
    }
}
