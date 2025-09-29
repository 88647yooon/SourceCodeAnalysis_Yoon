package org.newdawn.spaceinvaders;
import java.util.*;
import java.net.*;
import java.io.*;
public class LevelManager {
    public static int loadLastLevel(String dbUrl, String uid, String idToken) {
        if (uid == null || idToken == null) return 1; // 기본 레벨 1
        try {
            String endpoint = dbUrl + "/users/" + uid + "/lastLevel.json?auth=" + Game.urlEnc(idToken);
            String res = Game.httpGet(endpoint);
            if (res != null && !res.equals("null")) {
                return Integer.parseInt(res);
            }
        } catch (Exception e) {
            System.err.println("⚠️ 레벨 불러오기 실패: " + e.getMessage());
        }
        return 1; // 실패 시 기본 레벨
    }

    // ✅ 마지막 레벨 저장
    public static void saveLastLevel(String uid, String idToken, int level) {
        if (uid == null || idToken == null) return;
        try {
            Game.restSetJson( "users/" + uid + "/lastLevel", idToken, String.valueOf(level));
            System.out.println("✅ 마지막 레벨 저장: " + level);
        } catch (Exception e) {
            System.err.println("⚠️ 레벨 저장 실패: " + e.getMessage());
        }
    }

}
