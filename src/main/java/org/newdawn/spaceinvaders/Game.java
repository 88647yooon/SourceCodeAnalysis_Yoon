package org.newdawn.spaceinvaders;

import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferStrategy;

import java.util.*;
import java.text.SimpleDateFormat;
import java.io.FileInputStream;
import java.util.List;
import javax.swing.*;

import org.newdawn.spaceinvaders.database.*;
import org.newdawn.spaceinvaders.entity.player.PlayerSkills;
import org.newdawn.spaceinvaders.graphics.BackgroundRenderer;
import org.newdawn.spaceinvaders.input.GameKeyInputHandler;
import org.newdawn.spaceinvaders.manager.*;
import org.newdawn.spaceinvaders.screen.*;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import org.newdawn.spaceinvaders.entity.base.Entity;
import org.newdawn.spaceinvaders.entity.enemy.DiagonalShooterAlienEntity;
import org.newdawn.spaceinvaders.entity.enemy.RangedAlienEntity;
import org.newdawn.spaceinvaders.entity.enemy.AlienEntity;
import org.newdawn.spaceinvaders.entity.player.ShipEntity;
import org.newdawn.spaceinvaders.entity.projectile.ShotEntity;
import org.newdawn.spaceinvaders.screen.auth.AuthScreen;
import org.newdawn.spaceinvaders.util.SystemTimer;

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


    // 세션/DB 의존성
    private final transient DatabaseClient dbClient = new FirebaseDatabaseClient(DB_URL);
    private final transient GameDatabaseService gameDb = new GameDatabaseService(dbClient);
    private final transient StageProgressManager stageProgressManager = new StageProgressManager(gameDb);
    private transient AuthSession session; // 기존 SESSION_UID , EMAIL, ID_TOKEN 대체

    private final transient FirebaseAuthService authService = new FirebaseAuthService(API_KEY);
    public FirebaseAuthService getAuthService() { return authService; }
    public DatabaseClient getDbClient(){ return dbClient; }

    public void setSession(AuthSession session){ this.session = session; }
    public boolean hasSession(){ return session != null && session.isLoggedIn(); }
    public String getMessage(){ return message; }
	/** 페이지 넘김을 가속화 할 수 있는 전략 */
	private final transient BufferStrategy strategy;
	/** 게임이 현재 "실행 중"이라면, 즉 게임 루프가 반복되고 있습니다 */
	private final transient boolean gameRunning = true;

    private final transient EntityManager entityManager = new EntityManager(this);
    private final transient EntitySpawnManager spawnManager = new EntitySpawnManager(this, entityManager);

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
    /** 아무 키 대기 중인지 여부 */
    private boolean waitingForKeyPress = true;

    /** 입력 상태 */
    private boolean leftPressed = false;
    private boolean rightPressed = false;
    private boolean upPressed = false;
    private boolean downPressed = false;
    private boolean firePressed = false;

    /** 이번 루프에서 별도의 게임 로직(doLogic)을 적용해야 하는지 */
    private boolean logicRequiredThisLoop = false;

    /** FPS 측정 누적 시간(ms) */
    private long lastFpsTime;
    /** FPS 카운터 */
    private int fps;
    /** 윈도우 타이틀 기본값 */
    private final String windowTitle = "Space Invaders 102";
    /** 메시지 호출 */
    private String message = "";
    /** 게임 윈도우 */
    private final JFrame container;

    /** 배경 렌더러 */
    private final transient BackgroundRenderer backgroundRenderer;
    /** 활성 화면 */
    private transient Screen currentScreen;

    //entityManager에서 읽어갈 getter
    public boolean isLogicRequiredThisLoop() { return logicRequiredThisLoop; }

    //EntityManager에서 한 프레임 처리 후 flag를 꺼주는 setter
    public void resetLogicFlag() { logicRequiredThisLoop = false; }

    public AuthSession getSession() { return session; }

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

    private Map<Integer, Integer> stageStars = new HashMap<>();

    // 무한 모드/웨이브/보스
    private boolean infiniteMode = false;
    private int waveCount = 1;
    private int normalsClearedInCycle = 0;
    private boolean bossActive = false;

    //스테이지 잠금 상태 관리 배열
    private boolean[] stageUnlocked;
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

        container.pack();
        container.setResizable(false);
        container.setVisible(true);
        container.addWindowListener(new WindowAdapter() { public void windowClosing(WindowEvent e) { System.exit(0); } });

        addKeyListener(new GameKeyInputHandler(this));
        setFocusable(true);
        setFocusTraversalKeysEnabled(false);
        SwingUtilities.invokeLater(this::requestFocusInWindow);
        requestFocus();

        createBufferStrategy(2);
        strategy = getBufferStrategy();

        initEntities();

        SoundManager.get().setSfxVolume(-15.0f);// 전체 효과음 볼륨 설정
    }

    /**
     * 기존 엔티티/상태를 초기화하고 새 게임을 시작.
     * - 엔티티/제거 큐 초기화, 입력 리셋, 상태 PLAYING 전환
     */
    private void startGame() {
        entityManager.clearEntity();
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
        entityManager.clearEntity();

        ship = new ShipEntity(this, "sprites/ship.gif", 370, 550);
        entityManager.addEntity(ship);

        ((ShipEntity) ship).getStats().setInvulnerable(false);
        if (hasSession()) {
            ShipEntity s = getPlayerShip();

            // 레벨
            int[] saved = PlayerRepository.loadLastLevel(DB_URL,
                    session.getUid(),
                    session.getIdToken());
            s.getStats().setLevelAndXp(saved[0], saved[1]);

            // 스킬
            PlayerSkills ps = s.getStats().getSkills();
            s.getPersistence().loadSkills(ps);
        }

        alienCount = 0;
        if (infiniteMode) {
            spawnManager.spawnAliens();
        }

    }
    //wave
    public int getWaveCount() { return waveCount; }
    public void incrementWaveCount() { waveCount++; }

    //infiniteMode
    public boolean isInfiniteMode() { return infiniteMode; }

    //boss state
    public void setBossActive(boolean bossActive) { this.bossActive = bossActive; }
    public boolean isBossActive() { return bossActive; }

    //alien count
    public void setAlienCount(int count) { this.alienCount = count; }

    public void updateEntities(long delta) { entityManager.update(delta, waitingForKeyPress);}


    /** 보스 처치 콜백: 모드에 따라 웨이브 진행 또는 승리 처리 */
    public void onBossKilled() {
        bossActive = false;
        if (infiniteMode) {
            spawnManager.spawnAliens();
        } else {
            notifyWin();
        }
    }


    public int getStageStarScoreRequirements(int stageId) {
        return stageProgressManager.getRequiredScore(stageId);
    }

    private void evaluateStageResult(int stageId, int timeLeft, int damageTaken, int score) {
        int stars = stageProgressManager.evaluateStars(stageId, timeLeft, damageTaken, score);
        stageProgressManager.updateStageStars(session, stageId, stars);
    }

    public int getStageStars(int stageId) {
       return stageProgressManager.getStageStars(stageId);
    }

    public void saveStageStars(){
        stageProgressManager.saveAll(session);
    }

    public void loadStageStars(){
        stageProgressManager.loadFromDb(session);
        System.out.println(" 별 기록 불러오기 완료 " + stageStars);
    }

    // [수정] 별을 갱신/저장한 직후에도 재계산
    public void setStageStars(int stageId, int stars) {
       stageProgressManager.updateStageStars(session, stageId, stars);
    }

    // [추가] 당장 다음 스테이지(= 현재 stageId의 다음 인덱스)를 오픈
    private void unlockNextStageIfEligible(int currentStageId) {
        stageProgressManager.unlockNextStageIfEligible(currentStageId);
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


    /** 플레이어 사망 처리 */
    public void notifyDeath() {
        state = GameState.GAMEOVER;
        waitingForKeyPress = false;
        message = "";

        if (hasSession()) {
            ShipEntity ship = getPlayerShip();
            if (ship != null) {
                PlayerRepository.saveLastLevel(getDbClient(), session.getUid(), session.getIdToken(), getPlayerShip().getStats().getLevel(), getPlayerShip().getStats().getXpIntoLevel());
            }

        }

        setScreen(new GameOverScreen(this));
        uploadScoreIfLoggedIn();
    }


    /** 플레이어 승리 처리(별 평가/저장 포함) */
    public void notifyWin() {
        message = "Player Win!";
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
            ShipEntity playerShip = getPlayerShip();
            if (playerShip != null) {
                PlayerRepository.saveLastLevel(getDbClient(),session.getUid(), session.getIdToken(), getPlayerShip().getStats().getLevel(), getPlayerShip().getStats().getXpIntoLevel());
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

    public List<Entity> getEntities() { return entityManager.getEntities(); }

    public List<Entity> getMutableEntities() { return entityManager.getMutableEntities(); }

    public ShipEntity getPlayerShip() { return (ShipEntity) ship; }

    public void addEntity(Entity e) { entityManager.addEntity(e);}

    /** 엔티티 제거 요청(프레임 말미에 일괄 처리) */
    public void removeEntity(Entity entity) {
        entityManager.removeEntity(entity);
    }

    public int getAlienCount() {
        return alienCount;
    }

    public void setLeftPressed(boolean value) { leftPressed = value; }
    public void setRightPressed(boolean value) { rightPressed = value; }
    public void setFirePressed(boolean value) { firePressed = value; }
    public void setUpPressed(boolean value) { upPressed = value; }
    public void setDownPressed(boolean value) { downPressed = value; }

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
        if(getMutableEntities().contains(alien)){
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
            spawnManager.spawnBoss();

        } else {
            spawnManager.spawnAliens();
        }
    }

    private void aliensClearedInStageMode() {
        if(bossActive) {
            return; // 보스 처리는 onBossKilled 에서 처리
        }

        if(currentStageId == 5) {
            spawnManager.spawnBoss();
        } else {
            notifyWin();
        }
    }

    //남은 Alien들 속도 증가
    private void speedUpAliens() {
        for(Entity entity : getMutableEntities()) {
            if(entity instanceof AlienEntity) {
                entity.setHorizontalMovement(entity.getHorizontalMovement() * 1.02);
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
        entityManager.addEntity(shot);

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
            long currentTime = SystemTimer.getTime();
            long delta = currentTime - lastLoopTime;
            lastLoopTime = currentTime;

            updateFps(delta);
            updateScreen(delta);

            Graphics2D g = beginFrame();
            render(g);

            if (state == GameState.MENU) {
                endFrame(g, lastLoopTime);
                continue;
            }

            if (!doesScreenDriveGame()) {
                updateEntities(delta);
            }

            endFrame(g, lastLoopTime); //dispose + show +sleep
            ShipKeyInputHandler(); // Ship의 방향키 및 발사 처리
        }
    }
    // FPS업데이트
    private void updateFps(long delta) {
        lastFpsTime += delta;
        fps++;

        if (lastFpsTime >= 1000) {
            container.setTitle(windowTitle + " (FPS: " + fps + ")");
            lastFpsTime = 0;
            fps = 0;
        }

    }

    //Screen업데이트
    private void updateScreen(long delta){
        if (currentScreen != null) {
            currentScreen.update(delta);
        }
    }
    //화면 그리기 시작
    private Graphics2D beginFrame(){
        return (Graphics2D) strategy.getDrawGraphics();
    }

    //화면 그리기 종료
    private void endFrame(Graphics2D g, long lastLoopTime){
        g.dispose();
        strategy.show();
        SystemTimer.sleep(lastLoopTime + 10 - SystemTimer.getTime());
    }

    private boolean doesScreenDriveGame(){
        return currentScreen != null;
    }

    //플레이어 입력 로직
    private void ShipKeyInputHandler(){
        ship.setHorizontalMovement(0);
        ship.setVerticalMovement(0);

        if ((upPressed) && (!downPressed)) {
            ship.setVerticalMovement(-moveSpeed);
        } else if ((downPressed) && (!upPressed)) {
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
    }


    /** Screen 교체 및 컨텍스트에 맞춘 BGM 갱신 */
    public void setScreen(Screen screen) {
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

    public boolean isStageUnlocked(int stageIndex){
        return stageProgressManager.isStageUnlocked(stageIndex);
    }



    // 로드된 stageStars(1..N) 기반으로 잠금 상태 재계산
    public void rebuildStageUnlocks() {
        stageProgressManager.rebuildStageUnlocks();
    }

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


    public static String now() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
    }

    public void restartLastMode() {
        if (currentMode == Mode.INFINITE) {
            startInfiniteMode();
        } else {
            startStageMode(1);
        }
    }

    public void requestExit(){
        System.exit(0);
    }

    /** 엔트리 포인트: Firebase 초기화 → Game 생성/루프 실행 */
    public static void main(String[] argv) {
        try (FileInputStream serviceAccount = new FileInputStream(DB_KEYFILE)){

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
}