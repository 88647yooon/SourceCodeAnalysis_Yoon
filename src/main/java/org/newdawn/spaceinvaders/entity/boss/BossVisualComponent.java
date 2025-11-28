package org.newdawn.spaceinvaders.entity.boss;

import org.newdawn.spaceinvaders.graphics.Sprite;
import org.newdawn.spaceinvaders.graphics.SpriteStore;

import java.awt.*;

public class BossVisualComponent {
    private final BossEntity boss;

    //애니메이션 관련
    private final Sprite[] frames = new Sprite[3];
    private int frameNumber =0;
    private long lastFrameChange = 0;

    //화면 경계
    private static final int BOSS_W = 120;
    private static final int BOSS_H = 120;

    public BossVisualComponent(BossEntity boss) {
        this.boss = boss;
        initSprites();
    }

    private void initSprites() {
        frames[0] = SpriteStore.get().getSprite("sprites/boss1.png");
        frames[1] = SpriteStore.get().getSprite("sprites/boss2.png");
        frames[2] = SpriteStore.get().getSprite("sprites/boss3.png");
    }

    public Sprite updateAnimation(long now){
        long frameDuration = 120;
        if(now - lastFrameChange > frameDuration){
            lastFrameChange = now;
            frameNumber = (frameNumber + 1) % frames.length;
        }
        return frames[frameNumber];
    }

    public void draw(Graphics2D g,double x, double y,long now, long phaseHoldUntil) {
        Sprite currentSprite = frames[frameNumber];
        // 1. 보스 본체
        g.drawImage(currentSprite.getImage(), (int)x, (int)y, BOSS_W, BOSS_H, null);

        drawHealthBar(g);

        if(now < phaseHoldUntil){
            drawBlinkEffect(g,now);
        }
    }

    private void drawHealthBar(Graphics2D g) {
        int w = BossVisualComponent.BOSS_W;
        int h = 6;
        double ratio = boss.getCurrentHP() / (double)boss.getMaxHP();
        int bw = (int)Math.max(0,Math.round(ratio * w));

        g.setColor(Color.RED);
        g.fillRect(boss.getX(),boss.getY()-10,bw,h);
        g.setColor(Color.DARK_GRAY);
        g.drawRect(boss.getX(),boss.getY()-10,w,h);
    }

    private void drawBlinkEffect(Graphics2D g, long now) {
        Stroke oldS = g.getStroke();
        float a = 0.4f + 0.6f * (float)Math.abs(Math.sin((now % 600) / 600.0 * Math.PI * 2));
        g.setColor(new Color(255, 255, 0, (int)(a * 255)));
        g.setStroke(new BasicStroke(3f));
        g.drawRect(boss.getX(), boss.getY(), BOSS_W, BOSS_H);
        g.setStroke(oldS);

    }
}
