package org.newdawn.spaceinvaders.entity;

import org.newdawn.spaceinvaders.Game;
import org.newdawn.spaceinvaders.Sprite;
import org.newdawn.spaceinvaders.SpriteStore;

import java.awt.*;

public class BossEntity extends Entity {
    private final Game game;

    // 애니메이션
    private Sprite[] frames = new Sprite[3];
    private long lastFrameChange = 0;
    private long frameDuration = 120; // 빨리 깜빡이게
    private int frameNumber = 0;

    // 보스 속성
    private int hp = 50;                 // 체력
    private double baseSpeed = 80;       // 기본 이동 속도
    private double t = 0;                // 패턴용 시간값


    // 보스 크기 (그림 + 판정 영역)
    private static final int BOSS_W = 120;
    private static final int BOSS_H = 120;

    public BossEntity(Game game, int x, int y) {
        super("sprites/boss1.gif", x, y); // 시작 프레임
        this.game = game;

        // 프레임 로드 (경로/확장자 맞춰라!)
        SpriteStore store = SpriteStore.get();
        frames[0] = store.getSprite("sprites/boss1.gif");
        frames[1] = store.getSprite("sprites/boss2.gif");
        frames[2] = store.getSprite("sprites/boss3.gif");

        // 초기에 느리게 오른쪽으로
        setHorizontalMovement(baseSpeed);
        setVerticalMovement(10);
    }
    //보스 gif 크기 조절
    @Override
    public void draw(Graphics g) {
        // 보스를 100×100 크기로 강제 축소
        g.drawImage(sprite.getImage(), (int)x, (int)y, 100, 100, null);

        // 체력바 (보스 위에 표시)
        g.setColor(Color.RED);
        int barWidth = (int)((hp / 50.0) * 100); // 최대 100px
        g.fillRect((int)x, (int)y - 10, barWidth, 5);
        g.setColor(Color.WHITE);
        g.drawRect((int)x, (int)y - 10, 100, 5);
    }

    @Override
    public void move(long delta) {
        // 애니메이션
        lastFrameChange += delta;
        if (lastFrameChange > frameDuration) {
            lastFrameChange = 0;
            frameNumber = (frameNumber + 1) % frames.length;
            sprite = frames[frameNumber];
        }

        // 간단한 패턴: 좌우 왕복 + 살짝 상하 요동
        t += delta / 1000.0;
        // 좌우 경계 체크
        if (x < 20) setHorizontalMovement(Math.abs(getHorizontalMovement()));
        if (x > 760) setHorizontalMovement(-Math.abs(getHorizontalMovement()));
        // 상하 파동
        setVerticalMovement(10 * Math.sin(t * 2.0));

        // super.move()는 dx,dy 반영해서 위치 이동
        super.move(delta);

        // Y축 흔들림 (약간 파동)
        y += Math.sin(t * 2.0);

        // 너무 내려가면 패배 처리
        if (y > 520) {
            game.notifyDeath();
        }
    }

    @Override
    public boolean collidesWith(Entity other) {
        // 보스는 120x120 hitbox 기준
        Rectangle me = new Rectangle((int)x, (int)y, BOSS_W, BOSS_H);
        Rectangle him = new Rectangle(other.getX(), other.getY(),
                other.sprite.getWidth(), other.sprite.getHeight());
        return me.intersects(him);
    }


    @Override
    public void collidedWith(Entity other) {
        // 총알에 맞으면 체력 감소
        if (other instanceof ShotEntity) {
            game.removeEntity(other);  // 탄 제거
            hp -= 1;
            if (hp <= 0) {
                // 보스 처치
                game.removeEntity(this);
                game.onBossKilled();   // Game에 처리 훅 추가 (아래에 코드 있음)
            }
        }
        // 플레이어와 충돌 시 즉사 규칙 등은 선택적으로 추가
    }
}
