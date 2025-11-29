package org.newdawn.spaceinvaders.database;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.net.HttpURLConnection;

import java.net.URL;
import java.nio.charset.StandardCharsets;

public class FirebaseAuthService {
    public final String apiKey;

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
    public AuthResult restSignUp(String email, String password) throws Exception{
        return callAuth("accounts:signUp", "SignUp",email, password);
    }

    /** 로그인 */
    public AuthResult restSignIn(String email, String password) throws Exception {
        return callAuth("accounts:signInWithPassword", "SignIn", email, password);
    }

    private AuthResult callAuth(String urlPaht, String actionName, String email, String password) throws Exception {
        String endpoint = "https://identitytoolkit.googleapis.com/v1/" + urlPaht + "?key=" + apiKey;
        String body = "{"
                + "\"email\":" + FirebaseDatabaseClient.quote(email) + ","
                + "\"password\":" + FirebaseDatabaseClient.quote(password) + ","
                + "\"returnSecureToken\":true"
                + "}";

        String res = httpPostJson(endpoint, body);
        return parseAuthResponse(res, actionName);
    }

    private AuthResult parseAuthResponse(String res, String actionName) throws IOException {
        String idToken = jget(res, "idToken");
        String refreshToken = jget(res, "refreshToken");
        String localId = jget(res, "localId");
        String emailOut = jget(res, "email");

        if (idToken == null || localId == null) {
            throw new IOException(actionName + " parse failed: " + res);
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
        JsonObject object = JsonParser.parseString(json).getAsJsonObject();
        if(!object.has(key) || !object.get(key).isJsonPrimitive()){
            return null;
        }
        return object.get(key).getAsString();
    }


}
