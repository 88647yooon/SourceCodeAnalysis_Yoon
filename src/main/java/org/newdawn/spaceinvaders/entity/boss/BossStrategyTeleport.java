package org.newdawn.spaceinvaders.entity.boss;

import java.awt.*;
import java.util.concurrent.ThreadLocalRandom;
import org.newdawn.spaceinvaders.entity.player.ShipEntity;

public class BossStrategyTeleport implements BossAttackStrategy {
    private final long teleportCooldown = 2400;
    private long lastTeleportAt = 0;

    private final Point[] teleportPoints = {
            new Point(40, 60), new Point(260, 60), new Point(480, 60),
            new Point(40, 180), new Point(260, 180), new Point(480, 180)
    };
    private final int burstCount = 5;
    private final long burstGapMs = 120;

    public BossStrategyTeleport(){
        this.lastTeleportAt = System.currentTimeMillis() - teleportCooldown;
    }

    @Override
    public void update(BossEntity boss, long delta) {
        long now = System.currentTimeMillis();

        // 쿨다운 체크
        if (now - lastTeleportAt >= teleportCooldown) {
            lastTeleportAt = now;

            // 1. 무작위 텔레포트
            Point p = teleportPoints[ThreadLocalRandom.current().nextInt(teleportPoints.length)];
            boss.teleportTo(p.x, p.y);

            // 2. 버스트(플레이어 조준) 사격 스케줄링
            ShipEntity target = boss.getTarget();
            if (target == null) return;

            // 텔레포트 직후 버스트(플레이어 조준)
            for (int i = 0; i < burstCount; i++) {
                final long delay = i * burstGapMs;

                // 지연 실행 큐에 태스크 예약
                boss.schedule(delay, () -> {
                    // 발사 시점에 다시 각도 계산 (플레이어가 움직였을 수 있으므로)
                    double angle = boss.angleTo(target);
                    // 보스 중심에서 발사
                    boss.spawnAngleShot(boss.getX() + 60, boss.getY() + 60, angle, 260);
                });
            }
        }
    }

    @Override
    public void draw(Graphics2D g, BossEntity boss){
        // 텔레포트는 순간 이동이므로 별도의 지속적인 그래픽 표시가 없음.
    }
}
