package org.newdawn.spaceinvaders.entity;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.newdawn.spaceinvaders.Game;
import org.newdawn.spaceinvaders.LevelManager;
import org.newdawn.spaceinvaders.PlayerSkills;
//대시시 잔상 그리기
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.AlphaComposite;
import java.awt.Composite;
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

    // 레벨 / XP관련
    private int level = 1;
    private int xpIntoLevel = 0;
    private int xpToNext = reqFor(1);
    private int levelUpPoints = 0;
    private final PlayerSkills skills = new PlayerSkills();
    //레벨 및 경험치 저장
    // ShipEntity.java 안에 추가
    public void setLevelAndXp(int lvl, int xp) {
        this.level = Math.max(1, lvl);
        this.xpIntoLevel = Math.max(0, xp);
        this.xpToNext = reqFor(this.level);
        S_LEVEL = this.level;
        S_XP_INTO_LEVEL = this.xpIntoLevel;
    }



    //레벨업시 스탯관련 필드
    public boolean hasUnspentLevelUp() { return levelUpPoints > 0; }
    public void grantLevelUpPoint()    { levelUpPoints++; }
    public void spendLevelUpPoint()    { if (levelUpPoints > 0) levelUpPoints--; }



    // 발사/대시 기준값(네 프로젝트 기준에 맞춰 값 사용)
    private long baseShotIntervalMs = 220;   // 기존 사격 간격
    private int  baseShotDamage     = 1;     // 기본 탄 피해
    private long lastShotAt         = 0;

    //대시 기능 컴포넌트
    private final ShipDashComponent dashComponent;

    private long baseDashCooldownMs = 1200;  // 대시 쿨 기준
    private int  baseDashIframesMs  = 220;   // 대시 무적 기준



    //대시 관련 필드
    private boolean dashing = false;

    // 잔상 데이터
    private static final class Trail { final int x, y; final long t; Trail(int x,int y,long t){this.x=x; this.y=y; this.t=t;} }
    private final java.util.ArrayDeque<Trail> dashTrail = new java.util.ArrayDeque<>();

    // ShipEntity.java (클래스 필드)
    private boolean invulnerable = false;

    // 필요 경험치 공식 (느리게): req(L) = 200 + 50 * L * (L - 1)
    private static int reqFor(int L) { return 200 + 50 * L * (L - 1); }
    //레벨 저장용 setter
    public void setLevel(int lvl) {
        this.level = Math.max(1, lvl);
        this.xpIntoLevel = 0;
        this.xpToNext = reqFor(this.level);
        S_LEVEL = this.level;
        S_XP_INTO_LEVEL = this.xpIntoLevel;
    }

    // HUD용 Getter
    public int  getLevel()         { return level; }
    public int  getXpIntoLevel()   { return xpIntoLevel; }
    public int  getXpToNextLevel() { return xpToNext; }
    public int  getXpPercent()     { return (int)Math.round(100.0 * xpIntoLevel / Math.max(1, xpToNext)); }

    public void setInvulnerable(boolean inv) { this.invulnerable = inv; }

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

        //  레벨 공유 상태 로드
        this.level = S_LEVEL;
        this.xpIntoLevel = S_XP_INTO_LEVEL;
        this.xpToNext = reqFor(level);

        //컴포넌트 초기화
        this.dashComponent = new ShipDashComponent(this);
	}
    public int getMaxHP(){
        return maxHP;
    }
    public int getCurrentHP(){
        return currentHP;
    }

    public void damage(int d) {
        if (isInvulnerable()) return; // 모든 무적 경로를 한곳에서 처리

        currentHP -= d;
        game.onPlayerHit();
        if (currentHP <= 0) {
            game.notifyDeath();
        }
        lastDamageTime = System.currentTimeMillis();

    }


    /**
	 * Request that the ship move itself based on an elapsed ammount of
	 * time
	 * 
	 * @param delta The time that has elapsed since last move (ms)
	 */
	public void move(long delta) {
        long now = System.currentTimeMillis();

        //대시 로직 위임
        dashComponent.update(delta,now);

        // 화면 경계 처리 — 상단에서 멈추고 대시 종료(순간정지감 완화)
        if ((dy < 0) && (y < 10)) {
            y = 10; // 살짝 클램프
            if (dashing) { dashing = false; setVerticalMovement(0); }
            return;
        }
        if ((dy > 0) && (y > 568)) {
            y = 568;
            if (dashing) { dashing = false; setVerticalMovement(0); }
            return;
        }

        // 좌우 경계 (수직 대시에는 보통 영향 없지만 안전하게 유지)
        if ((dx < 0) && (x < 10)) {
            x = 10;
            if (dashing) { dashing = false; setHorizontalMovement(0); }
            return;
        }
        if ((dx > 0) && (x > 750)) {
            x = 750;
            if (dashing) { dashing = false; setHorizontalMovement(0); }
            return;
        }

        // 잔상 수명 관리: 오래된 스냅샷 정리 (최대 220ms 유지)
        while (!dashTrail.isEmpty() && now - dashTrail.getLast().t > 220) {
            dashTrail.removeLast();
        }

        super.move(delta);
	}
    //대시 시도 위임
    public void tryDash(){
        dashComponent.tryDash();
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

            grantLevelUpPoint();
        }

        //  공유 상태 저장 (스테이지↔무한 공통)
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
    @Override
    public void draw(Graphics g) {
        //잔상 그리기 위임
        dashComponent.drawTrails((Graphics2D)g, sprite);

        super.draw(g);
    }
    //무적 판정 위임
    public boolean isInvulnerable(){
        long now = System.currentTimeMillis();
        boolean dashIFrames = dashComponent.isInvulnerabe(now);
        boolean postHitIFrames = (now - lastDamageTime) < invincible;
        return invulnerable || dashIFrames || postHitIFrames;
    }
    //위치 강제 설정을 위한 세터
    public void setY(int y){this.y = y;}
    public PlayerSkills getSkills(){return skills;}



    public void saveSkillsToCloud() {
        if (Game.SESSION_UID == null || Game.SESSION_ID_TOKEN == null) {
            System.out.println(" UID/TOKEN 없음 - 로그인 후에만 스킬 저장 가능");
            return;
        }
        try {
            LevelManager.saveSkills(game.getDbClient(), Game.SESSION_UID, Game.SESSION_ID_TOKEN, skills);
        } catch (Exception e) {
            System.err.println(" ShipEntity: 스킬 저장 실패 - " + e.getMessage());
        }
    }

    // 🔹 Firebase에서 스킬 불러오기
    public void loadSkillsFromCloud() {
        if (Game.SESSION_UID == null || Game.SESSION_ID_TOKEN == null) {
            System.out.println(" UID/TOKEN 없음 - 로그인 후에만 스킬 불러오기 가능");
            return;
        }
        try {
            LevelManager.loadSkills(Game.DB_URL, Game.SESSION_UID, Game.SESSION_ID_TOKEN, skills);
        } catch (Exception e) {
            System.err.println(" ShipEntity: 스킬 불러오기 실패 - " + e.getMessage());
        }
    }

}