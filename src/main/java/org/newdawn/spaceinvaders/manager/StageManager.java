package org.newdawn.spaceinvaders.manager;

import org.newdawn.spaceinvaders.Game;
import org.newdawn.spaceinvaders.entity.base.Entity;
import org.newdawn.spaceinvaders.entity.player.ShipEntity;

import java.util.List;

public final class StageManager {

    private StageManager() {}

    public static void applyStage(int stageNum, Game game) {
        // 1. 초기화 (플레이어만 남기기)
        ShipEntity ship = game.getPlayerShip();
        List<Entity> list = game.getMutableEntities();
        list.removeIf(e -> e != ship);

        ship.teleportTo(370, 550);
        ship.setHorizontalMovement(0);
        ship.setVerticalMovement(0);

        EntitySpawnManager spawner = game.getSpawnManager();
        int count = 0;

        // 2. 스테이지별 "주문서" (행, 열, 대각선확률, 원거리확률)
        switch (stageNum) {
            case 1:
                // 일반 적만 100% (대각선 0, 원거리 0)
                count = spawner.spawnUniversalGrid(5, 10, 0.0, 0.0);
                break;

            case 2:
                // 일반 + 공격(원거리) 30% 섞기
                count = spawner.spawnUniversalGrid(5, 12, 0.0, 0.3);
                break;

            case 3:
                // 일반 + 대각선 30% 섞기
                count = spawner.spawnUniversalGrid(6, 12, 0.3, 0.0);
                break;

            case 4:
                // 전부 다 섞기 (대각선 20%, 원거리 20%, 나머지 60% 일반)
                count = spawner.spawnUniversalGrid(5, 12, 0.2, 0.2);
                break;

            case 5:
                // 보스전 (보스만 소환)
                spawner.spawnBoss();
                count = 0; // 보스는 alienCount에 포함 안 하거나 1로 하거나 게임 규칙에 따름
                break;

            default:
                // 기본
                count = spawner.spawnUniversalGrid(5, 10, 0.0, 0.0);
        }

        game.setAlienCount(count);
    }
}