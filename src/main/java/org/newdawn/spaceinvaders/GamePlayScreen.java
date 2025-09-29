package org.newdawn.spaceinvaders;

import org.newdawn.spaceinvaders.entity.Entity;
import org.newdawn.spaceinvaders.entity.ShipEntity;
import org.newdawn.spaceinvaders.PlayerSkills;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;

public class GamePlayScreen implements Screen{
    private Game game;
    private BackgroundRenderer backgroundRenderer;
    /**HUDRenderer필드 추가**/
    private HUDRenderer hudRenderer;

    //스탯 상승 관련 오버레이
    private boolean levelUpActive = false; // 오버레이 열렸는지
    private int levelUpIndex = 0;          // 0:공격력, 1:연사속도, 2:대시쿨
    private long levelUpOpenedAt = 0;      // (선택) 약간의 진입 애니메이션용

    //보기용 라벨
    private static final String[] LEVELUP_OPTIONS = {
            "공격력 +8%",
            "연사속도 +7%",
            "대시 쿨타임 -12%"
    };

    private final ShipEntity ship;

    public GamePlayScreen(Game game) {
        this.game = game;
        this.backgroundRenderer = new BackgroundRenderer();
        this.hudRenderer = new HUDRenderer(game);
        this.ship = game.getPlayerShip();
    }

    @Override
    public void render(Graphics2D g){
        // 웨이브별 배경 (10 웨이브마다 교체)
        backgroundRenderer.render(g, game.waveCount, 800, 600);

        // 엔티티 그리기
        for (Entity e : game.getEntities()) {
            e.draw(g);
        }

        //HUD는 플레이 화면에서만 표시
        hudRenderer.render(g);

        // "Press any key" 상태 표시
        if (game.isWaitingForKeyPress()) {
            g.setColor(Color.white);
            String msg = "Press any key";
            g.drawString(msg, (800 - g.getFontMetrics().stringWidth(msg)) / 2, 300);
        }

        if (levelUpActive) {
            drawLevelUpOverlay(g, ship);   // ← 오버레이 호출
        }

    }
    //화면 제일 위에 나오는 스코어, 하트 ,스테이지 등입니다.


    @Override
    public void update(long delta) {
        ShipEntity ship = game.getPlayerShip();

        // 포인트가 있고, 아직 오버레이가 안 떠 있으면 오픈
        if (ship.hasUnspentLevelUp() && !levelUpActive) {
            openLevelUpOverlay();
            // (선택) game.setPaused(true); 가 있다면 호출
        }

        // 오버레이 중에는 게임 로직 정지(엔티티 업데이트/스폰/탄막 등)
        if (levelUpActive) {
            return; // 드로우는 계속 되므로 화면은 멈춘 듯 보임
        }
        game.updateEntities(delta); // Game 내부 메서드로 엔티티 이동/충돌 처리
    }
    //레벨업 오버레이띄우기
    private void openLevelUpOverlay() {
        levelUpActive = true;
        levelUpIndex = 0;
        levelUpOpenedAt = System.currentTimeMillis();

        // 눌림 상태 초기화(선택)
        game.setLeftPressed(false);
        game.setRightPressed(false);
        game.setUpPressed(false);
        game.setDownPressed(false);
        game.setFirePressed(false);
    }

    //레벨업 오버레이 닫기
    private void closeLevelUpOverlay() {
        levelUpActive = false;
        // (선택) game.setPaused(false);
    }

