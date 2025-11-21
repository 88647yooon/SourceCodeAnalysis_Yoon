package org.newdawn.spaceinvaders.entity.player;

import org.newdawn.spaceinvaders.Game;
import org.newdawn.spaceinvaders.LevelManager;
import org.newdawn.spaceinvaders.PlayerSkills;
public class ShipPersistenceComponent {
    // 저장
    public void saveSkills(PlayerSkills skills, Game game) {
        if (Game.SESSION_UID == null || Game.SESSION_ID_TOKEN == null) {
            System.out.println(" [Persistence] 로그인 필요: 스킬 저장 불가");
            return;
        }
        try {
            LevelManager.saveSkills(game.getDbClient(), Game.SESSION_UID, Game.SESSION_ID_TOKEN, skills);
        } catch (Exception e) {
            System.err.println(" [Persistence] 스킬 저장 실패: " + e.getMessage());
        }
    }

    // 로드
    public void loadSkills(PlayerSkills skills) {
        if (Game.SESSION_UID == null || Game.SESSION_ID_TOKEN == null) {
            System.out.println(" [Persistence] 로그인 필요: 스킬 로드 불가");
            return;
        }
        try {
            LevelManager.loadSkills(Game.DB_URL, Game.SESSION_UID, Game.SESSION_ID_TOKEN, skills);
        } catch (Exception e) {
            System.err.println(" [Persistence] 스킬 로드 실패: " + e.getMessage());
        }
    }
}
