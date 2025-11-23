package org.newdawn.spaceinvaders.manager;

import org.newdawn.spaceinvaders.sound.SfxEngine;

import javax.sound.sampled.*;
import java.io.InputStream;
import java.io.BufferedInputStream;
import java.util.EnumMap;
import java.util.logging.Logger;

public final class SoundManager {
    // ===== BGM =====
    public enum Bgm { MENU, STAGE, BOSS }

    // ===== SFX =====
    public enum Sfx { SHOOT }

    private static final SoundManager I = new SoundManager();
    private static final Logger logger = Logger.getLogger(SoundManager.class.getName());

    public static SoundManager get() {
        return I;
    }

    // BGM 엔진
    private final EnumMap<Bgm, Clip> bgmClips = new EnumMap<>(Bgm.class);
    private Bgm currentBgm;

    // SFX 엔진 (분리된 클래스)
    private final SfxEngine sfxEngine = new SfxEngine();

    private SoundManager() { }

    /* 공통 유틸: 클래스패스에서 clip 로드 (SfxEngine에서도 사용) */
    public static Clip loadClip(String classpath) {
        if (classpath == null) return null;
        try (InputStream in = SoundManager.class.getResourceAsStream(classpath);
             BufferedInputStream bin = new BufferedInputStream(in);
             AudioInputStream ais = AudioSystem.getAudioInputStream(bin)) {
            Clip c = AudioSystem.getClip();
            c.open(ais);
            return c;
        } catch (Exception e) {
            logger.warning("Failed to load clip: " + classpath + " / " + e.getMessage());
            return null;
        }
    }

    private Clip clipOf(Bgm bgm) {
        Clip c = bgmClips.get(bgm);
        if (c != null) return c;

        String path;
        switch (bgm) {
            case MENU:  path = "/sounds/menu.wav";  break;
            case STAGE: path = "/sounds/stage.wav"; break;
            case BOSS:  path = "/sounds/boss.wav";  break;
            default:    path = null;                break;
        }
        c = loadClip(path);
        bgmClips.put(bgm, c);
        return c;
    }

    public void stop() {
        if (currentBgm != null) {
            Clip c = bgmClips.get(currentBgm);
            if (c != null && c.isRunning()) {
                c.stop();
            }
        }
        currentBgm = null;
    }

    public void play(Bgm bgm) {
        if (bgm == null || bgm == currentBgm) return;
        stop();
        Clip c = clipOf(bgm);
        if (c != null) {
            c.setFramePosition(0);
            c.loop(Clip.LOOP_CONTINUOUSLY);
            currentBgm = bgm;
        }
    }


    public void playSfx(Sfx s) {
        sfxEngine.playSfx(s);
    }

    public void setSfxVolume(float db) {
        sfxEngine.setSfxVolume(db);
    }
}