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


/***
 import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.JDialog;
***/
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
 * The main hook of our game. This class with both act as a manager
 * for the display and central mediator for the game logic. 
 * 
 * Display management will consist of a loop that cycles round all
 * entities in the game asking them to move and then drawing them
 * in the appropriate place. With the help of an inner class it
 * will also allow the player to control the main ship.
 * 
 * As a mediator it will be informed when entities within our game
 * detect events (e.g. alient killed, played died) and will take
 * appropriate game actions.
 * 
 * @author Kevin Glass
 */
public class Game extends Canvas 
{  /// 아래 5개는 회원가입, 로그인과 관련된 필드
    private static final String API_KEY = "AIzaSyCdY9-wpF3Ad2DXkPTXGcqZEKWBD1qRYKE";
    private static final String DB_URL  = "https://sourcecodeanalysis-donggyu-default-rtdb.asia-southeast1.firebasedatabase.app/";
    private static final String DB_KEYFILE = "src/main/resources/serviceAccountKey.json";
    private static String SESSION_UID   = null;
    private static String SESSION_EMAIL = null;
    private static String SESSION_ID_TOKEN = null;
	/** The stragey that allows us to use accelerate page flipping */
	private BufferStrategy strategy;
	/** True if the game is currently "running", i.e. the game loop is looping */
	private boolean gameRunning = true;
    /** The list of all the entities that exist in our game */
    private ArrayList<Entity> entities = new ArrayList<>();
    /** The list of entities that need to be removed from the game this loop */
    private ArrayList<Entity> removeList = new ArrayList<>();
    /** The entity representing the player */
	private Entity ship;
	/** The speed at which the player's ship should move (pixels/sec) */
	private double moveSpeed = 300;
	/** The time at which last fired a shot */
	private long lastFire = 0;
	/** The interval between our players shot (ms) */
	private long firingInterval = 500;
	/** The number of aliens left on the screen */
	private int alienCount;
	/** 위험한 상황이 발생했을 시**/
    private boolean dangerMode = false;
	/** The message to display which waiting for a key press */
	private String message = "";
	/** True if we're holding up game play until a key has been pressed */
	private boolean waitingForKeyPress = true;
	/** True if the left cursor key is currently pressed */
	private boolean leftPressed = false;
	/** True if the right cursor key is currently pressed */
	private boolean rightPressed = false;
    /** True if the upper cursor key is currently pressed */
    private boolean UpPressed = false;
    /** True if the lower cursor key is currently pressed */
    private boolean DownPressed = false;
	/** True if we are firing */
	private boolean firePressed = false;
	/** True if game logic needs to be applied this loop, normally as a result of a game event */
	private boolean logicRequiredThisLoop = false;
	/** The last time at which we recorded the frame rate */
	private long lastFpsTime;
	/** The current number of frames recorded */
	private int fps;
	/** The normal title of the game window */
	private String windowTitle = "Space Invaders 102";
	/** The game window that we'll update with the frame count */
	private JFrame container;
    /** 백그라운드렌더러 선언**/
    private BackgroundRenderer backgroundRenderer;
    /**스크린**/
    private Screen currentScreen;


    // 게임 모드/점수/세션 측정
    private enum Mode { STAGE, INFINITE }
    private Mode currentMode = Mode.STAGE;
    private int score = 0;
    private long runStartedAtMs = 0L;
    //현재 스테이지 번호
    private int currentStageId = 1;
    // 스테이지 시작 시 플레이어 HP 저장
    private int stageStartHP = 0;
    // 스테이지 시간 제한 (일단 전체적으로 2분으로 잡아두었음)
    private static final int STAGE_TIME_LIMIT_MS = 120_000;



    // Game.java (필드)
    private enum GameState { MENU, PLAYING,GAMEOVER, SCOREBOARD, EXIT }
    private GameState state = GameState.MENU;
    private Map<Integer, Integer> stageStarScoreRequirements = new HashMap<>();
    //스테이지 별 개수 저장소
    private Map<Integer, Integer> stageStars = new HashMap<>();
    private String[] menuItems = {"스테이지 모드", "무한 모드", "스코어보드", "게임 종료"};
    private int menuIndex = 0;

    //무한 모드 관련 필드 추가
    private boolean infiniteMode = false;// 메뉴 선택 결과를 저장할 예시 플래그
	int waveCount =1;// 현재 웨이브 번호
    //무한 모드 보스 사이클
    private int normalsClearedInCycle = 0;

    private static final double RANGED_ALIEN_RATIO = 0.25; // 25% 확률로 원거리
    //보스 관련 필드 추가
    private boolean bossActive = false;
    /**
	 * Construct our game and set it running.
	 */

