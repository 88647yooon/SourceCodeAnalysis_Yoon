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

import java.text.SimpleDateFormat;

import org.newdawn.spaceinvaders.entity.AlienEntity;
import org.newdawn.spaceinvaders.entity.Entity;
import org.newdawn.spaceinvaders.entity.ShipEntity;
import org.newdawn.spaceinvaders.entity.ShotEntity;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

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
    private static final String DB_URL  = "https://sourcecodeanalysis-donggyu-default-rtdb.asia-southeast1.firebasedatabase.app";
    private static String SESSION_UID   = null;
    private static String SESSION_EMAIL = null;
    private static String SESSION_ID_TOKEN = null;
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
		leftPressed = false;
		rightPressed = false;
		firePressed = false;
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
		for (int row=0;row<5;row++) {
			for (int x=0;x<12;x++) {
				Entity alien = new AlienEntity(this,100+(x*50),(50)+row*30);
				entities.add(alien);
				alienCount++;
			}
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
			notifyWin();
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
			// if we're waiting for an "any key" typed then we don't 
			// want to do anything with just a "press"
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
			if (waitingForKeyPress) {
				if (pressCount == 1) {
					// since we've now recieved our key typed
					// event we can mark it as such and start 
					// our new game
					waitingForKeyPress = false;
					startGame();
					pressCount = 0;
				} else {
					pressCount++;
				}
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
    /// 로그인 화면
    private static void showAuthDialogAndLogin() {
        final JDialog dlg = new JDialog((JFrame)null, "로그인 / 회원가입", true);
        JTabbedPane tabs = new JTabbedPane();

        // 로그인 탭
        JPanel login = new JPanel(new java.awt.GridBagLayout());
        JTextField loginEmail = new JTextField(20);
        JPasswordField loginPw = new JPasswordField(20);
        JButton btnLogin = new JButton("로그인");
        java.awt.GridBagConstraints c = gbc();
        login.add(new JLabel("이메일"), c); c.gridx=1; login.add(loginEmail, c);
        c = gbc(0,1); login.add(new JLabel("비밀번호"), c); c.gridx=1; login.add(loginPw, c);
        c = gbc(0,2); c.gridwidth=2;
        btnLogin.addActionListener(ev -> {
            try {
                AuthResult ar = restSignIn(loginEmail.getText().trim(), new String(loginPw.getPassword()));
                SESSION_UID = ar.localId; SESSION_EMAIL = ar.email; SESSION_ID_TOKEN = ar.idToken;
                JOptionPane.showMessageDialog(dlg, "로그인 성공: " + ar.email);
                dlg.dispose();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(dlg, "로그인 실패\n" + ex.getMessage(), "오류", JOptionPane.ERROR_MESSAGE);
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
        signup.add(new JLabel("이메일"), c); c.gridx=1; signup.add(signEmail, c);
        c = gbc(0,1); signup.add(new JLabel("비밀번호"), c); c.gridx=1; signup.add(signPw, c);
        c = gbc(0,2); signup.add(new JLabel("비밀번호 확인"), c); c.gridx=1; signup.add(signPw2, c);
        c = gbc(0,3); c.gridwidth=2;
        btnSign.addActionListener(ev -> {
            String pw1 = new String(signPw.getPassword());
            String pw2 = new String(signPw2.getPassword());
            if (!pw1.equals(pw2)) {
                JOptionPane.showMessageDialog(dlg, "비밀번호가 일치하지 않습니다.", "오류", JOptionPane.ERROR_MESSAGE);
                return;
            }
            try {
                AuthResult ar = restSignUp(signEmail.getText().trim(), pw1);
                SESSION_UID = ar.localId; SESSION_EMAIL = ar.email; SESSION_ID_TOKEN = ar.idToken;
                JOptionPane.showMessageDialog(dlg, "회원가입 성공: " + ar.email);
                // 기본 프로필 저장(선택)
                restSetJson("users/"+SESSION_UID+"/profile", SESSION_ID_TOKEN,
                        "{\"email\":"+quote(SESSION_EMAIL)+",\"createdAt\":"+quote(now())+"}");
                dlg.dispose();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(dlg, "회원가입 실패\n" + ex.getMessage(), "오류", JOptionPane.ERROR_MESSAGE);
            }
        });
        signup.add(btnSign, c);

        tabs.add("로그인", login);
        tabs.add("회원가입", signup);

        dlg.setContentPane(tabs);
        dlg.pack();
        dlg.setLocationRelativeTo(null);
        dlg.setVisible(true);

        if (SESSION_ID_TOKEN == null) System.exit(0); // 취소 시 종료(원하면 다르게 처리)
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

    private static String restSetJson(String path, String idToken, String json) throws Exception {
        String endpoint = DB_URL + "/" + path + ".json?auth=" + urlEnc(idToken);
        return httpPutJson(endpoint, json);
    }

    // =========================
    // 🔧 HTTP & 미니 JSON 유틸 (의존성 없음)
    // =========================
    private static String httpPostJson(String endpoint, String body) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(endpoint).openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type","application/json; charset=UTF-8");
        try (OutputStream os = conn.getOutputStream()) {
            os.write(body.getBytes(StandardCharsets.UTF_8));
        }
        return readResp(conn);
    }

    private static String httpPutJson(String endpoint, String body) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(endpoint).openConnection();
        conn.setRequestMethod("PUT");
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type","application/json; charset=UTF-8");
        try (OutputStream os = conn.getOutputStream()) {
            os.write(body.getBytes(StandardCharsets.UTF_8));
        }
        return readResp(conn);
    }

    private static String readResp(HttpURLConnection conn) throws Exception {
        int code = conn.getResponseCode();
        try (InputStream is = (code >= 200 && code < 300) ? conn.getInputStream() : conn.getErrorStream()) {
            String txt = readFully(is, "UTF-8");
            if (code >= 200 && code < 300) return txt;
            throw new RuntimeException("HTTP " + code + ": " + txt);
        }
    }
    private static String readFully(InputStream is, String charset) throws Exception {
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

    private static String jget(String json, String key) {
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

    private static String quote(String s) {
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

    private static String urlEnc(String s) {
        try { return java.net.URLEncoder.encode(s, "UTF-8"); }
        catch (Exception e) { return s; }
    }

    private static String now() {
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

            // 1) 로그인/회원가입 먼저
            SwingUtilities.invokeLater(() -> showAuthDialogAndLogin());
            // 로그인 다이얼로그가 modal이므로 여기서 잠시 대기
            try {
                // modal dialog가 닫히는 동안 메인 스레드가 바로 진행되지 않게 약간 대기
                while (SESSION_ID_TOKEN == null) Thread.sleep(100);
            } catch (InterruptedException ignored) {}

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

