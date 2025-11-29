package org.newdawn.spaceinvaders.entity.boss;

import java.awt.*;
import java.util.concurrent.ThreadLocalRandom;
import org.newdawn.spaceinvaders.entity.player.ShipEntity;

public class BossStrategyTeleport implements BossAttackStrategy {
    private final long teleportCooldown = 2000; // 쿨다운 적당히 조정
    private long lastTeleportAt = 0;

    private final Point[] teleportPoints = {
            new Point(40, 60), new Point(260, 60), new Point(480, 60),
            new Point(40, 180), new Point(260, 180), new Point(480, 180)
    };

    //  버스트 횟수와 간격
    private final int burstCount = 5;     // 5번 연속 사격
    private final long burstGapMs = 150;  // 사격 간격

    public BossStrategyTeleport(){
        this.lastTeleportAt = System.currentTimeMillis() - teleportCooldown;
    }

    @Override
    public void update(BossEntity boss, long delta) {
        long now = System.currentTimeMillis();

        if (now - lastTeleportAt >= teleportCooldown) {
            lastTeleportAt = now;

            // 1. 텔레포트 이동
            Point p = teleportPoints[ThreadLocalRandom.current().nextInt(teleportPoints.length)];
            boss.teleportTo(p.x, p.y);

            // 등장 충격파 (Shockwave)
            // 보스가 나타나자마자 주변으로 12발의 탄을 원형으로 뿌림
            double centerX = boss.getX() + 60; // 보스 중심 (120/2)
            double centerY = boss.getY() + 60;
            for (int i = 0; i < 12; i++) {
                boss.spawnAngleShot(centerX, centerY, i * 30, 180); // 30도 간격, 속도 180
            }

            // 2. 버스트 사격 스케줄링 (플레이어 조준)
            ShipEntity target = boss.getTarget();
            if (target == null) return;

            for (int i = 0; i < burstCount; i++) {
                final long delay = 400 + (i * burstGapMs); // 충격파 후 0.4초 뒤부터 사격 시작

                boss.schedule(delay, () -> {
                    // 발사 시점에 플레이어 각도 재계산
                    double baseAngle = boss.angleTo(target);

                    // 부채꼴 산탄 (Shotgun)
                    // 플레이어 방향을 중심으로 -20도, 0도, +20도 3갈래 발사
                    double[] offsets = { -20, 0, 20 };

                    for (double offset : offsets) {
                        boss.spawnAngleShot(boss.getX() + 60, boss.getY() + 60, baseAngle + offset, 280);
                    }
                });
            }
        }
    }

    @Override
    public void draw(Graphics2D g, BossEntity boss){
        // 텔레포트 예고 이펙트 등을 추가하고 싶다면 여기에 구현
    }
}