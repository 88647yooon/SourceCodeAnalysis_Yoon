package org.newdawn.spaceinvaders;
import java.util.*;
import java.net.*;
import java.io.*;

public class LevelManager {

    // 🔹 마지막 레벨 불러오기
    public static int[] loadLastLevel(String dbUrl, String uid, String idToken) {
        if (uid == null || idToken == null) {
            System.err.println("⚠️ UID 또는 TOKEN이 null → 로그인 전 호출됨");
            return new int[]{1, 0};
        }

        try {
            String endpoint = dbUrl + "/users/" + uid + "/lastLevel.json?auth=" + Game.urlEnc(idToken);
            String res = Game.httpGet(endpoint);

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
    public static void saveLastLevel(String uid, String idToken, int level, int xpIntoLevel) {
        if (uid == null || idToken == null) {
            System.err.println("⚠️ UID 또는 TOKEN이 null → 저장 안 함");
            return;
        }
        try {
            String json = "{"
                    + "\"level\":" + level + ","
                    + "\"xpIntoLevel\":" + xpIntoLevel
                    + "}";
            Game.restSetJson("users/" + uid + "/lastLevel", idToken, json);
            System.out.println("✅ 마지막 레벨 저장 완료 → " + json);
        } catch (Exception e) {
            System.err.println("⚠️ 레벨 저장 실패: " + e.getMessage());
        }
    }
}
