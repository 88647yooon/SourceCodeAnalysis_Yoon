package org.newdawn.spaceinvaders.entity.boss;

import java.awt.Graphics2D;
public class BossStrategySwirl implements BossAttackStrategy {
    private double swirlAngle = 0.0;
    private double swirlRotationSpeed = 90; // 기존 60 (회전 속도 증가)
    private int bulletsPerWave = 18;        // 기존 12 (한 번에 뿌리는 양 1.5배)
    private double bulletSpeed = 160;       // 기존 120 (탄속 증가)
    private long intervalMs = 600;
    private long lastFireAt = 0;

    @Override
    public void update(BossEntity boss, long delta){
        long now = System.currentTimeMillis();
        swirlAngle += swirlRotationSpeed *(delta/1000.0);

        if(now - lastFireAt >= intervalMs){
            lastFireAt = now;

            double bx = boss.getX() + boss.getHeight()/2.0;
            double by = boss.getY() + boss.getHeight()/2.0;

            for (int i=0;i<bulletsPerWave;i++){
                double angle = swirlAngle + (360.0*i/bulletsPerWave);
                boss.spawnAngleShot(bx,by,angle,bulletSpeed);
            }
        }
    }

    @Override
    public void draw(Graphics2D g,BossEntity boss) {
        //스월 패턴은 별도의 그래픽 표시가 없음
    }
}
