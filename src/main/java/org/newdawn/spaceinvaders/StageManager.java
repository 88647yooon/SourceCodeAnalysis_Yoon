package org.newdawn.spaceinvaders;

import org.newdawn.spaceinvaders.entity.*;
import java.util.List;
import java.util.ArrayList;

/**
 * 화면/입력과 분리된 스테이지 구성 전담 매니저.
 * Game은 건드리지 않고, 선택된 스테이지에 맞게 엔티티를 재배치한다.
 */
public final class StageManager {

    private StageManager() {}

    /** 스테이지 번호에 맞는 배치를 Game에 적용한다. */
    public static void applyStage(int stageNum, Game game) {
        // 1) 기존 엔티티 중 플레이어만 남기고 정리
        ShipEntity ship = game.getPlayerShip();
        List<Entity> list = game.getEntities();

        // 여기서 바로 list.clear() 후 ship만 다시 넣어도 되지만,
        // 혹시 모를 참조 안정성을 위해 removeIf로 정리
        list.removeIf(e -> e != ship);

        // 플레이어 위치 초기화(원하면 유지해도 됨)
        ship.teleportTo(370, 550);
        ship.setHorizontalMovement(0);
        ship.setVerticalMovement(0);
        //alienCount 초기화
        int count = 0;

        // 2) 스테이지별 구성
        switch (stageNum) {
            case 1:
                count = spawnGrid(game, 5, 10, 100, 50, 50, 30); // 기본
                break;
            case 2:
                count = spawnGrid(game, 5, 12, 80, 50, 48, 28);
                count += sprinkleRanged(game, ship, 4);           // 원거리 몇 기 추가
                break;
            case 3:
                count = spawnGrid(game, 6, 12, 70, 50, 48, 26);
                count += sprinkleDiagonal(game, 6);               // 대각 사수 추가
                break;
            case 4:
                count = spawnMixed(game, ship);                  // 혼합 편성(난이도↑)
                break;
            case 5:
                count = spawnBossOnly(game, ship);               // ✅ 바로 보스!
                break;
            default:
                count = spawnGrid(game, 5, 10, 100, 50, 50, 30);
        }

        //배치된 적 수를 alienCount로 반영
        game.setAlienCount(count);
    }

    /* ---------- 헬퍼들 ---------- */

    /** 단순 격자 배치 */
    private static int spawnGrid(Game game, int rows, int cols,
                                  int startX, int startY, int gapX, int gapY) {
        int count = 0;
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                int x = startX + c * gapX;
                int y = startY + r * gapY;
                game.addEntity(new AlienEntity(game, x, y));
                count++;
            }
        }
        return count;
    }

    /** 원거리 적 몇 기 뿌리기 */
    private static int sprinkleRanged(Game game, ShipEntity ship, int count) {
        int[][] spots = {
                {140, 120}, {420, 150}, {620, 180}, {240, 180},
                {340, 210}, {540, 120}
        };
        int actual = 0;
        for (int i = 0; i < count && i < spots.length; i++) {
            int x = spots[i][0];
            int y = spots[i][1];
            game.addEntity(new RangedAlienEntity(game, x, y, ship));
            actual++;
        }
        return actual;
    }

    /** 대각 사수 몇 기 뿌리기 */
    private static int sprinkleDiagonal(Game game, int count) {
        int[][] spots = {
                {160, 120}, {460, 140}, {660, 170}, {260, 200},
                {360, 170}, {560, 140}
        };
        int actual = 0;
        for (int i = 0; i < count && i < spots.length; i++) {
            int x = spots[i][0];
            int y = spots[i][1];
            game.addEntity(new DiagonalShooterAlienEntity(game, x, y));
            actual++;
        }
        return actual;
    }

    /** 혼합 편성(중후반용) */
    private static int spawnMixed(Game game, ShipEntity ship) {
        int count = 0;
        // 중앙은 기본 외계인 격자
        count += spawnGrid(game, 5, 11, 90, 50, 52, 28);
        // 상단·측면에 특수 병종
        count += sprinkleRanged(game, ship, 5);
        count += sprinkleDiagonal(game, 5);
        // 인질 약간
        game.addEntity(new HostageEntity(game, 120, 40));
        game.addEntity(new HostageEntity(game, 680, 40));
        return count;
    }

    /** 스테이지5: 보스 단독 */
    private static int spawnBossOnly(Game game, ShipEntity ship) {
        // 필요하면 인트로 연출용으로 아래에 잔챙이 약간 섞어도 됨 (지금은 보스만)
        game.addEntity(new BossEntity(game, 360, 60, ship));
        return 1;
    }
}