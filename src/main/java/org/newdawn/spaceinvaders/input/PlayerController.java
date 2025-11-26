package org.newdawn.spaceinvaders.input;

import org.newdawn.spaceinvaders.Game;
import org.newdawn.spaceinvaders.entity.player.ShipEntity;
import org.newdawn.spaceinvaders.entity.projectile.ShotEntity;
import org.newdawn.spaceinvaders.manager.SoundManager;

public class PlayerController {

    private final Game game;
    private final InputState inputState = new InputState();
    private ShipEntity ship;

    private long lastFire = 0;

    public PlayerController(Game game) {
        this.game = game;
    }

    public InputState getInputState() {
        return inputState;
    }

    public void setShip(ShipEntity ship) {
        this.ship = ship;
    }

    /** 매 프레임 호출해서 입력 상태 → Ship 움직임/발사로 변환 */
    public void update() {
        if (ship == null) {
            return;
        }
        //대시 중일때는 컨트롤러가 움직임을 방해하지 않도록 리턴
        if(ship.getDash().isDashing()){
            return;
        }

        ship.setHorizontalMovement(0);
        ship.setVerticalMovement(0);

        // 이동 처리
        // 원래 Game에 있던 것들 이동
        double moveSpeed = 300;
        if (inputState.isUp() && !inputState.isDown()) {
            ship.setVerticalMovement(-moveSpeed);
        } else if (inputState.isDown() && !inputState.isUp()) {
            ship.setVerticalMovement(moveSpeed);
        }

        if (inputState.isLeft() && !inputState.isRight()) {
            ship.setHorizontalMovement(-moveSpeed);
        } else if (inputState.isRight() && !inputState.isLeft()) {
            ship.setHorizontalMovement(moveSpeed);
        }

        // 발사 처리
        if (inputState.isFire()) {
            tryToFire();
        }
    }

    private void tryToFire() {
        long now = System.currentTimeMillis();
        long baseInterval = 500;
        long firingInterval = (long) (baseInterval * ship.getStats().getSkills().fireCooldownMul());

        if (now - lastFire < firingInterval || ship == null) {
            return;
        }
        lastFire = now;

        ShotEntity shot = new ShotEntity(
                game,
                "sprites/shot.gif",
                ship.getX() + 10,
                ship.getY() - 30
        );
        int currentAtkLv = ship.getStats().getSkills().atkLv;
        shot.setDamage(1 + currentAtkLv);

        game.addEntity(shot);
        SoundManager.getSound().playSfx(SoundManager.Sfx.SHOOT);
    }

}
