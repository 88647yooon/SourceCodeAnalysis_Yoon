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
import java.io.FileInputStream;
import java.util.ArrayList;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;


import javax.swing.JFrame;
import javax.swing.JPanel;

import org.newdawn.spaceinvaders.entity.AlienEntity;
import org.newdawn.spaceinvaders.entity.Entity;
import org.newdawn.spaceinvaders.entity.ShipEntity;
import org.newdawn.spaceinvaders.entity.ShotEntity;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

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
{
	/** The stragey that allows us to use accelerate page flipping */
	private BufferStrategy strategy;
	/** True if the game is currently "running", i.e. the game loop is looping */
	private boolean gameRunning = true;
	/** The list of all the entities that exist in our game */
	private ArrayList entities = new ArrayList();
	/** The list of entities that need to be removed from the game this loop */
	private ArrayList removeList = new ArrayList();
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
	
	/** The message to display which waiting for a key press */
	private String message = "";
	/** True if we're holding up game play until a key has been pressed */
	private boolean waitingForKeyPress = true;
	/** True if the left cursor key is currently pressed */
	private boolean leftPressed = false;
	/** True if the right cursor key is currently pressed */
	private boolean rightPressed = false;
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


    // Game.java (필드)
    private enum GameState { MENU, PLAYING, SCOREBOARD, EXIT }
    private GameState state = GameState.MENU;

    private String[] menuItems = {"스테이지 모드", "무한 모드", "스코어보드", "게임 종료"};
    private int menuIndex = 0;

    //무한 모드 관련 필드 추가
    private boolean infiniteMode = false;// 메뉴 선택 결과를 저장할 예시 플래그
	private int waveCount =1;// 현재 웨이브 번호

    //보스 관련 필드 추가
    private boolean bossActive = false;
    /**
	 * Construct our game and set it running.
	 */

	public Game() {
		// create a frame to contain our game
		container = new JFrame("Space Invaders 102");
		
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
		
		// create a block of aliens (5 rows, by 12 aliens, spaced evenly)
		alienCount = 0;
        if(infiniteMode){
            //무한모드 : 웨이브 스폰으로 시작
            spawnAliens();
        }else{
            //기존 스테이지 모드 : 5*12 고정 배치

            for (int row=0;row<5;row++) {
                for (int x=0;x<12;x++) {
                    Entity alien = new AlienEntity(this,100+(x*50),(50)+row*30);
                    entities.add(alien);
                    alienCount++;
                }
            }
        }
	}

    //무한모드 메소드
    private void spawnAliens(){
        // 난이도 조절용: waveCount 증가
        int rows = 3 + (waveCount % 3);  // 점점 늘어나도록
        int cols = 6 + (waveCount % 6);
        alienCount = 0;

        for (int row=0;row<rows;row++) {
            for (int x=0;x<cols;x++) {
                Entity alien = new AlienEntity(this,100+(x*50),(50)+row*30);
                entities.add(alien);
                alienCount++;
            }
        }
        waveCount++;
    }

    //보스 소환 메소드
    private void spawnBoss() {

        int bossW = 120;   // BossEntity.draw()에서 쓰는 축소 크기와 일치
        int bossH = 120;
        int startX = (800 - bossW) / 2;  // 화면 가로 800px 기준 중앙
        int startY = 50;                 // 상단에서 50px 아래

        Entity boss = new org.newdawn.spaceinvaders.entity.BossEntity(this, 360, 60);
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
		message = "Oh no! They got you, try again?";
		waitingForKeyPress = true;
	}
	
	/**
	 * Notification that the player has won since all the aliens
	 * are dead.
	 */
	public void notifyWin() {
		message = "Well done! You Win!";
		waitingForKeyPress = true;
	}
	
	/**
	 * Notification that an alien has been killed
	 */
	public void notifyAlienKilled() {
		// reduce the alient count, if there are none left, the player has won!
		alienCount--;
		
		if (alienCount == 0) {
            if (infiniteMode) {
                if (!bossActive && (waveCount % 1 == 0)){
                    spawnBoss(); //무한모드에서 매 웨이브마다 보스가 생성
                }else{
                    spawnAliens(); // 무한모드일 경우 새로운 웨이브
                }
            } else {
                //스테이지 모드 : 전멸후 보스 라운드였따면 보스 소호나
                if (!bossActive) {
                    spawnBoss(); // 마지막 스테이지라면 호출
                }
                notifyWin();   // 원래 로직
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
			
			// Get hold of a graphics context for the accelerated 
			// surface and blank it out
			Graphics2D g = (Graphics2D) strategy.getDrawGraphics();
			g.setColor(Color.black);
			g.fillRect(0,0,800,600);

            if (state == GameState.MENU) {
                drawMenu(g);
                g.dispose();
                strategy.show();
                SystemTimer.sleep(lastLoopTime+10-SystemTimer.getTime());
                continue; // 메뉴일 땐 이하(엔티티 이동/충돌) 스킵
            }
			
			// cycle round asking each entity to move itself
			if (!waitingForKeyPress) {
				for (int i=0;i<entities.size();i++) {
					Entity entity = (Entity) entities.get(i);
					
					entity.move(delta);
				}
			}
			
			// cycle round drawing all the entities we have in the game
			for (int i=0;i<entities.size();i++) {
				Entity entity = (Entity) entities.get(i);
				
				entity.draw(g);
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
			
			// if we're waiting for an "any key" press then draw the 
			// current message 
			if (waitingForKeyPress) {
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

    private void drawMenu(Graphics2D g) {
        g.setColor(Color.white);
        String title = "Space Invaders - Main Menu";
        g.drawString(title, (800 - g.getFontMetrics().stringWidth(title))/2, 200);

        for (int i=0;i<menuItems.length;i++) {
            String item = (i==menuIndex ? "> " : "  ") + menuItems[i];
            g.drawString(item, (800 - g.getFontMetrics().stringWidth(item))/2, 260 + i*30);
        }
        String help = "↑/↓: 이동, ENTER: 선택, ESC: 종료";
        g.drawString(help, (800 - g.getFontMetrics().stringWidth(help))/2, 420);
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
		public void keyPressed(KeyEvent e) {
            if (state == GameState.MENU) {
                if (e.getKeyCode()==KeyEvent.VK_UP)    menuIndex = (menuIndex-1+menuItems.length)%menuItems.length;
                if (e.getKeyCode()==KeyEvent.VK_DOWN)  menuIndex = (menuIndex+1)%menuItems.length;
                return;
            }
            // ===Playing === 기존처리
			if (waitingForKeyPress) {
				return;
			}
			
			
			if (e.getKeyCode() == KeyEvent.VK_LEFT) {
				leftPressed = true;
			}
			if (e.getKeyCode() == KeyEvent.VK_RIGHT) {
				rightPressed = true;
			}
			if (e.getKeyCode() == KeyEvent.VK_SPACE) {
				firePressed = true;
			}
		} 
		
		/**
		 * Notification from AWT that a key has been released.
		 *
		 * @param e The details of the key that was released 
		 */
		public void keyReleased(KeyEvent e) {
			// if we're waiting for an "any key" typed then we don't 
			// want to do anything with just a "released"

            if (state == GameState.MENU) return;

			if (waitingForKeyPress) {
				return;
			}
			
			if (e.getKeyCode() == KeyEvent.VK_LEFT) {
				leftPressed = false;
			}
			if (e.getKeyCode() == KeyEvent.VK_RIGHT) {
				rightPressed = false;
			}
			if (e.getKeyCode() == KeyEvent.VK_SPACE) {
				firePressed = false;
			}
		}

		/**
		 * Notification from AWT that a key has been typed. Note that
		 * typing a key means to both press and then release it.
		 *
		 * @param e The details of the key that was typed. 
		 */
		public void keyTyped(KeyEvent e) {
			// if we're waiting for a "any key" type then
			// check if we've recieved any recently. We may
			// have had a keyType() event from the user releasing
			// the shoot or move keys, hence the use of the "pressCount"
			// counter.
            if (e.getKeyChar() == 27) {  // ESC
                if (state == GameState.MENU) System.exit(0);
                else state = GameState.MENU; // 게임 중 ESC로 메뉴로
                return;
            }
            if (state == GameState.MENU && e.getKeyChar()=='\n') {
                // ENTER 선택
                switch (menuIndex) {
                    case 0: infiniteMode = false; startGame(); break; // 스테이지 모드
                    case 1: infiniteMode = true;  startGame(); break; // 무한 모드
                    case 2: state = GameState.SCOREBOARD; break;      // 점수판(화면만 우선)
                    case 3: System.exit(0);
                }
                return;
			}
			
			// if we hit escape, then quit the game
			if (e.getKeyChar() == 27) {
				System.exit(0);
			}
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
            FileInputStream serviceAccount = new FileInputStream("src/main/resources/serviceAccountKey.json");

            // Firebase 옵션 설정
            FirebaseOptions options = new FirebaseOptions.Builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .setDatabaseUrl("https://sourcecodeanalysis-donggyu-default-rtdb.asia-southeast1.firebasedatabase.app/")
                    .build();

            // Firebase 초기화 (앱 실행 시 딱 1번만!)
            FirebaseApp.initializeApp(options);

            System.out.println("Firebase 초기화");
            writeLog("gamestart");
            Game g = new Game();

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

