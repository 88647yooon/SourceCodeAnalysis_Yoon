package org.newdawn.spaceinvaders.database;

import java.io.IOException;
import java.net.HttpURLConnection;

import java.net.URL;
import java.nio.charset.StandardCharsets;

import java.util.logging.Logger;

public class FirebaseAuthService {
    public final String apiKey;
    private static final Logger logger = Logger.getLogger(FirebaseAuthService.class.getName());

    public FirebaseAuthService(String apiKey) {
        this.apiKey = apiKey;
    }

    public static class AuthResult{
        public final String idToken;
        public final String refreshToken;
        public final String localId;
        public final String email;

        public AuthResult(String idToken, String refreshToken, String localId, String email) {
            this.idToken = idToken;
            this.refreshToken = refreshToken;
            this.localId = localId;
            this.email = email;
        }
    }
    /**회원가입 메서드**/
    public AuthResult signUp(String email, String password) throws Exception{
        //endpoint의 url로 body의 Json을 Post로 전송해서 응답을 String으로 받아옴
        String endpoint = "https://identitytoolkit.googleapis.com/v1/accounts:signUp?key=" + apiKey;
        String body = "{"
                + "\"email\":" + FirebaseDatabaseClient.quote(email) + ","
                + "\"password\":" + FirebaseDatabaseClient.quote(password) + ","
                + "\"returnSecureToken\":true"
                + "}";

        String res = httpPostJson(endpoint, body);
        String idToken = jget(res, "idToken");
        String refreshToken = jget(res, "refreshToken");
        String localId = jget(res, "localId");
        String emailOut = jget(res, "email");

        if(idToken == null || localId == null){
            throw new IOException("SignIn parse failed: " + res);
        }
        return new AuthResult(idToken, refreshToken, localId, emailOut);
    }

    /** 로그인 */
    public AuthResult signIn(String email, String password) throws Exception {
        String endpoint = "https://identitytoolkit.googleapis.com/v1/accounts:signInWithPassword?key=" + apiKey;
        String body = "{"
                + "\"email\":" + FirebaseDatabaseClient.quote(email) + ","
                + "\"password\":" + FirebaseDatabaseClient.quote(password) + ","
                + "\"returnSecureToken\":true"
                + "}";

        String res = httpPostJson(endpoint, body);
        String idToken      = jget(res, "idToken");
        String refreshToken = jget(res, "refreshToken");
        String localId      = jget(res, "localId");
        String emailOut     = jget(res, "email");

        if (idToken == null || localId == null) {
            throw new IOException("SignIn parse failed: " + res);
        }
        return new AuthResult(idToken, refreshToken, localId, emailOut);
    }

    private static String httpPostJson(String endpoint, String body) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(endpoint).openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        conn.setDoOutput(true);
        conn.getOutputStream().write(body.getBytes(StandardCharsets.UTF_8));

        return FirebaseDatabaseClient.readResp(conn);
    }

    /** 매우 단순한 "키:문자열" 추출(필요 필드만) */
    private static String jget(String json, String key) {
        int valueStart = findStringValueStart(json, key);
        if (valueStart < 0) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        int i = valueStart;
        while (i < json.length()) {
            char c = json.charAt(i++);
            if (c == '\\') {
                if (i >= json.length()) {
                    logger.warning("Unexpected end of JSON parsing:");
                }
                char n = json.charAt(i++);
                appendStringbuilder(json, sb, n, i);

            } else if (c == '"') {
                break;
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }
    private static int findStringValueStart(String json, String key){
        String k = "\"" + key.replace("\"", "\\\"") + "\"";
        int i = json.indexOf(k);
        if (i < 0) return -1;

        i = json.indexOf(':', i);
        if (i < 0) return -1;

        i++; // ':' 다음
        while (i < json.length() && Character.isWhitespace(json.charAt(i))) {
            i++;
        }
        if (i >= json.length() || json.charAt(i) != '"') {
            return -1;
        }
        return i + 1;
    }

    private static void appendStringbuilder(String json, StringBuilder sb, char n, int i) {
        if (n == 'u') {
            appendUnicode(json, sb, i);
            return;
        }

        switch (n) {
            case '\\':
                sb.append('\\');
                break;
            case '"':
                sb.append('"');
                break;
            case 'n':
                sb.append('\n');
                break;
            case 'r':
                sb.append('\r');
                break;
            case 't':
                sb.append('\t');
                break;
            case 'b':
                sb.append('\b');
                break;
            case 'f':
                sb.append('\f');
                break;
            default:
                sb.append(n);
                break;
        }
    }

    private static void appendUnicode(String json, StringBuilder sb, int i) {
        if (i + 3 >= json.length()) {
            return;
        }

        String hex = json.substring(i, i + 4);
        try {
            sb.append((char) Integer.parseInt(hex, 16));
        } catch (NumberFormatException e) {
            logger.warning("Failed to parse hex: " + hex);
        }
    }
}
