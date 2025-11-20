package org.newdawn.spaceinvaders.entity;

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
        long t = now - phaseStartAt;
        long mod = t % cycleMs;

        //사이클 시작: 조준 및 충전 시작
        if(!active && chargeStart ==0 && mod<50){
            chargeStart = now;
            if(boss.getTarget() !=null){
                angle = boss.angleTo(boss.getTarget());
            }
        }

        //충전 완료 -> 발사
        if(chargeStart > 0 && now - chargeStart >= chargeDuration){
            active = true;
        }

        //발사중
        if(active){
            if (checkLaserCollision(boss,boss.getTarget())){//각도,폭,길이
                boss.getTarget().damage(1);
            }

            //발사 종료
            if(now - chargeStart >= chargeDuration + fireDuration){
                active = false;
                chargeStart = 0;
            }
        }
    }

    private boolean checkLaserCollision(BossEntity boss, ShipEntity player){
        if(player == null) return false;

        double cx = boss.getX() + boss.getWidth() /2.0;
        double cy = boss.getY() + boss.getHeight() /2.0;

        double px = player.getX() + player.getWidth() / 2.0;
        double py = player.getY() + player.getHeight() / 2.0;

        double rad = Math.toRadians(this.angle);
        double lx = Math.cos(rad);
        double ly = Math.sin(rad);

        double vx = px - cx;
        double vy = py - cy;

        double t = vx *lx + vy * ly;
        t = Math.max(0,Math.min(t,this.laserLengthPx));

        double qx = cx + lx*t;
        double qy = cy +ly *t;

        double dx = px -qx;
        double dy = py - qy;
        double dist = Math.hypot(dx,dy);

        return dist <= (this.laserWidthPx /2.0);
    }

    @Override
    public void draw(Graphics2D g, BossEntity boss){
        long now = System.currentTimeMillis();
        //충전중이거나 발사중일때
        if(chargeStart>0 || active){
            Stroke old = g.getStroke(); // 기존 Stroke 저장

            // 1. 상태에 따른 Stroke 및 색상 설정 (원래 BossEntity의 로직)
            if (active) { // 발사 중: 굵은 빨간 선
                g.setColor(new Color(255, 60, 60, 200));
                g.setStroke(new BasicStroke(laserWidthPx));
            } else { // 충전 중: 얇은 점선
                float[] dash = { 6f, 6f };
                g.setColor(new Color(255, 230, 80, 180));
                g.setStroke(new BasicStroke(2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, dash, 0));
            }

            // 2. 레이저 끝점 계산
            double rad = Math.toRadians(angle); // Strategy의 angle 필드 사용

            // 보스 중심 좌표
            int cx = (int) (boss.getX() + boss.getWidth()/2); // BossEntity 위치 참조
            int cy = (int) (boss.getY() + boss.getHeight()/2); // BossEntity 위치 참조

            // 레이저 끝점 (BossEntity의 laserLengthPx와 동일)
            int ex = (int) (cx + Math.cos(rad) * laserLengthPx);
            int ey = (int) (cy + Math.sin(rad) * laserLengthPx);

            // 3. 선 그리기
            g.drawLine(cx, cy, ex, ey);

            // 4. Stroke 복구
            g.setStroke(old);
        }
    }
}
