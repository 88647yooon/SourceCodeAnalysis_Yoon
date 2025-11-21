package org.newdawn.spaceinvaders.entity.player;

import org.newdawn.spaceinvaders.Game;
import org.newdawn.spaceinvaders.LevelManager;
import org.newdawn.spaceinvaders.PlayerSkills;
import org.newdawn.spaceinvaders.entity.Entity;
import org.newdawn.spaceinvaders.entity.enemy.AlienEntity;
import org.newdawn.spaceinvaders.entity.projectile.EnemyShotEntity;

import java.awt.Graphics;
import java.awt.Graphics2D;

/**
 * The entity that represents the players ship
 * 
 * @author Kevin Glass
 */
public class ShipEntity extends Entity {
	private final Game game;

    private final ShipStatsComponent stats;
    private final ShipDashComponent dash;
    private final ShipPersistenceComponent persistence;


    public ShipEntity(Game game, String ref, int x, int y) {
        super(ref, x, y);
        this.game = game;

        // 컴포넌트 조립
        this.stats = new ShipStatsComponent();
        this.dash = new ShipDashComponent(this); // 대시는 이동을 위해 Entity 참조 필요
        this.persistence = new ShipPersistenceComponent();
    }

    //컴포넌트 Getter추가
    public ShipStatsComponent getStats() { return stats;}
    public ShipDashComponent getDash(){return dash;}
    public ShipPersistenceComponent getPersistence(){return persistence;}

    @Override
    public void move(long delta) {
        long now = System.currentTimeMillis();

        //대시 처리위임
        dash.update(delta,now);

        //이동 경계 처리
        clampPosition();

        //장상 정리 등 나머지 로직
        super.move(delta);
    }
    public void setX(int x) {
        this.x = x;
    }
    public void setY(int y) {
        this.y = y;
    }

    //경계 처리 로직
    private void clampPosition(){
        if (y < 10) { y = 10; stopVerticalIfDashing(); }
        if (y > 568) { y = 568; stopVerticalIfDashing(); }
        if (x < 10) { x = 10; stopHorizontalIfDashing(); }
        if (x > 750) { x = 750; stopHorizontalIfDashing(); }
    }

    private void stopVerticalIfDashing() {
        if (dash.isDashing()) { dash.StopDash(); setVerticalMovement(0); }
    }
    private void stopHorizontalIfDashing() {
        if (dash.isDashing()) { dash.StopDash(); setHorizontalMovement(0); }
    }

    @Override
    public void collidedWith(Entity other) {
        if (other instanceof AlienEntity || other instanceof EnemyShotEntity) {
            damage(1);//즉사였는데 HP -1 로 바꿈

            if(other instanceof EnemyShotEntity){
                game.removeEntity(other);
            }
        }
    }

    public void damage(int d) {
        long now = System.currentTimeMillis();

        // 무적 상태 체크
        if (isInvulnerable()) return;

        boolean hit = stats.takeDamage(d, now);

        if(hit){
            game.onPlayerHit();
            if(stats.isDead()){
                game.notifyDeath();
            }
        }

    }

    public boolean isInvulnerable(){
        long now = System.currentTimeMillis();

        //대시무적 ||피격 무적||치트 무적
        return dash.isInvulnerabe(now) || stats.isInverlnerable(now);
    }

    @Override
    public void draw(Graphics g) {
        dash.drawTrails((Graphics2D)g, sprite); // 잔상 그리기
        super.draw(g);
    }
}