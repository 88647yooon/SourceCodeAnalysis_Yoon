package org.newdawn.spaceinvaders;

import java.awt.Canvas;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferStrategy;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.List;

import java.text.SimpleDateFormat;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;

import javax.swing.*;

import org.newdawn.spaceinvaders.entity.*;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * Game — Space Invaders 메인 오케스트레이션.
 *
 * 역할
 *  - 화면(Screen) 전환, 엔티티(Entity) 생명주기, 게임 루프, 점수/웨이브/스테이지 상태 관리.
 *
 * 프레임 순서(단일 스레드)
 *  1) currentScreen.update(deltaMs)
 *  2) render(g)  // 배경 → Screen 오버레이
 *  3) (레거시 경로인 경우) 엔티티 이동/충돌/doLogic
 *  4) ship 이동 벡터/발사 처리
 *  5) 10ms 슬립
 *
 * 단위/스레드
 *  - 시간: ms, 속도: px/s, 좌표: px
 *  - 싱글 스레드 전제. update/render 동안 블로킹 I/O 금지.
 */
public class Game extends Canvas {

    // 인증/DB 세션
    private static final String API_KEY = "AIzaSyCdY9-wpF3Ad2DXkPTXGcqZEKWBD1qRYKE";
    public static final String DB_URL = "https://sourcecodeanalysis-donggyu-default-rtdb.asia-southeast1.firebasedatabase.app/";
    private static final String DB_KEYFILE = "src/main/resources/serviceAccountKey.json";
    public  static String SESSION_UID   = null;
    public static String SESSION_EMAIL = null;
    public static String SESSION_ID_TOKEN = null;
	/** The stragey that allows us to use accelerate page flipping */
	private BufferStrategy strategy;
	/** True if the game is currently "running", i.e. the game loop is looping */
	private boolean gameRunning = true;
    /** The list of all the entities that exist in our game */
    private ArrayList<Entity> entities = new ArrayList<>();
    /** 이 프레임에서 제거할 엔티티 큐 */
    private ArrayList<Entity> removeList = new ArrayList<>();

    /** 플레이어(Ship) 엔티티 */
    private Entity ship;

    /** Ship 이동 속도(px/s) */
    private double moveSpeed = 300;
    /** 마지막 발사 시각(ms) */
    private long lastFire = 0;
    /** 발사 간격(ms) */
    private long firingInterval = 500;

    /** 화면에 남은 외계인 수 */
    private int alienCount;

    /** HP가 낮은 등 위험 상태 표시 */
    private boolean dangerMode = false;

    /** “아무 키나 누르기” 메시지 */
    private String message = "";
    /** 아무 키 대기 중인지 여부 */
    private boolean waitingForKeyPress = true;

    /** 입력 상태 */
    private boolean leftPressed = false;
    private boolean rightPressed = false;
    private boolean UpPressed = false;
    private boolean DownPressed = false;
    private boolean firePressed = false;

    /** 이번 루프에서 별도의 게임 로직(doLogic)을 적용해야 하는지 */
    private boolean logicRequiredThisLoop = false;

    /** FPS 측정 누적 시간(ms) */
    private long lastFpsTime;
    /** FPS 카운터 */
    private int fps;
    /** 윈도우 타이틀 기본값 */
    private String windowTitle = "Space Invaders 102";
    /** 게임 윈도우 */
    private JFrame container;

    /** 배경 렌더러 */
    private BackgroundRenderer backgroundRenderer;
    /** 활성 화면 */
    private Screen currentScreen;

    // 모드/점수/스테이지
    private enum Mode { STAGE, INFINITE }
    private Mode currentMode = Mode.STAGE;
    private int score = 0;
    private long runStartedAtMs = 0L;
    private int currentStageId = 1;
    private int stageStartHP = 0;
    private static final int STAGE_TIME_LIMIT_MS = 120_000;

    private enum GameState { MENU, PLAYING, GAMEOVER, SCOREBOARD, EXIT }
    private GameState state = GameState.MENU;

    private Map<Integer, Integer> stageStarScoreRequirements = new HashMap<>();
    private Map<Integer, Integer> stageStars = new HashMap<>();
    private String[] menuItems = {"스테이지 모드", "무한 모드", "스코어보드", "게임 종료"};
    private int menuIndex = 0;

    // 무한 모드/웨이브/보스
    private boolean infiniteMode = false;
    int waveCount = 1;
    private int normalsClearedInCycle = 0;
    private static final double RANGED_ALIEN_RATIO = 0.25;
    private boolean bossActive = false;

    //스테이지 잠금 상태 관리 배열
    private static boolean[] stageUnlocked;
    private static  final int TOTAL_STAGES = 5;

