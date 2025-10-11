package org.newdawn.spaceinvaders;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;

public class StageSelectScreen implements Screen{
    private Game game;

    private final int MaxStage = 5;

    //스테이지 이미지
    private final Image[] stageImages = new Image[MaxStage];

    //별 이미지
    private Image starFilled;
    private Image starEmpty;

    // 애니메이션용 오프셋
    private int Baseicon = 120;
    private int selectedSize = 200;

    private float currentIndex = 0;   // 현재 인덱스 (부드럽게 이동)
    private int SelectIndex = 0;
    private float slideSpeed = 0.2f;  // 보간 속도
    private int spacing = 220;        // 카드 간격

    //잠긴 스테이지 안내 메시지 관련
    private String lockMessage ="";
    private long lockMessageTimer = 0;


    public StageSelectScreen(Game game) {
        this.game = game;

        // 스테이지 이미지 로드 (없으면 기본 사각형)
        for (int i = 0; i < MaxStage; i++) {
            String path = "/sprites/stage" + (i + 1) + ".png";
            java.net.URL url = getClass().getResource(path);
            if (url != null) {
                stageImages[i] = new ImageIcon(url).getImage();
                System.out.println(" 로드 성공: " + path);
            } else {
                System.out.println(" 로드 실패: " + path);
                stageImages[i] = null;
            }
        }
        game.rebuildStageUnlocks();

        //별 이미지 로드
        starFilled = new ImageIcon(getClass().getResource("/sprites/star_filled.png")).getImage();
        starEmpty = new ImageIcon(getClass().getResource("/sprites/star_empty.png")).getImage();
    }

    @Override
    public void render(Graphics2D g) {
        g.setColor(Color.black);
        g.fillRect(0, 0, 800, 600);
        int centerX = 400;
        int y = 300;

        // 부드러운 효과
        currentIndex += (SelectIndex - currentIndex) * slideSpeed;

        for (int i = 0; i < MaxStage; i++) {
            float relative = i - currentIndex;

            // x 좌표 (중앙에서 relativeIndex만큼 간격 이동)
            int x = centerX + Math.round(relative * spacing);
            // 선택된 스테이지는 크게, 나머지는 작게
            int size = (Math.round(currentIndex) == i) ? selectedSize : Baseicon;

            int drawX = x - size / 2;
            int drawY = y - size / 2;

            boolean unlocked = game.isStageUnlocked(i);

            if (Math.round(currentIndex) == i) {
                g.setStroke(new BasicStroke(4));
                g.setColor(Color.BLACK);
                g.drawOval(drawX - 4, drawY - 4, size + 8, size + 8);

                g.setFont(new Font("Arial", Font.BOLD, 24));
                g.drawString("Stage " + (i + 1), centerX - 40, y + selectedSize / 2 + 40);

                if (!unlocked) {
                    g.setFont(new Font("Arial", Font.BOLD, 20));
                    g.setColor(Color.RED);
                    g.drawString("Locked", centerX - 40, y + selectedSize / 2 + 70);
                }
            }

            if (stageImages[i] != null) {
                g.drawImage(stageImages[i], drawX, drawY, size, size, null);
            } else {
                g.setColor(Color.RED);
                g.fillOval(drawX, drawY, size, size);
            }

            if (Math.round(currentIndex) == i) {
                g.setStroke(new BasicStroke(4));
                g.drawOval(drawX - 4, drawY - 4, size + 8, size + 8);

                g.setFont(new Font("Arial", Font.BOLD, 24));
                g.setColor(Color.WHITE);
                g.drawString("Stage " + (i + 1),
                        centerX - 40, y + selectedSize / 2 + 40);


            }

            //모든 스테이지 아이콘 아래 별 표시
            int stars = game.getStageStars(i + 1);
            int starSize = 30;
            int starSpacing = 36;
            int starY = drawY + size - 15;
            int starX = x - (starSpacing * 3) / 2;

            for (int s = 0; s < 3; s++){
                Image starImg = (s <stars) ? starFilled : starEmpty;
                if(starImg != null){
                    g.drawImage(starImg, starX + s * starSpacing,
                            starY, starSize, starSize, null);
                }

            }

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

    }

    @Override
    public void handleKeyPress(int keyCode) {
        if (keyCode == KeyEvent.VK_LEFT && SelectIndex > 0) {
                SelectIndex--;
        }
        if (keyCode == KeyEvent.VK_RIGHT && SelectIndex < MaxStage - 1) {
                SelectIndex++;
        }

        if (keyCode == KeyEvent.VK_ENTER) {
            int stageNum = SelectIndex + 1;
            if (game.isStageUnlocked(SelectIndex)) {
                //  중복 초기화 제거: startStageMode 내부에서 StageManager.applyStage까지 처리됨
                game.startStageMode(stageNum);
            } else {
                if (SelectIndex > 0) {
                    int prevStars = game.getStageStars(SelectIndex); // 이전 스테이지는 (index) == (stageNum-1)
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
    public void handleKeyRelease(int keyCode) {}


}
