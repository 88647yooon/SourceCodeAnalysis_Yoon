package org.newdawn.spaceinvaders.entity.boss;

import org.newdawn.spaceinvaders.entity.player.ShipEntity;
import org.newdawn.spaceinvaders.entity.boss.BossEntity;
import org.newdawn.spaceinvaders.util.GeometryUtils;

import java.awt.*;

public class BossStrategyLaser implements BossAttackStrategy{
    private boolean active = false;
    private long chargeStart = 0;
    private long chargeDuration = 900;
    private long fireDuration = 900;
    private double angle = 90;
    private long cycleMs = 2800;
    private long phaseStartAt;

    private final int laserWidthPx = 14;
    private final int laserLengthPx = 900;

    public BossStrategyLaser(){
        this.phaseStartAt = System.currentTimeMillis();
    }

    @Override
    public void update(BossEntity boss, long delta){
        long now = System.currentTimeMillis();

        //충전 시작 체크
        tryStartCharging(boss,now);

        //충전 완료 -> 발사 전환 체크
        checkChargeComplete(now);

        //발사 중 로직
        if(active){
            processFiring(boss,now);
        }
    }
    // 충전 시작 조건 체크
    private void tryStartCharging(BossEntity boss, long now){
        long t = now - phaseStartAt;
        long mod = t % cycleMs;

        if (!active && chargeStart == 0 && mod < 50) {
            chargeStart = now;
            if (boss.getTarget() != null) {
                angle = boss.angleTo(boss.getTarget());
            }
        }
    }

    //충전 완료 체크
    private void checkChargeComplete(long now){
        if (chargeStart > 0 && !active) {
            if(now - chargeStart >= chargeDuration){
                active = true;
            }
        }
    }

    //발사 중 동작
    private void processFiring(BossEntity boss, long now){
        if(checkLaserCollision(boss, boss.getTarget())){
            boss.getTarget().damage(1);
        }
        if(now - chargeStart >= chargeDuration + fireDuration){
            active = false;
            chargeStart = 0;
        }
    }

    private boolean checkLaserCollision(BossEntity boss, ShipEntity player){
        if(player == null) return false;

        double cx = boss.getX() + boss.getWidth() /2.0;
        double cy = boss.getY() + boss.getHeight() /2.0;
        double px = player.getX() + player.getWidth() / 2.0;
        double py = player.getY() + player.getHeight() / 2.0;

        double dist = GeometryUtils.disToSegment(px,py,cx,cy,angle,laserLengthPx);

        return dist <= (laserWidthPx / 2.0);
    }

    @Override
    public void draw(Graphics2D g, BossEntity boss){
        if (chargeStart == 0 && !active) return;

        Stroke old = g.getStroke();

        // 스트로크 설정 분리
        setLaserStroke(g);

        // 선 그리기
        drawLaserLine(g, boss);

        g.setStroke(old);
    }
    private void setLaserStroke(Graphics2D g){
        if(active){
            g.setColor(new Color(255,60,60,200));
            g.setStroke(new BasicStroke(laserWidthPx));
        }else{
            float[] dash = { 6f, 6f };
            g.setColor(new Color(255, 230, 80, 180));
            g.setStroke(new BasicStroke(2f,BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, dash, 0));
        }
    }

    private void drawLaserLine(Graphics2D g,BossEntity boss){
        double rad = Math.toRadians(angle);
        int cx = (int) (boss.getX() + boss.getWidth()/2);
        int cy = (int) (boss.getY() + boss.getHeight()/2);
        int ex = (int) (cx + Math.cos(rad) * laserLengthPx);
        int ey = (int) (cy + Math.sin(rad) * laserLengthPx);

        g.drawLine(cx, cy, ex, ey);
    }
}