    /** 초기 화면·버퍼·입력·BGM·엔티티 설정 */
    public Game() {
        container = new JFrame("Space Invaders 102");
        backgroundRenderer = new BackgroundRenderer();

        setScreen(new MenuScreen(this)); // 시작 화면

        JPanel panel = (JPanel) container.getContentPane();
        panel.setPreferredSize(new Dimension(800, 600));
        panel.setLayout(null);

        setBounds(0, 0, 800, 600);
        panel.add(this);

        setIgnoreRepaint(true);
        stageStarScoreRequirements();

        container.pack();
        container.setResizable(false);
        container.setVisible(true);

        container.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                System.exit(0);
            }
        });

        addKeyListener(new KeyInputHandler());
        setFocusable(true);
        setFocusTraversalKeysEnabled(false);
        SwingUtilities.invokeLater(this::requestFocusInWindow);
        requestFocus();

        createBufferStrategy(2);
        strategy = getBufferStrategy();

        initStageUnlocks();
        initEntities();

        SoundManager.get().setSfxVolume(-15.0f);
    }

    /**
     * 기존 엔티티/상태를 초기화하고 새 게임을 시작.
     * - 엔티티/제거 큐 초기화, 입력 리셋, 상태 PLAYING 전환
     */
    private void startGame() {
        entities.clear();
        removeList.clear();
        alienCount = 0;

        initEntities();

        leftPressed = rightPressed = firePressed = false;
        waitingForKeyPress = false;
        state = GameState.PLAYING;

        waveCount = 1;
    }

    /**
     * Ship 생성 및 초기 배치. 무한 모드면 즉시 웨이브 스폰.
     */
    private void initEntities() {
        ship = new ShipEntity(this, "sprites/ship.gif", 370, 550);
        entities.add(ship);

        ((ShipEntity) ship).setInvulnerable(false);

        alienCount = 0;
        if (infiniteMode) {
            spawnAliens();
        } else {
            // 스테이지 모드는 StageManager.applyStage(...)에서 배치
        }
    }

    public ShipEntity getPlayerShip() {
        return (ShipEntity) ship;
    }

    public void addEntity(Entity e) {
        entities.add(e);
    }

    public int getWaveCount() {
        return waveCount;
    }

    /**
     * 무한 모드 웨이브 스폰:
     * - 난이도 계산/적용, Diagonal/Ranged/기본 적 배치, 인질 일부 추가, waveCount 증가
     */
    private void spawnAliens() {
        if (infiniteMode) {
            for (java.util.Iterator<Entity> it = entities.iterator(); it.hasNext(); ) {
                Entity e = it.next();
                if (e instanceof HostageEntity) {
                    it.remove();
                }
            }
        }
        Difficulty diff = computeDifficultyForWave(waveCount);

        int rows = 3 + (waveCount % 3);   // 3~5
        int cols = 6 + (waveCount % 6);   // 6~11
        alienCount = 0;

        int startX = 100, startY = 50, gapX = 50, gapY = 30;

        for (int row = 0; row < rows; row++) {
            for (int c = 0; c < cols; c++) {
                int x = startX + (c * gapX);
                int y = startY + (row * gapY);

                double diagonalProb = Math.min(0.05 + (waveCount - 1) * 0.02, 0.25);
                double rangedProb = RANGED_ALIEN_RATIO;

                double r = Math.random();
                Entity alien;
                if (r < diagonalProb) {
                    alien = new DiagonalShooterAlienEntity(this, x, y);
                } else if (r < diagonalProb + rangedProb) {
                    alien = new RangedAlienEntity(this, x, y, getPlayerShip());
                } else {
                    alien = new AlienEntity(this, x, y);
                }

                applyDifficultyToAlien(alien, diff);

                entities.add(alien);
                alienCount++;
            }
        }

        if (infiniteMode) {
            int hostageNum = 1 + (int) (Math.random() * 2);
            if (hostageNum > 0) {
                java.util.Set<Integer> usedCols = new java.util.HashSet<>();
                for (int i = 0; i < hostageNum; i++) {
                    int c;
                    int guard = 0;
                    do {
                        c = (int) (Math.random() * cols);
                    } while (usedCols.contains(c) && ++guard < 10);
                    usedCols.add(c);

                    int x = startX + (c * gapX);
                    int y = startY - 40;
                    Entity hostage = new HostageEntity(this, x, y);
                    entities.add(hostage);
                }
            }
        }
        waveCount++;
    }

    /** 보스 소환(인질 제거 후 중앙 배치) */
    private void spawnBoss() {
        for (java.util.Iterator<Entity> it = entities.iterator(); it.hasNext(); ) {
            Entity e = it.next();
            if (e instanceof HostageEntity) {
                it.remove();
            }
        }

        Entity boss = new org.newdawn.spaceinvaders.entity.BossEntity(this, 360, 60, getPlayerShip());
        entities.add(boss);
        bossActive = true;
    }

    /** 보스 처치 콜백: 모드에 따라 웨이브 진행 또는 승리 처리 */
    public void onBossKilled() {
        bossActive = false;
        if (infiniteMode) {
            spawnAliens();
        } else {
            notifyWin();
        }
    }

    /** 웨이브별 난이도 파라미터 */
    private static class Difficulty {
        int alienHP;
        double alienSpeedMul;
        double fireRateMul;
        double bulletSpeedMul;
    }

    /** 웨이브 → 난이도 계산 */
    private Difficulty computeDifficultyForWave(int wave) {
        Difficulty d = new Difficulty();
        d.alienHP = 1 + Math.max(0, (wave - 1) / 2);
        d.alienSpeedMul = Math.min(2.5, 1.0 + 0.08 * (wave - 1));
        d.fireRateMul = Math.min(3.0, 1.0 + 0.10 * (wave - 1));
        d.bulletSpeedMul = Math.min(2.0, 1.0 + 0.05 * (wave - 1));
        return d;
    }

    /** 난이도 적용 */
    private void applyDifficultyToAlien(Entity e, Difficulty d) {
        if (e instanceof AlienEntity) {
            AlienEntity a = (AlienEntity) e;
            a.setMaxHP(d.alienHP);
            a.applySpeedMultiplier(d.alienSpeedMul);
        }
        if (e instanceof RangedAlienEntity) {
            RangedAlienEntity r = (RangedAlienEntity) e;
            r.setFireRateMultiplier(d.fireRateMul);
            r.setBulletSpeedMultiplier(d.bulletSpeedMul);
        }
        if (e instanceof DiagonalShooterAlienEntity) {
            DiagonalShooterAlienEntity ds = (DiagonalShooterAlienEntity) e;
            ds.setFireRateMultiplier(d.fireRateMul);
            ds.setBulletSpeedMultiplier(d.bulletSpeedMul);
        }
    }

    /** 스테이지별 3성 기준 점수표 초기화 */
    private void stageStarScoreRequirements() {
        stageStarScoreRequirements.put(1, 1000);
        stageStarScoreRequirements.put(2, 2000);
        stageStarScoreRequirements.put(3, 3000);
        stageStarScoreRequirements.put(4, 4000);
        stageStarScoreRequirements.put(5, 5000);
    }

    public int getStageStarScoreRequirements(int stageId) {
        return stageStarScoreRequirements.getOrDefault(stageId, 1000);
    }

    /**
     * 스테이지 별(★) 평가:
     * 1★ 클리어 / 2★ 시간 내 클리어 / 3★ 시간 내 + 무피격 + 점수 기준 충족
     */
    private void evaluateStageResult(int stageId, int timeLeft, int damageTaken, int score) {
        int stars = 1;

        if (timeLeft > 0) {
            stars = 2;
            int requiredScore = getStageStarScoreRequirements(stageId);
            if (damageTaken == 0 && score >= requiredScore) {
                stars = 3;
            }
        }
        setStageStars(stageId, stars);
    }



    public int getStageStars(int stageId) {
        return stageStars.getOrDefault(stageId, 0);
    }

    /** 스테이지 별(★) 기록을 Firebase에 저장 */
    public void saveStageStars() {
        if (SESSION_UID == null || SESSION_ID_TOKEN == null) return;

        try {
            Map<String, Integer> stringKeyMap = new HashMap<>();
            for (Map.Entry<Integer, Integer> e : stageStars.entrySet()) {
                stringKeyMap.put("stage" + e.getKey(), e.getValue());
            }

            String json = new Gson().toJson(stringKeyMap);
            restSetJson("users/" + SESSION_UID + "/stageStars", SESSION_ID_TOKEN, json);

        } catch (Exception e) {
            System.err.println("별 기록 업로드 실패 " + e.getMessage());
        }
    }

    /** 로그인 시 스테이지 별(★) 기록 로드 */
    // [수정] 로그인 시 별 로드 후 재계산 호출
    public void loadStageStars() {
        if (SESSION_UID == null || SESSION_ID_TOKEN == null) return;
        try {
            String endpoint = DB_URL + "/users/" + SESSION_UID + "/stageStars.json?auth=" + urlEnc(SESSION_ID_TOKEN);
            String res = httpGet(endpoint);

            java.lang.reflect.Type mapType = new com.google.gson.reflect.TypeToken<Map<String, Integer>>() {}.getType();
            Map<String, Integer> loaded = new Gson().fromJson(res, mapType);

            stageStars.clear();
            if (loaded != null) {
                for (Map.Entry<String, Integer> e : loaded.entrySet()) {
                    if (e.getKey().startsWith("stage")) {
                        int stageId = Integer.parseInt(e.getKey().substring(5));
                        stageStars.put(stageId, e.getValue());
                    }
                }
            }

            // 로드가 끝났으니 잠금 재계산
            rebuildStageUnlocks();

            System.out.println(" 별 기록 불러오기 완료: " + stageStars);
        } catch (Exception e) {
            System.err.println(" 별 기록 불러오기 실패: " + e.getMessage());
        }
    }

    // [수정] 별을 갱신/저장한 직후에도 재계산
    public void setStageStars(int stageId, int stars) {
        int prev = stageStars.getOrDefault(stageId, 0);
        if (stars > prev) {
            stageStars.put(stageId, stars);
            saveStageStars();
            // 저장 직후 잠금 재계산
            rebuildStageUnlocks();
        }
    }

    // [추가] 당장 다음 스테이지(= 현재 stageId의 다음 인덱스)를 오픈
    private void unlockNextStageIfEligible(int currentStageId) {
        if (stageUnlocked == null) return;
        if (currentStageId >= TOTAL_STAGES) return;

        // 현재 스테이지의 최종 별 수가 3개 이상일 때만 다음 스테이지 오픈
        if (getStageStars(currentStageId) >= 3) {
            int nextIdx = currentStageId; // 1-based의 "다음"은 배열 인덱스로 currentStageId
            if (nextIdx < stageUnlocked.length) {
                stageUnlocked[nextIdx] = true;
            }
        }
    }

    public boolean isStageMode() {
        return currentMode == Mode.STAGE;
    }

    /** 스테이지 모드 시작 */
    public void startStageMode(int StageNum) {
        currentMode = Mode.STAGE;
        score = 0;
        runStartedAtMs = System.currentTimeMillis();
        infiniteMode = false;
        waveCount = StageNum;
        currentStageId = StageNum;
        normalsClearedInCycle = 0;

        startGame();

        stageStartHP = getPlayerShip().getCurrentHP();
        stageStarScoreRequirements();

        StageManager.applyStage(StageNum, this);

        if (StageNum == 5) {
            bossActive = true;
            if (alienCount < 0) alienCount = 0;
        }
        setScreen(new GamePlayScreen(this));
    }

    /** 무한 모드 시작 */
    public void startInfiniteMode() {
        currentMode = Mode.INFINITE;
        score = 0;
        runStartedAtMs = System.currentTimeMillis();
        infiniteMode = true;
        waveCount = 1;
        normalsClearedInCycle = 0;
        startGame();
        setScreen(new GamePlayScreen(this));
    }

    public void showScoreboard() {
        setScreen(new ScoreboardScreen(this));
    }

    /** 다음 프레임에서 doLogic 실행 요청 */
    public void updateLogic() {
        logicRequiredThisLoop = true;
    }

    /** 엔티티 제거 요청(프레임 말미에 일괄 처리) */
    public void removeEntity(Entity entity) {
        removeList.add(entity);
    }

    /** 플레이어 사망 처리 */
    public void notifyDeath() {
        state = GameState.GAMEOVER;
        waitingForKeyPress = false;
        message = "";
        menuIndex = 0;

        if (SESSION_UID != null && SESSION_ID_TOKEN != null) {
            LevelManager.saveLastLevel(SESSION_UID, SESSION_ID_TOKEN, getPlayerShip().getLevel(), getPlayerShip().getXpIntoLevel());
        }

        setScreen(new GameOverScreen(this));
        uploadScoreIfLoggedIn();
    }

    public boolean isInfiniteMode() {
        return infiniteMode;
    }

    /** 플레이어 승리 처리(별 평가/저장 포함) */
    public void notifyWin() {
        message = "Well done! You Win!";
        waitingForKeyPress = true;

        if (currentMode == Mode.STAGE) {
            long elapsed = (runStartedAtMs > 0) ? (System.currentTimeMillis() - runStartedAtMs) : 0L;
            int timeLeft = Math.max(0, STAGE_TIME_LIMIT_MS - (int) elapsed);

            int damageTaken = 0;
            ShipEntity p = getPlayerShip();
            if (p != null) {
                damageTaken = Math.max(0, stageStartHP - p.getCurrentHP());
            }
            evaluateStageResult(currentStageId, timeLeft, damageTaken, score);
            //조건 충족시 다음 스테이지 해제
            unlockNextStageIfEligible(currentStageId);

        }

        if (SESSION_UID != null && SESSION_ID_TOKEN != null) {
            LevelManager.saveLastLevel(SESSION_UID, SESSION_ID_TOKEN, getPlayerShip().getLevel(), getPlayerShip().getXpIntoLevel());
        }
        uploadScoreIfLoggedIn();
    }

    /** 남은 스테이지 시간(ms). 스테이지 모드가 아니면 0 */
    public int getStageTimeLimitMs() {
        if (currentMode != Mode.STAGE) return 0;
        if (runStartedAtMs == 0) return STAGE_TIME_LIMIT_MS;

        long elapsed = System.currentTimeMillis() - runStartedAtMs;
        int timeLeft = (int) (STAGE_TIME_LIMIT_MS - elapsed);
        return Math.max(0, timeLeft);
    }

    public java.util.List<Entity> getEntities() {
        return entities;
    }

    public boolean isWaitingForKeyPress() {
        return waitingForKeyPress;
    }

    public void setLeftPressed(boolean value) { leftPressed = value; }
    public void setRightPressed(boolean value) { rightPressed = value; }
    public void setFirePressed(boolean value) { firePressed = value; }
    public void setUpPressed(boolean value) { UpPressed = value; }
    public void setDownPressed(boolean value) { DownPressed = value; }

    /**
     * 엔티티 이동 → 충돌 → 제거 flush → (요청 시) doLogic 순서로 처리.
     * 이동은 waitingForKeyPress=false 일 때만 수행.
     */
    public void updateEntities(long delta) {
        if (!waitingForKeyPress) {
            for (Entity entity : new java.util.ArrayList<>(entities)) {
                entity.move(delta);
            }
        }
        for (int p = 0; p < entities.size(); p++) {
            for (int s = p + 1; s < entities.size(); s++) {
                Entity a = entities.get(p);
                Entity b = entities.get(s);
                if (a.collidesWith(b)) {
                    a.collidedWith(b);
                    b.collidedWith(a);
                }
            }
        }

        entities.removeAll(removeList);
        removeList.clear();

        if (logicRequiredThisLoop) {
            for (int i = 0; i < entities.size(); i++) {
                entities.get(i).doLogic();
            }
            logicRequiredThisLoop = false;
        }
    }

    public void setAlienCount(int count) {
        this.alienCount = count;
    }

    /**
     * 적 처치 처리:
     * - 점수 +100, 남은 수 감소/하한 0
     * - 위험 모드 갱신
     * - 전멸 시 모드/보스 여부에 따라 웨이브/승리 분기
     * - 생존 Alien 수평 속도 2% 가속
     */
    public void notifyAlienKilled() {
        score += 100;
        alienCount--;
        if (alienCount < 0) alienCount = 0;
        System.out.println("[DEBUG] Alien killed! alienCount=" + alienCount + " stage=" + currentStageId + " bossActive=" + bossActive);

        dangerMode = (getPlayerShip().getCurrentHP() < 2);

        if (alienCount == 0) {
            if (infiniteMode) {
                if (!bossActive) {
                    normalsClearedInCycle++;
                    if (normalsClearedInCycle >= 3) {
                        normalsClearedInCycle = 0;
                        spawnBoss();
                    } else {
                        spawnAliens();
                    }
                }
                return;
            } else {
                if (!bossActive) {
                    if (currentStageId == 5) {
                        spawnBoss();
                    } else {
                        notifyWin();
                    }
                }
            }
        }

        for (int i = 0; i < entities.size(); i++) {
            Entity entity = entities.get(i);
            if (entity instanceof AlienEntity) {
                entity.setHorizontalMovement(entity.getHorizontalMovement() * 1.02);
            }
        }
    }

    /** 발사 쿨다운을 만족하면 탄 1발 발사(SFX 포함) */
    public void tryToFire() {
        if (System.currentTimeMillis() - lastFire < firingInterval) {
            return;
        }
        lastFire = System.currentTimeMillis();
        ShotEntity shot = new ShotEntity(this, "sprites/shot.gif", ship.getX() + 10, ship.getY() - 30);
        entities.add(shot);

        SoundManager.get().playSfx(SoundManager.Sfx.SHOOT);
    }

    /** 플레이어 피격 이벤트(로그 출력) */
    public void onPlayerHit() {
        System.out.println("Player hit!");
    }

    /**
     * 메인 루프:
     * delta 계산 → Screen.update → render →
     * (레거시 경로일 경우) 이동/충돌/doLogic →
     * 입력 기반 ship 이동 벡터/발사 처리 → 10ms 슬립.
     */
    public void gameLoop() {
        long lastLoopTime = SystemTimer.getTime();

        while (gameRunning) {
            long delta = SystemTimer.getTime() - lastLoopTime;
            lastLoopTime = SystemTimer.getTime();

            lastFpsTime += delta;
            fps++;

            if (lastFpsTime >= 1000) {
                container.setTitle(windowTitle + " (FPS: " + fps + ")");
                lastFpsTime = 0;
                fps = 0;
            }

            if (currentScreen != null) {
                currentScreen.update(delta);
            }

            Graphics2D g = (Graphics2D) strategy.getDrawGraphics();
            render(g);

            if (state == GameState.MENU) {
                g.dispose();
                strategy.show();
                SystemTimer.sleep(lastLoopTime + 10 - SystemTimer.getTime());
                continue;
            }

            boolean screenDrivesGame = (currentScreen != null);
            if (!screenDrivesGame) {
                if (!waitingForKeyPress) {
                    for (int i = 0; i < entities.size(); i++) {
                        Entity entity = entities.get(i);
                        entity.move(delta);
                    }
                }

                for (int p = 0; p < entities.size(); p++) {
                    for (int s = p + 1; s < entities.size(); s++) {
                        Entity me = entities.get(p);
                        Entity him = entities.get(s);

                        if (me.collidesWith(him)) {
                            me.collidedWith(him);
                            him.collidedWith(me);
                        }
                    }
                }

                entities.removeAll(removeList);
                removeList.clear();

                if (logicRequiredThisLoop) {
                    for (int i = 0; i < entities.size(); i++) {
                        Entity entity = entities.get(i);
                        entity.doLogic();
                    }
                    logicRequiredThisLoop = false;
                }
            }

            if (state == GameState.PLAYING && waitingForKeyPress) {
                g.setColor(Color.white);
                g.drawString(message, (800 - g.getFontMetrics().stringWidth(message)) / 2, 250);
                g.drawString("Press any key", (800 - g.getFontMetrics().stringWidth("Press any key")) / 2, 300);
            }

            g.dispose();
            strategy.show();

            ship.setHorizontalMovement(0);
            ship.setVerticalMovement(0);

            if ((UpPressed) && (!DownPressed)) {
                ship.setVerticalMovement(-moveSpeed);
            } else if ((DownPressed) && (!UpPressed)) {
                ship.setVerticalMovement(moveSpeed);
            }

            if ((leftPressed) && (!rightPressed)) {
                ship.setHorizontalMovement(-moveSpeed);
            } else if ((rightPressed) && (!leftPressed)) {
                ship.setHorizontalMovement(moveSpeed);
            }

            if (firePressed) {
                tryToFire();
            }

            SystemTimer.sleep(lastLoopTime + 10 - SystemTimer.getTime());
        }
    }

    /** Screen 교체 및 컨텍스트에 맞춘 BGM 갱신 */
    void setScreen(Screen screen) {
        this.currentScreen = screen;
        this.waitingForKeyPress = false;
        updateBGMForContext();
    }

    /** 배경 → Screen 오버레이 순으로 렌더 */
    public void render(Graphics2D g) {
        if (backgroundRenderer != null) {
            int w = getWidth() > 0 ? getWidth() : 800;
            int h = getHeight() > 0 ? getHeight() : 600;

            int wcForBg = Math.max(1, getWaveCount());
            backgroundRenderer.render(g, wcForBg, w, h);
        } else {
            g.setColor(Color.black);
            g.fillRect(0, 0, 800, 600);
        }

        if (currentScreen != null) {
            currentScreen.render(g);
        }
    }

    public int getAlienCount() {
        return alienCount;
    }
