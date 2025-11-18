package org.newdawn.spaceinvaders.DataBase;


import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class FirebaseDatabaseClient implements DatabaseClient {
    private static final String JSON_AUTH_SUFFIX = ".json?auth=";
    private final String dbUrl;

    public FirebaseDatabaseClient(String dbUrl) {
        this.dbUrl = dbUrl;
    }

    private String buildEndpoint(String path, String idToken) throws Exception {
        return dbUrl + "/" + path + JSON_AUTH_SUFFIX + urlEnc(idToken);
    }
    private String buildEndpoint(String path, String idToken, String query) throws Exception {
        String base = buildEndpoint(path, idToken);
        return base + "&" + query;
    }


    @Override
    public String get(String path, String idToken) throws Exception {
        return httpGet(buildEndpoint(path, idToken));
    }

    @Override
    public String getWithQuery(String path, String idToken, String query) throws Exception {
        return httpGet(buildEndpoint(path, idToken, query));
    }

    @Override
    public String put(String path, String idToken, String json) throws Exception {
        return httpPutJson(buildEndpoint(path, idToken), json);
    }

    @Override
    public String post(String path, String idToken, String json) throws Exception {
        return httpPostJson(buildEndpoint(path, idToken), json);
    }

    //Json 문자열 escape
    //public static으로 두어야 Game, AuthScreen에서 바로 사용가능
    public static String quote(String s) {
        if (s == null) return "null";
        StringBuilder sb = new StringBuilder("\"");
        for (int i=0;i<s.length();i++) {
            char c = s.charAt(i);
            switch (c) {
                case '\\': sb.append("\\\\"); break;
                case '"': sb.append("\\\""); break;
                case '\b': sb.append("\\b"); break;
                case '\f': sb.append("\\f"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default:
                    if (c < 0x20) sb.append(String.format("\\u%04x",(int)c));
                    else sb.append(c);
            }
        }
        sb.append("\"");
        return sb.toString();
    }

    // HTTP 유틸
    public static String httpGet(String endpoint) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(endpoint).openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Accept", "application/json");
        return readResp(conn);
    }

    public static String httpPostJson(String endpoint, String body) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(endpoint).openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        try (OutputStream os = conn.getOutputStream()) {
            os.write(body.getBytes(StandardCharsets.UTF_8));
        }
        return readResp(conn);
    }

    public static String httpPutJson(String endpoint, String body) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(endpoint).openConnection();
        conn.setRequestMethod("PUT");
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        try (OutputStream os = conn.getOutputStream()) {
            os.write(body.getBytes(StandardCharsets.UTF_8));
        }
        return readResp(conn);
    }

    public static String readResp(HttpURLConnection conn) throws Exception {
        int code = conn.getResponseCode();
        InputStream is = (code >= 200 && code < 300) ? conn.getInputStream() : conn.getErrorStream();
        if(is == null) return null;

        try (BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            for (String line; (line = br.readLine()) != null; ) {
                sb.append(line);
            }
            return sb.toString();
        }

    }



    public static String urlEnc(String s) throws Exception {
        return java.net.URLEncoder.encode(s, "UTF-8")
                .replace("+", "%20")
                .replace("%21", "!")
                .replace("%27", "'")
                .replace("%28", "(")
                .replace("%29", ")")
                .replace("%7E", "~");
    }
}
