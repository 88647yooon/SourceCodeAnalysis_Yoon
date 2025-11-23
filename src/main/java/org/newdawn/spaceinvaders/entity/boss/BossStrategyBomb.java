package org.newdawn.spaceinvaders.entity.boss;

import org.newdawn.spaceinvaders.entity.base.Entity;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class BossStrategyBomb implements BossAttackStrategy{
    private long intervalMs = 1600;
    private long lastBombAt = 0;
    private int bombsPerVolley = 2;

    //그림자 이펙트 관리를 위한 내부 클래스
    private static class Shadow{
        final double x;
        final int y;
        final long startAt;
        final long explodeAt;
        Shadow(double x, int y, long startAt, long explodeAt) {
            this.x = x;
            this.y = y;
            this.startAt = startAt;
            this.explodeAt = explodeAt;
        }
    }
    private final List<Shadow> shadows = new ArrayList<>();

    public BossStrategyBomb(){
        this.lastBombAt = 0;
    }

    public void initializeTimer(){
        this.lastBombAt = System.currentTimeMillis() - intervalMs;
    }

    @Override
    public void update(BossEntity boss, long delta){
        long now = System.currentTimeMillis();
        if(now - lastBombAt < intervalMs) return;
        lastBombAt = now;

        //폭탄 투하 로직
        int minY = 300, maxY = 520;
        int explodeY = minY + (int)(Math.random() * (maxY - minY));
        int left = 20, right = 760;//화면 범위 설정

        for(int i=0;i<bombsPerVolley;i++){
            double tx = left + Math.random() * (right - left);
            double startY = boss.getY() + boss.getHeight()/2.0;

            double fallSpeed = 120;
            double distY = Math.max(0.0001,explodeY - startY);

            double timeSec = distY /fallSpeed;

            double dx = (tx - (boss.getX() + boss.getWidth()/2.0)) / timeSec;

            long explodeDelay = (long)(timeSec*1000.0);

            //폭탄 생성
            Entity bomb = boss.spawnShotEntity(boss.getX() + boss.getWidth()/2.0,startY,dx,fallSpeed,0);

            //그림자 등록
            shadows.add(new Shadow(tx,explodeY,now,now+explodeDelay));

            //폭발 스케줄링
            boss.schedule(explodeDelay,()->{
                boss.getGame().removeEntity(bomb); //폭탄제거
                //파편 생성
                for(int k=0;k<8;k++){
                    boss.spawnAngleShot(tx,explodeY,k*45,220);
                }
            });
        }

        //만료된 그림자 정리
        shadows.removeIf(s -> s.explodeAt <= now);
    }

    @Override
    public void draw(Graphics2D g, BossEntity boss){
        long now = System.currentTimeMillis();
        for(Shadow s: shadows){
            if(now>= s.explodeAt)continue;
            //그림자 그리기 로직
            g.setColor(new Color(0,0,0,50));
            g.fillOval((int)s.x - 12,s.y-12,24,24);

            //폭발 직전 깜빡임
            if(s.explodeAt - now <900){
                if((now/100)%2==0){
                    g.setColor(Color.RED);
                    g.drawOval((int)s.x - 15,s.y-15,30,30);
                }
            }
        }
    }
}
