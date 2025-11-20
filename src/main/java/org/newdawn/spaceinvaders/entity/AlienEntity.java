package org.newdawn.spaceinvaders.entity;

import org.newdawn.spaceinvaders.Game;
import org.newdawn.spaceinvaders.Sprite;
import org.newdawn.spaceinvaders.SpriteStore;
import java.util.concurrent.ThreadLocalRandom;

/**
 * An entity which represents one of our space invader aliens.
 * 
 * @author Kevin Glass
 */
public class AlienEntity extends Entity {
	/** The speed at which the alient moves horizontally */
	private final double moveSpeed = 75;
	/** The game in which the entity exists */
	protected final Game game;
	/** The animation frames */
	private final Sprite[] frames = new Sprite[4];
	/** The time since the last frame change took place */
	private long lastFrameChange;
	/** The frame duration in milliseconds, i.e. how long any given frame of animation lasts */
	private final long frameDuration = 250;
	/** The current frame of animation being displayed */
	private int frameNumber;

    //최대 체력과 현재 체력
    private int maxHP = 1;
    private int currentHP=1;
    /** 웨이브에 따라 외계인 이동 속도를 키울 때 사용하는 배수 (1.0 = 기본) */
    private double speedMul = 1.0;

    // --- [리팩토링 - 신규 필드 (자식 클래스에서 Pull Up)] ---
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
		
		// if we need to change the frame, update the frame number
		// and flip over the sprite in use
		if (lastFrameChange > frameDuration) {
			// reset our frame change time counter
			lastFrameChange = 0;
			
			// update the frame
			frameNumber++;
			if (frameNumber >= frames.length) {
				frameNumber = 0;
			}
			
			sprite = frames[frameNumber];
		}
		
		// if we have reached the left hand side of the screen and
		// are moving left then request a logic update 
		if ((dx < 0) && (x < 10)) {
			game.updateLogic();
		}
		// and vice vesa, if we have reached the right hand side of 
		// the screen and are moving right, request a logic update
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
		
		// if we've reached the bottom of the screen then the player
		// dies
		if (y > 570) {
			game.notifyDeath();
		}
	}

    // 최대 체력 증가 메소드
    public void setMaxHP(int hp) {
        this.maxHP = Math.max(1, hp);
        this.currentHP = this.maxHP;
    }

    /** @return true면 사망 처리 필요 */
    //적 체력 감소 메소드
    public boolean takeDamage(int dmg) {
        currentHP -= Math.max(1, dmg);
        return currentHP <= 0;
    }

    public int getCurrentHP() {
        return currentHP;
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
  	/*
  	*
	 * Notification that this alien has collided with another entity
	 * 
	 * @param other The other entity
	 */
    @Override
	public void collidedWith(Entity other) {
        // [리팩토링]
        // 플레이어 탄환(ShotEntity)과의 충돌은 ShotEntity가 감지하여
        // 이 클래스의 wasHitBy()를 호출하는 방식으로 변경되었습니다.

        // 이 메소드는 플레이어 함선(ShipEntity) 등 다른 엔티티와의 충돌을 처리합니다.
        // (현재 ShipEntity가 Alien과 충돌 시 스스로 damage를 입으므로 여기선 비워둡니다)
	}

    /**
     * [리팩토링 - 신규 메소드]
     * ShotEntity로부터 "맞았다"는 알림을 받는 메소드.
     * 캡슐화 원칙에 따라, 피격 처리는 AlienEntity가 스스로 담당합니다.
     *
     * @param shot A-Me-를 맞춘 플레이어의 총알
     */
    public void wasHitBy(ShotEntity shot) {
        // 1. 총알로부터 데미지를 받아와 스스로의 체력을 깎음
        boolean isDead = this.takeDamage(shot.getDamage());

        // 2. 만약 이 피격으로 인해 죽었다면,
        if (isDead) {
            // 3. 'Game' 객체에게 "내가 죽었다"고 알림.
            //    (점수, XP, 엔티티 제거 등 모든 게임 규칙은 Game 클래스가 처리)
            game.onAlienKilled(this); // 'this'는 이 AlienEntity 인스턴스 자신
        }
    }

    /** 리팩토링
     * [신규] 자식 클래스가 발사 주기를 설정하기 위한 메소드 (protected)
     */
    protected void setFireCooldown(long baseMs, long jitterMs) {
        this.baseCooldownMs = baseMs;
        this.cooldownJitterMs = jitterMs;
    }
    /**
     * [신규] 템플릿 메소드: 발사 타이머 로직 (공통)
     * 이 메소드는 final이므로 자식 클래스가 오버라이드할 수 없습니다.
     */
    protected final void checkFire(long delta) {
        // 쿨다운이 너무 길면 (기본값) 발사 기능이 없는 것으로 간주
        if (baseCooldownMs > 900000) {
            return;
        }

        long now = System.currentTimeMillis();

        // [중복 코드] 난이도 배수 적용
        long effectiveCooldown = Math.max(200, (long)(baseCooldownMs / fireRateMul));
        long effectiveJitter   = Math.max(0,   (long)(cooldownJitterMs / fireRateMul));
        long nextWindow = lastShotAt
                + effectiveCooldown
                + ThreadLocalRandom.current().nextLong(effectiveJitter + 1);

        // [중복 코드] 시간 비교
        if (now >= nextWindow) {
            // [분리된 부분] 실제 발사 로직은 자식에게 위임
            performFire();
            lastShotAt = now;
        }
    }

    /**
     * [신규] Hook 메소드: 실제 발사 로직 (자식이 오버라이드)
     * 기본 Alien은 아무것도 하지 않습니다.
     */
    protected void performFire() {
        // Do nothing (Ranged, Diagonal 등이 이 메소드를 오버라이드)
    }

    /**
     * [신규] 난이도 설정을 위해 Pull Up (자식 클래스에서 이동)
     */
    public void setFireRateMultiplier(double mul) {
        this.fireRateMul = Math.max(0.25, mul); // 하한 보호
    }

    /**
     * [신규] 난이도 설정을 위해 Pull Up (자식 클래스에서 이동)
     */
    public void setBulletSpeedMultiplier(double mul) {
        this.bulletSpeedMul = Math.max(0.5, mul);
    }

    /**
     * [신규] 자식 클래스가 탄속 배수를 참조할 수 있도록 getter 제공 (protected)
     */
    protected double getBulletSpeedMultiplier() {
        return this.bulletSpeedMul;
    }
    // --- [리팩토링 끝] ---
}