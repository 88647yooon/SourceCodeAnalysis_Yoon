package org.newdawn.spaceinvaders.entity.player;

import org.newdawn.spaceinvaders.DataBase.AuthSession;
import org.newdawn.spaceinvaders.DataBase.DatabaseClient;
import org.newdawn.spaceinvaders.Game;
import org.newdawn.spaceinvaders.LevelManager;
import org.newdawn.spaceinvaders.PlayerSkills;

import java.util.logging.Logger;

public class ShipPersistenceComponent {
    private static final Logger logger =
            Logger.getLogger(ShipPersistenceComponent.class.getName());

    private final Game game;

    public ShipPersistenceComponent(Game game) {
        this.game = game;
    }

    /** 스킬 저장 */
    public void saveSkills(PlayerSkills skills) {
        AuthSession session = game.getSession();
        if (session == null || !session.isLoggedIn()) {
            logger.info("[Persistence] 로그인 필요: 스킬 저장 불가");
            return;
        }

        try {
            DatabaseClient db = game.getDbClient();
            LevelManager.saveSkills(
                    db,
                    session.getUid(),
                    session.getIdToken(),
                    skills
            );
        } catch (Exception e) {
            logger.warning("[Persistence] 스킬 저장 실패: " + e.getMessage());
        }
    }

    /** 스킬 로드 */
    public void loadSkills(PlayerSkills skills) {
        AuthSession session = game.getSession();
        if (session == null || !session.isLoggedIn()) {
            logger.info("[Persistence] 로그인 필요: 스킬 로드 불가");
            return;
        }

        try {
            LevelManager.loadSkills(
                    Game.DB_URL,
                    session.getUid(),
                    session.getIdToken(),
                    skills
            );
        } catch (Exception e) {
            logger.warning("[Persistence] 스킬 로드 실패: " + e.getMessage());
        }
    }
}