	public Game() {
		// create a frame to contain our game
		container = new JFrame("Space Invaders 102");
		// backgroundRenderer 생성자
        backgroundRenderer = new BackgroundRenderer();

        setScreen(new MenuScreen(this)); //시작화면 = 메뉴
		// get hold the content of the frame and set up the resolution of the game
		JPanel panel = (JPanel) container.getContentPane();
		panel.setPreferredSize(new Dimension(800,600));
		panel.setLayout(null);
		
		// setup our canvas size and put it into the content of the frame
		setBounds(0,0,800,600);
		panel.add(this);
		
		// Tell AWT not to bother repainting our canvas since we're
		// going to do that our self in accelerated mode
		setIgnoreRepaint(true);
		//별 점수 기준 초기화
        stageStarScoreRequirements();
		// finally make the window visible 
		container.pack();
		container.setResizable(false);
		container.setVisible(true);
		
		// add a listener to respond to the user closing the window. If they
		// do we'd like to exit the game
		container.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				System.exit(0);
			}
		});
		
		// add a key input system (defined below) to our canvas
		// so we can respond to key pressed
		addKeyListener(new KeyInputHandler());
		
		// request the focus so key events come to us
		requestFocus();

		// create the buffering strategy which will allow AWT
		// to manage our accelerated graphics
		createBufferStrategy(2);
		strategy = getBufferStrategy();
		
		// initialise the entities in our game so there's something
		// to see at startup
		initEntities();
	}
	
	/**
	 * Start a fresh game, this should clear out any old data and
	 * create a new set.
	 */
	private void startGame() {
		// clear out any existing entities and intialise a new set
		entities.clear();
        removeList.clear();
        alienCount = 0;

        initEntities();

		// blank out any keyboard settings we might currently have
        leftPressed = rightPressed = firePressed = false;
        //실재로 플레이 가능 상태로 전환
        waitingForKeyPress = false;
        state = GameState.PLAYING;  // 메뉴에서 게임 시작으로 전환

        //무한모드일시 웨이브 카운트 초기화
        waveCount = 1;
	}
	
	/**
	 * Initialise the starting state of the entities (ship and aliens). Each
	 * entitiy will be added to the overall list of entities in the game.
	 */
	private void initEntities() {
		// create the player ship and place it roughly in the center of the screen
		ship = new ShipEntity(this,"sprites/ship.gif",370,550);
		entities.add(ship);

        // ✅ 보스 테스트용: 무적 켜기
        ((ShipEntity) ship).setInvulnerable(true);
		
		// create a block of aliens (5 rows, by 12 aliens, spaced evenly)
		alienCount = 0;
        if(infiniteMode){
            //무한모드 : 웨이브 스폰으로 시작
            spawnAliens();
        }else{
            //스테이지 모드에서는 여기에 적을 깔지 않음
            //StageManager.applyStage()가 적 배치 담당

        }
	}
    public ShipEntity getPlayerShip() { return (ShipEntity) ship; }

    public void addEntity(Entity e) {
        entities.add(e);

    }

    public void spawnEnemyShot(double x, double y, double vx, double vy) {
        double speed = Math.sqrt(vx*vx + vy*vy);
        double dirX = (speed == 0) ? 0 : vx / speed;
        double dirY = (speed == 0) ? 1 : vy / speed;

        //  여기서 절대속도로 변환해서 넘긴다
        double dx = dirX * speed;
        double dy = dirY * speed;

        EnemyShotEntity s = new EnemyShotEntity(
                this,
                "sprites/enemy_bullet.png",  // 아래 2번과 일치시킴
                x, y,
                dx, dy,                      //  px/s
                speed
        );
        entities.add(s);
    }
    public int getWaveCount() { return waveCount; }   // BackgroundRenderer에서 필요
    public void notifyPlayerHit() {
        // TODO: HP 감소/이펙트/사운드 등
        // 일단 컴파일만 되게 스텁
    }

    //무한모드 메소드
    private void spawnAliens() {
        // 0) 이전 웨이브의 인질 제거 (누적 방지)
        if (infiniteMode) {
            for (java.util.Iterator<Entity> it = entities.iterator(); it.hasNext();) {
                Entity e = it.next();
                if (e instanceof HostageEntity) {
                    it.remove(); // 즉시 제거 (removeList 대기 없이)
                }
            }
        }
        //난이도 조절
        Difficulty diff = computeDifficultyForWave(waveCount);
        // 난이도 조절용: waveCount 증가
        int rows = 3 + (waveCount % 3);   // 3~5
        int cols = 6 + (waveCount % 6);   // 6~11
        alienCount = 0;

        int startX = 100;
        int startY = 50;
        int gapX = 50;
        int gapY = 30;

        for (int row = 0; row < rows; row++) {
            for (int c = 0; c < cols; c++) {
                int x = startX + (c * gapX);
                int y = startY + (row * gapY);

                // 확률 설정: 초반 웨이브는 낮고, 점점 증가(최대 25%)
                double diagonalProb = Math.min(0.05 + (waveCount - 1) * 0.02, 0.25); // 5% → 25%
                double rangedProb   = RANGED_ALIEN_RATIO; // 기존 원거리 확률(25%)

                double r = Math.random();
                Entity alien;
                if (r < diagonalProb) {
                    alien = new DiagonalShooterAlienEntity(this, x, y);
                } else if (r < diagonalProb + rangedProb) {
                    alien = new RangedAlienEntity(this, x, y, getPlayerShip());
                } else {
                    alien = new AlienEntity(this, x, y);
                }
                //생성직후 난이도 조정
                applyDifficultyToAlien(alien, diff);

                entities.add(alien);
                alienCount++;
            }
        }
        //  무한모드일 때 일정 확률로 인질 추가
        if (infiniteMode) {
            int hostageNum = 1 + (int)(Math.random() * 2);
            if (hostageNum > 0) {
                // 서로 다른 열에 배치(중복 방지)
                java.util.Set<Integer> usedCols = new java.util.HashSet<>();
                for (int i = 0; i < hostageNum; i++) {
                    int c;
                    int guard = 0; // 무한루프 방지
                    do {
                        c = (int)(Math.random() * cols);
                    } while (usedCols.contains(c) && ++guard < 10);
                    usedCols.add(c);

                    int x = startX + (c * gapX);
                    int y = startY - 40; // 맨 윗줄보다 살짝 위
                    Entity hostage = new HostageEntity(this, x, y);
                    entities.add(hostage);
                }
            }
        }
        waveCount++;
    }

    //보스 소환 메소드
    private void spawnBoss() {
        //  보스 웨이브 시작: 기존 인질 전부 제거
        for (java.util.Iterator<Entity> it = entities.iterator(); it.hasNext();) {
            Entity e = it.next();
            if (e instanceof HostageEntity) {
                it.remove();
            }
        }

        int bossW = 120;   // BossEntity.draw()에서 쓰는 축소 크기와 일치
        int bossH = 120;
        int startX = (800 - bossW) / 2;  // 화면 가로 800px 기준 중앙
        int startY = 50;                 // 상단에서 50px 아래

        Entity boss = new org.newdawn.spaceinvaders.entity.BossEntity(this, 360, 60, getPlayerShip());
        entities.add(boss);
        bossActive = true;
    }
    // 보스 처치시 콜백
    public void onBossKilled() {
        bossActive = false;
        if (infiniteMode) {
            // 무한모드라면 다음 웨이브 이어가기
            spawnAliens();
        } else {
            // 스테이지 모드라면 승리 처리
            notifyWin();
        }
    }
	/**
	 * Notification from a game entity that the logic of the game
	 * should be run at the next opportunity (normally as a result of some
	 * game event)
	 */
    //추가 난이도 구조
    private static class Difficulty {
        int alienHP;
        double alienSpeedMul;     // 좌우 이동 속도 배수(→ 드랍 빈도도 체감상 증가)
        double fireRateMul;       // 높을수록 더 자주 발사(쿨타임 나눔)
        double bulletSpeedMul;    // 적 탄속 배수
    }
    //웨이브당 난이도 구조
    private Difficulty computeDifficultyForWave(int wave) {
        Difficulty d = new Difficulty();
        // HP: 2웨이브마다 +1 (1,1,2,2,3,3, ...)
        d.alienHP = 1 + Math.max(0, (wave - 1) / 2);

        // 수평 이동 속도: 웨이브마다 +8% (최대 2.5배)
        d.alienSpeedMul   = Math.min(2.5, 1.0 + 0.08 * (wave - 1));

        // 연사 속도: 웨이브마다 +10% (최대 3배) → 쿨타임을 나눠 적용
        d.fireRateMul     = Math.min(3.0, 1.0 + 0.10 * (wave - 1));

        // 탄속: 웨이브마다 +5% (최대 2배)
        d.bulletSpeedMul  = Math.min(2.0, 1.0 + 0.05 * (wave - 1));
        return d;
    }

    //개별 적에게 난이도 적용하는 헬퍼
    private void applyDifficultyToAlien(Entity e, Difficulty d) {
        if (e instanceof org.newdawn.spaceinvaders.entity.AlienEntity) {
            org.newdawn.spaceinvaders.entity.AlienEntity a = (org.newdawn.spaceinvaders.entity.AlienEntity) e;
            a.setMaxHP(d.alienHP);
            a.applySpeedMultiplier(d.alienSpeedMul);
        }
        if (e instanceof org.newdawn.spaceinvaders.entity.RangedAlienEntity) {
            org.newdawn.spaceinvaders.entity.RangedAlienEntity r = (org.newdawn.spaceinvaders.entity.RangedAlienEntity) e;
            r.setFireRateMultiplier(d.fireRateMul);
            r.setBulletSpeedMultiplier(d.bulletSpeedMul);
        }
        if (e instanceof org.newdawn.spaceinvaders.entity.DiagonalShooterAlienEntity) {
            org.newdawn.spaceinvaders.entity.DiagonalShooterAlienEntity ds = (org.newdawn.spaceinvaders.entity.DiagonalShooterAlienEntity) e;
            ds.setFireRateMultiplier(d.fireRateMul);
            ds.setBulletSpeedMultiplier(d.bulletSpeedMul);
        }
    }

    private  void stageStarScoreRequirements(){

        //3별기준 점수테이블
        stageStarScoreRequirements.put(1, 1000);
        stageStarScoreRequirements.put(2, 2000);
        stageStarScoreRequirements.put(3, 3000);
        stageStarScoreRequirements.put(4, 4000);
        stageStarScoreRequirements.put(5, 5000);
    }

    public int getStageStarScoreRequirements(int stageId) {
        return stageStarScoreRequirements.getOrDefault(stageId, 1000);
    }

    //별 시스템
    //⭐ 1개 -> 클리어 성공
    //
    //⭐⭐ 2개 -> 클리어 + 시간 제한 내 클리어
    //
    //⭐⭐⭐ 3개 -> 클리어 + 시간 제한 + 무피격 + 스테이지별 점수 조건 충족
    private void evaluateStageResult(int stageId, int timeLeft, int damageTaken, int score) {
        int stars = 1; // 기본: 클리어 성공

        if (timeLeft > 0) {
            stars = 2;

            int requiredScore = getStageStarScoreRequirements(stageId);

            if (damageTaken == 0 && score >= requiredScore) {
                stars = 3;
            }
        }
        //별 저장
        setStageStars(stageId, stars);
    }

    //별 저장
    public void setStageStars(int stageId, int stars) {
        int prev = stageStars.getOrDefault(stageId, 0);

        // 기존보다 높은 기록만 저장 (예: 예전엔 2별, 새로 3별 달성 → 3별로 갱신)
        if (stars > prev) {
            stageStars.put(stageId, stars);

            //파이어베이스에 업로드
            saveStageStars();
        }
    }

    //별 조회
    public int getStageStars(int stageId) {
        return stageStars.getOrDefault(stageId, 0);
    }

    //별 저장(파이어베이스용)
    public void saveStageStars() {
        if(SESSION_UID == null || SESSION_ID_TOKEN == null) return;
        try{
            String json = new Gson().toJson(stageStars);
            restSetJson("user/" + SESSION_UID + "/stageStars", SESSION_ID_TOKEN, json);
            System.out.println("✅ 별 기록 업로드 완료: " + json);
        } catch (Exception e){
            System.err.println("❌ 별 기록 업로드 실패: " + e.getMessage());
        }
    }

    //별 불러오기(파이어베이스에서 게임으로 로그인 하면 불러오는 방식)
    public void loadStageStars() {
        if(SESSION_UID == null || SESSION_ID_TOKEN == null) return;
        try{
            String endpoint = DB_URL + "/users/" + SESSION_UID + "/stageStars";
            String res = httpGet(endpoint);

            java.lang.reflect.Type mapType = new com.google.gson.reflect.TypeToken<Map<Integer,Integer>>(){}.getType();
            Map<Integer,Integer> loaded = new Gson().fromJson(res, mapType);

            if(loaded != null) {
                stageStars.clear();
                stageStars.putAll(loaded);
            }
            System.out.println("✅ 별 기록 불러오기 완료: " + stageStars);
        } catch (Exception e){
            System.out.println("❌ 별 기록 불러오기 실패: " + e.getMessage());
        }
    }

    //스테이지모드 체크용
    public boolean isStageMode() {
        return currentMode == Mode.STAGE;
    }

    //스테이지모드
    public void startStageMode(int StageNum){
        currentMode = Mode.STAGE;
        score = 0;
        runStartedAtMs = System.currentTimeMillis();
        infiniteMode = false;   // 스테이지 모드
        waveCount = StageNum;
        currentStageId = StageNum;
        normalsClearedInCycle = 0; // 웨이브 초기화
        startGame();            // 기존 startGame() 호출
        //시작시 HP스냅샷
        stageStartHP = getPlayerShip().getCurrentHP();
        //별 점수 기준 초기화
        stageStarScoreRequirements();
        //스테이지 배치 적용
        StageManager.applyStage(StageNum, this);
        // 스테이지5에서 보스 두마리 생성 방지
        if (StageNum == 5) {
            bossActive = true;   // 보스전 플래그 ON
            // 혹시 안전하게
            if (alienCount < 0) alienCount = 0;
        }
        setScreen(new GamePlayScreen(this)); // 게임 화면 전환

    }
    //무한모드
    public void startInfiniteMode(){
        currentMode = Mode.INFINITE;
        score = 0;
        runStartedAtMs = System.currentTimeMillis();
        infiniteMode = true; // 무한모드
        waveCount = 1; // 웨이브 초기화
        normalsClearedInCycle = 0;
        startGame(); //기존 startGame() 호출하기
        setScreen(new GamePlayScreen(this)); // 게임 화면 전환
    }
    public void showScoreboard(){
        setScreen(new ScoreboardScreen(this)); //점수판 화면으로 전환
    }
  



	public void updateLogic() {
		logicRequiredThisLoop = true;
	}
	
	/**
	 * Remove an entity from the game. The entity removed will
	 * no longer move or be drawn.
	 * 
	 * @param entity The entity that should be removed
	 */
	public void removeEntity(Entity entity) {
		removeList.add(entity);
	}
	
	/**
	 * Notification that the player has died. 
	 */
	public void notifyDeath() {
		state = GameState.GAMEOVER;
        waitingForKeyPress = false;
        message = "";
		menuIndex =0;
        if (SESSION_UID != null && SESSION_ID_TOKEN != null) {
            LevelManager.saveLastLevel(SESSION_UID, SESSION_ID_TOKEN, getPlayerShip().getLevel());
        }

        setScreen(new GameOverScreen(this));
        uploadScoreIfLoggedIn(); ///사용자 사망시 파이어베이스에 업로드
	}

    public boolean isInfiniteMode() {
        return infiniteMode;
    }

	/**
	 * Notification that the player has won since all the aliens
	 * are dead.
	 */
	public void notifyWin() {
		message = "Well done! You Win!";
		waitingForKeyPress = true;

        if (currentMode == Mode.STAGE) {
            // 경과시간 기반으로 남은시간 계산
            long elapsed = (runStartedAtMs > 0) ? (System.currentTimeMillis() - runStartedAtMs) : 0L;
            int timeLeft = Math.max(0, STAGE_TIME_LIMIT_MS - (int)elapsed);

            //받은 피해량 계산(시작 HP - 현재 HP)
            int damageTaken = 0;
            ShipEntity p = getPlayerShip();
            if (p != null) {
                damageTaken = Math.max(0, stageStartHP - p.getCurrentHP());
            }
            //별 평가 호출
            evaluateStageResult(currentStageId, timeLeft, damageTaken, score);
        }

        if (SESSION_UID != null && SESSION_ID_TOKEN != null) {
            LevelManager.saveLastLevel(SESSION_UID, SESSION_ID_TOKEN, getPlayerShip().getLevel());
        }
        uploadScoreIfLoggedIn(); /// 사용자 승리 시 또한 파이어베이스에 업로드
	}

    public int getStageTimeLimitMs(){
        //무한 모드에서는 숨김
        if (currentMode != Mode.STAGE) return 0;
        if(runStartedAtMs == 0)return STAGE_TIME_LIMIT_MS;

        long elapsed = System.currentTimeMillis() - runStartedAtMs;
        int timeLeft = (int)(STAGE_TIME_LIMIT_MS - elapsed);
        //음수 방지, 0으로 고정
        return Math.max(0, timeLeft);
    }


    // 엔티티 리스트 접근
    public java.util.List<Entity> getEntities() {
        return entities;
    }

    // waitingForKeyPress 상태 확인
    public boolean isWaitingForKeyPress() {
        return waitingForKeyPress;
    }

    // 키 입력 상태 설정
    public void setLeftPressed(boolean value) { leftPressed = value; }
    public void setRightPressed(boolean value) { rightPressed = value; }
    public void setFirePressed(boolean value) { firePressed = value; }
    public void setUpPressed(boolean value) { UpPressed = value;}
    public void setDownPressed(boolean value) { DownPressed = value;}
    // 엔티티 업데이트 로직 호출
    public void updateEntities(long delta) {
        if (!waitingForKeyPress) {
            for (Entity entity : new java.util.ArrayList<>(entities)) {
                entity.move(delta);
            }
        }
        // 충돌
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

        // 제거/정리
        entities.removeAll(removeList);
        removeList.clear();

        // ★ 핵심: logicRequiredThisLoop 처리
        if (logicRequiredThisLoop) {
            for (int i = 0; i < entities.size(); i++) {
                entities.get(i).doLogic();
            }
            logicRequiredThisLoop = false;
        }


    }

    // alienCount setter 추가
    public void setAlienCount(int count) {
        this.alienCount = count;
    }

	/**
	 * Notification that an alien has been killed
	 */
	public void notifyAlienKilled() {
        score += 100;
        // 1) 안전하게 카운트
        alienCount--;
        if (alienCount < 0) alienCount = 0;
        System.out.println("[DEBUG] Alien killed! alienCount=" + alienCount + " stage=" + currentStageId + " bossActive=" + bossActive);

        // 2) 위험 모드 갱신
        dangerMode = (getPlayerShip().getCurrentHP() < 2);

        // 3) 전멸했을 때만 분기
        if (alienCount == 0) {
            if (infiniteMode) {
                if (!bossActive) {
                    normalsClearedInCycle++;
                    if (normalsClearedInCycle >= 3) {
                        // 3회 클리어 → 보스 소환
                        normalsClearedInCycle = 0;
                        spawnBoss();
                    } else {
                        // 다음 일반 웨이브 진행
                        spawnAliens();
                    }
                }
                return;
            }else {
                // ⬇️ 스테이지 모드: 보스 웨이브/승리 처리
                if (!bossActive) {
                    if (currentStageId == 5) {
                        spawnBoss();
                    } else {
                        notifyWin();
                    }
                }
            }
        }
		// if there are still some aliens left then they all need to get faster, so
		// speed up all the existing aliens
		for (int i=0;i<entities.size();i++) {
			Entity entity = (Entity) entities.get(i);
			
			if (entity instanceof AlienEntity) {
				// speed up by 2%
				entity.setHorizontalMovement(entity.getHorizontalMovement() * 1.02);
			}
		}
	}
	
	/**
	 * Attempt to fire a shot from the player. Its called "try"
	 * since we must first check that the player can fire at this 
	 * point, i.e. has he/she waited long enough between shots
	 */
	public void tryToFire() {
		// check that we have waiting long enough to fire
		if (System.currentTimeMillis() - lastFire < firingInterval) {
			return;
		}
		
		// if we waited long enough, create the shot entity, and record the time.
		lastFire = System.currentTimeMillis();
		ShotEntity shot = new ShotEntity(this,"sprites/shot.gif",ship.getX()+10,ship.getY()-30);
		entities.add(shot);
	}
    //플레이어 피격시
    public void onPlayerHit() {
        System.out.println("Player hit!");
    }
	
	/**
	 * The main game loop. This loop is running during all game
	 * play as is responsible for the following activities:
	 * <p>
	 * - Working out the speed of the game loop to update moves
	 * - Moving the game entities
	 * - Drawing the screen contents (entities, text)
	 * - Updating game events
	 * - Checking Input
	 * <p>
	 */
	public void gameLoop() {
		long lastLoopTime = SystemTimer.getTime();
		
		// keep looping round til the game ends
		while (gameRunning) {
			// work out how long its been since the last update, this
			// will be used to calculate how far the entities should
			// move this loop
			long delta = SystemTimer.getTime() - lastLoopTime;
			lastLoopTime = SystemTimer.getTime();

			// update the frame counter
			lastFpsTime += delta;
			fps++;

			// update our FPS counter if a second has passed since
			// we last recorded
			if (lastFpsTime >= 1000) {
				container.setTitle(windowTitle+" (FPS: "+fps+")");
				lastFpsTime = 0;
				fps = 0;
			}
            //화면 업데이트를 먼저 돌림(오버레이 처리 /일시 정지)
            if (currentScreen != null) {
                currentScreen.update(delta);
            }

			// Get hold of a graphics context for the accelerated
			// surface and blank it out
			Graphics2D g = (Graphics2D) strategy.getDrawGraphics();

            // 배경 + 엔티티를 통합 렌더링
            render(g);

            if (state == GameState.MENU) {
                g.dispose();
                strategy.show();
                SystemTimer.sleep(lastLoopTime+10-SystemTimer.getTime());
                continue; // 메뉴일 땐 이하(엔티티 이동/충돌) 스킵
            }

            //스크린이 주도한다면 레거시 엔티티/충돌/로직 블록을 건너뜀
            boolean screenDrivesGame = (currentScreen != null);
            if (!screenDrivesGame) {
                // cycle round asking each entity to move itself
                if (!waitingForKeyPress) {
                    for (int i=0;i<entities.size();i++) {
                        Entity entity = (Entity) entities.get(i);

                        entity.move(delta);
                    }
                }

                // brute force collisions, compare every entity against
                // every other entity. If any of them collide notify
                // both entities that the collision has occured
                for (int p=0;p<entities.size();p++) {
                    for (int s=p+1;s<entities.size();s++) {
                        Entity me = (Entity) entities.get(p);
                        Entity him = (Entity) entities.get(s);

                        if (me.collidesWith(him)) {
                            me.collidedWith(him);
                            him.collidedWith(me);
                        }
                    }
                }

                // remove any entity that has been marked for clear up
                entities.removeAll(removeList);
                removeList.clear();

                // if a game event has indicated that game logic should
                // be resolved, cycle round every entity requesting that
                // their personal logic should be considered.
                if (logicRequiredThisLoop) {
                    for (int i=0;i<entities.size();i++) {
                        Entity entity = (Entity) entities.get(i);
                        entity.doLogic();
                    }

                    logicRequiredThisLoop = false;
                }
            }
			
			// if we're waiting for an "any key" press then draw the 
			// current message 
			if (state == GameState.PLAYING && waitingForKeyPress) {
				g.setColor(Color.white);
				g.drawString(message,(800-g.getFontMetrics().stringWidth(message))/2,250);
				g.drawString("Press any key",(800-g.getFontMetrics().stringWidth("Press any key"))/2,300);
			}
			
			// finally, we've completed drawing so clear up the graphics
			// and flip the buffer over
			g.dispose();
			strategy.show();
			
			// resolve the movement of the ship. First assume the ship 
			// isn't moving. If either cursor key is pressed then
			// update the movement appropraitely
			ship.setHorizontalMovement(0);
            ship.setVerticalMovement(0);


            //상하 이동
			if((UpPressed) && (!DownPressed)){
                ship.setVerticalMovement(-moveSpeed);
            } else if((DownPressed) && (!UpPressed)){
                ship.setVerticalMovement(moveSpeed);
            }

            //좌우 이동
			if ((leftPressed) && (!rightPressed)) {
				ship.setHorizontalMovement(-moveSpeed);
			} else if ((rightPressed) && (!leftPressed)) {
				ship.setHorizontalMovement(moveSpeed);
			}
			
			// if we're pressing fire, attempt to fire
			if (firePressed) {
				tryToFire();
			}
			
			// we want each frame to take 10 milliseconds, to do this
			// we've recorded when we started the frame. We add 10 milliseconds
			// to this and then factor in the current time to give 
			// us our final value to wait for
			SystemTimer.sleep(lastLoopTime+10-SystemTimer.getTime());
		}
	}
    public void setScreen(Screen screen) {
        this.currentScreen = screen;
    }

    public void render(Graphics2D g) {
        // 1) 항상 배경 먼저
        if (backgroundRenderer != null) {
            int w = getWidth()  > 0 ? getWidth()  : 800;
            int h = getHeight() > 0 ? getHeight() : 600;

            // 메뉴에서도 깔 배경이 필요하니 waveCount가 1 미만이면 1로 고정
            int wcForBg = Math.max(1, getWaveCount());
            backgroundRenderer.render(g, wcForBg, w, h);
        } else {
            g.setColor(Color.black);
            g.fillRect(0, 0, 800, 600);
        }

        // 2) 그 위에 상태별 UI 오버레이
        if (currentScreen != null) {
            currentScreen.render(g);
        }


    }
    public boolean isDangerMode(){
        return dangerMode;
    }

    public int getAlienCount() {
        return alienCount;
    }

    /**
	 * A class to handle keyboard input from the user. The class
	 * handles both dynamic input during game play, i.e. left/right 
	 * and shoot, and more static type input (i.e. press any key to
	 * continue)
	 * 
	 * This has been implemented as an inner class more through 
	 * habbit then anything else. Its perfectly normal to implement
	 * this as seperate class if slight less convienient.
	 * 
	 * @author Kevin Glass
	 */
	private class KeyInputHandler extends KeyAdapter {
		/** The number of key presses we've had while waiting for an "any key" press */
		private int pressCount = 1;
		
		/**
		 * Notification from AWT that a key has been pressed. Note that
		 * a key being pressed is equal to being pushed down but *NOT*
		 * released. Thats where keyTyped() comes in.
		 *
		 * @param e The details of the key that was pressed 
		 */
        @Override
		public void keyPressed(KeyEvent e) {
            // 상태 상관없이 Screen에게 위임
            if (currentScreen != null) {
                currentScreen.handleKeyPress(e.getKeyCode());
            }
		} 
		
		/**
		 * Notification from AWT that a key has been released.
		 *
		 * @param e The details of the key that was released 
		 */
        @Override
		public void keyReleased(KeyEvent e) {
            if (currentScreen != null) {
                currentScreen.handleKeyRelease(e.getKeyCode());
            }
		}

		/**
		 * Notification from AWT that a key has been typed. Note that
		 * typing a key means to both press and then release it.
		 *
		 * @param e The details of the key that was typed. 
		 */
		public void keyTyped(KeyEvent e) {
            if (e.getKeyChar() == 27) { // ESC
                // ESC만 상태 전환(타이틀 복귀 등) 처리하고,
                // 선택/방향은 Screen이 처리
                state = GameState.MENU;
                setScreen(new MenuScreen(Game.this));
                return;
            }
		}
	}
