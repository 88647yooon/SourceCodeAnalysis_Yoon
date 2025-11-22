package org.newdawn.spaceinvaders;

import java.awt.Canvas;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferStrategy;

import java.util.*;
import java.text.SimpleDateFormat;
import java.io.FileInputStream;
import javax.swing.*;

import org.newdawn.spaceinvaders.DataBase.*;
import org.newdawn.spaceinvaders.Screen.Screen;
import org.newdawn.spaceinvaders.Screen.StageSelectScreen;
import org.newdawn.spaceinvaders.entity.*;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import org.newdawn.spaceinvaders.entity.base.Entity;
import org.newdawn.spaceinvaders.entity.enemy.DiagonalShooterAlienEntity;
import org.newdawn.spaceinvaders.entity.enemy.HostageEntity;
import org.newdawn.spaceinvaders.entity.enemy.RangedAlienEntity;
import org.newdawn.spaceinvaders.entity.enemy.AlienEntity;
import org.newdawn.spaceinvaders.entity.player.ShipEntity;
import org.newdawn.spaceinvaders.entity.projectile.ShotEntity;

/**
 * Game — Space Invaders 메인 오케스트레이션.
 * 역할
 *  - 화면(Screen) 전환, 엔티티(Entity) 생명주기, 게임 루프, 점수/웨이브/스테이지 상태 관리.
 * 프레임 순서(단일 스레드)
 *  1) currentScreen.update(deltaMs)
 *  2) render(g)  // 배경 → Screen 오버레이
 *  3) (레거시 경로인 경우) 엔티티 이동/충돌/doLogic
 *  4) ship 이동 벡터/발사 처리
 *  5) 10ms 슬립
 * 단위/스레드
 *  - 시간: ms, 속도: px/s, 좌표: px
 *  - 싱글 스레드 전제. update/render 동안 블로킹 I/O 금지.
 */

public class Game extends Canvas {

    // 인증/DB 관련 필드
    private final transient String API_KEY = "AIzaSyCdY9-wpF3Ad2DXkPTXGcqZEKWBD1qRYKE";
    public static final String DB_URL = "https://sourcecodeanalysis-donggyu-default-rtdb.asia-southeast1.firebasedatabase.app";
    // Firebase Admin SDK 용 서비스 키
    private static final String DB_KEYFILE = "src/main/resources/serviceAccountKey.json";

    public static String SESSION_UID   = null;
    public static String SESSION_EMAIL = null;
    public static String SESSION_ID_TOKEN = null;

    // 세션/DB 의존성
    private final transient DatabaseClient dbClient = new FirebaseDatabaseClient(DB_URL);
    private final transient GameDatabaseService gameDb = new GameDatabaseService(dbClient);
    private transient AuthSession session; // 기존 SESSION_UID , EMAIL, ID_TOKEN 대체

    private final transient FirebaseAuthService authService = new FirebaseAuthService(API_KEY);
    public FirebaseAuthService getAuthService() { return authService; }

    public AuthSession getSession(){ return session; }
    public DatabaseClient getDbClient(){ return dbClient; }
    public GameDatabaseService getGameDb(){ return gameDb; }
    public void setSession(AuthSession session){ this.session = session; }
    public boolean hasSession(){ return session != null && session.isLoggedIn(); }

	/** 페이지 넘김을 가속화 할 수 있는 전략 */
	private final transient BufferStrategy strategy;
	/** 게임이 현재 "실행 중"이라면, 즉 게임 루프가 반복되고 있습니다 */
	private final transient boolean gameRunning = true;
    /** 우리 게임에 존재하는 모든 엔티티 목록 */
    private final transient ArrayList<Entity> entities = new ArrayList<>();
    /** 이 프레임에서 제거할 엔티티 큐 */
    private final transient ArrayList<Entity> removeList = new ArrayList<>();

    /** 플레이어(Ship) 엔티티 */
    private transient Entity ship;

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
    private transient BackgroundRenderer backgroundRenderer;
    /** 활성 화면 */
    private transient Screen currentScreen;

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
    private int waveCount = 1;
    private int normalsClearedInCycle = 0;
    private static final double RANGED_ALIEN_RATIO = 0.25;
    private boolean bossActive = false;

    //스테이지 잠금 상태 관리 배열
    private static boolean[] stageUnlocked;
    private static  final int TOTAL_STAGES = 5;

