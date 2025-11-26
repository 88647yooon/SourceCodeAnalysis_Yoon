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
    private boolean isPaused = false;

    private PauseOverlayScreen pauseOverlayScreen;

    public GamePlayScreen(Game game) {
        this.game = game;
        this.backgroundRenderer = new BackgroundRenderer();
        this.hudRenderer = new HUDRenderer(game);
        this.ship = game.getPlayerShip();

        this.levelUpOverlayScreen = new LevelUpOverlayScreen(this);
        this.pauseOverlayScreen = new PauseOverlayScreen(this);
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

    public void setPaused(boolean paused) { this.isPaused = paused; }
    public boolean isPaused() { return isPaused; }

    @Override
    public void render(Graphics2D g){
        Font uiFont;
        uiFont = new Font("맑은 고딕", Font.BOLD, 20);

        // 웨이브별 배경 (10 웨이브마다 교체)
        backgroundRenderer.render(g, game.getWaveCount(), 800, 600);

        // 엔티티 그리기
        for (Entity e : game.getEntities()) {
            e.draw(g);
        }

        //HUD는 플레이 화면에서만 표시
        hudRenderer.render(g);

        if (isPaused) {
            if (pauseOverlayScreen != null) {
                pauseOverlayScreen.draw(g); // 일시정지 화면 그리기
            }
        } else if (levelUpActive) {
            levelUpOverlayScreen.drawLevelUpOverlay(g, ship);
        }

        if (game.isWaitingForKeyPress()) {
            g.setColor(Color.white);
            g.setFont(uiFont);

            String winMsg = game.getMessage();
            String msg = "아무 키나 누르세요";

            int cx = 800;
            if(winMsg != null && !winMsg.isEmpty()){
                int winX = (cx - g.getFontMetrics().stringWidth(winMsg)) / 2;
                g.drawString(winMsg, winX, 260);
            }
            g.drawString(msg, (800 - g.getFontMetrics().stringWidth(msg)) / 2, 300);
        }

        if (levelUpActive) {
            levelUpOverlayScreen.drawLevelUpOverlay(g, ship);   // ← 오버레이 호출
        }

    }


    @Override
    public void update(long delta) {
        if (isPaused) {
            return;
        }

        ShipEntity s = game.getPlayerShip();
        if (s != null && s.getStats().hasUnspentLevelUp() && !levelUpActive) {
            levelUpOverlayScreen.openLevelUpOverlay();
            // (선택) game.setPaused(true); 가 있다면 호출
        }
    }



    @Override
    public void handleKeyPress(int keyCode){
        ShipEntity s = game.getPlayerShip();
        if (isPaused) {
            pauseOverlayScreen.handleKeyPress(keyCode);
            return;
        }
        if (levelUpActive) {
            levelUpOverlayScreen.handleKeyPress(keyCode);
            return;
        }
        handleGameInput(keyCode);
    }

    private void handleGameInput(int keyCode) {
        // ESC: 일시정지 진입
        if (keyCode == KeyEvent.VK_ESCAPE) {
            isPaused = true;
            return;
        }

        ShipEntity s = game.getPlayerShip();
        if (s == null) return;

        if (keyCode == KeyEvent.VK_SHIFT) {
            s.getDash().tryDash();
        }
        if (keyCode == KeyEvent.VK_LEFT) {
            game.setLeftPressed(true);
        }
        if (keyCode == KeyEvent.VK_RIGHT) {
            game.setRightPressed(true);
        }
        if (keyCode == KeyEvent.VK_UP) {
            game.setUpPressed(true);
        }
        if (keyCode == KeyEvent.VK_DOWN) {
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