//스테이지모드 잠금 설정 기능. 스테이지 토탈을 1부터 5까지 정해놓고 클리어될시 다음 스테이지 오픈
    public void initStageUnlocks(){
        stageUnlocked = new boolean[TOTAL_STAGES];
        stageUnlocked[0]=true;
    }

    public boolean isStageUnlocked(int stageIndex){
        if (stageUnlocked == null || stageIndex < 0 || stageIndex >= stageUnlocked.length)
            return false;

        return stageUnlocked[stageIndex];
    }



    // 로드된 stageStars(1..N) 기반으로 잠금 상태 재계산
    public void rebuildStageUnlocks() {
        if (stageUnlocked == null || stageUnlocked.length != TOTAL_STAGES) {
            stageUnlocked = new boolean[TOTAL_STAGES];
        }
        // 스테이지 1(인덱스 0)은 항상 오픈
        stageUnlocked[0] = true;

        // 규칙: i번째 스테이지(1-based, 인덱스 i-1)는
        // "이전 스테이지 별이 3개 이상이면" 오픈
        for (int stageId = 2; stageId <= TOTAL_STAGES; stageId++) {
            int prevStars = getStageStars(stageId - 1); // 1..N
            stageUnlocked[stageId - 1] = (prevStars >= 3);
        }
    }

    /**
     * 키 입력: Screen에 위임.
     * - keyTyped: AuthScreen이면 문자 전달, ESC는 메뉴 복귀 및 점수 업로드
     */
    private class KeyInputHandler extends KeyAdapter {
        @Override
        public void keyPressed(KeyEvent e) {
            if (currentScreen != null) {
                currentScreen.handleKeyPress(e.getKeyCode());
            }
            if (waitingForKeyPress) {
                waitingForKeyPress = false;
                setScreen(new StageSelectScreen(Game.this));
            }
        }

        @Override
        public void keyReleased(KeyEvent e) {
            if (currentScreen != null) {
                currentScreen.handleKeyRelease(e.getKeyCode());
            }
        }

        public void keyTyped(KeyEvent e) {
            if (currentScreen instanceof AuthScreen) {
                ((AuthScreen) currentScreen).handleCharTyped(e.getKeyChar());
                return;
            }

            if (e.getKeyChar() == 27) { // ESC
                if (score > 0 && SESSION_UID != null && SESSION_ID_TOKEN != null) {
                    System.out.println("[ESC] 중간 점수 업로드: score=" + score);
                    uploadScoreIfLoggedIn();
                }
                state = GameState.MENU;
                setScreen(new MenuScreen(Game.this));
            }
        }
    }

    /** 화면 컨텍스트에 맞춰 BGM 선택/재생 */
    private void updateBGMForContext() {
        SoundManager sm = SoundManager.get();

        if (currentScreen instanceof MenuScreen) {
            System.out.println("[BGM] MENU");
            sm.play(SoundManager.Bgm.MENU);
            return;
        }

        if (currentScreen instanceof GamePlayScreen) {
            if (currentMode == Mode.STAGE && currentStageId == 5) {
                System.out.println("[BGM] BOSS (Stage 5)");
                sm.play(SoundManager.Bgm.BOSS);
            } else {
                System.out.println("[BGM] STAGE");
                sm.play(SoundManager.Bgm.STAGE);
            }
            return;
        }

        System.out.println("[BGM] MENU (fallback)");
        sm.play(SoundManager.Bgm.MENU);
    }

    // =========================
    // Backend (Firebase + REST)
    // =========================

    /** 점수 데이터 모델 */
    protected static class ScoreEntry {
        String mode;
        String email;
        Integer score;
        Integer wave;
        Long durationMs;
        String timestamp;
        Integer level;
    }

    /** 내 상위 점수 조회 */
    protected static List<ScoreEntry> fetchMyTopScores(int limit) {
        List<Game.ScoreEntry> list = new ArrayList<>();
        if (SESSION_UID == null || SESSION_ID_TOKEN == null) return list;

        try {
            String endpoint = DB_URL + "/users/" + SESSION_UID
                    + "/scores.json?auth=" + urlEnc(SESSION_ID_TOKEN)
                    + "&orderBy=%22score%22&limitToLast=" + limit;
            String res = httpGet(endpoint);

            java.lang.reflect.Type mapType =
                    new TypeToken<java.util.Map<String, Game.ScoreEntry>>() {}.getType();
            java.util.Map<String, Game.ScoreEntry> map = new Gson().fromJson(res, mapType);
            if (map != null) list.addAll(map.values());

            list.sort((a, b) -> Integer.compare(
                    b.score == null ? 0 : b.score,
                    a.score == null ? 0 : a.score
            ));
        } catch (Exception e) {
            System.err.println("점수 조회 실패: " + e.getMessage());
        }
        return list;
    }

    /** 글로벌 상위 점수 조회 */
    protected static List<ScoreEntry> fetchGlobalTopScores(int limit) {
        List<Game.ScoreEntry> list = new ArrayList<>();
        if (SESSION_ID_TOKEN == null) return list;

        try {
            String endpoint = DB_URL + "/globalScores.json?auth=" + urlEnc(SESSION_ID_TOKEN)
                    + "&orderBy=%22score%22&limitToLast=" + limit;
            String res = httpGet(endpoint);

            java.lang.reflect.Type mapType =
                    new com.google.gson.reflect.TypeToken<Map<String, Game.ScoreEntry>>() {}.getType();
            Map<String, Game.ScoreEntry> map = new com.google.gson.Gson().fromJson(res, mapType);
            if (map != null) list.addAll(map.values());

            list.sort((a, b) -> Integer.compare(
                    b.score == null ? 0 : b.score,
                    a.score == null ? 0 : a.score
            ));
        } catch (Exception e) {
            System.err.println(" 글로벌 점수 조회 실패: " + e.getMessage());
        }

        return list;
    }

    /** 로그인된 경우 점수 업로드(개인/글로벌) */
    private void uploadScoreIfLoggedIn() {
        if (SESSION_UID == null || SESSION_ID_TOKEN == null) return;

        long durationMs = (runStartedAtMs > 0) ? (System.currentTimeMillis() - runStartedAtMs) : 0L;
        String modeStr = (currentMode == Mode.STAGE) ? "STAGE" : "INFINITE";

        String json = "{"
                + "\"uid\":" + quote(SESSION_UID) + ","
                + "\"email\":" + quote(SESSION_EMAIL) + ","
                + "\"level\":" + ((ShipEntity) ship).getLevel() + ","
                + "\"mode\":" + quote(modeStr) + ","
                + "\"score\":" + score + ","
                + "\"wave\":" + waveCount + ","
                + "\"durationMs\":" + durationMs + ","
                + "\"timestamp\":" + quote(now())
                + "}";

        try {
            restPushJson("users/" + SESSION_UID + "/scores", SESSION_ID_TOKEN, json);
            restPushJson("globalScores", SESSION_ID_TOKEN, json);

            System.out.println("점수 업로드 완료: " + json);
        } catch (Exception e) {
            System.err.println("점수 업로드 실패: " + e.getMessage());
        }
    }

    /** 간단 로그(Realtime DB) */
    private static void writeLog(String eventType) {
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("logs");

        String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());

        Map<String, Object> logEntry = new HashMap<>();
        logEntry.put("event", eventType);
        logEntry.put("timestamp", timestamp);

        ref.push().setValueAsync(logEntry);

        System.out.println(" 로그 저장: " + eventType + " at " + timestamp);
    }

    private static java.awt.GridBagConstraints gbc() { return gbc(0, 0); }
    private static java.awt.GridBagConstraints gbc(int x, int y) {
        java.awt.GridBagConstraints c = new java.awt.GridBagConstraints();
        c.gridx = x; c.gridy = y; c.insets = new java.awt.Insets(5, 5, 5, 5);
        c.anchor = java.awt.GridBagConstraints.WEST; c.fill = java.awt.GridBagConstraints.HORIZONTAL;
        return c;
    }

    // =========================
    // 🌐 Firebase Auth (REST)
    // =========================
    protected static class AuthResult {
        final String idToken, refreshToken, localId, email;
        AuthResult(String idToken, String refreshToken, String localId, String email) {
            this.idToken = idToken; this.refreshToken = refreshToken; this.localId = localId; this.email = email;
        }
    }

    protected static AuthResult restSignUp(String email, String password) throws Exception {
        String endpoint = "https://identitytoolkit.googleapis.com/v1/accounts:signUp?key=" + API_KEY;
        String body = "{"
                + "\"email\":" + quote(email) + ","
                + "\"password\":" + quote(password) + ","
                + "\"returnSecureToken\":true"
                + "}";
        String res = httpPostJson(endpoint, body);
        String idToken = jget(res, "idToken");
        String refreshToken = jget(res, "refreshToken");
        String localId = jget(res, "localId");
        String emailOut = jget(res, "email");
        if (idToken == null || localId == null) throw new RuntimeException("SignUp parse failed: " + res);
        return new AuthResult(idToken, refreshToken, localId, emailOut);
    }

    protected static AuthResult restSignIn(String email, String password) throws Exception {
        String endpoint = "https://identitytoolkit.googleapis.com/v1/accounts:signInWithPassword?key=" + API_KEY;
        String body = "{"
                + "\"email\":" + quote(email) + ","
                + "\"password\":" + quote(password) + ","
                + "\"returnSecureToken\":true"
                + "}";
        String res = httpPostJson(endpoint, body);
        String idToken = jget(res, "idToken");
        String refreshToken = jget(res, "refreshToken");
        String localId = jget(res, "localId");
        String emailOut = jget(res, "email");
        if (idToken == null || localId == null) throw new RuntimeException("SignIn parse failed: " + res);
        return new AuthResult(idToken, refreshToken, localId, emailOut);
    }

    // =========================
    // 🗄️ Realtime Database (REST)
    // =========================
    private static void restLogEvent(String type) {
        if (SESSION_ID_TOKEN == null || SESSION_UID == null) return;
        String ts = now();
        String json = "{"
                + "\"event\":" + quote(type) + ","
                + "\"timestamp\":" + quote(ts) + "}";
        try {
            restPushJson("users/" + SESSION_UID + "/logs", SESSION_ID_TOKEN, json);
        } catch (Exception e) {
            System.err.println("⚠ 로그 저장 실패: " + e.getMessage());
        }
    }

    private static String restPushJson(String path, String idToken, String json) throws Exception {
        String endpoint = DB_URL + "/" + path + ".json?auth=" + urlEnc(idToken);
        return httpPostJson(endpoint, json);
    }

    protected static String restSetJson(String path, String idToken, String json) throws Exception {
        String endpoint = DB_URL + "/" + path + ".json?auth=" + urlEnc(idToken);
        return httpPutJson(endpoint, json);
    }

    // =========================
    // 🔧 HTTP & 미니 JSON 유틸
    // =========================
    protected static String httpGet(String endpoint) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(endpoint).openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Accept", "application/json");
        return readResp(conn);
    }

    protected static String httpPostJson(String endpoint, String body) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(endpoint).openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        try (OutputStream os = conn.getOutputStream()) {
            os.write(body.getBytes(StandardCharsets.UTF_8));
        }
        return readResp(conn);
    }

    protected static String httpPutJson(String endpoint, String body) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(endpoint).openConnection();
        conn.setRequestMethod("PUT");
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        try (OutputStream os = conn.getOutputStream()) {
            os.write(body.getBytes(StandardCharsets.UTF_8));
        }
        return readResp(conn);
    }

    protected static String readResp(HttpURLConnection conn) throws Exception {
        int code = conn.getResponseCode();
        try (InputStream is = (code >= 200 && code < 300) ? conn.getInputStream() : conn.getErrorStream()) {
            String txt = readFully(is, "UTF-8");
            if (code >= 200 && code < 300) return txt;
            throw new RuntimeException("HTTP " + code + ": " + txt);
        }
    }

    protected static String readFully(InputStream is, String charset) throws Exception {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buf = new byte[4096];
            int len;
            while ((len = is.read(buf)) != -1) {
                baos.write(buf, 0, len);
            }
            return baos.toString(charset);
        } finally {
            if (is != null) try { is.close(); } catch (Exception ignore) {}
        }
    }

    /** 매우 단순한 "키:문자열" 추출(필요 필드만) */
    protected static String jget(String json, String key) {
        String k = "\"" + key.replace("\"","\\\"") + "\"";
        int i = json.indexOf(k);
        if (i < 0) return null;
        i = json.indexOf(':', i);
        if (i < 0) return null;
        i++;
        while (i < json.length() && Character.isWhitespace(json.charAt(i))) i++;
        if (i >= json.length() || json.charAt(i) != '"') return null;
        i++;
        StringBuilder sb = new StringBuilder();
        while (i < json.length()) {
            char c = json.charAt(i++);
            if (c == '\\') {
                if (i >= json.length()) break;
                char n = json.charAt(i++);
                switch (n) {
                    case '\\': sb.append('\\'); break;
                    case '"':  sb.append('"');  break;
                    case 'n':  sb.append('\n'); break;
                    case 'r':  sb.append('\r'); break;
                    case 't':  sb.append('\t'); break;
                    case 'b':  sb.append('\b'); break;
                    case 'f':  sb.append('\f'); break;
                    case 'u':
                        if (i+3 < json.length()) {
                            String hex = json.substring(i, i+4);
                            try { sb.append((char)Integer.parseInt(hex,16)); } catch (Exception ignore) {}
                            i += 4;
                        }
                        break;
                    default: sb.append(n); break;
                }
            } else if (c == '"') {
                break;
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    protected static String quote(String s) {
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

    protected static String urlEnc(String s) {
        try { return java.net.URLEncoder.encode(s, "UTF-8"); }
        catch (Exception e) { return s; }
    }

    protected static String now() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
    }

    /** 엔트리 포인트: Firebase 초기화 → Game 생성/루프 실행 */
    public static void main(String argv[]) {
        try {
            FileInputStream serviceAccount = new FileInputStream(DB_KEYFILE);

            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .setDatabaseUrl(DB_URL)
                    .build();

            FirebaseApp.initializeApp(options);

            System.out.println("Firebase 초기화");
            writeLog("gamestart");

            Game g = new Game();
            g.setScreen(new AuthScreen(g));
            g.loadStageStars();

            ScoreboardScreen ss = new ScoreboardScreen(g);

            g.gameLoop();

            ss.reloadScores();
            writeLog("game over");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}