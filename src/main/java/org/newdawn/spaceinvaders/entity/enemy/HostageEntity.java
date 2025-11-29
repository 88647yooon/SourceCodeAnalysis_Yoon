package org.newdawn.spaceinvaders.entity.enemy;

import org.newdawn.spaceinvaders.Game;
import org.newdawn.spaceinvaders.graphics.Sprite;
import org.newdawn.spaceinvaders.graphics.SpriteStore;
import org.newdawn.spaceinvaders.entity.base.Entity;
import org.newdawn.spaceinvaders.entity.player.ShipEntity;
import org.newdawn.spaceinvaders.entity.projectile.ShotEntity;

public class HostageEntity extends Entity {
    private final Game game;
    private final Sprite[] frames = new Sprite[3];

    // --- animation state ---
    private int frameIndex = 0;
    private long frameTimerMs = 0;
    private long frameDurationMs = 240; // 프레임 유지 시간(조절해서 속도 변경)

    public HostageEntity(Game game, int x, int y) {
        super("sprites/hostage1.png", x, y); // 스프라이트는 따로 준비 필요

        // setup the animatin frames
        frames[0] = sprite;
        frames[1] = SpriteStore.get().getSprite("sprites/hostage2.png");
        frames[2] = SpriteStore.get().getSprite("sprites/hostage3.png");
        this.game = game;
    }
    @Override
    public void move(long delta) {
        // 애니메이션 타이머만 갱신
        frameTimerMs += delta;
        if (frameTimerMs >= frameDurationMs) {
            frameTimerMs -= frameDurationMs;
            frameIndex = (frameIndex + 1) % frames.length; // 0→1→2→0…
            sprite = frames[frameIndex];
        }
    }
    @Override
    public void collidedWith(Entity other) {
        if (other instanceof ShotEntity) {
            //플레이어 탄 제거
            game.getEntityManager().removeEntity(other);

            // 페널티: 플레이어 HP 1 감소
            ShipEntity player = game.getPlayerShip();
            if (player != null) {
                player.damage(1); // BossEntity에서 쓰던 동일 API
            }
            //인질은 제거
            game.getEntityManager().removeEntity(this);

        }
    }
}
