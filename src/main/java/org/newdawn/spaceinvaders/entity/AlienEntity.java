package org.newdawn.spaceinvaders.entity;

import org.newdawn.spaceinvaders.Game;
import org.newdawn.spaceinvaders.Sprite;
import org.newdawn.spaceinvaders.SpriteStore;

/**
 * An entity which represents one of our space invader aliens.
 * 
 * @author Kevin Glass
 */
public class AlienEntity extends Entity {
	/** The speed at which the alient moves horizontally */
	private final double moveSpeed = 75;
	/** The game in which the entity exists */
	private final Game game;
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
    private boolean dead = false;
    /** 웨이브에 따라 외계인 이동 속도를 키울 때 사용하는 배수 (1.0 = 기본) */
    private double speedMul = 1.0;


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
        //플레이어 탄환과의 충돌은 shotEntity에서 처리, 여기서는 아무것도 안함
	}
}