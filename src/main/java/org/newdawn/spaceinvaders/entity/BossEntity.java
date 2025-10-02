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
public class BossEntity extends Entity {
    private final Game game;
    private final ShipEntity player;

    // 스프라이트/렌더
    private Sprite sprite;
    private Sprite[] frames = new Sprite[3];
    private long lastFrameChange = 0;
    private long frameDuration = 120;
    private int frameNumber = 0;

    // 체력/페이즈
    private int maxHP = 50;
    private int hp = maxHP;
    private int phase = 0; // 0,1,2

    // 공통 타이머
    private long now;
    private long lastFireAt = 0;
    private long lastTeleportAt = 0;
    private long lastSpawnAt = 0;
    private long phaseStartAt = 0;

    // 패턴 파라미터(스월)
    private double swirlAngle = 0.0;
    private double swirlRotationSpeed = 60; // deg/sec
    private int swirlBullets = 12;
    private long swirlInterval = 650; // ms
    private double swirlBulletSpeed = 120;

    // 텔레포트/버스트
    private long teleportCooldown = 2400;
    private Point[] teleportPoints = {
            new Point(40, 60), new Point(260, 60), new Point(480, 60),
            new Point(40, 180), new Point(260, 180), new Point(480, 180)
    };
    private int burstCount = 5;     // 텔레포트 직후 플레이어 조준 버스트
    private long burstGapMs = 120;  // 버스트 간격

    // 미니언 소환
    private long spawnCooldown = 6500;

    // 레이저(경고선 + 본체)
    private boolean laserActive = false;
    private long laserChargeStart = 0; // 경고 시간 시작
    private long laserChargeMs = 900;
    private long laserDurationMs = 900;
    private double laserAngleDeg = 90;
    private int laserWidthPx = 14;
    private int laserLengthPx = 900; //  유한 길이(화면 끝까지X)

    // 발사 스프라이트
    private static final String ENEMY_BULLET = "sprites/enemy_bullet.png";
    private static final String BOSS_SPRITE = "sprites/boss1.png";

    // 보스 크기 (그림 + 판정 영역)
    private static final int BOSS_W = 120;
    private static final int BOSS_H = 120;

    //  화면 경계(가로) — 프로젝트 해상도에 맞춰 필요시 조정
    //   Game에 화면 폭을 제공하는 메서드가 있다면 그걸로 대체하세요.
    private final int minX = 20;
    private final int maxX; // (화면폭 - 보스폭 - 여유)
    private final int screenWidthFallback = 640; // 없으면 640 기준
    private final int marginX = 20;

    // --- 페이즈 전환 그레이스 타임(패턴 잠금) ---
    private long phaseIntroMs = 1500;    // 전환 후 쉬는 시간(ms) — 원하면 1200~2000 사이로 조절
    private long phaseHoldUntil = 0;     // 이 시각 전까지는 어떤 공격 패턴도 실행하지 않음


    // --- Phase 0: 낙하 폭탄(이펙트 포함) ---
    private long   p0BombIntervalMs   = 1600;
    private long   p0LastBombAt       = 0;
    private int    p0BombsPerVolley   = 2;
    private double p0BombFallSpeed    = 120;  // px/s
    private int    p0ExplodeY;
    private int    p0ShardCount       = 8;
    private double p0ShardSpeed       = 220;
    private long   p0ShadowBlinkMs    = 900;  // 마지막 경고 깜빡임 구간(폭발 직전 ms)
    // 필드에 범위 값 추가
    private int p0ExplodeYMin = 300;  // 최소 터지는 높이
    private int p0ExplodeYMax = 520;  // 최대 터지는 높이

    // 폭탄 착지 경고(그림자) 이펙트용
    private static class Shadow {
        final double x; final int y; final long startAt; final long explodeAt;
        Shadow(double x, int y, long startAt, long explodeAt) {
            this.x = x; this.y = y; this.startAt = startAt; this.explodeAt = explodeAt;
        }
    }
    private final java.util.List<Shadow> shadows = new java.util.ArrayList<>();



    // Phase 2 스월 간격(기본 320ms → 완화 )
    private long p2SwirlIntervalMs = 900;

