package org.newdawn.spaceinvaders.entity;

import java.awt.Graphics;
import org.newdawn.spaceinvaders.Game;
import org.newdawn.spaceinvaders.Sprite;
import org.newdawn.spaceinvaders.SpriteStore;

public class EnemyShotEntity extends Entity{
    private final Game game;
    private final double speed;

    private final Sprite sprite;

    public EnemyShotEntity(Game game, String spriteRef, double x, double y, double dx, double dy, double speed) {
        super(spriteRef, (int)Math.round(x), (int)Math.round(y));
        this.game = game;
        this.speed = speed;
        this.dx = dx * speed;
        this.dy = dy * speed;
        this.sprite = SpriteStore.get().getSprite(spriteRef);
    }

    @Override
    public void move(long delta) {
        // 기본 이동
        x += (dx * delta) / 1000.0;
        y += (dy * delta) / 1000.0;

        // 화면 벗어나면 제거
        if (y < -100 || y > game.getHeight() + 100 || x < -100 || x > game.getWidth() + 100) {
            game.removeEntity(this);
        }
    }

    @Override
    public void draw(Graphics g) {
        sprite.draw(g, (int) x, (int) y);
    }

    @Override
    public void collidedWith(Entity other) {
        if (other instanceof ShipEntity) {
            game.removeEntity(this);
            ((ShipEntity) other).damage(1); // 플레이어 피격 처리(체력/목숨 감소 메서드)
        }
        // 적 탄은 외계인과 충돌 시 무시
    }
}
