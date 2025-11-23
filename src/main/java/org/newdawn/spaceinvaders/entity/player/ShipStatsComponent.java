package org.newdawn.spaceinvaders.entity.player;

public class ShipStatsComponent {
    private int maxHP = 3;
    private int currentHP = 3;

    //레벨 시스템
    private static int S_LEVEL = 1;
    private static int S_XP_INTO_LEVEL = 0;
    private int level = 1;
    private int xpIntoLevel = 0;
    private int xpToNext = reqFor(1);
    private int levelUpPoints = 0;

    private final PlayerSkills skills = new PlayerSkills();

    //무적 관리
    private long lastDamageTime = 0;
    private long invincibleDuration = 500;
    private boolean forceInvulnerable = false; //치트

    public ShipStatsComponent(){
        this.level = S_LEVEL;
        this.xpIntoLevel = S_XP_INTO_LEVEL;
        this.xpToNext = reqFor(level);

    }
    public int getXpPercent() {
        if (xpToNext <= 0) return 100; // 0으로 나누기 방지

        // 자신의 필드(xpIntoLevel, xpToNext)를 직접 사용
        return (int)Math.round(100.0 * xpIntoLevel / xpToNext);
    }

    public int getXpToNextLevel() {
        return xpToNext;
    }

    public void setLevelAndXp(int lvl, int xp) {
        this.level = Math.max(1, lvl);
        this.xpIntoLevel = Math.max(0, xp);

        // 다음 레벨업 경험치 재계산
        this.xpToNext = reqFor(this.level);

        updateSharedState(this.level,this.xpToNext);
    }

    //체력 관련
    public int getMaxHP(){
        return maxHP;
    }
    public int getCurrentHP(){
        return currentHP;
    }
    public boolean takeDamage(int dmg,long now){
        if (isInverlnerable(now)) return false;

        currentHP -= dmg;
        lastDamageTime = now;
        return true;
    }

    public boolean isDead(){
        return currentHP <=0;
    }

    public boolean isInverlnerable(long now){
        boolean postHit = (now - lastDamageTime) < invincibleDuration;
        return forceInvulnerable || postHit;
    }
    public void setInvulnerable(boolean inv){
        this.forceInvulnerable = inv;
    }

    //레벨업 관련 요구
    private static int reqFor(int L){
        return 200+50*L*(L-1);
    }

    private static void updateSharedState(int level,int xp){
        S_LEVEL = level;
        S_XP_INTO_LEVEL = xp;
    }

    public void addXp(int amount){
        if(amount <=0) return;
        xpIntoLevel += amount;
        while(xpIntoLevel >=xpToNext){
            xpIntoLevel -= xpToNext;
            level ++;
            xpToNext = reqFor(level);
            levelUpPoints++;
        }
        updateSharedState(S_LEVEL,xpIntoLevel);
    }

    public boolean hasUnspentLevelUp() {
        return levelUpPoints > 0;
    }

    public void grantLevelUpPoint() {
        levelUpPoints++;
    }

    public void spendLevelUpPoint() {
        if (levelUpPoints > 0) {
            levelUpPoints--;
        }
    }


    public PlayerSkills getSkills() { return skills; }
    public int getLevel() { return level; }
    public int getXpIntoLevel() { return xpIntoLevel; }

}
