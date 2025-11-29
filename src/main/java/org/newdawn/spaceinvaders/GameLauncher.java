package org.newdawn.spaceinvaders;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import org.newdawn.spaceinvaders.screen.ScoreboardScreen;
import org.newdawn.spaceinvaders.screen.auth.AuthScreen;

import java.io.FileInputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class GameLauncher {
    private static final String DB_KEYFILE = "src/main/resources/serviceAccountKey.json";
    private static final String DB_URL = Game.DB_URL; // 혹은 상수 복붙

    public static void main(String[] args) {
        try (FileInputStream serviceAccount = new FileInputStream(DB_KEYFILE)) {

            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .setDatabaseUrl(DB_URL)
                    .build();

            FirebaseApp.initializeApp(options);

            writeLog("game Start");

            Game g = new Game();
            g.setScreen(new AuthScreen(g));
            ScoreboardScreen ss = new ScoreboardScreen(g);

            g.gameLoop();

            ss.reloadScores();
            writeLog("game Over");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void writeLog(String eventType) {
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("logs");

        String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());

        Map<String, Object> logEntry = new HashMap<>();
        logEntry.put("event", eventType);
        logEntry.put("timestamp", timestamp);

        ref.push().setValueAsync(logEntry);

        System.out.println(" 로그 저장: " + eventType + " at " + timestamp);
    }
}
