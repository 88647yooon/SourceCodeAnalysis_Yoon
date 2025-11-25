package org.newdawn.spaceinvaders.screen;

import org.newdawn.spaceinvaders.graphics.BackgroundRenderer;
import org.newdawn.spaceinvaders.Game;
import org.newdawn.spaceinvaders.graphics.HUDRenderer;
import org.newdawn.spaceinvaders.entity.base.Entity;
import org.newdawn.spaceinvaders.entity.player.ShipEntity;

import java.awt.*;
import java.awt.event.KeyEvent;

public class GamePlayScreen implements Screen {
    private Game game;
    private BackgroundRenderer backgroundRenderer;
    private LevelUpOverlayScreen levelUpOverlayScreen = new LevelUpOverlayScreen(this);
    private HUDRenderer hudRenderer;
    private final ShipEntity ship;
    //스탯 상승 관련 오버레이
    private boolean levelUpActive = false; // 오버레이 열렸는지
    private int levelUpIndex = 0;          // 0:공격력, 1:연사속도, 2:대시쿨

    public GamePlayScreen(Game game) {
        this.game = game;
        this.backgroundRenderer = new BackgroundRenderer();
        this.hudRenderer = new HUDRenderer(game);
        this.ship = game.getPlayerShip();
    }

    public Game getGame(){
        return this.game;
    }
    public ShipEntity getShip(){
        return this.ship;
    }


    public int getLevelUpIndex(){
        return levelUpIndex;
    }
    public void setLevelUpIndex(int levelUpIndex){
        this.levelUpIndex = levelUpIndex;
    }

    public void setLevelUpActive(boolean active){
        levelUpActive = active;
    }
    public boolean getLevelUpActive(){
        return levelUpActive;
    }

    @Override
    public void render(Graphics2D g){
        // 웨이브별 배경 (10 웨이브마다 교체)
        backgroundRenderer.render(g, game.getWaveCount(), 800, 600);

        // 엔티티 그리기
        for (Entity e : game.getEntities()) {
            e.draw(g);
        }

        //HUD는 플레이 화면에서만 표시
        hudRenderer.render(g);

        if (levelUpActive) {
            levelUpOverlayScreen.drawLevelUpOverlay(g, ship);   // ← 오버레이 호출
        }

    }

    @Override
    public void update(long delta) {
        ShipEntity s = game.getPlayerShip();


        if (s.getStats().hasUnspentLevelUp() && !levelUpActive) {
            levelUpOverlayScreen.openLevelUpOverlay();
            // (선택) game.setPaused(true); 가 있다면 호출
        }

        if (levelUpActive) {
            return;
        }
        game.updateEntities(delta);
    }



    @Override
    public void handleKeyPress(int keyCode){
        ShipEntity s = game.getPlayerShip();
        //레벨업시 키조작
        levelUpOverlayScreen.handleKeyPress(keyCode);

        if (keyCode == KeyEvent.VK_SHIFT) {
            s.getDash().tryDash();   // ← 방금 만든 대시 호출
        }
        if (keyCode == KeyEvent.VK_LEFT) {
            game.setLeftPressed(true);
        }
        if (keyCode == KeyEvent.VK_RIGHT) {
            game.setRightPressed(true);
        }
        if(keyCode == KeyEvent.VK_UP) {
            game.setUpPressed(true);
        }
        if(keyCode == KeyEvent.VK_DOWN) {
            game.setDownPressed(true);
        }
        if (keyCode == KeyEvent.VK_SPACE) {
            game.setFirePressed(true);
        }
    }


    @Override
    public void handleKeyRelease(int keyCode) {
        if (keyCode == KeyEvent.VK_LEFT) {
            game.setLeftPressed(false);
        }
        if (keyCode == KeyEvent.VK_RIGHT) {
            game.setRightPressed(false);
        }
        if(keyCode == KeyEvent.VK_UP) {
            game.setUpPressed(false);
        }
        if (keyCode == KeyEvent.VK_DOWN) {
            game.setDownPressed(false);
        }
        if (keyCode == KeyEvent.VK_SPACE) {
            game.setFirePressed(false);
        }
    }

}
