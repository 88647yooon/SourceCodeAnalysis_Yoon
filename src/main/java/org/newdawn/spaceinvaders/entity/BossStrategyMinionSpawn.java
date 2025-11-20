package org.newdawn.spaceinvaders.entity;

import org.newdawn.spaceinvaders.Game;

import java.awt.Graphics2D;

public class BossStrategyMinionSpawn implements BossAttackStrategy {
    //미니언 소환 관련 필드
    private final long spawnCooldown = 6500;
    private long lastSpawnAt = 0;

    private final BossEntity boss;

    public BossStrategyMinionSpawn(BossEntity boss){
        this.boss = boss;

        this.lastSpawnAt = System.currentTimeMillis() - spawnCooldown;
    }

    @Override
    public void update(BossEntity boss, long delta){
        long now = System.currentTimeMillis();

        if (now - lastSpawnAt > spawnCooldown){
            lastSpawnAt = now;

            Game game = boss.getGame();
            ShipEntity player = boss.getTarget();

            // 1. 미니언 소환
            // RangedAlienEntity (원거리) 소환
            game.addEntity(new RangedAlienEntity(game, (int)(boss.getX() - 30), (int)(boss.getY() + 120), player));
            // DiagonalShooterAlienEntity (대각) 소환
            game.addEntity(new DiagonalShooterAlienEntity(game, (int)(boss.getX() + 60), (int)(boss.getY() + 120)));

            // 2. 미니언 카운트 갱신 (Game 클래스에 알림)
            game.setAlienCount(game.getAlienCount() + 2);
        }
    }

    @Override
    public void draw(Graphics2D g, BossEntity boss){

    }
}
