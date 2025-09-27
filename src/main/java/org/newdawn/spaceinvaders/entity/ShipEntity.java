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

    // ⬇⬇ "공유"용 정적 상태 (게임 모드가 바뀌어도 유지)
    private static int S_LEVEL = 1;
    private static int S_XP_INTO_LEVEL = 0;

    // === Level / XP (no stat effects) ===
    private int level = 1;
    private int xpIntoLevel = 0;
    private int xpToNext = reqFor(1);

    // ShipEntity.java (클래스 필드)
    private boolean invulnerable = false;

    // 필요 경험치 공식 (느리게): req(L) = 200 + 50 * L * (L - 1)
    private static int reqFor(int L) { return 200 + 50 * L * (L - 1); }

    // HUD용 Getter
    public int  getLevel()         { return level; }
    public int  getXpIntoLevel()   { return xpIntoLevel; }
    public int  getXpToNextLevel() { return xpToNext; }
    public int  getXpPercent()     { return (int)Math.round(100.0 * xpIntoLevel / Math.max(1, xpToNext)); }

    public void setInvulnerable(boolean inv) { this.invulnerable = inv; }
    public boolean isInvulnerable() { return invulnerable; }
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

        // ✅ 레벨 공유 상태 로드
        this.level = S_LEVEL;
        this.xpIntoLevel = S_XP_INTO_LEVEL;
        this.xpToNext = reqFor(level);
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
        //테스트 용 무적모드
        if (invulnerable) {
            return; // ✅ 무적이면 데미지 무시
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
    //초기 위치 설정
    public void teleportTo(int nx, int ny) {
        this.x = nx;
        this.y = ny;
        setHorizontalMovement(0);
        setVerticalMovement(0);
    }
    // XP 지급 (레벨업 자동 처리)
    public void addXp(int amount) {
        if (amount <= 0) return;
        xpIntoLevel += amount;
        while (xpIntoLevel >= xpToNext) {
            xpIntoLevel -= xpToNext;
            level++;
            xpToNext = reqFor(level);
            // TODO: 레벨업 이펙트/사운드/토스트 등
        }

        // ✅ 공유 상태 저장 (스테이지↔무한 공통)
        S_LEVEL = level;
        S_XP_INTO_LEVEL = xpIntoLevel;
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