    //  내부 지연 실행용 태스크 큐(스레드 미사용)
    private static class Task {
        final long executeAt;
        final Runnable r;
        Task(long t, Runnable r) { this.executeAt = t; this.r = r; }
    }
    private final PriorityQueue<Task> tasks =
            new PriorityQueue<>(Comparator.comparingLong(t -> t.executeAt));

    public BossEntity(Game game, int x, int y, ShipEntity player) {
        super(BOSS_SPRITE, x, y);
        this.game = game;
        this.player = player;

        frames[0] = SpriteStore.get().getSprite(BOSS_SPRITE);
        frames[1] = SpriteStore.get().getSprite("sprites/boss2.png");
        frames[2] = SpriteStore.get().getSprite("sprites/boss3.png");
        sprite = frames[0];

        phaseStartAt = System.currentTimeMillis();

        // 초기 이동(천천히 부유)
        setHorizontalMovement(50);
        setVerticalMovement(10);

        // 화면 폭 추정(있으면 사용, 없으면 폴백)
        int sw = screenWidthFallback;
        try {
            // Game에 가로폭 접근자가 있을 경우 사용(없으면 예외 무시)
            sw = (int) Game.class.getMethod("getScreenWidth").invoke(game);
        } catch (Exception ignored) {}
        maxX = sw - BOSS_W - marginX;
    }

    @Override
    public void draw(Graphics g) {
        // 보스 그리기
        g.drawImage(sprite.getImage(), (int)x, (int)y, BOSS_W, BOSS_H, null);

        // HP 바
        g.setColor(Color.RED);
        int w = BOSS_W;
        int h = 6;
        int bw = (int)Math.max(0, Math.round((hp/(double)maxHP) * w));
        g.fillRect((int)x, (int)y - 10, bw, h);
        g.setColor(Color.DARK_GRAY);
        g.drawRect((int)x, (int)y - 10, w, h);

        //  폭탄 착지 경고(그림자) 이펙트
        if (!shadows.isEmpty()) {
            Graphics2D g2 = (Graphics2D) g;
            long t = now;

            for (int i = 0; i < shadows.size(); i++) {
                Shadow s = shadows.get(i);
                if (t >= s.explodeAt) continue;  // 이미 폭발 끝난 경고는 그리지 않음

                long remain = s.explodeAt - t;
                // 기본: 반투명 검은 원(점점 진해짐)
                float alpha = (float) Math.min(1.0, (double) (t - s.startAt) / Math.max(1, (s.explodeAt - s.startAt)));
                alpha = 0.2f + 0.6f * alpha; // 0.2 → 0.8
                g2.setColor(new Color(0f, 0f, 0f, alpha));

                int r = 12; // 그림자 반경
                g2.fillOval((int) s.x - r, s.y - r, r * 2, r * 2);

                // 폭발 직전 깜빡임: 빨간 링(반짝)
                if (remain <= p0ShadowBlinkMs) {
                    // 0~1 깜빡임 파형
                    double phase = (remain / 100.0); // 10Hz-ish
                    double pulse = 0.5 * (1 + Math.sin(phase));
                    float a = (float) (0.4 + 0.6 * pulse); // 0.4~1.0
                    g2.setColor(new Color(1f, 0.2f, 0.2f, a));
                    g2.setStroke(new BasicStroke(2f));
                    g2.drawOval((int) s.x - (r + 4), s.y - (r + 4), (r + 4) * 2, (r + 4) * 2);
                }
            }
        }

        // 레이저 경고선/본체(유한 길이)
        if (laserActive || (laserChargeStart > 0 && now - laserChargeStart < laserChargeMs)) {
            Graphics2D g2 = (Graphics2D) g;
            Stroke old = g2.getStroke();

            if (laserActive) {
                g2.setColor(new Color(255, 60, 60, 200));
                g2.setStroke(new BasicStroke(laserWidthPx));
            } else {
                float[] dash = { 6f, 6f };
                g2.setColor(new Color(255, 230, 80, 180));
                g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, dash, 0));
            }

