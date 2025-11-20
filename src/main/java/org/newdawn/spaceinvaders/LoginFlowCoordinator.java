package org.newdawn.spaceinvaders;
import org.newdawn.spaceinvaders.DataBase.AuthSession;
import org.newdawn.spaceinvaders.DataBase.FirebaseAuthService;
import org.newdawn.spaceinvaders.DataBase.FirebaseDatabaseClient;


public class LoginFlowCoordinator {
    Game game;
    AuthFormState authFormState;

    public LoginFlowCoordinator(Game game, AuthFormState authFormState) {
        this.game = game;
        this.authFormState = authFormState;
    }

    public void tryAuth() {

        try {
            FirebaseAuthService.AuthResult ar;
            if (authFormState.isSignupMode()) {
                if (!authFormState.getPassword().equals(authFormState.getPassword2())) {
                    authFormState.setMessage("비밀번호가 일치하지 않습니다!");
                    return;
                }
                ar = game.getAuthService().signUp(authFormState.getEmail().trim(), authFormState.getPassword());
                // 기본 프로필 저장
                String profileJson = "{"
                        + "\"email\":" + FirebaseDatabaseClient.quote(ar.email) + ","
                        + "\"createdAt\":" + FirebaseDatabaseClient.quote(Game.now())
                        + "}";
                game.getDbClient().put("users/" + ar.localId + "/profile", ar.idToken, profileJson);
            } else {
                ar = game.getAuthService().signIn(authFormState.getEmail().trim(), authFormState.getPassword());
            }

            AuthSession s = new AuthSession(ar.localId, ar.email, ar.idToken);
            game.setSession(s);

            //로그인/회원가입 성공 시
            Game.SESSION_UID = ar.localId;
            Game.SESSION_EMAIL = ar.email;
            Game.SESSION_ID_TOKEN = ar.idToken;

            // 사용자별 별 기록 로드
            game.loadStageStars();
            int[] saved = LevelManager.loadLastLevel(Game.DB_URL, Game.SESSION_UID, Game.SESSION_ID_TOKEN);
            /// 과한 의존 시작
            game.getPlayerShip().getStats().setLevelAndXp(saved[0], saved[1]);
            PlayerSkills ps = game.getPlayerShip().getStats().getSkills();
            game.getPlayerShip().getPersistence().loadSkills(ps);
            LevelManager.loadSkills(Game.DB_URL, Game.SESSION_UID, Game.SESSION_ID_TOKEN, ps);
            /// 과한 의존 끝
            authFormState.setMessage((authFormState.isSignupMode() ? "회원가입" : "로그인") + " 성공!");
            game.setScreen(new MenuScreen(game)); //  메뉴 화면으로 이동
        } catch (Exception e) {
            authFormState.setMessage("실패: " + e.getMessage());
        }
    }
}
