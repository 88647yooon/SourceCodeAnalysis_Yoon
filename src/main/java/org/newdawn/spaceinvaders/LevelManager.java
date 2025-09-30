package org.newdawn.spaceinvaders;
import java.util.*;
import java.net.*;
import java.io.*;
public class LevelManager {
    public static int[] loadLastLevel(String dbUrl, String uid, String idToken) {
        if (uid == null || idToken == null) return new int[]{1, 0}; // 기본값: Lv1, XP 0
        try {
            String endpoint = dbUrl + "/users/" + uid + "/lastLevel.json?auth=" + Game.urlEnc(idToken);
            String res = Game.httpGet(endpoint);
            if (res != null && !res.equals("null")) {
                // 단순 파싱
                int level = 1, xp = 0;
                if (res.contains("\"level\"")) {
                    String lvStr = res.replaceAll(".*\"level\"\\s*:\\s*(\\d+).*", "$1");
                    level = Integer.parseInt(lvStr);
                }
                if (res.contains("\"xpIntoLevel\"")) {
                    String xpStr = res.replaceAll(".*\"xpIntoLevel\"\\s*:\\s*(\\d+).*", "$1");
                    xp = Integer.parseInt(xpStr);
                }
                return new int[]{level, xp};
            }
        } catch (Exception e) {
            System.err.println("⚠️ 레벨 불러오기 실패: " + e.getMessage());
        }
        return new int[]{1, 0};
    }


    //  마지막 레벨 저장
    public static void saveLastLevel(String uid, String idToken, int level, int xpIntoLevel) {
        if (uid == null || idToken == null) return;
        try {
            String json = "{"
                    + "\"level\":" + level + ","
                    + "\"xpIntoLevel\":" + xpIntoLevel
                    + "}";
            Game.restSetJson( "users/" + uid + "/lastLevel", idToken, json);
            System.out.println( "마지막 레벨 저장: " + json);
        } catch (Exception e) {
            System.err.println("레벨 저장 실패: " + e.getMessage());
        }
    }

}
