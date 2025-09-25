package org.newdawn.spaceinvaders.entity;

import org.newdawn.spaceinvaders.Game;

/**
 * The entity that represents the players ship
 * 
 * @author Kevin Glass
 */
public class ShipEntity extends Entity {
	/** The game in which the ship exists */
	private final Game game;
    private int maxHP = 3; // 최대 체력
    private int currentHP = 3; // 현재 체력
    private long lastDamageTime = 0;
    private long invincible = 500; //500ms 무적
	/**
	 * Create a new entity to represent the players ship
	 *  
	 * @param game The game in which the ship is being created
	 * @param ref The reference to the sprite to show for the ship
	 * @param x The initial x location of the player's ship
	 * @param y The initial y location of the player's ship
	 */
	public ShipEntity(Game game,String ref,int x,int y) {
		super(ref,x,y);
		
		this.game = game;
	}
    public int getMaxHP(){
        return maxHP;
    }
    public int getCurrentHP(){
        return currentHP;
    }

    public void damage(int d) {
        long now = System.currentTimeMillis();

        if(now - lastDamageTime < invincible){
            return;//아직 무적 상태라면 무시
        }

        currentHP -= d;
        game.onPlayerHit(); // UI/이펙트/무적시간 등

        if (currentHP <= 0) {
            game.notifyDeath();
        }

        lastDamageTime = now;// 마지막 피격 시간 갱신

    }
    public void heal(int d) {
        currentHP += d;
        if(currentHP > maxHP){
            currentHP = maxHP;
        }
    }

    public boolean isAlive() {
        return currentHP > 0;
    }
    public boolean isDead() {
        return currentHP <= 0;
    }
    /**
	 * Request that the ship move itself based on an elapsed ammount of
	 * time
	 * 
	 * @param delta The time that has elapsed since last move (ms)
	 */
	public void move(long delta) {
		// if we're moving left and have reached the left hand side
		// of the screen, don't move
		if ((dx < 0) && (x < 10)) {
			return;
		}
		// if we're moving right and have reached the right hand side
		// of the screen, don't move
		if ((dx > 0) && (x > 750)) {
			return;
		}

        //화면의 아래에 닿았을때 움직이지 마라
		if((dy < 0)&&(y<10)){
            return;
        }
        //화면의 위에 닿았을때 움직이지 마라
        //배가 32픽셀이라 800바이 600으로 설정했을때 600-32 = 568이 되어 안정적으로 아래쪽을 뚫지 않게 됨
        if ((dy > 0) && (y > 568)) {
            return;
        }
		super.move(delta);
	}
	
	/**
	 * Notification that the player's ship has collided with something
	 * 
	 * @param other The entity with which the ship has collided
	 */
	public void collidedWith(Entity other) {
		// if its an alien, notify the game that the player
		// is dead
		if (other instanceof AlienEntity) {
            damage(1);//즉사였는데 HP -1 로 바꿈
		}

        if (other instanceof EnemyShotEntity) {
            damage(1);
            game.removeEntity(other);
        }
	}
}