    @Override
    public void handleKeyPress(int keyCode) {
        ShipEntity ship = game.getPlayerShip();
        //레벨업시 키조작
        if (levelUpActive) {
            if (keyCode == java.awt.event.KeyEvent.VK_UP) {
                levelUpIndex = (levelUpIndex + LEVELUP_OPTIONS.length - 1) % LEVELUP_OPTIONS.length;
                return;
            }
            if (keyCode == java.awt.event.KeyEvent.VK_DOWN) {
                levelUpIndex = (levelUpIndex + 1) % LEVELUP_OPTIONS.length;
                return;
            }
            if (keyCode == java.awt.event.KeyEvent.VK_ENTER || keyCode == java.awt.event.KeyEvent.VK_Z) {
                applyLevelUpChoice(ship, levelUpIndex); // ★ 실제 스탯 증가
                ship.spendLevelUpPoint();

                if (ship.hasUnspentLevelUp()) {
                    // 포인트가 남아 있으면 오버레이 유지 (연속 업그레이드)
                    levelUpIndex = 0;
                } else {
                    closeLevelUpOverlay();
                }
                return;
            }
            // 숫자 1~3으로도 바로 선택 가능
            if (keyCode == java.awt.event.KeyEvent.VK_1) { levelUpIndex = 0; applyLevelUpChoice(ship, 0); ship.spendLevelUpPoint(); finishOrStay(ship); return; }
            if (keyCode == java.awt.event.KeyEvent.VK_2) { levelUpIndex = 1; applyLevelUpChoice(ship, 1); ship.spendLevelUpPoint(); finishOrStay(ship); return; }
            if (keyCode == java.awt.event.KeyEvent.VK_3) { levelUpIndex = 2; applyLevelUpChoice(ship, 2); ship.spendLevelUpPoint(); finishOrStay(ship); return; }

            return; // ★ 오버레이 중엔 아래 플레이 입력 차단
        }

        if (keyCode == KeyEvent.VK_SHIFT) {
            ship.tryDash();   // ← 방금 만든 대시 호출
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

    private void finishOrStay(ShipEntity ship) {
        if (ship.hasUnspentLevelUp()) {
            levelUpIndex = 0;
        } else {
            closeLevelUpOverlay();
        }
    }

    private void applyLevelUpChoice(ShipEntity ship, int index) {
        PlayerSkills s = ship.getSkills();
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
        }
        // (선택) 효과음/플래시
        // game.playSfx("skill_up");
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

    //레벨업 오버레이 그리기
    private void drawLevelUpOverlay(java.awt.Graphics2D g2, ShipEntity ship) {
        // 반투명 배경
        g2.setColor(new java.awt.Color(0,0,0,160));
        g2.fillRect(0, 0, 800, 600); // 해상도에 맞게

        int panelW = 420, panelH = 220;
        int px = (800 - panelW)/2, py = (600 - panelH)/2;

        // 패널
        g2.setColor(new java.awt.Color(30, 30, 40, 230));
        g2.fillRoundRect(px, py, panelW, panelH, 16, 16);
        g2.setColor(new java.awt.Color(255, 215, 120));
        g2.drawRoundRect(px, py, panelW, panelH, 16, 16);

        // 제목
        g2.setFont(new java.awt.Font("Dialog", java.awt.Font.BOLD, 20));
        g2.drawString("레벨 업! 강화할 스탯을 선택하세요", px + 24, py + 40);

        // 현재 레벨 표기(선택)
        g2.setFont(new java.awt.Font("Dialog", java.awt.Font.PLAIN, 14));
        g2.drawString("남은 포인트: " + (ship.hasUnspentLevelUp()? "1+" : "0"), px + 24, py + 62);

        // 옵션 리스트
        int oy = py + 90;
        for (int i = 0; i < LEVELUP_OPTIONS.length; i++) {
            boolean sel = (i == levelUpIndex);
            if (sel) {
                g2.setColor(new java.awt.Color(255,255,255,40));
                g2.fillRoundRect(px + 14, oy - 18, panelW - 28, 28, 10, 10);
            }
            g2.setColor(java.awt.Color.WHITE);
            g2.setFont(new java.awt.Font("Dialog", sel? java.awt.Font.BOLD : java.awt.Font.PLAIN, 16));
            g2.drawString((i+1) + ". " + LEVELUP_OPTIONS[i], px + 24, oy);
            oy += 36;
        }

        // 설명
        g2.setFont(new java.awt.Font("Dialog", java.awt.Font.PLAIN, 12));
        g2.setColor(new java.awt.Color(220,220,220));
        g2.drawString("↑/↓ 이동, Enter(또는 숫자 1~3)로 확정", px + 24, py + panelH - 18);
    }


}