/// 여기서부터는 모두 백엔드 코드

// 점수 업로드(로그인 필요: SESSION_UID/SESSION_ID_TOKEN 사용)
    protected static class ScoreEntry {
       String mode;
       Integer score;
       Integer wave;
       Long durationMs;
       String timestamp;
       Integer level;
   }

     protected static List<ScoreEntry> fetchMyTopScores(int limit) {
         List<Game.ScoreEntry> list = new ArrayList<>();
         if (SESSION_UID == null || SESSION_ID_TOKEN == null) return list;

         try {
             String endpoint = DB_URL + "/users/" + SESSION_UID
                     + "/scores.json?auth=" + urlEnc(SESSION_ID_TOKEN)
                     + "&orderBy=%22score%22&limitToLast=" + limit;
             String res = httpGet(endpoint);

             java.lang.reflect.Type mapType =
                     new TypeToken<java.util.Map<String, Game.ScoreEntry>>(){}.getType();
             java.util.Map<String, Game.ScoreEntry> map = new Gson().fromJson(res, mapType);
             if (map != null) list.addAll(map.values());

             // Firebase는 오름차순 → 내림차순 정렬
             list.sort((a,b) -> Integer.compare(
                     b.score == null ? 0 : b.score,
                     a.score == null ? 0 : a.score
             ));
         } catch (Exception e) {
             System.err.println("❌ 점수 조회 실패: " + e.getMessage());
         }
         return list;
    }
   private void uploadScoreIfLoggedIn() {
      if (SESSION_UID == null || SESSION_ID_TOKEN == null) return;

      long durationMs = (runStartedAtMs > 0) ? (System.currentTimeMillis() - runStartedAtMs) : 0L;
      String modeStr = (currentMode == Mode.STAGE) ? "STAGE" : "INFINITE";

      String json = "{"
              + "\"level\":" + ((ShipEntity) ship).getLevel() + ","
            + "\"mode\":" + quote(modeStr) + ","
            + "\"score\":" + score + ","
            + "\"wave\":" + waveCount + ","              // 마지막 웨이브(참고용)
            + "\"durationMs\":" + durationMs + ","
            + "\"timestamp\":" + quote(now())
            + "}";

      try {
        restPushJson("users/" + SESSION_UID + "/scores", SESSION_ID_TOKEN, json);
        System.out.println("✅ 점수 업로드 완료: " + json);
        } catch (Exception e) {
        System.err.println("❌ 점수 업로드 실패: " + e.getMessage());
     }
   }

    private static void writeLog(String eventType) {
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("logs");

        String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());

        Map<String, Object> logEntry = new HashMap<>();
        logEntry.put("event", eventType);
        logEntry.put("timestamp", timestamp);

        ref.push().setValueAsync(logEntry);

        System.out.println("✅ 로그 저장: " + eventType + " at " + timestamp);
    }
    /// 로그인 화면
    private static void showAuthDialogAndLogin() {
        final JDialog dlg = new JDialog((JFrame) null, "로그인 / 회원가입", true);
        JTabbedPane tabs = new JTabbedPane();

        // 로그인 탭
        JPanel login = new JPanel(new java.awt.GridBagLayout());
        JTextField loginEmail = new JTextField(20);
        JPasswordField loginPw = new JPasswordField(20);
        JButton btnLogin = new JButton("로그인");
        java.awt.GridBagConstraints c = gbc();
        login.add(new JLabel("이메일"), c);
        c.gridx = 1;
        login.add(loginEmail, c);

        c = gbc(0, 1);
        login.add(new JLabel("비밀번호"), c);
        c.gridx = 1;
        login.add(loginPw, c);

        c = gbc(0, 2);
        c.gridwidth = 2;
        btnLogin.addActionListener(ev -> {
            try {
                AuthResult ar = restSignIn(loginEmail.getText().trim(), new String(loginPw.getPassword()));
                SESSION_UID = ar.localId;
                SESSION_EMAIL = ar.email;
                SESSION_ID_TOKEN = ar.idToken;

                JOptionPane.showMessageDialog(dlg, "로그인 성공: " + ar.email);
                dlg.dispose();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(dlg, "로그인 실패\n" + ex.getMessage(),
                        "오류", JOptionPane.ERROR_MESSAGE);
            }
        });
        login.add(btnLogin, c);

        // 회원가입 탭
        JPanel signup = new JPanel(new java.awt.GridBagLayout());
        JTextField signEmail = new JTextField(20);
        JPasswordField signPw = new JPasswordField(20);
        JPasswordField signPw2 = new JPasswordField(20);
        JButton btnSign = new JButton("회원가입");
        c = gbc();
        signup.add(new JLabel("이메일"), c);
        c.gridx = 1;
        signup.add(signEmail, c);

        c = gbc(0, 1);
        signup.add(new JLabel("비밀번호"), c);
        c.gridx = 1;
        signup.add(signPw, c);

        c = gbc(0, 2);
        signup.add(new JLabel("비밀번호 확인"), c);
        c.gridx = 1;
        signup.add(signPw2, c);

        c = gbc(0, 3);
        c.gridwidth = 2;
        btnSign.addActionListener(ev -> {
            String pw1 = new String(signPw.getPassword());
            String pw2 = new String(signPw2.getPassword());
            if (!pw1.equals(pw2)) {
                JOptionPane.showMessageDialog(dlg, "비밀번호가 일치하지 않습니다.",
                        "오류", JOptionPane.ERROR_MESSAGE);
                return;
            }
            try {
                AuthResult ar = restSignUp(signEmail.getText().trim(), pw1);
                SESSION_UID = ar.localId;
                SESSION_EMAIL = ar.email;
                SESSION_ID_TOKEN = ar.idToken;

                JOptionPane.showMessageDialog(dlg, "회원가입 성공: " + ar.email);

                // ✅ 선택적으로 기본 프로필 저장
                restSetJson("users/" + SESSION_UID + "/profile", SESSION_ID_TOKEN,
                        "{\"email\":" + quote(SESSION_EMAIL) + ",\"createdAt\":" + quote(now()) + "}");

                dlg.dispose();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(dlg, "회원가입 실패\n" + ex.getMessage(),
                        "오류", JOptionPane.ERROR_MESSAGE);
            }
        });
        signup.add(btnSign, c);

        // === 탭 추가 ===
        tabs.add("로그인", login);
        tabs.add("회원가입", signup);

        dlg.setContentPane(tabs);
        dlg.pack();
        dlg.setLocationRelativeTo(null);
        dlg.setVisible(true);

        // 로그인/회원가입 성공 못하면 프로그램 종료 (필요에 따라 제거 가능)
        if (SESSION_ID_TOKEN == null) System.exit(0);
    }

    private static java.awt.GridBagConstraints gbc() { return gbc(0,0); }
    private static java.awt.GridBagConstraints gbc(int x, int y) {
        java.awt.GridBagConstraints c = new java.awt.GridBagConstraints();
        c.gridx = x; c.gridy = y; c.insets = new java.awt.Insets(5,5,5,5);
        c.anchor = java.awt.GridBagConstraints.WEST; c.fill = java.awt.GridBagConstraints.HORIZONTAL;
        return c;
    }

    // =========================
    // 🌐 Firebase Auth (REST)
    // =========================
    private static class AuthResult {
        final String idToken, refreshToken, localId, email;
        AuthResult(String idToken, String refreshToken, String localId, String email) {
            this.idToken=idToken; this.refreshToken=refreshToken; this.localId=localId; this.email=email;
        }
    }

    private static AuthResult restSignUp(String email, String password) throws Exception {
        String endpoint = "https://identitytoolkit.googleapis.com/v1/accounts:signUp?key=" + API_KEY;
        String body = "{"
                + "\"email\":"+quote(email)+","
                + "\"password\":"+quote(password)+","
                + "\"returnSecureToken\":true"
                + "}";
        String res = httpPostJson(endpoint, body);
        String idToken = jget(res, "idToken");
        String refreshToken = jget(res, "refreshToken");
        String localId = jget(res, "localId");
        String emailOut = jget(res, "email");
        if (idToken==null || localId==null) throw new RuntimeException("SignUp parse failed: " + res);
        return new AuthResult(idToken, refreshToken, localId, emailOut);
    }

    private static AuthResult restSignIn(String email, String password) throws Exception {
        String endpoint = "https://identitytoolkit.googleapis.com/v1/accounts:signInWithPassword?key=" + API_KEY;
        String body = "{"
                + "\"email\":"+quote(email)+","
                + "\"password\":"+quote(password)+","
                + "\"returnSecureToken\":true"
                + "}";
        String res = httpPostJson(endpoint, body);
        String idToken = jget(res, "idToken");
        String refreshToken = jget(res, "refreshToken");
        String localId = jget(res, "localId");
        String emailOut = jget(res, "email");
        if (idToken==null || localId==null) throw new RuntimeException("SignIn parse failed: " + res);
        return new AuthResult(idToken, refreshToken, localId, emailOut);
    }

    // =========================
    // 🗄️ Realtime Database (REST)
    // =========================
    private static void restLogEvent(String type) {
        if (SESSION_ID_TOKEN == null || SESSION_UID == null) return;
        String ts = now();
        String json = "{"
                + "\"event\":"+quote(type)+","
                + "\"timestamp\":"+quote(ts)+"}";
        try {
            restPushJson("users/"+SESSION_UID+"/logs", SESSION_ID_TOKEN, json);
        } catch (Exception e) {
            System.err.println("⚠️ 로그 저장 실패: " + e.getMessage());
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
    // 🔧 HTTP & 미니 JSON 유틸 (의존성 없음)
    // =========================
    protected static String httpGet(String endpoint) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(endpoint).openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Accept","application/json");
        return readResp(conn);
    }

    protected static String httpPostJson(String endpoint, String body) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(endpoint).openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type","application/json; charset=UTF-8");
        try (OutputStream os = conn.getOutputStream()) {
            os.write(body.getBytes(StandardCharsets.UTF_8));
        }
        return readResp(conn);
    }

    protected static String httpPutJson(String endpoint, String body) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(endpoint).openConnection();
        conn.setRequestMethod("PUT");
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type","application/json; charset=UTF-8");
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

    protected static String jget(String json, String key) {
        // 매우 단순한 "키:문자열" 추출. (필요한 필드만)
        // "key" : "value"
        String k = "\"" + key.replace("\"","\\\"") + "\"";
        int i = json.indexOf(k);
        if (i < 0) return null;
        i = json.indexOf(':', i);
        if (i < 0) return null;
        i++;
        // skip spaces
        while (i < json.length() && Character.isWhitespace(json.charAt(i))) i++;
        if (i >= json.length() || json.charAt(i) != '"') return null;
        i++; // skip opening "
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


    /**
	 * The entry point into the game. We'll simply create an
	 * instance of class which will start the display and game
	 * loop.
	 * 
	 * @param argv The arguments that are passed into our game
	 */
	public static void main(String argv[]) {
        try {
            // serviceAccountKey.json 불러오기
              FileInputStream serviceAccount = new FileInputStream(DB_KEYFILE);

            // Firebase 옵션 설정
              FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .setDatabaseUrl(DB_URL)
                    .build();

            // Firebase 초기화 (앱 실행 시 딱 1번만!)
            FirebaseApp.initializeApp(options);

            System.out.println("Firebase 초기화");
            writeLog("gamestart");

            // 1) 로그인/회원가입 먼저
            SwingUtilities.invokeLater(() -> showAuthDialogAndLogin());
            // 로그인 다이얼로그가 modal이므로 여기서 잠시 대기
            try {
                // modal dialog가 닫히는 동안 메인 스레드가 바로 진행되지 않게 약간 대기
                while (SESSION_ID_TOKEN == null) Thread.sleep(100);
            } catch (InterruptedException ignored) {}

            Game g = new Game();
            /// 사용자 레벨 불러오가
            int savedLevel = LevelManager.loadLastLevel(DB_URL, SESSION_UID, SESSION_ID_TOKEN);
            g.getPlayerShip().setLevel(savedLevel);
            // Start the main game loop, note: this method will not
            // return until the game has finished running. Hence we are
            // using the actual main thread to run the game.
            g.gameLoop();
            writeLog("game over");



        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

