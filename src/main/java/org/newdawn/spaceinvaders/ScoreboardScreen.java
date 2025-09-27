package org.newdawn.spaceinvaders;
import java.awt.*;
import java.util.List;
/// 제발 좀 커밋 제발제발
public class ScoreboardScreen implements Screen {
    private final Game game;
    private List<Game.ScoreEntry> scores;

    public ScoreboardScreen(Game game) {
        this.game = game;
        this.scores = game.fetchMyTopScores(20); // 상위 20개
    }

    @Override
    public void render(Graphics2D g) {
        // 배경
        g.setColor(Color.black);
        g.fillRect(0,0,800,600);

        // 제목
        g.setColor(Color.white);
        g.setFont(new Font("Dialog", Font.BOLD, 28));
        String title = "내 스코어보드";
        g.drawString(title, (800 - g.getFontMetrics().stringWidth(title))/2, 80);

        // 안내
        g.setFont(new Font("Dialog", Font.PLAIN, 14));
        String hint = "ESC: 뒤로";
        g.drawString(hint, 20, 30);

        // 표 헤더
        int startY = 140;
        g.setFont(new Font("Monospaced", Font.BOLD, 16));
        g.drawString(String.format("%-6s %-8s %-10s %-20s", "순위", "점수", "모드", "플레이시간(ms) / 날짜"), 120, startY);

        // 데이터
        g.setFont(new Font("Monospaced", Font.PLAIN, 16));
        int y = startY + 30;
        for (int i = 0; i < scores.size(); i++) {
            Game.ScoreEntry s = scores.get(i);
            String line = String.format("%-6s %-8s %-10s %-20s",
                    (i+1), (s.score==null?0:s.score),
                    (s.mode==null?"-":s.mode),
                    ( (s.durationMs==null?0:s.durationMs) + " / " + (s.timestamp==null?"-":s.timestamp) )
            );
            g.drawString(line, 120, y);
            y += 28;
        }

        if (scores.isEmpty()) {
            g.drawString("기록이 없습니다. 한 판 플레이해보세요!", 200, startY + 40);
        }
    }

    @Override public void update(long delta) { }
    @Override public void handleKeyPress(int keyCode) { }
    @Override public void handleKeyRelease(int keyCode) { }
}