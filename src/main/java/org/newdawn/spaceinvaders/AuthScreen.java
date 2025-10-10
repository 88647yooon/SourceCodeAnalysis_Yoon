package org.newdawn.spaceinvaders;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.Map;

public class AuthScreen implements Screen {
    private final Game game;

    private boolean signupMode = false; // false=로그인, true=회원가입
    private String email = "";
    private String password = "";
    private String password2 = ""; // 회원가입용
    private String message = "";
    private int fieldIndex = 0; // 0=email, 1=password, 2=password2 (회원가입 모드만)

    public AuthScreen(Game game) {
        this.game = game;
    }

    @Override
    public void render(Graphics2D g) {
        g.setColor(Color.black);
        g.fillRect(0, 0, 800, 600);

        g.setColor(Color.white);
        g.setFont(new Font("Dialog", Font.BOLD, 28));
        String title = signupMode ? "회원가입" : "로그인";
        g.drawString(title, (800 - g.getFontMetrics().stringWidth(title)) / 2, 100);

        g.setFont(new Font("Dialog", Font.PLAIN, 20));
        g.drawString("이메일: " + email + (fieldIndex == 0 ? "_" : ""), 200, 200);
        g.drawString("비밀번호: " + mask(password) + (fieldIndex == 1 ? "_" : ""), 200, 240);

        if (signupMode) {
            g.drawString("비밀번호 확인: " + mask(password2) + (fieldIndex == 2 ? "_" : ""), 200, 280);
        }

        g.setFont(new Font("Dialog", Font.PLAIN, 16));
        g.drawString("[Enter] 확인 | [Tab] 로그인/회원가입 전환 | ↑↓ 이동 | [ESC] 종료", 120, 360);

        if (!message.isEmpty()) {
            g.setColor(Color.yellow);
            g.drawString(message, 200, 420);
        }
    }

    private String mask(String pw) {
        if (pw == null) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < pw.length(); i++) {
            sb.append('*');
        }
        return sb.toString();
    }

    @Override
    public void update(long delta) {}

    @Override
    public void handleKeyPress(int keyCode) {
        if (keyCode == KeyEvent.VK_TAB) {
            signupMode = !signupMode;
            message = signupMode ? "회원가입 모드" : "로그인 모드";
            fieldIndex = 0;
            return;
        }
        if (keyCode == KeyEvent.VK_ENTER) {
            tryAuth();
            return;
        }
        if (keyCode == KeyEvent.VK_ESCAPE) {
            System.exit(0);
            return;
        }
        if (keyCode == KeyEvent.VK_UP) {
            fieldIndex = Math.max(0, fieldIndex - 1);
            return;
        }
        if (keyCode == KeyEvent.VK_DOWN) {
            int max = signupMode ? 2 : 1;
            fieldIndex = Math.min(max, fieldIndex + 1);
            return;
        }
    }

    @Override
    public void handleKeyRelease(int keyCode) {}

    private void tryAuth() {
        try {
            Game.AuthResult ar;
            if (signupMode) {
                if (!password.equals(password2)) {
                    message = "비밀번호가 일치하지 않습니다!";
                    return;
                }
                ar = Game.restSignUp(email.trim(), password);
                // 기본 프로필 저장
                Game.restSetJson("users/" + ar.localId + "/profile", ar.idToken,
                        "{\"email\":" + Game.quote(ar.email) + ",\"createdAt\":" + Game.quote(Game.now()) + "}");
            } else {
                ar = Game.restSignIn(email.trim(), password);
            }
            //로그인/회원가입 성공 시
            Game.SESSION_UID = ar.localId;
            Game.SESSION_EMAIL = ar.email;
            Game.SESSION_ID_TOKEN = ar.idToken;

            // 사용자별 별 기록 로드
            game.loadStageStars();

            int[] saved = LevelManager.loadLastLevel(Game.DB_URL, Game.SESSION_UID, Game.SESSION_ID_TOKEN);
            game.getPlayerShip().setLevelAndXp(saved[0], saved[1]);
            game.getPlayerShip().loadSkillsFromCloud();
            System.out.println("🎯 로그인 후 레벨 복원 완료: Lv." + saved[0] + " (XP " + saved[1] + ")");


            PlayerSkills ps = game.getPlayerShip().getSkills();
            LevelManager.loadSkills(Game.DB_URL, Game.SESSION_UID, Game.SESSION_ID_TOKEN, ps);
            message = (signupMode ? "회원가입" : "로그인") + " 성공!";
            game.setScreen(new MenuScreen(game)); // ✅ 메뉴 화면으로 이동
        } catch (Exception e) {
            message = "실패: " + e.getMessage();
        }
    }

    // 키보드 문자 입력 처리 (Game.KeyInputHandler에서 호출해줘야 함)
    public void handleCharTyped(char c) {
        if (c == '\b') { // 백스페이스
            if (fieldIndex == 0 && email.length() > 0) email = email.substring(0, email.length() - 1);
            if (fieldIndex == 1 && password.length() > 0) password = password.substring(0, password.length() - 1);
            if (fieldIndex == 2 && password2.length() > 0) password2 = password2.substring(0, password2.length() - 1);
            return;
        }
        if (Character.isISOControl(c)) return;

        if (fieldIndex == 0) email += c;
        if (fieldIndex == 1) password += c;
        if (fieldIndex == 2) password2 += c;
    }
}
