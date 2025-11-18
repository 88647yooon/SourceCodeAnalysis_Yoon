package org.newdawn.spaceinvaders;
import java.util.*;
import java.net.*;
import java.io.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.newdawn.spaceinvaders.DataBase.DatabaseClient;
import org.newdawn.spaceinvaders.DataBase.FirebaseDatabaseClient;

public class LevelManager {

    public static void saveSkills(DatabaseClient dbClient,String uid, String idToken, PlayerSkills s) {
        if (uid == null || idToken == null || s == null) {
            System.out.println(" saveSkills: uid/token/skills null");
            return;
        }
        try {
            String json = "{"
                    + "\"atkLv\":" + s.atkLv + ","
                    + "\"rofLv\":" + s.rofLv + ","
                    + "\"dashLv\":" + s.dashLv
                    + "}";

            dbClient.put("users/" + uid + "/skills", idToken, json);
            System.out.println("스킬 저장: " + json);
        } catch (Exception e) {
            System.err.println("스킬 저장 실패: " + e.getMessage());
        }
    }

    public static void loadSkills(String dbUrl, String uid, String idToken, PlayerSkills s) {
        if (uid == null || idToken == null || s == null) {
            System.out.println("️ loadSkills: uid/token/skills null");
            return;
        }
        try {
            String endpoint = dbUrl + "/users/" + uid + "/skills.json?auth=" + FirebaseDatabaseClient.urlEnc(idToken);
            String res = FirebaseDatabaseClient.httpGet(endpoint);
            if (res == null || res.equals("null")) {
                System.out.println(" 스킬 데이터 없음 → 기본값 사용");
                return;
            }

            s.atkLv         = extractInt(res, "atkLv", 0);
            s.rofLv        = extractInt(res, "rofLv", 0);
            s.dashLv      = extractInt(res, "dashLv", 0);

            System.out.println(" 스킬 불러오기 완료: atk=" + s.atkLv + ", rof=" + s.rofLv + ", dashLv=" + s.dashLv );
        } catch (Exception e) {
            System.err.println(" 스킬 불러오기 실패: " + e.getMessage());
        }
    }

    private static int extractInt(String json, String key, int def) {
        String pattern = "\""+Pattern.quote(key)+"\"\\s*:\\s*(-?\\d+)";
        Matcher m = Pattern.compile(pattern).matcher(json);
        if (m.find()) {
            try { return Integer.parseInt(m.group(1)); } catch (Exception ignore) {}
        }
        return def;
    }




    // 마지막 레벨 불러오기
    public static int[] loadLastLevel(String dbUrl, String uid, String idToken) {
        if (uid == null || idToken == null) {
            System.err.println("⚠️ UID 또는 TOKEN이 null → 로그인 전 호출됨");
            return new int[]{1, 0};
        }

        try {
            String endpoint = dbUrl + "/users/" + uid + "/lastLevel.json?auth=" + FirebaseDatabaseClient.urlEnc(idToken);
            String res = FirebaseDatabaseClient.httpGet(endpoint);

            System.out.println("📡 요청 URL: " + endpoint);
            System.out.println("📥 응답 데이터: " + res);

            if (res != null && !res.equals("null")) {
                int level = 1, xp = 0;

                if (res.contains("\"level\"")) {
                    String lvStr = res.replaceAll(".*\"level\"\\s*:\\s*\"?(\\d+)\"?.*", "$1");
                    level = Integer.parseInt(lvStr);
                }
                if (res.contains("\"xpIntoLevel\"")) {
                    String xpStr = res.replaceAll(".*\"xpIntoLevel\"\\s*:\\s*\"?(\\d+)\"?.*", "$1");
                    xp = Integer.parseInt(xpStr);
                }

                System.out.println("✅ 불러온 레벨: " + level + ", 경험치: " + xp);
                return new int[]{level, xp};
            }
        } catch (Exception e) {
            System.err.println("⚠️ 레벨 불러오기 실패: " + e.getMessage());
        }

        return new int[]{1, 0};
    }

    // 🔹 마지막 레벨 저장
    public static void saveLastLevel(DatabaseClient dbClient,String uid, String idToken, int level, int xpIntoLevel) {
        if (uid == null || idToken == null) {
            System.err.println("⚠️ UID 또는 TOKEN이 null → 저장 안 함");
            return;
        }
        try {
            String json = "{"
                    + "\"level\":" + level + ","
                    + "\"xpIntoLevel\":" + xpIntoLevel
                    + "}";
            dbClient.put("users/" + uid + "/lastLevel", idToken, json);
            System.out.println("✅ 마지막 레벨 저장 완료 → " + json);
        } catch (Exception e) {
            System.err.println("⚠️ 레벨 저장 실패: " + e.getMessage());
        }
    }
}
