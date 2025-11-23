package org.newdawn.spaceinvaders.manager;

import org.newdawn.spaceinvaders.Game;
import org.newdawn.spaceinvaders.entity.base.Entity;
import org.newdawn.spaceinvaders.entity.enemy.AlienEntity;
import org.newdawn.spaceinvaders.entity.enemy.DiagonalShooterAlienEntity;
import org.newdawn.spaceinvaders.entity.enemy.HostageEntity;
import org.newdawn.spaceinvaders.entity.enemy.RangedAlienEntity;

import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

public class EntitySpawnManager {
    private final Game game;
    private final EntityManager entityManager;
    private final Random random = new Random();

    private static final double RANGED_ALIEN_RATIO = 0.25;

    public EntitySpawnManager(Game game, EntityManager entityManager) {
        this.game = game;
        this.entityManager = entityManager;
    }

    public void spawnAliens(){
        clearHostagesForInfiniteMode();

        Difficulty diff = computeDifficultyForWave(game.getWaveCount());

        int rows = 3 + (game.getWaveCount() % 3);   // 3~5
        int cols = 6 + (game.getWaveCount() % 6);   // 6~11
        int startX = 100;
        int startY = 50;
        int gapX = 50;
        int gapY = 30;

        int alienCount = spawnAlienGrid(diff, rows, cols, startX, startY, gapX, gapY);

        // Game 쪽 alienCount/waveCount 갱신
        game.setAlienCount(alienCount);

        if (game.isInfiniteMode()) {
            spawnHostages(cols, startX, startY, gapX);
        }

        game.incrementWaveCount();
    }

    public void spawnBoss() {
        List<Entity> entities = entityManager.getMutableEntities();
        entities.removeIf(HostageEntity.class::isInstance);

        Entity boss = new org.newdawn.spaceinvaders.entity.boss.BossEntity(
                game,
                360,
                60,
                game.getPlayerShip()
        );
        entityManager.addEntity(boss);
        game.setBossActive(true);
    }
    private void clearHostagesForInfiniteMode() {
        if (!game.isInfiniteMode()) return;

        List<Entity> entities = entityManager.getMutableEntities();
        entities.removeIf(HostageEntity.class::isInstance);
    }

    private int spawnAlienGrid(Difficulty diff,
                                      int rows, int cols,
                                      int startX, int startY,
                                      int gapX, int gapY) {

        int alienCount = 0;

        for (int row = 0; row < rows; row++) {
            for (int c = 0; c < cols; c++) {
                int x = startX + (c * gapX);
                int y = startY + (row * gapY);

                Entity alien = createAlienForPosition(x, y);
                applyDifficultyToAlien(alien, diff);

                entityManager.addEntity(alien);
                alienCount++;
            }
        }
        return alienCount;
    }

    private Entity createAlienForPosition(int x, int y) {
        int waveCount = game.getWaveCount();

        double diagonalProb = Math.min(0.05 + (waveCount - 1) * 0.02, 0.25);

        double r = Math.random();

        if (r < diagonalProb) {
            return new DiagonalShooterAlienEntity(game, x, y);
        }
        if (r < diagonalProb + RANGED_ALIEN_RATIO) {
            return new RangedAlienEntity(game, x, y, game.getPlayerShip());
        }
        return new AlienEntity(game, x, y);
    }

    private void spawnHostages(int cols, int startX, int startY, int gapX) {
        int hostageNum = 1 + random.nextInt(2);

        Set<Integer> usedCols = new HashSet<>();

        for (int i = 0; i < hostageNum; i++) {
            int c = chooseHostageColumn(cols, usedCols);
            usedCols.add(c);

            int x = startX + (c * gapX);
            int y = startY - 40;
            Entity hostage = new HostageEntity(game, x, y);
            entityManager.addEntity(hostage);
        }
    }

    private int chooseHostageColumn(int cols, Set<Integer> usedCols) {
        int guard = 0;
        int c;
        do {
            c = random.nextInt(cols);
        } while (usedCols.contains(c) && ++guard < 10);
        return c;
    }

    // 난이도 관련
    private static class Difficulty {
        int alienHP;
        double alienSpeedMul;
        double fireRateMul;
        double bulletSpeedMul;
    }

    private Difficulty computeDifficultyForWave(int wave) {
        Difficulty d = new Difficulty();
        d.alienHP = 1 + Math.max(0, (wave - 1) / 2);
        d.alienSpeedMul = Math.min(2.5, 1.0 + 0.08 * (wave - 1));
        d.fireRateMul = Math.min(3.0, 1.0 + 0.10 * (wave - 1));
        d.bulletSpeedMul = Math.min(2.0, 1.0 + 0.05 * (wave - 1));
        return d;
    }

    private void applyDifficultyToAlien(Entity e, Difficulty d) {
        if (e instanceof AlienEntity) {
            AlienEntity a = (AlienEntity) e;
            a.setMaxHP(d.alienHP);
            a.applySpeedMultiplier(d.alienSpeedMul);
        }
        if (e instanceof RangedAlienEntity) {
            RangedAlienEntity r = (RangedAlienEntity) e;
            r.setFireRateMultiplier(d.fireRateMul);
            r.setBulletSpeedMultiplier(d.bulletSpeedMul);
        }
        if (e instanceof DiagonalShooterAlienEntity) {
            DiagonalShooterAlienEntity ds = (DiagonalShooterAlienEntity) e;
            ds.setFireRateMultiplier(d.fireRateMul);
            ds.setBulletSpeedMultiplier(d.bulletSpeedMul);
        }
    }
}
