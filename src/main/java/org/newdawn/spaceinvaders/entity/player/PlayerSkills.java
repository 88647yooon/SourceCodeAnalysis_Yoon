package org.newdawn.spaceinvaders.entity.player;



public class PlayerSkills {
    public int atkLv = 0;      // 0..5
    public int rofLv = 0;      // 0..5
    public int dashLv = 0;

    // 대시: 쿨감 배수(작을수록 짧음), 0.5 하한 / 무적 보너스
    public double dashCdMul() { return Math.max(0.50, 1.0 - 0.12 * dashLv); }
    public int dashIframesBonusMs() { return 20 * dashLv; }

}
