package org.newdawn.spaceinvaders.entity;

public class GeometryUtils {
    private GeometryUtils(){}
    /**
     * 점(px, py)과 선분(시작점 cx,cy, 각도, 길이) 사이의 최단 거리를 계산합니다.
     * * @param px       점의 X 좌표 (플레이어 중심)
     * @param py       점의 Y 좌표 (플레이어 중심)
     * @param cx       선분 시작점 X (보스 중심)
     * @param cy       선분 시작점 Y (보스 중심)
     * @param angleDeg 선분의 각도 (도 단위)
     * @param length   선분의 길이
     * @return 점과 선분 사이의 최단 거리 (픽셀)
     */
    public static double disToSegment(double px,double py,double cx, double cy,double angleDeg,double length){
        //각도를 라디안으로 변환하여 단위 벡터 계산
        double rad = Math.toRadians(angleDeg);
        double lx = Math.cos(rad);
        double ly = Math.sin(rad);

        //시작점 (cx,cy)에서 목표 점 (px,py)까지의 벡터 계산
        double vx = px - cx;
        double vy = py - cy;

        //t는 시작점으로부터 선분 방향으로 얼마나 떨어져 있는지를 나타냄
        double t = vx*lx + vy *ly;

        // t< 0 이면 시작점이 가장 가깝고 , t> length 이면 끝점이 가장 가까움
        t = Math.max(0,Math.min(t,length));

        //선분 위의 가장 가까운 점 (qx,qy)좌표 계산
        double qx = cx +lx *t;
        double qy = cy + ly * t;

        //목표 점 (px,py)과 선분위 가장 가까운점 (qx,qy)사이의 거리 반환
        return Math.hypot(px-qx,py-qy);
    }
}
