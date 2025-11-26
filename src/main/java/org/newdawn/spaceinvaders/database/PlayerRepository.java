package org.newdawn.spaceinvaders.database;
import java.util.logging.Logger;

import com.google.gson.GsonBuilder;

import com.google.gson.Gson;
import org.newdawn.spaceinvaders.entity.player.PlayerSkills;

public class PlayerRepository {
    private static final Gson GSON = new GsonBuilder().create();
    private  static final Logger logger  = Logger.getLogger(PlayerRepository.class.getName());
    private static final String MESSAGE1 = "UID 또는 TOKEN이 null";
    private static final String MESSAGE2 = "UID, TOKEN, SKILLS가 null";

    private static class LevelData{
        int level = 1;
        int xpIntoLevel = 0;
    }

    private PlayerRepository(){ }


    public static void saveSkills(DatabaseClient dbClient,String uid, String idToken, PlayerSkills s) {
        if (uid == null || idToken == null || s == null) {
            logger.info(" saveSkills: uid/token/skills null");
            return;
        }
        try {
            String json = GSON.toJson(s);
            dbClient.put("users/" + uid + "/skills", idToken, json);
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
            s.atkLv  = loaded.atkLv;
            s.rofLv  = loaded.rofLv;
            s.dashLv = loaded.dashLv;

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
            if (res != null && !res.equals("null") && !res.isEmpty()) {
                //기존 Regex 방식 제거 -> Gson 사용
                LevelData data = GSON.fromJson(res, LevelData.class);
                if (data != null) {
                    // level이 0이하로 오는 경우 방지
                    int safeLevel = Math.max(1, data.level);
                    return new int[]{ safeLevel, data.xpIntoLevel };
                }
            }
        } catch (Exception e) {
            logger.warning("레벨 불러오기 실패 (초기화됨) " + e.getMessage());
        }

        return new int[]{1, 0};
    }

    //  마지막 레벨 저장 (Gson 사용으로 변경하였음)
    public static void saveLastLevel(DatabaseClient dbClient,String uid, String idToken, int level, int xpIntoLevel) {
        if (uid == null || idToken == null) {
            logger.warning(MESSAGE1);
            return;
        }
        try {
            // 수동 문자열 결합 제거 -> 객체 생성 후 Gson 변환
            LevelData data = new LevelData();
            data.level = level;
            data.xpIntoLevel = xpIntoLevel;

            String json = GSON.toJson(data);
            dbClient.put("users/" + uid + "/lastLevel", idToken, json);
        } catch (Exception e) {
            logger.warning("레벨 저장 실패: " + e.getMessage());
        }
    }
}
