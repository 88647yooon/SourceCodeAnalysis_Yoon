package org.newdawn.spaceinvaders.database;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class GameDatabaseService {
    private final DatabaseClient db;
    private final Logger logger = Logger.getLogger(GameDatabaseService.class.getName());

    public GameDatabaseService(DatabaseClient db) {
        this.db = db;
    }

    //스테이지 별 저장
    /** 스테이지 별(★) 기록을 Firebase에 저장 */
    public void saveStageStars(AuthSession session, Map<Integer, Integer> stageStars) {
        if (session == null || !session.isLoggedIn()) return;

        try {
            Map<String, Integer> stringKeyMap = new HashMap<>();
            for (Map.Entry<Integer, Integer> e : stageStars.entrySet()) {
                stringKeyMap.put("stage" + e.getKey(), e.getValue());
            }

            String json = new Gson().toJson(stringKeyMap);

            db.put("user/"+ session.getUid() + "/stageStars", session.getIdToken(), json);

        } catch (Exception e) {
            logger.warning("별 기록 업로드 실패 " + e.getMessage());
        }
    }

    /** 로그인 시 스테이지 별(★) 기록 로드 */
    // [수정] 로그인 시 별 로드 후 재계산 호출
    public Map<Integer, Integer> loadStageStars(AuthSession session) {
        Map<Integer, Integer> result = new HashMap<>();
        if (session == null || !session.isLoggedIn()) {
            // 세션도 없는데 굳이 로그까지 남길 필요는 없음
            return result;
        }

        try {
            String path = "users/" + session.getUid() + "/stageStars";
            String res  = db.get(path, session.getIdToken());

            // 데이터 자체가 없을 때 (신규 유저 등)
            if (res == null || res.isEmpty() || "null".equals(res.trim())) {
                return result;
            }

            // 권한 문제는 그래도 한 번은 경고 남겨두는 게 좋음
            if (res.contains("Permission denied")) {
                logger.warning("loadStageStars: Permission denied");
                return result;
            }

            // 최소한의 파싱만
            java.lang.reflect.Type mapType =
                    new com.google.gson.reflect.TypeToken<Map<String, Integer>>() {}.getType();
            Map<String, Integer> loaded =
                    new com.google.gson.Gson().fromJson(res, mapType);

            if (loaded == null) {
                return result;
            }

            for (Map.Entry<String, Integer> e : loaded.entrySet()) {
                String key = e.getKey();
                if (!key.startsWith("stage")) {
                    continue;
                }
                String numPart = key.substring("stage".length());
                if (!numPart.matches("\\d+")) {
                    continue;
                }
                int stageId = Integer.parseInt(numPart);
                Integer stars = e.getValue();
                if (stars != null) {
                    result.put(stageId, stars);
                }
            }
        } catch (Exception e) {
            // 진짜 문제 있을 때만 한 줄
            logger.warning("loadStageStars 실패: " + e.getMessage());
        }

        return result;
    }

    public void uploadScore(AuthSession session, ScoreEntry entry) {
        if (session == null || !session.isLoggedIn()) return;
        try{
            String json = "{"
                    + "\"uid\":" + FirebaseDatabaseClient.quote(session.getUid()) + ","
                    + "\"email\":" + FirebaseDatabaseClient.quote(session.getEmail()) + ","
                    + "\"level\":" + entry.getLevel() + ","
                    + "\"mode\":" + FirebaseDatabaseClient.quote(entry.getMode()) + ","
                    + "\"score\":" + entry.getScore() + ","
                    + "\"wave\":" + entry.getWave() + ","
                    + "\"durationMs\":" + entry.getDurationMs() + ","
                    + "\"timestamp\":" + FirebaseDatabaseClient.quote(entry.getTimestamp())
                    + "}";

            db.post("user/" + session.getUid() + "/scores", session.getIdToken(), json);
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

            String res = db.getWithQuery("users/" + session.getUid() + "/scores", session.getIdToken() , query);

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
            db.post("users/" + session.getUid() + "/logs", session.getIdToken(), json);
        } catch (Exception e) {
            logger.warning("로그 저장 실패: " + e.getMessage());
        }
    }


}
