package org.newdawn.spaceinvaders;

import javax.sound.sampled.*;
import java.io.InputStream;
import java.io.BufferedInputStream;
import java.util.HashMap;
import java.util.Map;

public final class SoundManager {
    // ===== BGM =====
    public enum Bgm { MENU, STAGE, BOSS }

    private static final SoundManager I = new SoundManager();
    public static SoundManager get() { return I; }

    private final Map<Bgm, Clip> bgmClips = new HashMap<>();
    private Bgm currentBgm;

    private float sfxGainDb = 0f;

    private SoundManager() {}

    private Clip loadClip(String classpath) {
        if (classpath == null) return null;
        try (InputStream in = SoundManager.class.getResourceAsStream(classpath);
             BufferedInputStream bin = new BufferedInputStream(in);
             AudioInputStream ais = AudioSystem.getAudioInputStream(bin)) {
            Clip c = AudioSystem.getClip();
            c.open(ais);
            return c;
        } catch (Exception ignore) {
            return null;
        }
    }

    private Clip clipOf(Bgm bgm) {
        Clip c = bgmClips.get(bgm);
        if (c != null) return c;

        String p;
        switch (bgm) {
            case MENU:  p = "/sounds/menu.wav";  break;
            case STAGE: p = "/sounds/stage.wav"; break;
            case BOSS:  p = "/sounds/boss.wav";  break;
            default:    p = null;                break;
        }
        c = loadClip(p);
        bgmClips.put(bgm, c);
        return c;
    }

    public void stop() {
        if (currentBgm != null) {
            Clip c = bgmClips.get(currentBgm);
            if (c != null && c.isRunning()) c.stop();
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

    // ===== SFX (effect) =====
    public enum Sfx { SHOOT }

    private static final int SFX_POOL_SIZE = 8;
    private final Map<Sfx, Clip[]> sfxPools = new HashMap<>();

    private Clip loadSfx(String classpath) {
        return loadClip(classpath); // 동일 로더 사용
    }

    private Clip[] ensureSfxPool(Sfx s) {
        Clip[] pool = sfxPools.get(s);
        if (pool != null) return pool;

        String p;
        switch (s) {
            case SHOOT: p = "/sounds/shoot.wav"; break;
            default:    p = null;                break;
        }

        pool = new Clip[SFX_POOL_SIZE];
        for (int i = 0; i < SFX_POOL_SIZE; i++) {
            pool[i] = loadSfx(p);
            applyGain(pool[i]);
        }
        sfxPools.put(s, pool);
        return pool;
    }

    /** 짧은 효과음 재생(겹쳐 재생 지원) */
    public void playSfx(Sfx s) {
        Clip[] pool = ensureSfxPool(s);
        if (pool == null) return;

        for (int i = 0; i < pool.length; i++) {
            Clip c = pool[i];
            if (c == null) continue;
            if (!c.isRunning()) {
                try {
                    applyGain(c);
                    c.setFramePosition(0);
                    c.start();
                } catch (Exception ignore) {}
                return;
            }
        }
        // 모두 재생 중이면 0번을 재시작
        Clip c0 = pool[0];
        if (c0 != null) {
            try {
                applyGain(c0);
                c0.stop();
                c0.setFramePosition(0);
                c0.start();
            } catch (Exception ignore) {}
        }
    }

    /** (선택) SFX 전체 볼륨 조절 (dB) */
    public void setSfxVolume(float db) {
        sfxGainDb = db;
        for (Clip[] pool : sfxPools.values()) {
            if (pool == null) continue;
            for (Clip c : pool) {
                if (c == null) continue;
                try {
                    FloatControl gain = (FloatControl) c.getControl(FloatControl.Type.MASTER_GAIN);
                    gain.setValue(db);
                } catch (Exception ignore) {}
            }
        }
    }

    private void applyGain(Clip c) {
        if (c == null) return;
        try {
            FloatControl gain = (FloatControl) c.getControl(FloatControl.Type.MASTER_GAIN);
            // 지원 범위 클램프
            float min = gain.getMinimum();
            float max = gain.getMaximum();
            float val = Math.max(min, Math.min(max, sfxGainDb));
            gain.setValue(val);
        } catch (Exception ignore) {}
    }

    /** 리소스 정리 */
    public void shutdown() {
        stop();
        for (Clip c : bgmClips.values()) {
            try { if (c != null) c.close(); } catch (Exception ignore) {}
        }
        bgmClips.clear();

        for (Clip[] pool : sfxPools.values()) {
            if (pool == null) continue;
            for (Clip c : pool) {
                try { if (c != null) c.close(); } catch (Exception ignore) {}
            }
        }
        sfxPools.clear();
    }
}