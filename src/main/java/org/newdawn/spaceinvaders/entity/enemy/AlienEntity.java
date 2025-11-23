package org.newdawn.spaceinvaders.entity.enemy;

import org.newdawn.spaceinvaders.Game;
import org.newdawn.spaceinvaders.Sprite;
import org.newdawn.spaceinvaders.SpriteStore;
import java.util.concurrent.ThreadLocalRandom;
import org.newdawn.spaceinvaders.entity.base.Entity;
import org.newdawn.spaceinvaders.entity.base.EnemyEntity;
import org.newdawn.spaceinvaders.entity.projectile.ShotEntity;

/**
 * An entity which represents one of our space invader aliens.
 * 
 * @author Kevin Glass
 */
public class AlienEntity extends EnemyEntity {
	private final double moveSpeed = 75;
	protected final Game game;
	private final Sprite[] frames = new Sprite[4];
	private long lastFrameChange;
	private final long frameDuration = 250;
	private int frameNumber;

    /** 웨이브에 따라 외계인 이동 속도를 키울 때 사용하는 배수 (1.0 = 기본) */
    private double speedMul = 1.0;

    /** 마지막 발사 시각 (ms) */
    private long lastShotAt = 0L;
    /** 기본 발사 쿨다운 (ms) - 기본값은 발사 안 함 */
    private long baseCooldownMs = Long.MAX_VALUE;
    /** 쿨다운 랜덤 지터 (ms) */
    private long cooldownJitterMs = 0;
    /** 난이도: 발사 속도 배수 (1.0 = 100%) */
    private double fireRateMul = 1.0;
    /** 난이도: 탄속 배수 (1.0 = 100%) */
    private double bulletSpeedMul = 1.0;

    /**
	 * Create a new alien entity
	 * 
	 * @param game The game in which this entity is being created
	 * @param x The intial x location of this alien
	 * @param y The intial y location of this alient
	 */
	public AlienEntity(Game game,int x,int y) {
		super("sprites/alien.gif",x,y);
		
		// setup the animatin frames
		frames[0] = sprite;
		frames[1] = SpriteStore.get().getSprite("sprites/alien2.gif");
		frames[2] = sprite;
		frames[3] = SpriteStore.get().getSprite("sprites/alien3.gif");
		
		this.game = game;
		dx = -moveSpeed;
	}
    public AlienEntity(Game game,String ref,int x,int y) {
        super(ref, x, y);              //  ref를 그대로 사용
        this.game = game;
        dx = -moveSpeed;

        // ref가 기본 외계인일 때만 애니메이션 사용
        if ("sprites/alien.gif".equals(ref)) {
            frames[0] = sprite;
            frames[1] = SpriteStore.get().getSprite("sprites/alien2.gif");
            frames[2] = sprite;
            frames[3] = SpriteStore.get().getSprite("sprites/alien3.gif");
        } else {
            // 커스텀 스프라이트는 단일 프레임(프레임 전환 시 모양 안 바뀌게)
            frames[0] = sprite;
            frames[1] = sprite;
            frames[2] = sprite;
            frames[3] = sprite;
        }
    }

	/**
	 * Request that this alien moved based on time elapsed
	 * 
	 * @param delta The time that has elapsed since last move
	 */
	public void move(long delta) {
		// since the move tells us how much time has passed
		// by we can use it to drive the animation, however
		// its the not the prettiest solution
		lastFrameChange += delta;

		if (lastFrameChange > frameDuration) {
			lastFrameChange = 0;
			frameNumber++;
			if (frameNumber >= frames.length) {
				frameNumber = 0;
			}
			sprite = frames[frameNumber];
		}

		if ((dx < 0) && (x < 10)) {
			game.updateLogic();
		}
		if ((dx > 0) && (x > 750)) {
			game.updateLogic();
		}
		
		// proceed with normal move
		super.move(delta);

        checkFire(delta);

	}
	
	/**
	 * Update the game logic related to aliens
	 */
	public void doLogic() {
		// swap over horizontal movement and move down the
		// screen a bit
		dx = -dx;
		y += 10;

		if (y > 570) {
			game.notifyDeath();
		}
	}


    /** 수평 이동 속도 배수 적용 (좌/우 방향은 유지) */
    public void applySpeedMultiplier(double mul) {
        if (mul <= 0) return;
        this.speedMul = mul;

        // moveSpeed는 이 클래스의 private final이지만 동일 클래스 안이라 접근 가능
        double base = Math.abs(dx) > 0 ? moveSpeed : moveSpeed; // 방향은 dx의 부호를 따름
        double dir = (dx < 0) ? -1.0 : 1.0;
        setHorizontalMovement(dir * base * speedMul);
    }

    @Override
	public void collidedWith(Entity other) {
        // 충돌 처리는 ShotEntity와 wasHitBy에서 담당하므로 비워둠
	}

    /**
     * 부모 (EnemyEntity)의 추상 메소드 구현
     * ShotEntity로부터 피격 알림을 받았을 때 호출됨
     */
    public void wasHitBy(ShotEntity shot) {
        // 1. 부모의 takeDamage 호출(체력 감소)
        boolean isDead = this.takeDamage(shot.getDamage());

        // 2. 사망 시 Game에 알림
        if (isDead) {
            game.onAlienKilled(this);
        }
    }

    /**
     * 자식 클래스가 발사 주기를 설정하기 위한 메소드
     */
    protected void setFireCooldown(long baseMs, long jitterMs) {
        this.baseCooldownMs = baseMs;
        this.cooldownJitterMs = jitterMs;
    }
    /**
     * 템플릿 메소드: 발사 타이머 로직 (공통)
     * 이 메소드는 final이므로 자식 클래스가 오버라이드할 수 없습니다.
     */
    protected final void checkFire(long delta) {
        // 쿨다운이 너무 길면 (기본값) 발사 기능이 없는 것으로 간주
        if (baseCooldownMs > 900000) {
            return;
        }

        long now = System.currentTimeMillis();

        // 난이도 배수 적용
        long effectiveCooldown = Math.max(200, (long)(baseCooldownMs / fireRateMul));
        long effectiveJitter   = Math.max(0,   (long)(cooldownJitterMs / fireRateMul));
        long nextWindow = lastShotAt
                + effectiveCooldown
                + ThreadLocalRandom.current().nextLong(effectiveJitter + 1);

        // [ 시간 비교
        if (now >= nextWindow) {
            // 실제 발사 로직은 자식에게 위임
            performFire();
            lastShotAt = now;
        }
    }

    /**
     * Hook 메소드: 실제 발사 로직 (자식이 오버라이드)
     * 기본 Alien은 아무것도 하지 않음
     */
    protected void performFire() {
        // Do nothing (Ranged, Diagonal 등이 이 메소드를 오버라이드)
    }

    /**
     * 난이도 설정을 위해 Pull Up (자식 클래스에서 이동)
     */
    public void setFireRateMultiplier(double mul) {
        this.fireRateMul = Math.max(0.25, mul); // 하한 보호
    }

    /**
     * 난이도 설정을 위해 Pull Up (자식 클래스에서 이동)
     */
    public void setBulletSpeedMultiplier(double mul) {
        this.bulletSpeedMul = Math.max(0.5, mul);
    }

    /**
     * 자식 클래스가 탄속 배수를 참조할 수 있도록 getter 제공 (protected)
     */
    protected double getBulletSpeedMultiplier() {
        return this.bulletSpeedMul;
    }
}