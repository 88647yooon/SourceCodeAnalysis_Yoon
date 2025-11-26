package org.newdawn.spaceinvaders.entity.boss;

import org.newdawn.spaceinvaders.Game;
import org.newdawn.spaceinvaders.graphics.Sprite;
import org.newdawn.spaceinvaders.graphics.SpriteStore;
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

        // 스프라이트/렌더
        private Sprite[] frames = new Sprite[3];
        private long lastFrameChange = 0;
        private int frameNumber = 0;

        // 체력/페이즈
        private static final int BOSS_MAX_HP = 50;
        private int phase = -1; // 0,1,2

        // 화면 경계
        private static final int BOSS_W = 120;
        private static final int BOSS_H = 120;
         //화면폭 - 보스폭 - 여유

        //페이즈 전환 공통인프라
        private long phaseIntroMs = 1500;    // 전환 후 쉬는시간
        private long phaseHoldUntil = 0;     // 이 시각 전까지는 어떤 공격 패턴도 실행하지 않음

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

            setMaxHP(BOSS_MAX_HP);

            frames[0] = SpriteStore.get().getSprite("sprites/boss1.png");
            frames[1] = SpriteStore.get().getSprite("sprites/boss2.png");
            frames[2] = SpriteStore.get().getSprite("sprites/boss3.png");
            sprite = frames[0];

            // 초기 이동
            setHorizontalMovement(50);
            setVerticalMovement(10);

            //초기 페이즈 설정
            enterPhase(0);
        }
    @Override
    public void move(long delta) {
        this.now = System.currentTimeMillis(); // 공유 시간 업데이트

        processTasks();                 // 1. 예약된 태스크 실행
        updateAnimation();              // 2. 애니메이션 프레임 업데이트
        handleMovementAndBoundaries();  // 3. 이동 및 화면 경계 처리
        checkPhaseTransition();         // 4. HP 기반 페이즈 전환 체크
        executeStrategy(delta);         // 5. 현재 공격 패턴 실행

        super.move(delta);              // 6. 물리 이동 적용 (AlienEntity)
    }

    private void processTasks() {
        while (!tasks.isEmpty() && tasks.peek().executeAt <= now) {
            tasks.poll().r.run();
        }
    }

    private void updateAnimation() {
        long frameDuration = 120;
        if (now - lastFrameChange > frameDuration) {
            lastFrameChange = now;
            frameNumber = (frameNumber + 1) % frames.length;
            sprite = frames[frameNumber];
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
        if (phase == 0 && getCurrentHP() <= (BOSS_MAX_HP * 2) / 3) {
            enterPhase(1);
        } else if (phase == 1 && getCurrentHP() <= BOSS_MAX_HP / 3) {
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
        // 1. 보스 본체
        g.drawImage(sprite.getImage(), (int)x, (int)y, BOSS_W, BOSS_H, null);

        // 2. HP 바 (헬퍼 메소드 분리)
        drawHealthBar(g);

        // 3. 패턴 그래픽
        if (currentStrategy != null) {
            currentStrategy.draw((Graphics2D)g, this);
        }

        // 4. 페이즈 전환 깜빡임 (헬퍼 메소드 분리)
        if (now < phaseHoldUntil) {
            drawBlinkEffect((Graphics2D)g);
        }
    }

    private void drawHealthBar(Graphics g) {
        int w = BOSS_W;
        int h = 6;
        int bw = (int)Math.max(0, Math.round((getCurrentHP() / (double)BOSS_MAX_HP) * w));
        g.setColor(Color.RED);
        g.fillRect((int)x, (int)y - 10, bw, h);
        g.setColor(Color.DARK_GRAY);
        g.drawRect((int)x, (int)y - 10, w, h);
    }

    private void drawBlinkEffect(Graphics2D g2) {
        Stroke oldS = g2.getStroke();
        float a = 0.4f + 0.6f * (float)Math.abs(Math.sin((now % 600) / 600.0 * Math.PI * 2));
        g2.setColor(new Color(255, 255, 0, (int)(a * 255)));
        g2.setStroke(new BasicStroke(3f));
        g2.drawRect((int)x, (int)y, BOSS_W, BOSS_H);
        g2.setStroke(oldS);
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
            game.removeEntity(this);
            game.onBossKilled(); //보스 전용 사망 처리 호출
        }
    }

    @Override
    public void collidedWith(Entity other) {
        //ShotEntity와의 충돌 로직은 wasHitBy로 이동했으므로 제거
    }

    //전략 클래스가 사용할 수 있도록 탄환 생성로직을 public으로 공개
    public void spawnShot(double x, double y, double vx, double vy,double speed){
        game.addEntity(new EnemyShotEntity(game, "sprites/enemy_bullet.png", x, y, vx, vy));
    }

    // 폭탄 전략이 Entity를 반환받아 제거할 때 사용 (BombStrategy에서 필요)
    public Entity spawnShotEntity(double x, double y, double vx, double vy, double speed) {
        EnemyShotEntity shot = new EnemyShotEntity(game, "sprites/enemy_bullet.png", x, y, vx, vy);
        game.addEntity(shot);
        return shot;
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