            double rad = Math.toRadians(laserAngleDeg);
            int cx = (int) (x + BOSS_W/2);
            int cy = (int) (y + BOSS_H/2);
            int ex = (int) (cx + Math.cos(rad) * laserLengthPx);
            int ey = (int) (cy + Math.sin(rad) * laserLengthPx);
            g2.drawLine(cx, cy, ex, ey);
            g2.setStroke(old);
        }

        // ▶ 페이즈 전환 중이면 보스 테두리 깜빡임(선택)
        if (now < phaseHoldUntil) {
            Graphics2D g2 = (Graphics2D) g;
            Stroke oldS = g2.getStroke();
            float a = 0.4f + 0.6f * (float)Math.abs(Math.sin((now % 600) / 600.0 * Math.PI * 2));
            g2.setColor(new Color(255, 255, 0, (int)(a * 255)));
            g2.setStroke(new BasicStroke(3f));
            g2.drawRect((int)x, (int)y, BOSS_W, BOSS_H);
            g2.setStroke(oldS);
        }
    }

    @Override
    public void move(long delta) {
        now = System.currentTimeMillis();

        // (0) 내부 태스크 실행(지연 실행 큐)
        while (!tasks.isEmpty() && tasks.peek().executeAt <= now) {
            tasks.poll().r.run();
        }

        // (A) 애니메이션 프레임 전환
        if (now - lastFrameChange > frameDuration) {
            lastFrameChange = now;
            frameNumber = (frameNumber + 1) % frames.length;
            sprite = frames[frameNumber];
        }

        // (B) 상단 영역 유지용 Y 제한
        if (getY() > 140) {
            setVerticalMovement(-40);
        } else if (getY() < 40) {
            setVerticalMovement(40);
        }

        //  (B-2) X 경계 반전으로 화면 이탈 방지
        double vx = getHorizontalMovement();
        if (x <= minX && vx < 0) {
            setHorizontalMovement(Math.abs(vx));
        } else if (x >= maxX && vx > 0) {
            setHorizontalMovement(-Math.abs(vx));
        }

        //  페이즈 전환 (퍼센트 기준)
        if (phase == 0 && hp <= (maxHP * 2) / 3) enterPhase(1);
        if (phase == 1 && hp <= (maxHP) / 3)     enterPhase(2);

        //  그레이스 타임 체크
        boolean phaseUnlocked = now >= phaseHoldUntil;

        //  패턴 실행
        if (phaseUnlocked) {
            switch (phase) {
                case 0:
                    doPhase0Bombs();        // 낙하 폭탄 + 파편
                    break;
                case 1:
                    doTeleportBurst();
                    doMinionSpawn();
                    break;
                case 2:
                    doSwirl(delta, swirlBullets + 4, swirlRotationSpeed * 1.6, p2SwirlIntervalMs, swirlBulletSpeed + 40);
                    doLaserSweep(2800);
                    break;
            }
        }

        //  마지막에 실제 이동
        super.move(delta);
    }

    @Override
    public void doLogic() { /* 보스는 별도 없음 */ }

    @Override
    public boolean collidesWith(Entity other) {
        // 120x120 hitbox 기준
        Rectangle me = new Rectangle((int)x, (int)y, BOSS_W, BOSS_H);
        Rectangle him = new Rectangle(other.getX(), other.getY(),
                other.sprite.getWidth(), other.sprite.getHeight());
        return me.intersects(him);
    }

    private void enterPhase(int next) {
        phase = next;
        phaseStartAt = now;
        // ▶ 전환 후 일정 시간 공격 금지
        phaseHoldUntil = now + phaseIntroMs;

        // ▶ 진행 중이던 패턴/타이머 초기화(즉시 공격 방지)
        laserActive = false;
        laserChargeStart = 0;

        lastFireAt = now;       // 탄막 타이머 리셋
        lastTeleportAt = now;   // 텔레포트/버스트 타이머 리셋
        lastSpawnAt = now;      // 소환 타이머 리셋

        tasks.clear();          // 이전 페이즈에서 예약된 지연 태스크 제거(유효탄 방지)
    }

    /* ------------------------------
       패턴 1: 스월(회전 탄막)
       ------------------------------ */
    private void doSwirl(long delta, int bulletsPerWave, double rotationSpeedDegPerSec, long intervalMs, double bulletSpeed) {
        swirlAngle += rotationSpeedDegPerSec * (delta / 1000.0);
        if (now - lastFireAt >= intervalMs) {
            lastFireAt = now;

            for (int i = 0; i < bulletsPerWave; i++) {
                double angle = swirlAngle + (360.0 * i / bulletsPerWave);
                spawnAngleShot(x + BOSS_W/2.0, y + BOSS_H/2.0, angle, bulletSpeed);
            }
        }
    }

    /* -----------------------------------------
       패턴 2: 텔레포트 + 플레이어 조준 버스트
       (내부 태스크 큐 사용: 스레드 미사용)
       ----------------------------------------- */
    private void doTeleportBurst() {
        if (now - lastTeleportAt >= teleportCooldown) {
            lastTeleportAt = now;

            // 무작위 텔레포트 위치
            Point p = teleportPoints[(int)(Math.random() * teleportPoints.length)];
            this.x = p.x;
            this.y = p.y;

            // 텔레포트 직후 버스트(플레이어 조준)
            final double angleToPlayer = angleTo(playerCenterX(), playerCenterY());
            for (int i = 0; i < burstCount; i++) {
                final int delay = (int) (i * burstGapMs);
                schedule(delay, () ->
                        spawnAngleShot(x + BOSS_W/2.0, y + BOSS_H/2.0, angleToPlayer, 260));
            }
        }
    }

    /* ------------------------
       패턴 3: 미니언 소환
       ------------------------ */
    private void doMinionSpawn() {
        if (now - lastSpawnAt >= spawnCooldown) {
            lastSpawnAt = now;

            game.addEntity(new RangedAlienEntity(game, (int)(x - 30), (int)(y + 120), player));
            game.addEntity(new DiagonalShooterAlienEntity(game, (int)(x + 60), (int)(y + 120)));

            //보스가 잡몹소환시 alienCount도 또한 같이 증가하도록 바꿈
            game.setAlienCount(game.getAlienCount() + 2);
        }
    }

    /* ------------------------
       패턴 5: 레이저 스윕 (라이트 버전)
       ------------------------ */
    private void doLaserSweep(long cycleMs) {
        long t = now - phaseStartAt;
        long mod = t % cycleMs;

        if (!laserActive && laserChargeStart == 0 && mod < 20) {
            // 새 사이클 시작: 경고 시작
            laserChargeStart = now;
            // 플레이어 방향으로 레이저 설치
            laserAngleDeg = angleTo(playerCenterX(), playerCenterY());
        }

        // 경고 중 → 발사 전환
        if (!laserActive && laserChargeStart > 0 && now - laserChargeStart >= laserChargeMs) {
            laserActive = true;
        }

        // 발사 중
        if (laserActive) {
            if (intersectsLaserSegment(player)) {
                player.damage(1);
            }
            if (now - laserChargeStart >= laserChargeMs + laserDurationMs) {
                laserActive = false;
                laserChargeStart = 0;
            }
        }
    }

    //  유한 길이 선분 레이저 판정
    private boolean intersectsLaserSegment(ShipEntity ship) {
        // 보스 중심
        double cx = x + BOSS_W / 2.0;
        double cy = y + BOSS_H / 2.0;

        // 플레이어 중심
        double px = ship.getX() + ship.sprite.getWidth() / 2.0;
        double py = ship.getY() + ship.sprite.getHeight() / 2.0;

        // 레이저 단위 방향벡터
        double rad = Math.toRadians(laserAngleDeg);
        double lx = Math.cos(rad), ly = Math.sin(rad);

        // 보스→플레이어 벡터를 레이저 축에 투영한 길이 t
        double vx = px - cx, vy = py - cy;
        double t = vx * lx + vy * ly;                 // 투영 길이(스칼라)
        t = Math.max(0, Math.min(t, laserLengthPx));  // 선분으로 클램프

        // 선분상의 최근점
        double qx = cx + lx * t;
        double qy = cy + ly * t;

        // 거리(플레이어 중심 ↔ 레이저 선분)
        double dx = px - qx, dy = py - qy;
        double dist = Math.hypot(dx, dy);

        return dist <= (laserWidthPx / 2.0);
    }

    private double angleTo(double tx, double ty) {
        return Math.toDegrees(Math.atan2(ty - (y + BOSS_H/2.0), tx - (x + BOSS_W/2.0)));
    }

    private double playerCenterX() {
        return player.getX() + player.sprite.getWidth() / 2.0;
    }
    private double playerCenterY() {
        return player.getY() + player.sprite.getHeight() / 2.0;
    }

    /** 각도(도) + 속도로 적탄 생성 */
    private void spawnAngleShot(double sx, double sy, double angleDeg, double speed) {
        double rad = Math.toRadians(angleDeg);
        double dx = Math.cos(rad) * speed;
        double dy = Math.sin(rad) * speed;
        EnemyShotEntity shot = new EnemyShotEntity(game, ENEMY_BULLET, sx, sy, dx, dy, speed);
        game.addEntity(shot);
    }

    @Override
    public void collidedWith(Entity other) {
        if (other instanceof ShotEntity) {
            ShotEntity shot = (ShotEntity) other;
            //탄은 항상 제거
            game.removeEntity(other);
            hp -= Math.max(1, shot.getDamage());    // ★ 공격력 반영
            // 페이즈 전환시 무적: 데미지 무시
            if (System.currentTimeMillis() < phaseHoldUntil) {
                return;
            }

            //평소엔 데미지 적용
            hp -= 1;
            if (hp <= 0) {
                game.removeEntity(this);
                game.onBossKilled();
            }
        }
    }

    /** Phase 0: 폭탄을 떨어뜨리고, 착지 시 파편으로 터뜨린다(그림자 경고 + 깜빡임) */
    private void doPhase0Bombs() {
        if (now - p0LastBombAt < p0BombIntervalMs) return;
        p0LastBombAt = now;

        int p0ExplodeY = p0ExplodeYMin + (int)(Math.random() * (p0ExplodeYMax - p0ExplodeYMin));

        // 화면 폭(좌/우) 계산
        int sw = screenWidthFallback;
        try { sw = (int) Game.class.getMethod("getScreenWidth").invoke(game); } catch (Exception ignored) {}
        int left = minX;
        int right = Math.max(left + 40, sw - marginX);

        // 보스 중심
        double sx0 = x + BOSS_W / 2.0;
        double sy0 = y + BOSS_H / 2.0;

        for (int i = 0; i < p0BombsPerVolley; i++) {
            // 무작위 착지 X
            double tx = left + Math.random() * (right - left);

            // 폭발까지 시간
            double distY = Math.max(0, p0ExplodeY - sy0);
            double timeToExplode = distY / p0BombFallSpeed; // seconds
            long   explodeDelay  = (long)(timeToExplode * 1000.0);

            // x 방향 속도: sx0 → tx 로 정확히 도달하게
            double dx = (tx - sx0) / timeToExplode; // px/s
            double dy = p0BombFallSpeed;

            // 폭탄(느리게 낙하)
            EnemyShotEntity bomb = new EnemyShotEntity(
                    game, ENEMY_BULLET, sx0, sy0, dx, dy, Math.hypot(dx, dy)
            );
            game.addEntity(bomb);

            // 그림자 경고 등록(폭발 시각까지 표시)
            shadows.add(new Shadow(tx, p0ExplodeY, now, now + explodeDelay));

            // 폭발 시점: 폭탄 제거 + 파편 생성 + 그림자 제거
            schedule(explodeDelay, () -> {
                game.removeEntity(bomb);

                // 폭발 지점/원형 파편
                for (int k = 0; k < p0ShardCount; k++) {
                    double ang = 360.0 * k / p0ShardCount;
                    spawnAngleShot(tx, p0ExplodeY, ang, p0ShardSpeed);
                }

                // 만료된 그림자들 정리
                for (int idx = shadows.size() - 1; idx >= 0; idx--) {
                    if (shadows.get(idx).explodeAt <= now) {
                        shadows.remove(idx);
                    }
                }
            });
        }
    }


    /* 내부 지연 실행: 스레드 대신 게임 루프 시점에 처리 */
    private void schedule(long delayMs, Runnable r) {
        tasks.add(new Task(now + delayMs, r));
    }
}