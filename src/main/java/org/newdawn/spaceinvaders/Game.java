package org.newdawn.spaceinvaders;

import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferStrategy;

import java.util.List;
import javax.swing.*;

import org.newdawn.spaceinvaders.database.*;
import org.newdawn.spaceinvaders.entity.player.PlayerSkills;
import org.newdawn.spaceinvaders.graphics.BackgroundRenderer;
import org.newdawn.spaceinvaders.input.GameKeyInputHandler;
import org.newdawn.spaceinvaders.input.PlayerController;
import org.newdawn.spaceinvaders.manager.*;
import org.newdawn.spaceinvaders.screen.*;

import org.newdawn.spaceinvaders.entity.base.Entity;
import org.newdawn.spaceinvaders.entity.enemy.AlienEntity;
import org.newdawn.spaceinvaders.entity.player.ShipEntity;
import org.newdawn.spaceinvaders.state.*;
import org.newdawn.spaceinvaders.util.SystemTimer;
import org.newdawn.spaceinvaders.manager.SessionManager;
import org.newdawn.spaceinvaders.entity.projectile.EnemyShotEntity;
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

    /*
     * 인증/DB 세션
     */

    private final transient String API_KEY = "AIzaSyCdY9-wpF3Ad2DXkPTXGcqZEKWBD1qRYKE";
    public static final String DB_URL = "https://sourcecodeanalysis-donggyu-default-rtdb.asia-southeast1.firebasedatabase.app";
    // 세션/DB 의존성
    private final transient DatabaseClient dbClient = new FirebaseDatabaseClient(DB_URL);
    private final transient GameDatabaseService gameDb = new GameDatabaseService(dbClient);
    private final transient StageProgressManager stageProgressManager = new StageProgressManager(gameDb);
    private final transient SessionManager sessionManager = new SessionManager();
    private final transient FirebaseAuthService authService = new FirebaseAuthService(API_KEY);
    
    public GameDatabaseService getGameDb() { return gameDb;}
    public FirebaseAuthService getAuthService() { return authService; }
    public DatabaseClient getDbClient(){ return dbClient; }

    public void setSession(AuthSession session){ sessionManager.setSession(session); }
    public boolean hasSession(){ return sessionManager.hasSession(); }
    public AuthSession getSession() { return sessionManager.getSession(); }
    public String getMessage(){ return message; }


    //렌더링 / 윈도우 / 루프
	private final transient BufferStrategy strategy;
	private final transient boolean gameRunning = true;
    private final JFrame container;
    private final String windowTitle = "Space Invaders 102";
    /** 배경 렌더러 */
    private final transient BackgroundRenderer backgroundRenderer;
    /** 스크린 라우터 */
    private final transient ScreenNavigator screenNavigator = new ScreenNavigator(this);

    /** FPS 측정 누적 시간(ms) */
    private long lastFpsTime;
    private int fps;

    /*
     * 엔티티 / 전투 / 입력
     */

    private final transient EntityManager entityManager = new EntityManager(this);
    private final transient EntitySpawnManager spawnManager = new EntitySpawnManager(this, entityManager);
    private final transient CombatManager combatManager = new CombatManager(this, entityManager, spawnManager);
    private final GameProgressContext context = new GameProgressContext();
    private final transient PlayerController playerController = new PlayerController(this);
    /** 플레이어(Ship) 엔티티 */
    private transient Entity ship;

    /* --------------------
     *   게임 플래그 / 상태
     * --------------------
     */

    /** 아무 키 대기 중인지 여부 */
    private boolean waitingForKeyPress = true;
    /** 이번 루프에서 별도의 게임 로직(doLogic)을 적용해야 하는지 */
    private boolean logicRequiredThisLoop = false;
    /** 메시지 호출 */
    private String message = "";
    //ScreenNavigator getter
    public ScreenNavigator getScreenNavigator() { return screenNavigator; }

    //entityManager에서 읽어갈 getter
    public boolean isLogicRequiredThisLoop() { return logicRequiredThisLoop; }

    public EntitySpawnManager getSpawnManager() { return spawnManager; }

    //EntityManager에서 한 프레임 처리 후 flag를 꺼주는 setter
    public void resetLogicFlag() { logicRequiredThisLoop = false; }

    //message setter
    public void setMessage(String message) { this.message = message; }



    private static final int STAGE_TIME_LIMIT_MS = 120_000;
    private transient GameState state;

    
    public Game() {
        // 프레임에 띄울 게임 명
        container = new JFrame(windowTitle);
        // 배경화면 렌더링
        backgroundRenderer = new BackgroundRenderer();
        // 메뉴 스크린 불러오기
        changeState(new MenuState(this));

        // 패널
        JPanel panel = (JPanel) container.getContentPane();
        panel.setPreferredSize(new Dimension(800, 600));
        panel.setLayout(null);

        setBounds(0, 0, 800, 600);
        panel.add(this);

        setIgnoreRepaint(true);

        container.pack();
        container.setResizable(false);
        container.setVisible(true);
        container.addWindowListener(new WindowAdapter() { public void windowClosing(WindowEvent e) { System.exit(0); } });

        // 키 입력
        addKeyListener(new GameKeyInputHandler(this));
        setFocusable(true);
        setFocusTraversalKeysEnabled(false);
        SwingUtilities.invokeLater(this::requestFocusInWindow);
        requestFocus();

        //더블 버퍼
        createBufferStrategy(2);
        strategy = getBufferStrategy();
        //엔티티 초기화
        initEntities();
        // SFX 볼륨
        SoundManager.getSound().setSfxVolume(-15.0f);// 전체 효과음 볼륨 설정
    }

    private void startGame() {
        entityManager.clearEntity();
        context.getAlienCount();

        initEntities();
        
        playerController.getInputState().reset();
        waitingForKeyPress = false;
        
        changeState(new PlayingState(this));
    }


    private void initEntities() {
        entityManager.clearEntity();

        ship = new ShipEntity(this, "sprites/ship.gif", 370, 550);
        entityManager.addEntity(ship);
        playerController.setShip((ShipEntity) ship);

        //무적모드(테스트용)
        ((ShipEntity) ship).getStats().setInvulnerable(true);

        if (hasSession()) {
            ShipEntity s = getPlayerShip();
            AuthSession current = getSession();

            // 레벨
            int[] saved = PlayerRepository.loadLastLevel(
                    DB_URL,
                    current.getUid(),
                    current.getIdToken());
            s.getStats().setLevelAndXp(saved[0], saved[1]);

            // 스킬
            PlayerSkills ps = s.getStats().getSkills();
            s.getPersistence().loadSkills(ps);
        }

        if (context.isInfiniteMode()) {
            spawnManager.spawnAliensForInfiniteMode();
        }

    }

    public void stepIdle(long delta) {
        // 메뉴, 게임오버 상태에서 공통으로 돌릴 로직이 생기면 여기 작성하면 됨.
        // 현재는 따로 처리할 로직 없기에 비워둠.
    }

    //wave
    public int getWaveCount() { return context.getWaveCount(); }
    public void incrementWaveCount() { context.incrementWaveCount(); }

    //boss state
    public void setBossActive(boolean bossActive) { context.setBossActive(bossActive); }
    public boolean isBossActive() { return context.isBossActive(); }

    //alien count
    public void setAlienCount(int count) { context.setAlienCount(count); }
    public int getAlienCount() { return context.getAlienCount(); }

    // 점수 관련
    public void addScore(int delta){ context.addScore(delta); }
    public int getScore(){ return context.getScore(); }

    //Alien  감소 로직
    public void decrementAlienCount(){ context.decrementAlienCount(); }
    //Danger Mode
    public void setDangerMode(boolean dangerMode){ context.setDangerMode(dangerMode); }
    public boolean isDangerMode() { return context.isDangerMode(); }

    //모드 확인
    public int getCurrentStageId() { return context.getCurrentStageId(); }
    public boolean isInfiniteMode() { return context.isInfiniteMode(); }
    public boolean isStageMode() { return context.isStageMode(); }


    public void updateEntities(long delta) { entityManager.update(delta, waitingForKeyPress);}
    public PlayerController getPlayerController() { return playerController; }
    public EntityManager getEntityManager() { return entityManager; }

    /** 남은 스테이지 시간(ms). 스테이지 모드가 아니면 0 */
    public int getStageTimeLimitMs() {
        if (!context.isStageMode()) return 0;
        if (context.getRunStartedAtMs() == 0) return STAGE_TIME_LIMIT_MS;

        long elapsed = System.currentTimeMillis() - context.getRunStartedAtMs();
        int timeLeft = (int) (STAGE_TIME_LIMIT_MS - elapsed);
        return Math.max(0, timeLeft);
    }


    /** 보스 처치 */
    public void onBossKilled() {
        context.setBossActive(false);
        entityManager.removeEntitiesByClass(EnemyShotEntity.class); // 총알 제거
        entityManager.removeEntitiesByClass(AlienEntity.class);

        if (context.isInfiniteMode()) {
            setAlienCount(0);
            spawnManager.spawnAliensForInfiniteMode();
        } else {
            notifyWin();
        }
    }


    public void onAlienKilled(AlienEntity alien) { combatManager.onAlienKilled(alien); }

    private void evaluateStageResult(int stageId, int timeLeft, int damageTaken, int score) {
        int stars = stageProgressManager.evaluateStars(stageId, timeLeft, damageTaken, score);
        stageProgressManager.updateStageStars(getSession(), stageId, stars);
    }

    public int getStageStars(int stageId) { return stageProgressManager.getStageStars(stageId); }

    public void loadStageStars(){ stageProgressManager.loadFromDb(getSession()); }

    // [추가] 당장 다음 스테이지(= 현재 stageId의 다음 인덱스)를 오픈
    private void unlockNextStageIfEligible(int currentStageId) { stageProgressManager.unlockNextStageIfEligible(currentStageId); }



    /** 스테이지 모드 시작 */
    public void startStageMode(int stageNum) {

        int currentHP = (getPlayerShip() != null) ? getPlayerShip().getStats().getCurrentHP() : 3;
        context.initStageMode(stageNum, currentHP);

        initEntities();
        playerController.getInputState().reset();

        StageManager.applyStage(stageNum, this);

        changeState(new PlayingState(this));
    }

    /** 무한 모드 시작 */
    public void startInfiniteMode() {

        context.initInfiniteMode();
        //엔티티 초기화 로직
        startGame();
        //state 위임
        changeState(new PlayingState(this));
    }

    public void showScoreboard() { changeState(new ScoreboardState(this)); }

    /** 다음 프레임에서 doLogic 실행 요청 */
    public void updateLogic() { logicRequiredThisLoop = true; }

    /*
     *  승패 처리
     */

    /** 플레이어 사망 처리 */
    public void notifyDeath() {
        changeState(new GameOverState(this)); //상태 전환
        waitingForKeyPress = false;
        message = "";
        
        handleGameEndDbOps();
    }

    private void handleGameEndDbOps() {
        if (!hasSession()) return;

        ShipEntity p = getPlayerShip();
        if (p == null) return;

        // 1. 레벨 저장
        gameDb.savePlayerLevel(getSession(), p.getStats().getLevel(), p.getStats().getXpIntoLevel());

        // 2. 점수 저장
        long startedAt = context.getRunStartedAtMs();
        long durationMs = (startedAt > 0) ? (System.currentTimeMillis() - startedAt) : 0L;
        String modeStr = context.isStageMode() ? "STAGE" : "INFINITE";

        gameDb.saveGameResult(
                getSession(),
                context.getScore(),
                context.getWaveCount(),
                p.getStats().getLevel(),
                modeStr,
                durationMs
        );
    }


    /** 플레이어 승리 처리(별 평가/저장 포함) */
    public void notifyWin() {
        message = "Player Win!";
        waitingForKeyPress = true;

        if (context.isStageMode()) {
            long startedAt = context.getRunStartedAtMs();
            long elapsed = (startedAt > 0) ? (System.currentTimeMillis() - startedAt) : 0L;
            int timeLeft = Math.max(0, STAGE_TIME_LIMIT_MS - (int) elapsed);

            int damageTaken = 0;
            ShipEntity p = getPlayerShip();
            if (p != null) {
                damageTaken = Math.max(0, context.getStageStartHP() - p.getStats().getCurrentHP());
            }
            evaluateStageResult(context.getCurrentStageId(), timeLeft, damageTaken, context.getScore());
            //조건 충족시 다음 스테이지 해제
            unlockNextStageIfEligible(context.getCurrentStageId());

        }

        if (hasSession()) {
            ShipEntity playerShip = getPlayerShip();
            AuthSession current = getSession();

            if (playerShip != null) {
                PlayerRepository.saveLastLevel(getDbClient(),current.getUid(), current.getIdToken(), getPlayerShip().getStats().getLevel(), getPlayerShip().getStats().getXpIntoLevel());
            }
        }
        uploadScoreIfLoggedIn();
    }



    /**
     * 엔티티 접근자
     */

    public ShipEntity getPlayerShip() { return (ShipEntity) ship; }

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

            //FPS 업데이트
            updateFps(delta);
            //화면 업데이트
            updateScreen(delta);

            Graphics2D g = beginFrame();
            render(g);

            //현재 상태 업데이트
            if(state != null) {
                state.update(delta);
            }

            endFrame(g, lastLoopTime); //dispose + show +sleep
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

    /*
     * Stat 전환 / 게임 플레이 한 프레임
     */

    //상태 변경 헬퍼
    public void changeState(GameState newState) {
        if(this.state != null) this.state.onExit();
        this.state = newState;
        if(this.state != null) this.state.onEnter();
    }
    //실제 게임 플레이(엔티티 + 플레이어 입력) 한 프레임 진행
    public void stepGamePlay(long delta){
        updateEntities(delta);
        playerController.update();
    }


    /*
     * Screen 업데이트 / 렌더
     */

    //Screen업데이트
    private void updateScreen(long delta){ screenNavigator.update(delta); }
    //화면 그리기 시작
    private Graphics2D beginFrame(){ return (Graphics2D) strategy.getDrawGraphics(); }
    //화면 그리기 종료
    private void endFrame(Graphics2D g, long lastLoopTime){
        g.dispose();
        strategy.show();
        SystemTimer.sleep(lastLoopTime + 10 - SystemTimer.getTime());
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

        screenNavigator.render(g);
    }

    /*
     * Stage / Screen 관련 헬퍼
     */

    /** Screen 교체 및 컨텍스트에 맞춘 BGM 갱신 */

    public boolean isStageUnlocked(int stageIndex){ return stageProgressManager.isStageUnlocked(stageIndex); }
    // 로드된 stageStars(1..N) 기반으로 잠금 상태 재계산
    public void rebuildStageUnlocks() { stageProgressManager.rebuildStageUnlocks(); }
    //현재 스크린의 상태 조회하기
    public Screen getCurrentScreen() { return screenNavigator.getCurrentScreen(); }
    //아무 키나 누르기
    public boolean isWaitingForKeyPress() { return waitingForKeyPress; }
    public void setWaitingForKeyPress(boolean value) { this.waitingForKeyPress = value; }
    //스테이지 선택 화면으로 이동
    public void goToStageSelectScreen(){ changeState(new StageSelectState(this)); }
    //메뉴 화면으로 돌아가기(ESC 눌렀을때)
    public void goToMenuScreen(){ changeState(new MenuState(this)); }
    // Screen 교체 및 컨텍스트에 맞춘 BGM 갱신
    public void setScreen(Screen screen) {
        this.waitingForKeyPress = false;
        screenNavigator.setScreen(screen);
        updateBGMForContext();
    }

    private void updateBGMForContext(){
        SoundManager.getSound().updateBGMForContext(
                getCurrentScreen(),         // screenNavigator에서 가져온 현재 Screen
                context.isStageMode(),      //스테이지 모드인지 여부
                context.getCurrentStageId() //현재 스테이지 번호
        );
    }

    /* ----------------
     * 점수 업로드 / 랭킹
     * ----------------
     */

    public void uploadScoreIfLoggedIn() {
        handleGameEndDbOps();
    }

    //자신의 상태를 확인하여 스스로 재시작 처리
    public void restartLastMode() {
        if (context.isInfiniteMode()) {
            startInfiniteMode();
        } else {
            //저장된 스테이지 정보 등을 활용해 재시작
            startStageMode(1);
        }
    }

    public void requestExit(){
        System.exit(0);
    }
}