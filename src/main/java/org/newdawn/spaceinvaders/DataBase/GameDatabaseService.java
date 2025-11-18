package org.newdawn.spaceinvaders.DataBase;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GameDatabaseService {
    private final DatabaseClient db;

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
            System.err.println("별 기록 업로드 실패 " + e.getMessage());
        }
    }

    /** 로그인 시 스테이지 별(★) 기록 로드 */
    // [수정] 로그인 시 별 로드 후 재계산 호출
    public Map<Integer, Integer> loadStageStars(AuthSession session) {
        Map<Integer, Integer> result = new HashMap<>();
        if (session == null || !session.isLoggedIn()) return result;

        try {
            String res = db.get("users/" + session.getUid() + "/stageStars", session.getIdToken());

            java.lang.reflect.Type mapType = new TypeToken<Map<String, Integer>>() {
            }.getType();
            Map<String, Integer> loaded = new Gson().fromJson(res, mapType);

            if (loaded != null) {
                for (Map.Entry<String, Integer> e : loaded.entrySet()) {
                    if (e.getKey().startsWith("stage")) {
                        int stageId = Integer.parseInt(e.getKey().substring(5));
                        result.put(stageId, e.getValue());
                    }
                }
            }
        } catch (Exception e) {
            System.err.println(" 별 기록 불러오기 실패: " + e.getMessage());
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

            db.post("users/" + session.getUid() + "/scores", session.getIdToken(), json);
            db.post("globalScores", session.getIdToken(), json);

            System.out.println("점수 업로드 완료: " + json);
        } catch (Exception e) {
            System.err.println("점수 업로드 실패: " + e.getMessage());
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
            System.err.println("점수 조회 실패: " + e.getMessage());
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
            System.err.println(" 글로벌 점수 조회 실패: " + e.getMessage());
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
            System.err.println("⚠ 로그 저장 실패: " + e.getMessage());
        }
    }


}
