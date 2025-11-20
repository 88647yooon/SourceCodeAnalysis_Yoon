package org.newdawn.spaceinvaders.entity;

import org.newdawn.spaceinvaders.Sprite;
import java.awt.*;
import java.util.ArrayDeque;

public class ShipDashComponent {
    private final ShipEntity ship;

    //대시 관련 필드
    private boolean dashing = false;
    private long dashStartAt =0L;
    private long dashDurationMs = 140;
    private long dashDistancePx = 1200;
    private long lastTrailAt = 0l;

    //쿨다운/무적 관련
    private long baseDashCooldownMs = 1200;  // 대시 쿨 기준
    private int  baseDashIframesMs  = 220;   // 대시 무적 기준
    private long lastDashAt         = 0;
    private long invulnUntil        = 0;//플레이어 무적 종료 시간

    // 화면 경계(ShipEntity와 동일하게 맞춤)
    private static final int TOP_MARGIN = 10;

    // 잔상 파라미터
    private static final int TRAIL_INTERVAL_MS = 22;   // 스냅샷 간격
    private static final int TRAIL_LIFETIME_MS = 240;  // 잔상 유지시간
    private static final int TRAIL_MAX = 10;           // 최대 스냅샷 개수

    //잔상 데이터
    private static final class Trail {
        final int x, y;
        final long t;
        Trail(int x,int y,long t){this.x=x; this.y=y; this.t=t;}
    }
    private final ArrayDeque<Trail> dashTrail = new ArrayDeque<>();

    public ShipDashComponent(ShipEntity ship) {
        this.ship = ship;
    }

    //대시 시도
    public void tryDash() {
        long now = System.currentTimeMillis();
        long cooldown = (long)Math.round(baseDashCooldownMs*ship.getStats().getSkills().dashCdMul());
        if (now - lastDashAt < cooldown) return;

        lastDashAt = now;
        dashStartAt = now;
        dashing = true;

        // 실제 이동할 거리 기준으로 등속도 설정
        double vy = -(dashDistancePx * 1000.0) / dashDurationMs; // 음수 = 위로

        ship.setHorizontalMovement(0);      // 수평 입력과 독립
        ship.setVerticalMovement(vy);

        // 대시 무적 (skills가 반영된 currentDashIframes() 사용)
        int bonus = ship.getStats().getSkills().dashIframesBonusMs();
        invulnUntil = now + baseDashIframesMs + bonus;

        // 첫 잔상 추가
        dashTrail.addFirst(new Trail(ship.getX(), ship.getY(), now));
        lastTrailAt = now;
    }


    //매 프레임 업데이트
    public void update(long delta, long now){
        if (dashing) {
            ship.setHorizontalMovement(0); // 수직 대시이므로 항상 0

            // 잔상 생성
            if (now - lastTrailAt >= 22) {
                dashTrail.addFirst(new Trail(ship.getX(), ship.getY(), now));
                while (dashTrail.size() > 8) dashTrail.removeLast(); // 개수 제한
                lastTrailAt = now;
            }

            // 대시 종료 체크
            if (now - dashStartAt >= dashDurationMs) {
                StopDash(now);
            }
        }

        // 경계 도달 시 강제
        if(ship.getY() <TOP_MARGIN && ship.getVerticalMovement() < 0){
            ship.setY(TOP_MARGIN);
            if(dashing) StopDash(now);
        }
        if(ship.getY() > 568 && ship.getVerticalMovement() > 0){
            ship.setY(568);
            if(dashing) StopDash(now);
        }

        // 오래된 잔상 제거
        while (!dashTrail.isEmpty() && now - dashTrail.getLast().t > 220) {
            dashTrail.removeLast();
        }
    }

    public void StopDash(long now){
        dashing = false;
        ship.setVerticalMovement(0);
        spawnArrivalEchoes(now);
    }
    public void StopDash(){
        StopDash(System.currentTimeMillis());
    }

    private void spawnArrivalEchoes(long now){
        for (int i=0;i<3;i++){
            dashTrail.addFirst(new Trail(ship.getX(), ship.getY(), now));
        }
        while (dashTrail.size() > TRAIL_MAX) dashTrail.removeLast();
    }

    //렌더링
    public void drawTrails(Graphics2D g, Sprite sprite){
        long now = System.currentTimeMillis();
        int i =0;

        Composite old = g.getComposite(); //기존 합성 모드 저장

        for (Trail tr : dashTrail) {
            float age = (now - tr.t) / (float) TRAIL_LIFETIME_MS; // ← 상수 사용
            float alpha = Math.max(0f, 0.36f * (1f - age));
            if (alpha <= 0f) continue;

            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
            int tailOffsetY = Math.min(14, i * 2);
            g.drawImage(sprite.getImage(), tr.x, tr.y + tailOffsetY, null);
            i++;
        }
        g.setComposite(old);
    }

    public boolean isDashing(){return dashing;}

    public boolean isInvulnerabe(long now){
        return now < invulnUntil;
    }
}
