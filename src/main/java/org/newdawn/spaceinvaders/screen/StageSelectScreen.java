package org.newdawn.spaceinvaders.screen;

import org.newdawn.spaceinvaders.Game;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;

public class StageSelectScreen implements Screen {
    private Game game;
    private StageSelectImageRenderer renderer;
    private static final int MAX_STAGE = 5;

    //스테이지 이미지
    final Image[] stageImages = new Image[MAX_STAGE];

    private float currentIndex = 0;   // 현재 인덱스 (부드럽게 이동)
    private int selectIndex = 0;
        

    //잠긴 스테이지 안내 메시지 관련
    private String lockMessage ="";
    private long lockMessageTimer = 0;


    public StageSelectScreen(Game game) {
        this.game = game;
        renderer = new StageSelectImageRenderer(game, this);
        loadStageImages();
        refreshStageUnlocks();
    }
    public float getCurrentIndex() {
        return currentIndex;
    }

    private void loadStageImages() {
        for (int i = 0; i < MAX_STAGE; i++) {
            String path = "/sprites/stage" + (i + 1) + ".png";
            java.net.URL url = getClass().getResource(path);

            if (url != null) {
                stageImages[i] = new ImageIcon(url).getImage();
            } else {
                stageImages[i] = null;
            }
        }
    }



    /** Game 쪽 정보 기반으로 스테이지 잠금 상태 갱신 */
    private void refreshStageUnlocks() {
        game.rebuildStageUnlocks();
    }


    @Override
    public void render(Graphics2D g) {
        int spacing = 220; // 카드 간격
        g.setColor(Color.black);
        g.fillRect(0, 0, 800, 600);
        int centerX = 400;
        int y = 300;

        // 부드러운 효과
        currentIndex += (selectIndex - currentIndex) * 0.2f;

        for (int i = 0; i < MAX_STAGE; i++) {
            float relative = i - currentIndex;

            // x 좌표 (중앙에서 relativeIndex만큼 간격 이동)
            int x = centerX + Math.round(relative * spacing);
            // 선택된 스테이지는 크게, 나머지는 작게
         renderer.renderSingleStage(g,i,x,y,centerX);

        }

        if (lockMessage != null && !lockMessage.isEmpty()) {
            long elapsed = System.currentTimeMillis() - lockMessageTimer;
            if (elapsed < 2000) { // 2초 동안 표시
                g.setFont(new Font("Dialog", Font.BOLD, 24));
                g.setColor(new Color(255, 100, 100));
                g.drawString(lockMessage, 270, 80);
            } else {
                lockMessage = ""; // 시간 지나면 제거
            }
        }

        // 안내 텍스트
        g.setFont(new Font("Dialog", Font.BOLD, 18));
        g.setColor(Color.LIGHT_GRAY);
        g.drawString("← → 키로 선택, ENTER로 확정, ESC로 메뉴로 돌아가기", 160, 550);
    }



    @Override
    public void update(long delta) {
       //여기서는 업데이트 할게 없음
    }

    @Override
    public void handleKeyPress(int keyCode) {
        if (keyCode == KeyEvent.VK_LEFT && selectIndex > 0) {
                selectIndex--;
        }
        if (keyCode == KeyEvent.VK_RIGHT && selectIndex < MAX_STAGE - 1) {
                selectIndex++;
        }
        if (keyCode == KeyEvent.VK_ESCAPE) {
            game.goToMenuScreen();
            return;
        }
        enterPressed(keyCode);

    }

    private void enterPressed(int keyCode) {
        if (keyCode == KeyEvent.VK_ENTER) {
            int stageNum = selectIndex + 1;
            if (game.isStageUnlocked(selectIndex)) {
                //  중복 초기화 제거: startStageMode 내부에서 StageManager.applyStage까지 처리됨
                game.startStageMode(stageNum);
            } else {
                if (selectIndex > 0) {
                    int prevStars = game.getStageStars(selectIndex); // 이전 스테이지는 (index) == (stageNum-1)
                    if (prevStars < 3) {
                        lockMessage = "잠금: 이전 스테이지 3★ 달성 필요";
                    } else {
                        lockMessage = "아직 잠긴 스테이지입니다.";
                    }
                } else {
                    lockMessage = "아직 잠긴 스테이지입니다.";
                }
                lockMessageTimer = System.currentTimeMillis();
            }
        }
    }

    @Override
    public void handleKeyRelease(int keyCode) {
        //여기서는 구현 안해도 됨
    }


}
