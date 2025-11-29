package org.newdawn.spaceinvaders.entity.boss;

import org.newdawn.spaceinvaders.Game;
import org.newdawn.spaceinvaders.entity.base.EnemyEntity;
import org.newdawn.spaceinvaders.entity.base.Entity;
import org.newdawn.spaceinvaders.entity.player.ShipEntity;
import org.newdawn.spaceinvaders.entity.projectile.EnemyShotEntity;
import org.newdawn.spaceinvaders.entity.projectile.ShotEntity;

import java.awt.*;
import java.util.Comparator;
import java.util.PriorityQueue;

/**
 * 보스 엔티티: 페이즈 기반 상태머신 + 패턴 혼합 (개선 버전)
 * - 화면 이탈 방지(X 경계 반전)
 * - 내부 태스크 큐 기반 지연 실행(스레드 미사용)
 * - 레이저를 유한 길이 선분 판정으로 변경
 */
public class BossEntity extends EnemyEntity {
        private final Game game;
        private final ShipEntity player;

        //현재 공격 전략
        private BossAttackStrategy currentStrategy;

        private final BossVisualComponent visualComponent;

        private int phase = -1;
        private static final int BOSS_W = 120;
        private static final int BOSS_H = 120;
        private long phaseIntroMs = 1500;
        private long phaseHoldUntil = 0;
        private long now;

        //  내부 지연 실행용 태스크 큐
        private static class Task {
            final long executeAt;
            final Runnable r;
            Task(long t, Runnable r) { this.executeAt = t; this.r = r; }
        }
        private final PriorityQueue<Task> tasks =
                new PriorityQueue<>(Comparator.comparingLong(t -> t.executeAt));

        public BossEntity(Game game, int x, int y, ShipEntity player) {
            super("sprites/boss1.png",x, y);
            this.game = game;
            this.player = player;

            int atkLv = player.getStats().getSkills().atkLv;
            int scaledHP = 150 + (atkLv * 50);

            setMaxHP(scaledHP);

            this.visualComponent = new BossVisualComponent(this);

            setHorizontalMovement(50);
            setVerticalMovement(10);
            enterPhase(0);
        }
    @Override
    public void move(long delta) {
        this.now = System.currentTimeMillis(); // 공유 시간 업데이트

        processTasks();
        this.sprite = visualComponent.updateAnimation(now);

        handleMovementAndBoundaries();
        checkPhaseTransition();
        executeStrategy(delta);

        super.move(delta);
    }

    private void processTasks() {
        while (!tasks.isEmpty() && tasks.peek().executeAt <= now) {
            tasks.poll().r.run();
        }
    }

    private void handleMovementAndBoundaries() {
        int minX =20;
        int maxX = 780 - BOSS_W;
        // Y축 경계 (위아래로 튕기기)
        if (getY() > 140) {
            setVerticalMovement(-40);
        } else if (getY() < 40) {
            setVerticalMovement(40);
        }

        // X축 경계 (좌우 반전)
        double vx = getHorizontalMovement();
        if ((x <= minX && vx < 0) || (x >= maxX && vx > 0)) {
            setHorizontalMovement(-vx); // 부호 반전으로 간단히 처리
        }
    }

    private void checkPhaseTransition() {
        if (phase == 0 && getCurrentHP() <= (getMaxHP() * 2) / 3) {
            enterPhase(1);
        } else if (phase == 1 && getCurrentHP() <= getMaxHP() / 3) {
            enterPhase(2);
        }
    }

    private void executeStrategy(long delta) {
        boolean phaseUnlocked = now >= phaseHoldUntil;
        if (phaseUnlocked && currentStrategy != null) {
            currentStrategy.update(this, delta);
        }
    }

    //페이즈 전환 로직
    private void enterPhase(int next) {
        if(phase == next) return;
        phase = next;

        setHorizontalMovement(50);
        setVerticalMovement(10);

        tasks.clear(); //이전 예약 작업 제거

        if(phase ==0){
            phaseHoldUntil = 0;
        } else{
            phaseHoldUntil = System.currentTimeMillis() + phaseIntroMs;
        }
       updatePhaseStrategy();


    }

    private void updatePhaseStrategy(){

        switch(phase) {
            case 0:
                //페이즈1 폭탄 낙하
                BossStrategyBomb bombStrategy = new BossStrategyBomb();
                bombStrategy.initializeTimer();
                this.currentStrategy = bombStrategy;


                break;
            case 1:
                //페이즈2 텔레포트 + 미니언 소환
                this.currentStrategy = new BossStrategyComposite(
                        new BossStrategyTeleport(),
                        new BossStrategyMinionSpawn()
                );
                break;
            case 2:
                //페이즈 3: 스월 + 레이저
                this.currentStrategy = new BossStrategyComposite(
                        new BossStrategySwirl(),
                        new BossStrategyLaser()
                );
                break;
            default:
                this.currentStrategy = null;
        }
    }
    @Override
    public void draw(Graphics g) {

        if (currentStrategy != null) {
            currentStrategy.draw((Graphics2D)g, this);
        }
        //UI/이펙트 그리기 위임
        visualComponent.draw((Graphics2D)g,x,y, now, phaseHoldUntil);
    }



    @Override
    public void wasHitBy(ShotEntity shot){
            //페이즈 전환 중 무적
        if(System.currentTimeMillis() < phaseHoldUntil){
            return;
        }
        // 데미지 적용(EnemyEntity의 takeDamage 사용)
        boolean isDead = takeDamage(shot.getDamage());

        //사망 처리
        if(isDead){
            game.getEntityManager().removeEntity(this);
            game.onBossKilled(); //보스 전용 사망 처리 호출
        }
    }

    @Override
    public void collidedWith(Entity other) {
        //ShotEntity와의 충돌 로직은 wasHitBy로 이동했으므로 제거
    }

    //전략 클래스가 사용할 수 있도록 탄환 생성로직을 public으로 공개
    public void spawnShot(double x, double y, double vx, double vy,double speed){
        game.getEntityManager().addEntity(new EnemyShotEntity(game, "sprites/enemy_bullet.png", x, y, vx, vy));
    }


    public void spawnAngleShot(double sx, double sy, double angleDeg, double speed) {
        double rad = Math.toRadians(angleDeg);
        double dx = Math.cos(rad) * speed;
        double dy = Math.sin(rad) * speed;
        spawnShot(sx, sy, dx, dy, speed);
    }

    // 지연 실행 스케줄러 공개
    public void schedule(long delayMs, Runnable r) {
        tasks.add(new Task(System.currentTimeMillis() + delayMs, r));
    }


    // 플레이어 각도 계산
    public double angleTo(ShipEntity target) {
        double px = target.getX() + target.getWidth() / 2.0;
        double py = target.getY() + target.getHeight() / 2.0;
        double cx = this.x + BOSS_W / 2.0;
        double cy = this.y + BOSS_H / 2.0;
        return Math.toDegrees(Math.atan2(py - cy, px - cx));
    }

    public ShipEntity getTarget() { return player; }
    public Game getGame() { return game; }

}