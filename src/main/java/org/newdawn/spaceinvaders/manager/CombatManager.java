package org.newdawn.spaceinvaders.manager;

import org.newdawn.spaceinvaders.Game;
import org.newdawn.spaceinvaders.entity.base.Entity;
import org.newdawn.spaceinvaders.entity.enemy.AlienEntity;
import org.newdawn.spaceinvaders.entity.enemy.DiagonalShooterAlienEntity;
import org.newdawn.spaceinvaders.entity.enemy.RangedAlienEntity;
import org.newdawn.spaceinvaders.entity.player.ShipEntity;

import java.util.List;

/*
 *전투 관련 로직 모음 클래스임
 */
public class CombatManager {
    private final Game game;
    private final EntityManager entityManager;
    private final EntitySpawnManager spawnManager;

    public CombatManager(Game game, EntityManager entityManager, EntitySpawnManager spawnManager) {
        this.game = game;
        this.entityManager = entityManager;
        this.spawnManager = spawnManager;
    }

    public void onAlienKilled(AlienEntity alien) {

        payXpForKill(alien); //XP 지급, 계산
        removeKilledAlien(alien); // 엔티티 제거
        updateScoreAndCount(); // 점수/카운트, dangerMode 갱신
        aliensCleared(); // 웨이브, 보스, 승리 처리
        speedUpAliens(); //남은 적 속도 증가

    }

    //XP 지급
    private void payXpForKill(AlienEntity alien) {
        int xp = calculateXpForAlien(alien);

        ShipEntity player = game.getPlayerShip();
        if(player != null) {
            player.getStats().addXp(xp);
        }
    }

    //Xp 계산
    private int calculateXpForAlien(AlienEntity alien) {
        if(alien instanceof RangedAlienEntity) {
            return 8; //RangedAlienEntity 처치시 8
        }
        if(alien instanceof DiagonalShooterAlienEntity){
            return 10; //DiagonalShooterAlienEntity 처치시 10 지급
        }
        return 5; // 기본Alien 지급 xp
    }

    //엔티티 제거
    private void removeKilledAlien(AlienEntity alien) {
        List<Entity> entities = entityManager.getMutableEntities();
        if(entities.contains(alien)) {
            entityManager.removeEntity(alien);
        }
    }

    //점수/카운트, dangerMode 갱신
    private void updateScoreAndCount(){
        game.addScore(100);
        game.decrementAlienCount();

        //디버깅 메시지
        System.out.println("[DEBUG] Alien killed! alienCount=" + game.getAlienCount() + " stage=" + game.getCurrentStageId() + " bossActive=" + game.isBossActive());

        ShipEntity player = game.getPlayerShip();
        if(player != null) {
            boolean dangerous = (player.getStats().getCurrentHP() < 1);
            game.setDangerMode(dangerous);
        }
    }

    //Alien을 전부 처리하였는지에 따른 처리(웨이브 / 보스 / 승리 통합 처리)
    private void aliensCleared() {
        if(game.getAlienCount() != 0) {
            return;
        }

        if(game.isInfiniteMode()) {
            aliensClearedInInfiniteMode();
        } else {
            aliensClearedInStageMode();
        }
    }

    //무한모드일때
    private void aliensClearedInInfiniteMode() {
        if(game.isBossActive()) {
            return; //보스전에는 해당 없음
        }

        spawnManager.spawnAliensForInfiniteMode();
    }

    private void aliensClearedInStageMode() {
        if(game.isBossActive()) {
            return; // 보스 처리는 Game.onBossKilled() 에서 처리
        }

        if(game.getCurrentStageId() == 5) {
            spawnManager.spawnBoss();
        } else {
            game.notifyWin();
        }
    }

    //남은 Alien들 속도 증가
    private void speedUpAliens() {
        for(Entity entity : entityManager.getMutableEntities()) {
            if(entity instanceof AlienEntity) {
                entity.setHorizontalMovement(entity.getHorizontalMovement() * 1.02);
            }
        }
    }

}
