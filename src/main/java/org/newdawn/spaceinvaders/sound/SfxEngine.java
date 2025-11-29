package org.newdawn.spaceinvaders.sound;

import javax.sound.sampled.Clip;
import javax.sound.sampled.FloatControl;
import java.util.EnumMap;
import java.util.logging.Logger;
import org.newdawn.spaceinvaders.manager.SoundManager;

public final class SfxEngine {

    private static final Logger logger = Logger.getLogger(SfxEngine.class.getName());
    private static final int SFX_POOL_SIZE = 8;

    // enum은 SoundManager 안에 있으니까 이렇게 참조
    private final EnumMap<SoundManager.Sfx, Clip[]> sfxPools =
            new EnumMap<>(SoundManager.Sfx.class);

    private float sfxGainDb = 0f;

    /** 특정 SFX 타입에 대한 풀 확보/초기화 */
    private Clip[] ensureSfxPool(SoundManager.Sfx s) {
        Clip[] pool = sfxPools.get(s);
        if (pool != null) return pool;

        String path = null;
        if (s == SoundManager.Sfx.SHOOT) {
            path =  "/sounds/shoot.wav";
        }

        pool = new Clip[SFX_POOL_SIZE];
        for (int i = 0; i < SFX_POOL_SIZE; i++) {
            pool[i] = SoundManager.loadClip(path);
            applyGain(pool[i]);
        }
        sfxPools.put(s, pool);
        return pool;
    }

    /** 짧은 효과음 재생(겹쳐 재생 지원) */
    public void playSfx(SoundManager.Sfx s) {
        Clip[] pool = ensureSfxPool(s);  // null 아님

        for (Clip c : pool) {
            if (c == null) continue;
            if (!c.isRunning()) {
                try {
                    applyGain(c);
                    c.setFramePosition(0);
                    c.start();
                } catch (Exception e) {
                    logger.warning("Failed to play sfx: " + e.getMessage());
                }
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
            } catch (Exception e) {
                logger.warning("Failed to restart sfx: " + e.getMessage());
            }
        }
    }

    /** SFX 전체 볼륨 조절 (dB) */
    public void setSfxVolume(float db) {
        sfxGainDb = db;
        for (Clip[] pool : sfxPools.values()) {
            if (pool == null) continue;
            for (Clip c : pool) {
                if (c == null) continue;
                try {
                    FloatControl gain =
                            (FloatControl) c.getControl(FloatControl.Type.MASTER_GAIN);
                    gain.setValue(db);
                } catch (Exception e) {
                    logger.warning("Failed to set volume for sfx: " + e.getMessage());
                }
            }
        }
    }

    private void applyGain(Clip c) {
        if (c == null) return;
        try {
            FloatControl gain =
                    (FloatControl) c.getControl(FloatControl.Type.MASTER_GAIN);
            float min = gain.getMinimum();
            float max = gain.getMaximum();
            float val = Math.max(min, Math.min(max, sfxGainDb));
            gain.setValue(val);
        } catch (Exception e) {
            logger.warning("Failed to apply gain: " + e.getMessage());
        }
    }
}