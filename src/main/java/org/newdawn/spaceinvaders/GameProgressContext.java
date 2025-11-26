package org.newdawn.spaceinvaders;

public class GameProgressContext {
    // 1. Game.java 내부에 있던 Mode Enum을 이쪽으로 이동 (혹은 별도 파일로 분리)
    public enum Mode { STAGE, INFINITE }

    // 2. 상태 변수 이동
    private Mode currentMode = Mode.STAGE;
    private int score = 0;
    private int waveCount = 1;
    private int alienCount = 0;
    private boolean bossActive = false;
    private boolean dangerMode = false;
    private int currentStageId = 1;
    private long runStartedAtMs = 0L;

    // 무한 모드 전용 변수
    private int normalsClearedInCycle = 0;

    // 스테이지 모드 전용 (타임어택, 데미지 계산용)
    private int stageStartHP = 0;

    // --- 생성자 ---
    public GameProgressContext() {
        reset();
    }

    // --- 비즈니스 로직 (단순 Setter보다 의미 있는 메소드) ---

    public void reset() {
        this.score = 0;
        this.alienCount = 0;
        this.bossActive = false;
        this.dangerMode = false;
        this.normalsClearedInCycle = 0;
        this.runStartedAtMs = 0L;
    }

    public void initStageMode(int stageNum, int currentHP) {
        this.currentMode = Mode.STAGE;
        this.score = 0;
        this.waveCount = stageNum;
        this.currentStageId = stageNum;
        this.stageStartHP = currentHP;
        this.runStartedAtMs = System.currentTimeMillis();

        this.bossActive = (stageNum == 5); // 5스테이지는 보스
        this.alienCount = 0;
        this.normalsClearedInCycle = 0;
        this.dangerMode = false;
    }

    public void initInfiniteMode() {
        this.currentMode = Mode.INFINITE;
        this.score = 0;
        this.waveCount = 0; // 0부터 시작하거나 1부터 시작
        this.runStartedAtMs = System.currentTimeMillis();

        this.bossActive = false;
        this.alienCount = 0;
        this.normalsClearedInCycle = 0;
        this.dangerMode = false;
    }

    public void addScore(int delta) {
        this.score += delta;
    }

    public void decrementAlienCount() {
        this.alienCount--;
        if (this.alienCount < 0) this.alienCount = 0;
    }

    // --- Getters & Setters ---
    public Mode getCurrentMode() { return currentMode; }
    public void setCurrentMode(Mode currentMode) { this.currentMode = currentMode; }

    public int getScore() { return score; }
    public void setScore(int score) { this.score = score; }

    public int getWaveCount() { return waveCount; }
    public void setWaveCount(int waveCount) { this.waveCount = waveCount; }
    public void incrementWaveCount() { this.waveCount++; }

    public int getAlienCount() { return alienCount; }
    public void setAlienCount(int alienCount) { this.alienCount = alienCount; }

    public boolean isBossActive() { return bossActive; }
    public void setBossActive(boolean bossActive) { this.bossActive = bossActive; }

    public boolean isDangerMode() { return dangerMode; }
    public void setDangerMode(boolean dangerMode) { this.dangerMode = dangerMode; }

    public int getCurrentStageId() { return currentStageId; }
    public void setCurrentStageId(int currentStageId) { this.currentStageId = currentStageId; }

    public long getRunStartedAtMs() { return runStartedAtMs; }

    public int getNormalsClearedInCycle() { return normalsClearedInCycle; }
    public void incrementNormalsClearedInCycle() { this.normalsClearedInCycle++; }
    public void resetNormalsClearedInCycle() { this.normalsClearedInCycle = 0; }

    public int getStageStartHP() { return stageStartHP; }

    public boolean isInfiniteMode() { return currentMode == Mode.INFINITE; }
    public boolean isStageMode() { return currentMode == Mode.STAGE; }
}
