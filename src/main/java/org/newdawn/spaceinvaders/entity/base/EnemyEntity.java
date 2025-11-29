package org.newdawn.spaceinvaders.entity.base;
import org.newdawn.spaceinvaders.entity.projectile.ShotEntity;

public abstract class EnemyEntity extends Entity {

    protected int maxHP = 1;
    protected int currentHP = 1;

    protected EnemyEntity(String ref, int x, int y) {
        super(ref, x, y);
    }

    public void setMaxHP(int hp) {
        this.maxHP = Math.max(1, hp);
        this.currentHP = this.maxHP;
    }

    public int getCurrentHP() {
        return currentHP;
    }

    public int getMaxHP() {
        return maxHP;
    }

    public boolean takeDamage(int dmg) {
        currentHP -= Math.max(1, dmg);
        return currentHP <= 0;
    }

    //자식들이 각자 구현해야할 피격시 행동
    public abstract void wasHitBy(ShotEntity shot);
}
