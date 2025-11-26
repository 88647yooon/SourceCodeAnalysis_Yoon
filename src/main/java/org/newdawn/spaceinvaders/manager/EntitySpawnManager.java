package org.newdawn.spaceinvaders.manager;

import org.newdawn.spaceinvaders.Game;
import org.newdawn.spaceinvaders.entity.base.Entity;
import org.newdawn.spaceinvaders.entity.boss.BossEntity;
import org.newdawn.spaceinvaders.entity.enemy.AlienEntity;
import org.newdawn.spaceinvaders.entity.enemy.DiagonalShooterAlienEntity;
import org.newdawn.spaceinvaders.entity.enemy.HostageEntity;
import org.newdawn.spaceinvaders.entity.enemy.RangedAlienEntity;
import org.newdawn.spaceinvaders.entity.player.ShipEntity;

import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

public class EntitySpawnManager {
    private final Game game;
    private final EntityManager entityManager;
    private final Random random = new Random();

    public EntitySpawnManager(Game game, EntityManager entityManager) {
        this.game = game;
        this.entityManager = entityManager;
    }

    /**
     * [핵심] 통합 그리드 소환 메서드
     * 스테이지 모드와 무한 모드 모두 이 메서드 하나를 사용합니다.
     */
    public int spawnUniversalGrid(int rows, int cols, double chanceDiagonal, double chanceRanged) {
        int startX = 100;
        int startY = 50;
        int gapX = 50;
        int gapY = 30;

        int count = 0;
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                int x = startX + c * gapX;
                int y = startY + r * gapY;

                // 1. 확률에 따라 적 종류 결정
                Entity alien = createRandomAlien(x, y, chanceDiagonal, chanceRanged);

                // 2. 난이도 적용 (무한모드용)
                if (game.isInfiniteMode()) {
                    applyDifficulty(alien);
                }

                // 3. 안전 소환 (자리 겹침 확인)
                if (trySafeSpawn(alien)) {
                    count++;
                }
            }
        }
        return count;
    }

    // 확률 기반 적 생성 공장
    private Entity createRandomAlien(int x, int y, double chanceDiag, double chanceRanged) {
        double roll = random.nextDouble(); // 0.0 ~ 1.0

        if (roll < chanceDiag) {
            return new DiagonalShooterAlienEntity(game, x, y);
        } else if (roll < chanceDiag + chanceRanged) {
            return new RangedAlienEntity(game, x, y, game.getPlayerShip());
        } else {
            return new AlienEntity(game, x, y);
        }
    }

    // =========================================================
    // 모드별 진입점 (Entry Points)
    // =========================================================

    /** 무한 모드: 웨이브에 따라 확률을 자동 계산해서 통합 메서드 호출 */
    public void spawnAliensForInfiniteMode() {
        clearHostagesForInfiniteMode();

        int wave = game.getWaveCount();
        if (wave % 5 == 0) {
            spawnBoss();
            game.setAlienCount(1);
        }
        else {
            int rows = 3 + (wave % 3);
            int cols = 6 + (wave % 6);

            // 무한 모드 난이도 공식
            double diagProb = Math.min(0.05 + (wave - 1) * 0.02, 0.25);
            double rangedProb = 0.25;

            // 통합 메서드 호출
            int count = spawnUniversalGrid(rows, cols, diagProb, rangedProb);
            game.setAlienCount(count);

            // 인질 소환 (통합 메서드 설정값인 startX=100, startY=50, gapX=50을 전달)
            if (game.isInfiniteMode()) {
                spawnHostages(cols, 100, 50, 50);
            }
        }
        game.incrementWaveCount();
    }

    // 보스 소환 (스테이지 5 전용)
    public void spawnBoss() {
        List<Entity> entities = entityManager.getMutableEntities();
        entities.removeIf(HostageEntity.class::isInstance);

        BossEntity boss = new BossEntity(game, 360, 60, game.getPlayerShip());
        entityManager.addEntity(boss);
        game.setBossActive(true);
    }

    // =========================================================
    // 유틸리티 / 헬퍼 (안전 소환 & 인질)
    // =========================================================

    public boolean trySafeSpawn(Entity entity) {
        if (isTooClose(entity.getX(), entity.getY())) return false;
        entityManager.addEntity(entity);
        return true;
    }

    private boolean isTooClose(int tx, int ty) {
        for (Entity e : entityManager.getEntities()) {
            if (e instanceof ShipEntity || e instanceof HostageEntity) continue;

            if (e.getClass().getSimpleName().contains("Shot")) continue;

            if (Math.abs(e.getX() - tx) < 30 && Math.abs(e.getY() - ty) < 30) {
                return true;
            }
        }
        return false;
    }


    private void clearHostagesForInfiniteMode() {
        if (!game.isInfiniteMode()) return;
        List<Entity> entities = entityManager.getMutableEntities();
        entities.removeIf(HostageEntity.class::isInstance);
    }

    private void spawnHostages(int cols, int startX, int startY, int gapX) {
        int hostageNum = 1 + random.nextInt(2);
        Set<Integer> usedCols = new HashSet<>();
        int hostageY = startY - 40;

        for (int i = 0; i < hostageNum; i++) {
            int c = chooseHostageColumn(cols, usedCols);
            usedCols.add(c);
            int x = startX + (c * gapX);
            entityManager.addEntity(new HostageEntity(game, x, hostageY));
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

    // =========================================================
    // 난이도 (Difficulty) 로직 복원
    // =========================================================

    private void applyDifficulty(Entity e) {
        if (!game.isInfiniteMode()) return;

        Difficulty d = computeDifficultyForWave(game.getWaveCount());

        if (e instanceof AlienEntity) {
            ((AlienEntity) e).setMaxHP(d.alienHP);
            ((AlienEntity) e).applySpeedMultiplier(d.alienSpeedMul);
        } else if (e instanceof RangedAlienEntity) {
            ((RangedAlienEntity) e).setFireRateMultiplier(d.fireRateMul);
            ((RangedAlienEntity) e).setBulletSpeedMultiplier(d.bulletSpeedMul);
        } else if (e instanceof DiagonalShooterAlienEntity) {
            ((DiagonalShooterAlienEntity) e).setFireRateMultiplier(d.fireRateMul);
            ((DiagonalShooterAlienEntity) e).setBulletSpeedMultiplier(d.bulletSpeedMul);
        }
    }

    private Difficulty computeDifficultyForWave(int wave) {
        Difficulty d = new Difficulty();
        d.alienHP = wave;

        d.alienSpeedMul = Math.min(2.5, 1.0 + 0.08 * (wave - 1));
        d.fireRateMul = Math.min(3.0, 1.0 + 0.10 * (wave - 1));
        d.bulletSpeedMul = Math.min(2.0, 1.0 + 0.05 * (wave - 1));
        return d;
    }

    private static class Difficulty {
        int alienHP;
        double alienSpeedMul;
        double fireRateMul;
        double bulletSpeedMul;
    }
}