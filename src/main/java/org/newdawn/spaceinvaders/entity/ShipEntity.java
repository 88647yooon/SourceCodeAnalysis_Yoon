package org.newdawn.spaceinvaders.entity;

import org.newdawn.spaceinvaders.Game;
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

    // === Level / XP (no stat effects) ===
    private int level = 1;
    private int xpIntoLevel = 0;
    private int xpToNext = reqFor(1);

    //레벨 관련 필드
    private int levelUpPoints = 0;

    //레벨업시 스탯관련 필드
    public boolean hasUnspentLevelUp() { return levelUpPoints > 0; }
    public void grantLevelUpPoint()    { levelUpPoints++; }
    public void spendLevelUpPoint()    { if (levelUpPoints > 0) levelUpPoints--; }

    //스텟 관련 생성
    private final PlayerSkills skills = new PlayerSkills();
    public PlayerSkills getSkills() { return skills; } // (HUD나 메뉴에서 레벨업 때 접근)

    // 발사/대시 기준값(네 프로젝트 기준에 맞춰 값 사용)
    private long baseShotIntervalMs = 220;   // 기존 사격 간격
    private int  baseShotDamage     = 1;     // 기본 탄 피해
    private long lastShotAt         = 0;

    private long baseDashCooldownMs = 1200;  // 대시 쿨 기준
    private int  baseDashIframesMs  = 220;   // 대시 무적 기준
    private long lastDashAt         = 0;
    private long invulnUntil        = 0;     // 플레이어 무적 종료 시각


    //대시 관련 필드
    private boolean dashing = false;
    private long dashStartAt = 0L;
    private long dashDurationMs = 140;//실제 이동 지속(ms): 120~180 튜닝 권장
    private int dashDistancePx = 1200;//위로 이동할 총 거리
    private long lastTrailAt = 0L;

    // 잔상 파라미터
    private static final int TRAIL_INTERVAL_MS = 22;   // 스냅샷 간격
    private static final int TRAIL_LIFETIME_MS = 240;  // 잔상 유지시간
    private static final int TRAIL_MAX = 10;           // 최대 스냅샷 개수

    // 화면 경계(지금 move에서 쓰는 값과 동일하게 맞춤)
    private static final int TOP_MARGIN = 10;
    private static final int BOTTOM_LIMIT = 568;

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

    //무적 관련 메소드
    public boolean isInvulnerable() {
        long now = System.currentTimeMillis();
        boolean dashIFrames = now < invulnUntil;
        boolean postHitIFrames = (now - lastDamageTime) < invincible; // 500ms
        return invulnerable || dashIFrames || postHitIFrames;
    }
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
        long now = System.currentTimeMillis();
        // 대시 중이면 수직 속도 유지 + 잔상 스냅샷 적층
        if (dashing) {
            setHorizontalMovement(0); // 수직 대시이므로 항상 0

            // 잔상: 18~24ms 간격으로 스냅샷 쌓기
            if (now - lastTrailAt >= 22) {
                dashTrail.addFirst(new Trail((int)x, (int)y, now));
                while (dashTrail.size() > 8) dashTrail.removeLast(); // 개수 제한
                lastTrailAt = now;
            }

            // 시간 만료 → 대시 종료 + 도착 잔상
            if (now - dashStartAt >= dashDurationMs) {
                dashing = false;
                setVerticalMovement(0);
                spawnArrivalEchoes(now); //  도착 잔상
            }
        }

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
    // 도착 잔상: 현재 위치로 몇 개 더 넣어 "도착감" 강화
    private void spawnArrivalEchoes(long now) {
        final int n = 3;                   // 필요하면 4~5로
        final int step = TRAIL_INTERVAL_MS;
        for (int i = 0; i < n; i++) {
            dashTrail.addFirst(new Trail((int)x, (int)y, now - i * step));
        }
        while (dashTrail.size() > TRAIL_MAX) dashTrail.removeLast();
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
    //스텟관련 추가
    private long currentShotInterval() { return (long)Math.round(baseShotIntervalMs * skills.rofIntervalMul()); }
    private int  currentShotDamage()   { return Math.max(1, (int)Math.round(baseShotDamage * skills.atkMul())); }

    private void tryFire() {
        long now = System.currentTimeMillis();
        if (now - lastShotAt < currentShotInterval()) return;
        lastShotAt = now;

        // ShotEntity 생성 시 데미지 값을 넘겨주도록 약간 확장
        ShotEntity shot = new ShotEntity(game, "sprites/shot.png", getX()+sprite.getWidth()/2, getY());
        shot.setDamage(currentShotDamage());      // ← setDamage 추가(아래 참고)
        game.addEntity(shot);
    }
    //대시 관련 스탯 메소드
    private long currentDashCooldown() { return (long)Math.round(baseDashCooldownMs * skills.dashCdMul()); }
    private int  currentDashIframes()  { return baseDashIframesMs + skills.dashIframesBonusMs(); }


    public void tryDash() {
        long now = System.currentTimeMillis();
        if (now - lastDashAt < currentDashCooldown()) return;

        lastDashAt = now;
        dashStartAt = now;
        dashing = true;

        // 위로 이동 가능한 최대 거리(화면 상단까지)로 클램프 → 텔레포트 느낌 방지
        int roomUp = (int)Math.max(0, y - TOP_MARGIN);
        int actualDistance = Math.min(dashDistancePx, roomUp);

        // 실제 이동할 거리 기준으로 등속도 설정
        double vy = -(dashDistancePx * 1000.0) / dashDurationMs; // 음수 = 위로
        setHorizontalMovement(0);      // 수평 입력과 독립
        setVerticalMovement(vy);

        // 대시 무적 (skills가 반영된 currentDashIframes() 사용)
        invulnUntil = now + currentDashIframes();

        // 출발 잔상 + 타임스탬프 초기화
        dashTrail.addFirst(new Trail((int)x, (int)y, now));
        lastTrailAt = now;
    }
    @Override
    public void draw(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        long now = System.currentTimeMillis();

        int i = 0;
        for (Trail tr : dashTrail) {
            float age = (now - tr.t) / (float)TRAIL_LIFETIME_MS; // ← 상수 사용
            float alpha = Math.max(0f, 0.36f * (1f - age));
            if (alpha <= 0f) continue;

            Composite old = g2.getComposite();
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));

            int tailOffsetY = Math.min(14, i * 2);  // 아래로 끌리는 꼬리
            g2.drawImage(sprite.getImage(), tr.x, tr.y + tailOffsetY, null);

            g2.setComposite(old);
            i++;
        }

        g.drawImage(sprite.getImage(), (int)x, (int)y, null);
    }


}