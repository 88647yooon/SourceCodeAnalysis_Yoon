package org.newdawn.spaceinvaders.entity;

import org.newdawn.spaceinvaders.Game;
import org.newdawn.spaceinvaders.Sprite;
import org.newdawn.spaceinvaders.SpriteStore;

import java.awt.*;
import java.util.Comparator;
import java.util.PriorityQueue;

/**
 * 보스 엔티티: 페이즈 기반 상태머신 + 패턴 혼합 (개선 버전)
 * - 화면 이탈 방지(X 경계 반전)
 * - 내부 태스크 큐 기반 지연 실행(스레드 미사용)
 * - 레이저를 유한 길이 선분 판정으로 변경
 */
public class BossEntity extends AlienEntity {
        private final Game game;
        private final ShipEntity player;

        //현재 공격 전략
        private BossAttackStrategy currentStrategy;

        // 스프라이트/렌더
        private Sprite sprite;
        private Sprite[] frames = new Sprite[3];
        private long lastFrameChange = 0;
        private long frameDuration = 120;
        private int frameNumber = 0;

        // 체력/페이즈
        private static final int BOSS_MAX_HP = 50;
        private int phase = -1; // 0,1,2

        // 화면 경계
        private static final int BOSS_W = 120;
        private static final int BOSS_H = 120;
        private final int minX =20;
        private final int maxX = 780 - BOSS_W; //화면폭 - 보스폭 - 여유

        //페이즈 전환 공통인프라
        private long phaseIntroMs = 1500;    // 전환 후 쉬는시간
        private long phaseHoldUntil = 0;     // 이 시각 전까지는 어떤 공격 패턴도 실행하지 않음


        //  내부 지연 실행용 태스크 큐
        private static class Task {
            final long executeAt;
            final Runnable r;
            Task(long t, Runnable r) { this.executeAt = t; this.r = r; }
        }
        private final PriorityQueue<Task> tasks =
                new PriorityQueue<>(Comparator.comparingLong(t -> t.executeAt));

        public BossEntity(Game game, int x, int y, ShipEntity player) {
            super(game,"sprites/boss1.png",x, y);
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
    //보스 이동 로직
    @Override
    public void move(long delta) {
        long now = System.currentTimeMillis();

        // 1.내부 태스트 실행
        while (!tasks.isEmpty() && tasks.peek().executeAt <= now) {
            tasks.poll().r.run();
        }

        // 2.애니메이션 프레임 전환
        if (now - lastFrameChange > frameDuration) {
            lastFrameChange = now;
            frameNumber = (frameNumber + 1) % frames.length;
            sprite = frames[frameNumber];
        }

        // 3.Y경계 유지
        if (getY() > 140) {
            setVerticalMovement(-40);
        } else if (getY() < 40) {
            setVerticalMovement(40);
        }

        // 4. X경계 반전
        double vx = getHorizontalMovement();
        if (x <= minX && vx < 0) {
            setHorizontalMovement(Math.abs(vx));
        } else if (x >= maxX && vx > 0) {
            setHorizontalMovement(-Math.abs(vx));
        }

        // 5.페이즈 전환 체크(HP기준)
        if (phase == 0 && getCurrentHP() <= (BOSS_MAX_HP * 2) / 3) enterPhase(1);
        if (phase == 1 && getCurrentHP() <= BOSS_MAX_HP / 3)     enterPhase(2);

        // 6. 페이즈 실행 위임
        boolean phaseUnlocked = now >= phaseHoldUntil;
        if (phaseUnlocked && currentStrategy != null) {
            currentStrategy.update(this,delta);
        }

        // 7.실제 이동
        super.move(delta);
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

        //페이즈에 따른 전략 객체 생성및 할당
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
                        new BossStrategyMinionSpawn(this)
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
        // 1.보스 본체 그리기
        g.drawImage(sprite.getImage(), (int)x,(int)y,BOSS_W,BOSS_H,null);

        // 2.HP 바
        int w = BOSS_W;
        int h = 6;
        int bw = (int)Math.max(0, Math.round((getCurrentHP()/(double)BOSS_MAX_HP)*w));
        g.setColor(Color.RED);
        g.fillRect((int)x,(int)y-10,bw,h);
        g.setColor(Color.DARK_GRAY);
        g.drawRect((int)x,(int)y-10,w,h);

        //3. 패턴 특수 그래픽 그리기 위임(그림자, 레이저)
        if(currentStrategy !=null){
            currentStrategy.draw((Graphics2D)g,this);
        }

        //4. 페이즈 전환 중 보스 테두리 깜빡임
        long now = System.currentTimeMillis();
        if(now < phaseHoldUntil) {
            Graphics2D g2 = (Graphics2D)g;
            Stroke oldS = g2.getStroke();
            float a = 0.4f + 0.6f * (float)Math.abs(Math.sin((now % 600) / 600.0 * Math.PI * 2));
            g2.setColor(new Color(255, 255, 0, (int)(a * 255)));
            g2.setStroke(new BasicStroke(3f));
            g2.drawRect((int)x, (int)y, BOSS_W, BOSS_H);
            g2.setStroke(oldS);
        }
    }

    //피격 로직
    @Override
    public void collidedWith(Entity other) {
        if (other instanceof ShotEntity) {
            ShotEntity shot = (ShotEntity) other;

            // 탄은 제거
            game.removeEntity(other);

            // 페이즈 전환 무적 (데미지 무시)
            if (System.currentTimeMillis() < phaseHoldUntil) {
                return;
            }

            // 데미지 적용
            boolean dead = takeDamage(shot.getDamage());

            if (dead) {
                game.removeEntity(this);
                game.onBossKilled();
            }
        }
    }

    //전략 클래스가 사용할 수 있도록 탄환 생성로직을 public으로 공개
    public void spawnShot(double x, double y, double vx, double vy,double speed){
        game.addEntity(new EnemyShotEntity(game, "sprites/enemy_bullet.png", x, y, vx, vy, speed));
    }

    // 폭탄 전략이 Entity를 반환받아 제거할 때 사용 (BombStrategy에서 필요)
    public Entity spawnShotEntity(double x, double y, double vx, double vy, double speed) {
        EnemyShotEntity shot = new EnemyShotEntity(game, "sprites/enemy_bullet.png", x, y, vx, vy, speed);
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

    public void teleportTo(int nx, int ny) {
        this.x = nx; this.y = ny;
        setHorizontalMovement(0);
        setVerticalMovement(0);
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