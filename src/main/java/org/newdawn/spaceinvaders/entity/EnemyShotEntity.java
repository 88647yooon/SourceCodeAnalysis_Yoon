package org.newdawn.spaceinvaders.entity;

import java.awt.Graphics;
import org.newdawn.spaceinvaders.Game;
import org.newdawn.spaceinvaders.Sprite;
import org.newdawn.spaceinvaders.SpriteStore;

public class EnemyShotEntity extends Entity {
    private final Game game;
    private final double speed;      // 기본 속도
    private double friction = 0.0;   // 매 프레임 감속 (선택)

    // 수명 관리
    private long ageMs = 0;
    private long lifeTimeMs = 15000; // 15초 후 제거

    // 호밍 관련
    private boolean homing = false;
    private long homingDelayMs = 0;
    private double homingAccel = 0;
    private double maxHomingSpeed = 280;
    private ShipEntity target;

    // 직선 강제 스위치
    private boolean forceNoHoming = false;

    private final Sprite sprite;

    public EnemyShotEntity(Game game, String spriteRef, double x, double y, double dx, double dy, double speed) {
        super(spriteRef, (int)Math.round(x), (int)Math.round(y));
        this.game = game;
        this.sprite = SpriteStore.get().getSprite(spriteRef);
        this.speed = speed;
        setHorizontalMovement(dx);
        setVerticalMovement(dy);

        // 디버그 로그
        //System.out.println("🟢 Bullet created: vx=" + dx + ", vy=" + dy + ", speed=" + speed);
    }

    /** 각도 기반 생성자 */
    public static EnemyShotEntity fromAngle(Game game, String spriteRef, double x, double y, double angleDeg, double speed) {
        double rad = Math.toRadians(angleDeg);
        double dx = Math.cos(rad) * speed;
        double dy = Math.sin(rad) * speed;
        return new EnemyShotEntity(game, spriteRef, x, y, dx, dy, speed);
    }

    /** 호밍 활성화 (보스 전용) */
    public EnemyShotEntity enableHoming(ShipEntity target, long delayMs, double accel, double maxSpeed) {
        System.err.println("⚠️ enableHoming called! bullet@(" + x + "," + y + ")");
        Thread.dumpStack(); // 호출 지점 추적
        this.homing = true;
        this.target = target;
        this.homingDelayMs = Math.max(0, delayMs);
        this.homingAccel = accel;
        this.maxHomingSpeed = maxSpeed;
        return this;
    }

    /** 직선탄 강제 (Ranged, Diagonal 전용) */
    public EnemyShotEntity disableHomingHard() {
        this.forceNoHoming = true;
        this.homing = false;
        this.target = null;
        return this;
    }

    /** 마찰 설정 (선택) */
    public EnemyShotEntity setFriction(double frictionPerSec) {
        this.friction = frictionPerSec;
        return this;
    }

    /** 수명 제한 (선택) */
    public EnemyShotEntity setLifeTimeMs(long ms) {
        this.lifeTimeMs = ms;
        return this;
    }

    @Override
    public void move(long delta) {
        ageMs += delta;

        // 직선 잠금일 때는 유도 완전 차단
        if (!forceNoHoming && homing && ageMs >= homingDelayMs && target != null) {
            double tx = target.getX() - this.x;
            double ty = target.getY() - this.y;
            double len = Math.sqrt(tx * tx + ty * ty);
            if (len > 1e-4) {
                tx /= len;
                ty /= len;
                double vx = getHorizontalMovement();
                double vy = getVerticalMovement();
                double targetVx = tx * maxHomingSpeed;
                double targetVy = ty * maxHomingSpeed;
                vx = vx + (targetVx - vx) * homingAccel;
                vy = vy + (targetVy - vy) * homingAccel;
                setHorizontalMovement(vx);
                setVerticalMovement(vy);
            }
        }

        // 감속
        if (friction > 0) {
            double vx = getHorizontalMovement();
            double vy = getVerticalMovement();
            double v = Math.sqrt(vx * vx + vy * vy);
            if (v > 0) {
                double dec = friction * (delta / 1000.0);
                v = Math.max(0, v - dec);
                if (v == 0) {
                    setHorizontalMovement(0);
                    setVerticalMovement(0);
                } else {
                    double nx = vx / Math.sqrt(vx * vx + vy * vy);
                    double ny = vy / Math.sqrt(vx * vx + vy * vy);
                    setHorizontalMovement(nx * v);
                    setVerticalMovement(ny * v);
                }
            }
        }

        // 수명 & 화면 밖 제거
        if (ageMs > lifeTimeMs || y < -64 || y > game.getHeight() + 64 || x < -64 || x > game.getWidth() + 64) {
            game.removeEntity(this);
            return;
        }

        super.move(delta);
    }

    @Override
    public void draw(Graphics g) {
        sprite.draw(g, (int)x, (int)y);
    }

    @Override
    public void collidedWith(Entity other) {
        if (other instanceof ShipEntity) {
            game.removeEntity(this);
            ((ShipEntity) other).damage(1);
        }
        // 적탄은 적과 충돌 무시
    }
}