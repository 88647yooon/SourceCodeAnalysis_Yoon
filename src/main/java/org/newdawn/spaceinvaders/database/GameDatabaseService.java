package org.newdawn.spaceinvaders.database;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.Logger;

public class GameDatabaseService {
    private final DatabaseClient db;
    private final Logger logger = Logger.getLogger(GameDatabaseService.class.getName());

    public GameDatabaseService(DatabaseClient db) {
        this.db = db;
    }

    //중복되는 문자열을 상수로 정의
    private static final String KEY_USERS = "users/";
    private static final String KEY_STAGE_PREFIX = "stage";

    public void saveGameResult(AuthSession session, int score, int wave, int level, String mode, long durationMs) {
        if (session == null || !session.isLoggedIn()) return;

        ScoreEntry entry = new ScoreEntry();
        entry.setMode(mode);
        entry.setEmail(session.getEmail());
        entry.setScore(score);
        entry.setWave(wave);
        entry.setDurationMs(durationMs);
        entry.setTimestamp(now());
        entry.setLevel(level);

        uploadScore(session, entry);
    }
    //추가 -> 레벨 저장 로직 래퍼 (Game.java에서 PlayerRepository 의존성 제거 용도
    public void savePlayerLevel(AuthSession session, int level, int xp) {
        if (session == null || !session.isLoggedIn()) return;
        // PlayerRepository의 정적 메서드 활용
        PlayerRepository.saveLastLevel(db, session.getUid(), session.getIdToken(), level, xp);
    }

    //스테이지 별 저장
    /** 스테이지 별(★) 기록을 Firebase에 저장 */
    public void saveStageStars(AuthSession session, Map<Integer, Integer> stageStars) {
        if (session == null || !session.isLoggedIn()) return;

        try {
            Map<String, Integer> stringKeyMap = new HashMap<>();
            for (Map.Entry<Integer, Integer> e : stageStars.entrySet()) {
                stringKeyMap.put(KEY_STAGE_PREFIX + e.getKey(), e.getValue());
            }

            String json = new Gson().toJson(stringKeyMap);

            db.put(KEY_USERS+ session.getUid() + "/stageStars", session.getIdToken(), json);

        } catch (Exception e) {
            logger.warning("별 기록 업로드 실패 " + e.getMessage());
        }
    }

    /** 로그인 시 스테이지 별(★) 기록 로드 */
    //  로그인 시 별 로드 후 재계산 호출
    public Map<Integer, Integer> loadStageStars(AuthSession session) {
        Map<Integer, Integer> result = new HashMap<>();
        if (session == null || !session.isLoggedIn()) return result;

        try {
            // 상수 사용
            String path = KEY_USERS + session.getUid() + "/stageStars";
            String res  = db.get(path, session.getIdToken());

            if (isValidResponse(res)) {
                java.lang.reflect.Type mapType =
                        new com.google.gson.reflect.TypeToken<Map<String, Integer>>() {}.getType();
                Map<String, Integer> loaded = new com.google.gson.Gson().fromJson(res, mapType);

                // 복잡한 로직을 분리한 메서드 호출
                return parseStageData(loaded);
            }

        } catch (Exception e) {
            // 진짜 문제 있을 때만 한 줄
            logger.warning("loadStageStars 실패: " + e.getMessage());
        }
        return result;
    }
    //유효성 검사 로직 분리
    private boolean isValidResponse(String res) {
        if (res == null || res.isEmpty() || "null".equals(res.trim())) {
            return false;
        }
        if (res.contains("Permission denied")) {
            logger.warning("loadStageStars: Permission denied");
            return false;
        }
        return true;
    }
    //실제 데이터 파싱 로직 분리
    private Map<Integer, Integer> parseStageData(Map<String, Integer> loaded) {
        Map<Integer, Integer> result = new HashMap<>();
        if (loaded == null) return result;

        for (Map.Entry<String, Integer> e : loaded.entrySet()) {
            String key = e.getKey();
            // 상수 사용 ("stage")
            if (key.startsWith(KEY_STAGE_PREFIX)) {
                String numPart = key.substring(KEY_STAGE_PREFIX.length());
                if (numPart.matches("\\d+")) {
                    result.put(Integer.parseInt(numPart), e.getValue());
                }
            }
        }
        return result;
    }

    public void uploadScore(AuthSession session, ScoreEntry entry) {
        if (session == null || !session.isLoggedIn()) return;
        try{
            String json = new Gson().toJson(entry);

            // 상수 사용
            db.post(KEY_USERS + session.getUid() + "/scores", session.getIdToken(), json);
            db.post("globalScores", session.getIdToken(), json);

            logger.info("점수 업로드 완료:");
        } catch (Exception e) {
            logger.info("점수 업로드 실패: " + e.getMessage());
        }
    }

    /** 내 상위 점수 조회 */
    public List<ScoreEntry> fetchMyTopScores(AuthSession session, int limit) {
        List<ScoreEntry> list = new ArrayList<>();
        if (session == null || !session.isLoggedIn()) return list;

        try {
            String query = "&orderBy=%22score%22&limitToLast=" + limit;

            String res = db.getWithQuery(KEY_USERS + session.getUid() + "/scores", session.getIdToken() , query);

            java.lang.reflect.Type mapType = new TypeToken<java.util.Map<String, ScoreEntry>>() {}.getType();
            Map<String, ScoreEntry> map = new Gson().fromJson(res, mapType);
            if (map != null) list.addAll(map.values());

            list.sort((a, b) -> Integer.compare(
                    b.getScore() == null ? 0 : b.getScore(),
                    a.getScore() == null ? 0 : a.getScore()
            ));
        } catch (Exception e) {
            logger.warning("점수 조회 실패: " + e.getMessage());
        }
        return list;
    }

    /** 글로벌 상위 점수 조회 */
    public List<ScoreEntry> fetchGlobalTopScores(AuthSession session, int limit) {
        List<ScoreEntry> list = new ArrayList<>();
        if (session == null || !session.isLoggedIn()) return list;

        try {
            String query = "&orderBy=%22score%22&limitToLast=" + limit;
            String res = db.getWithQuery("globalScores", session.getIdToken(), query);

            java.lang.reflect.Type mapType = new com.google.gson.reflect.TypeToken<Map<String, ScoreEntry>>() {}.getType();
            Map<String, ScoreEntry> map = new com.google.gson.Gson().fromJson(res, mapType);
            if (map != null) list.addAll(map.values());

            list.sort((a, b) -> Integer.compare(
                    b.getScore() == null ? 0 : b.getScore(),
                    a.getScore() == null ? 0 : a.getScore()
            ));
        } catch (Exception e) {
            logger.warning(" 글로벌 점수 조회 실패: " + e.getMessage());
        }

        return list;
    }


    //REST 기반 간단 로그
    public void logUserEvent(AuthSession session, String type, String timestamp) {
        if (session == null || !session.isLoggedIn()) return;
        String json = "{"
                + "\"event\":" + FirebaseDatabaseClient.quote(type) + ","
                + "\"timestamp\":" + FirebaseDatabaseClient.quote(timestamp)
                + "}";
        try {
            db.post(KEY_USERS + session.getUid() + "/logs", session.getIdToken(), json);
        } catch (Exception e) {
            logger.warning("로그 저장 실패: " + e.getMessage());
        }
    }

    public static String now() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
    }

}