    /** 초기 화면·버퍼·입력·BGM·엔티티 설정 */
    public Game() {
        // 프레임에 띄울 게임 명
        container = new JFrame("Space Invaders 102");
        // 배경화면 렌더링
        backgroundRenderer = new BackgroundRenderer();
        // 메뉴 스크린 불러오기
        setScreen(new MenuScreen(this)); // 시작 화면
        // 패널
        JPanel panel = (JPanel) container.getContentPane();
        // 사이즈
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

        addKeyListener(new GameKeyInputHandler(this));
        setFocusable(true);
        setFocusTraversalKeysEnabled(false);
        SwingUtilities.invokeLater(this::requestFocusInWindow);
        requestFocus();

        createBufferStrategy(2);
        strategy = getBufferStrategy();

        initStageUnlocks();
        initEntities();

        SoundManager.get().setSfxVolume(-15.0f);// 전체 효과음 볼륨 설정
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

        ((ShipEntity) ship).getStats().setInvulnerable(false);
        if (hasSession()) {
            ShipEntity s = getPlayerShip();

            // 레벨
            int[] saved = LevelManager.loadLastLevel(DB_URL,
                    session.getUid(),
                    session.getIdToken());
            s.getStats().setLevelAndXp(saved[0], saved[1]);

            // 스킬
            PlayerSkills ps = s.getStats().getSkills();
            s.getPersistence().loadSkills(ps);
        }

        alienCount = 0;
        if (infiniteMode) {
            spawnAliens();
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

    //무한모드일 때 기존 인질 제거
    private void clearHostagesForInfiniteMode() {
        if (!infiniteMode) {
            return;
        }

        for (Iterator<Entity> it = entities.iterator(); it.hasNext(); ) {
            Entity e = it.next();
            if(e instanceof HostageEntity) it.remove();
        }
    }

    //alien 그리드 생성
    private void spawnAlienGrid(Difficulty diff, int rows, int cols, int startX, int startY, int gapX, int gapY) {
        for (int row = 0; row < rows; row++) {
            for (int c = 0; c < cols; c++) {
                int x = startX + (c * gapX);
                int y = startY + (row * gapY);

                Entity alien = createAlienForPosition(x, y);
                applyDifficultyToAlien(alien, diff);

                entities.add(alien);
                alienCount++;
            }
        }
    }

    //위치별 외계인 타입 결정
    private Entity createAlienForPosition(int x, int y) {
        double diagonalProb = Math.min(0.05 + (waveCount -1) * 0.02, 0.25);
        double rangedProb = RANGED_ALIEN_RATIO;

        double r = Math.random();

        if(r < diagonalProb) {
            return new DiagonalShooterAlienEntity(this, x, y);
        }
        if(r > diagonalProb+ rangedProb) {
            return new RangedAlienEntity(this, x, y ,getPlayerShip());
        }
        return new AlienEntity(this, x, y);
    }

    //무한모드일때 인질 스폰
    private void spawnHostages(int cols, int startX, int startY, int gapX) {
        int hostageNum = 1 + (int)(Math.random()*2);
        if (hostageNum <=0){
            return;
        }

        Set<Integer> usedCols = new HashSet<>();

        for(int i=0; i< hostageNum; i++){
            int c = chooseHostageColumn(cols, usedCols);
            usedCols.add(c);

            int x = startX + (c *gapX);
            int y = startY - 40;
            Entity hostage = new HostageEntity(this, x, y);
            entities.add(hostage);
        }
    }

    //인질이 설 위치 컬럼 선택 로직
    private int chooseHostageColumn(int cols, Set<Integer> usedCols) {
        int guard = 0;
        int c;
        do{
            c = (int)(Math.random() * cols);
        } while (usedCols.contains(c) && ++ guard < 10);
        return c;
    }

    private void spawnAliens() {

        clearHostagesForInfiniteMode(); // 기존 인질 정리
        Difficulty diff = computeDifficultyForWave(waveCount);

        int rows = 3 + (waveCount % 3);   // 3~5
        int cols = 6 + (waveCount % 6);   // 6~11
        alienCount = 0;

        int startX = 100, startY = 50, gapX = 50, gapY = 30;

        spawnAlienGrid(diff, rows, cols, startX, startY, gapX, gapY); //외계인 배치

        if(infiniteMode) {
            spawnHostages(cols, startX, startY, gapX); // 인질 배치
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

        Entity boss = new org.newdawn.spaceinvaders.entity.boss.BossEntity(this, 360, 60, getPlayerShip());
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

    public void saveStageStars(){
        gameDb.saveStageStars(session, stageStars);
    }

    public void loadStageStars(){
        if (!hasSession()) return;

        Map<Integer, Integer> loaded = gameDb.loadStageStars(session);

        stageStars.clear();
        stageStars.putAll(loaded);

        rebuildStageUnlocks();
        System.out.println(" 별 기록 불러오기 완료 " + stageStars);
    }

    // [수정] 별을 갱신/저장한 직후에도 재계산
    public void setStageStars(int stageId, int stars) {
        int prev = stageStars.getOrDefault(stageId, 0);
        if(stars > prev){
            stageStars.put(stageId, stars);

            if (session != null && session.isLoggedIn()){
                gameDb.saveStageStars(session, stageStars);
            }

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

        stageStartHP = getPlayerShip().getStats().getCurrentHP();
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

        if (hasSession()) {
            ShipEntity ship = getPlayerShip();
            if (ship != null) {
                LevelManager.saveLastLevel(getDbClient(), session.getUid(), session.getIdToken(), getPlayerShip().getStats().getLevel(), getPlayerShip().getStats().getXpIntoLevel());
            }

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
                damageTaken = Math.max(0, stageStartHP - p.getStats().getCurrentHP());
            }
            evaluateStageResult(currentStageId, timeLeft, damageTaken, score);
            //조건 충족시 다음 스테이지 해제
            unlockNextStageIfEligible(currentStageId);

        }

        if (hasSession()) {
            ShipEntity ship = getPlayerShip();
            if (ship != null) {
                LevelManager.saveLastLevel(getDbClient(),session.getUid(), session.getIdToken(), getPlayerShip().getStats().getLevel(), getPlayerShip().getStats().getXpIntoLevel());
            }
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

    /** 원래 코드는 public java.util.List<Entity> getEntities() { return entities;}
     * 이렇게 되면 add(), set() 등 수정메서드를 사용 가능하기때문에 바꿀수 없고 보기만 가능하게 만들었다
     **/

    public List<Entity> getEntities() {
        return Collections.unmodifiableList(entities);
    }

    List<Entity> getMutableEntities() {
        return entities;
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

    /**
     * [리팩토링 - 신규 메소드]
     * * AlienEntity로부터 외계인이 사망했음을 알림받는 중앙 처리 메소드.
     * 기존 notifyAlienKilled()의 로직과 ShotEntity에 있던 XP, 엔티티 제거 로직을
     * 이곳에서 모두 통합하여 처리합니다. (SRP, 캡슐화)
     *
     * @param alien 사망한 외계인 엔티티
     */

    //XP 지급
    private void PayXpForKill(AlienEntity alien) {
        int xp = calculateXpForAlien(alien);

        ShipEntity player = getPlayerShip();
        if(player != null) {
            player.getStats().addXp(xp);
        }
    }

    //Xp 계산
    private int calculateXpForAlien(AlienEntity alien) {
        if(alien instanceof RangedAlienEntity) {
            return 8; //RangedAlienEntity 처치시 8
        }
        if(alien instanceof DiagonalShooterAlienEntity){
            return 10; //DiagonalShooterAlienEntity 처치시 10 지급
        }
        return 5; // 기본Alien 지급 xp
    }

    //엔티티 제거
    private void removeKilledAlien(AlienEntity alien) {
        if(entities.contains(alien)){
            removeEntity(alien);
        }
    }

    //점수/카운트, dangerMode 갱신
    private void updateScoreAndCount(){
        score += 100;
        alienCount--;
        if (alienCount < 0) alienCount = 0;

        //디버깅 메시지
        System.out.println("[DEBUG] Alien killed! alienCount=" + alienCount + " stage=" + currentStageId + " bossActive=" + bossActive);

        ShipEntity player = getPlayerShip();
        if(player != null) {
            dangerMode = (player.getStats().getCurrentHP() < 1); //원래 2였는데 1로 변경함.
        }
    }

    //Alien을 전부 처리하였는지에 따른 처리(웨이브 / 보스 / 승리 통합 처리)
    private void aliensCleared() {
        if(alienCount != 0) {
            return;
        }

        if(infiniteMode) {
            aliensClearedInInfiniteMode();
        } else {
            aliensClearedInStageMode();
        }
    }

    //무한모드일때
    private void aliensClearedInInfiniteMode() {
        if(bossActive) {
            return; //보스전에는 해당 없음
        }

        normalsClearedInCycle++;
        if(normalsClearedInCycle >= 3){

            normalsClearedInCycle = 0;
            spawnBoss();

        } else {
            spawnAliens();
        }
    }

    private void aliensClearedInStageMode() {
        if(bossActive) {
            return; // 보스 처리는 onBossKilled 에서 처리
        }

        if(currentStageId == 5) {
            spawnBoss();
        } else {
            notifyWin();
        }
    }

    //남은 Alien들 속도 증가
    private void speedUpAliens() {
        for(int i=0; i< entities.size(); i++){
            Entity entity = entities.get(i);
            if(entity instanceof AlienEntity) {
                entity.setHorizontalMovement(entity.getHorizontalMovement() + 1.04);
            }
        }
    }

    public void onAlienKilled(AlienEntity alien) {

        PayXpForKill(alien); //XP 지급, 계산
        removeKilledAlien(alien); // 엔티티 제거
        updateScoreAndCount(); // 점수/카운트, dangerMode 갱신
        aliensCleared(); // 웨이브, 보스, 승리 처리
        speedUpAliens(); //남은 적 속도 증가

    }

    /** 발사 쿨다운을 만족하면 탄 1발 발사(SFX 포함) */
    public void tryToFire() {
        if (System.currentTimeMillis() - lastFire < firingInterval) {
            return;
        }
        lastFire = System.currentTimeMillis();
        ShotEntity shot = new ShotEntity(this, "sprites/shot.gif", ship.getX() + 10, ship.getY() - 30);
        entities.add(shot);

        SoundManager.get().playSfx(SoundManager.Sfx.SHOOT); //플레이어 총소리
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
     * GameKeyInputHandler의 getter
     * 헬퍼 메서드 필드
     */

    //현재 스크린의 상태 조회하기
    public Screen getCurrentScreen() { return currentScreen; }

    //아무 키나 누르기
    public boolean isWaitingForKeyPress() { return waitingForKeyPress; }
    public void setWaitingForKeyPress(boolean value) { this.waitingForKeyPress = value; }

    //점수 조회(ESC 업로드용)
    public int getScore(){ return score; }

    //스테이지 선택 화면으로 이동
    public void goToStageSelectScreen(){ setScreen(new StageSelectScreen(this)); }

    //메뉴 화면으로 돌아가기(ESC 눌렀을때)
    public void goToMenuScreen(){
        state = GameState.MENU;
        setScreen(new MenuScreen(this));
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
                System.out.println("[BGM] BOSS (Stage 5)"); //보스전 bgm
                sm.play(SoundManager.Bgm.BOSS);
            } else {
                System.out.println("[BGM] STAGE"); //일반 스테이지 bgm
                sm.play(SoundManager.Bgm.STAGE);
            }
            return;
        }

        System.out.println("[BGM] MENU (fallback)"); //기본값
        sm.play(SoundManager.Bgm.MENU);
    }

    public void uploadScoreIfLoggedIn() {
        if (!hasSession()) return;

        long durationMs = (runStartedAtMs > 0) ? (System.currentTimeMillis() - runStartedAtMs) : 0L;
        String modeStr = (currentMode == Mode.STAGE) ? "STAGE" : "INFINITE";

        ScoreEntry entry = new ScoreEntry();
        entry.setMode(modeStr);
        entry.setEmail(session.getEmail());
        entry.setScore(score);
        entry.setWave(waveCount);
        entry.setDurationMs(durationMs);
        entry.setTimestamp(now());
        entry.setLevel(((ShipEntity) ship).getStats().getLevel());

        gameDb.uploadScore(session, entry);
    }

    public List<ScoreEntry> fetchMyTopScores(int limit) {
        if (session == null || !session.isLoggedIn()) {
            return Collections.emptyList();
        }
        return gameDb.fetchMyTopScores(session, limit);
    }


    public List<ScoreEntry> fetchGlobalTopScores(int limit) {
        if (session == null || !session.isLoggedIn()) {
            return Collections.emptyList();
        }
        return gameDb.fetchGlobalTopScores(session, limit);
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


    protected static String now() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
    }

    public void restartLastMode() {
        if (currentMode == Mode.INFINITE) {
            startInfiniteMode();
        } else {
            startStageMode(1);
        }
    }

    /** 엔트리 포인트: Firebase 초기화 → Game 생성/루프 실행 */
    public static void main(String[] argv) {
        try {
            FileInputStream serviceAccount = new FileInputStream(DB_KEYFILE);

            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .setDatabaseUrl(DB_URL)
                    .build();

            FirebaseApp.initializeApp(options);

            writeLog("gamestart");

            Game g = new Game();
            g.setScreen(new AuthScreen(g));
            LevelManager.loadLastLevel(DB_URL,SESSION_UID, SESSION_ID_TOKEN);

            ScoreboardScreen ss = new ScoreboardScreen(g);

            g.gameLoop();

            ss.reloadScores();
            writeLog("game over");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}