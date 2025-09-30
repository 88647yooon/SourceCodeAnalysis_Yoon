package org.newdawn.spaceinvaders;
import java.awt.*;
import java.util.List;

public class ScoreboardScreen implements Screen {
    private final Game game;
    private List<Game.ScoreEntry> scores;
    private boolean showGlobal = true; // 🔄 글로벌/내 점수 전환 플래그

    public ScoreboardScreen(Game game) {
        this.game = game;
        reloadScores();
    }

    public void reloadScores() {
        if (showGlobal) {
            this.scores = Game.fetchGlobalTopScores(20);
        } else {
            this.scores = Game.fetchMyTopScores(20);
        }
    }

    @Override
    public void render(Graphics2D g) {
        g.setColor(Color.black);
        g.fillRect(0,0,800,600);

        g.setColor(Color.white);
        g.setFont(new Font("Dialog", Font.BOLD, 28));
        String title = showGlobal ? "🌍 글로벌 스코어보드" : "📌 내 스코어보드";
        g.drawString(title, (800 - g.getFontMetrics().stringWidth(title))/2, 80);

        g.setFont(new Font("Dialog", Font.PLAIN, 14));
        g.drawString("ESC: 뒤로 | TAB: 전환", 20, 30);

        int startY = 140;
        g.setFont(new Font("Monospaced", Font.BOLD, 16));
        g.drawString(String.format("%-6s %-8s %-10s %-20s", "순위", "점수", "모드", "플레이시간 / 날짜"), 120, startY);

        g.setFont(new Font("Monospaced", Font.PLAIN, 16));
        int y = startY + 30;
        for (int i = 0; i < scores.size(); i++) {
            Game.ScoreEntry s = scores.get(i);
            String line = String.format("%-6s %-8s %-10s %-20s",
                    (i+1), (s.score==null?0:s.score),
                    (s.mode==null?"-":s.mode),
                    ((s.durationMs==null?0:s.durationMs) + " / " + (s.timestamp==null?"-":s.timestamp))
            );
            g.drawString(line, 120, y);
            y += 28;
        }

        if (scores.isEmpty()) {
            g.drawString("기록이 없습니다!", 200, startY + 40);
        }
    }

    @Override public void update(long delta) { }

    @Override
    public void handleKeyPress(int keyCode) {
        if (keyCode == java.awt.event.KeyEvent.VK_TAB) {
            showGlobal = !showGlobal;
            reloadScores();
        }
    }

    @Override public void handleKeyRelease(int keyCode) { }
}
