package org.newdawn.spaceinvaders;
import java.util.logging.Logger;

import com.google.gson.GsonBuilder;
import org.newdawn.spaceinvaders.database.DatabaseClient;
import org.newdawn.spaceinvaders.database.FirebaseDatabaseClient;

import com.google.gson.Gson;
public class LevelManager {
    private static final Gson GSON = new GsonBuilder().create();
    private  static final Logger logger  = Logger.getLogger(LevelManager.class.getName());
    private static final String MESSAGE1 = "UID 또는 TOKEN이 null";
    private static final String MESSAGE2 = "UID, TOKEN, SKILLS가 null";

    private LevelManager(){

    }

    public static void saveSkills(DatabaseClient dbClient,String uid, String idToken, PlayerSkills s) {
        if (uid == null || idToken == null || s == null) {
            logger.info(" saveSkills: uid/token/skills null");
            return;
        }
        try {
            String json = GSON.toJson(s);
            dbClient.put("users/" + uid + "/skills", idToken, json);
            logger.info("스킬 저장: " + json);
        } catch (Exception e) {
            logger.warning("스킬 저장 실패: " + e.getMessage());
        }
    }

    public static void loadSkills(String dbUrl, String uid, String idToken, PlayerSkills s) {
        if (uid == null || idToken == null || s == null) {
            logger.info(MESSAGE2);
            return;
        }
        try {
            String endpoint = dbUrl + "/users/" + uid + "/skills.json?auth=" + FirebaseDatabaseClient.urlEnc(idToken);
            String res = FirebaseDatabaseClient.httpGet(endpoint);
            if (res == null || res.equals("null")) {
                logger.info("스킬 데이터 없음 -> 기본값 사용");
                return;
            }
            PlayerSkills loaded = GSON.fromJson(res, PlayerSkills.class);
            if (loaded == null) {
                logger.warning("스킬 JSON 파싱 결과가 null. 기존 값 유지");
                return;
            }

            logger.info("스킬 불러오기 완료: atk=" + s.atkLv + ", rof=" + s.rofLv + ", dashLv=" + s.dashLv);
        } catch (Exception e) {
            logger.warning(" 스킬 불러오기 실패: " + e.getMessage());
        }
    }

    // 마지막 레벨 불러오기
    public static int[] loadLastLevel(String dbUrl, String uid, String idToken) {
        if (uid == null || idToken == null) {
            logger.warning(MESSAGE1);
            return new int[]{1, 0};
        }

        try {
            String endpoint = dbUrl + "/users/" + uid + "/lastLevel.json?auth=" + FirebaseDatabaseClient.urlEnc(idToken);
            String res = FirebaseDatabaseClient.httpGet(endpoint);
            if (res != null && !res.equals("null")) {
                int level = 1;
                int xp = 0;

                if (res.contains("\"level\"")) {
                    String lvStr = res.replaceAll(".*\"level\"\\s*:\\s*\"?(\\d+)\"?.*", "$1");
                    level = Integer.parseInt(lvStr);
                }
                if (res.contains("\"xpIntoLevel\"")) {
                    String xpStr = res.replaceAll(".*\"xpIntoLevel\"\\s*:\\s*\"?(\\d+)\"?.*", "$1");
                    xp = Integer.parseInt(xpStr);
                }

                logger.info("불러온 레벨: " + level + ", 경험치: " + xp);
                return new int[]{level, xp};
            }
        } catch (Exception e) {
            logger.warning("레벨 불러오기 실패: " + e.getMessage());
        }

        return new int[]{1, 0};
    }

    // 🔹 마지막 레벨 저장
    public static void saveLastLevel(DatabaseClient dbClient,String uid, String idToken, int level, int xpIntoLevel) {
        if (uid == null || idToken == null) {
            logger.warning(MESSAGE1);
            return;
        }
        try {
            String json = "{"
                    + "\"level\":" + level + ","
                    + "\"xpIntoLevel\":" + xpIntoLevel
                    + "}";
            dbClient.put("users/" + uid + "/lastLevel", idToken, json);
            logger.info("마지막 레벨 저장 완료 →" + json);
        } catch (Exception e) {
            logger.warning("레벨 저장 실패: " + e.getMessage());
        }
    }
}
