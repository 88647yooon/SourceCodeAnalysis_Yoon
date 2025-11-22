package org.newdawn.spaceinvaders;

import org.newdawn.spaceinvaders.Screen.Screen;
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

        // "Press any key" 상태 표시
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

    public void finishOrStay(ShipEntity ship) {
        if (ship.getStats().hasUnspentLevelUp()) {
            levelUpIndex = 0;
        } else {
            levelUpOverlayScreen.closeLevelUpOverlay();
        }

    }

    public void applyLevelUpChoice(ShipEntity ship, int index) {
        PlayerSkills s = ship.getStats().getSkills();

        switch (index) {
            case 0: // 공격력
                s.atkLv = Math.min(5, s.atkLv + 1);
                break;
            case 1: // 연사속도(간격 감소)
                s.rofLv = Math.min(5, s.rofLv + 1);
                break;
            case 2: // 대시 쿨타임 감소
                s.dashLv = Math.min(5, s.dashLv + 1);
                break;
            default:
                break;
        }
        ship.getPersistence().saveSkills(s);

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
