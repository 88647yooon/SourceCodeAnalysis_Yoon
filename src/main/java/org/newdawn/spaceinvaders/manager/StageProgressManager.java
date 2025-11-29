package org.newdawn.spaceinvaders.manager;

import org.newdawn.spaceinvaders.database.AuthSession;
import org.newdawn.spaceinvaders.database.GameDatabaseService;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class StageProgressManager {
    public static final int TOTAL_STAGE = 5;

    private final GameDatabaseService gameDb;

    private final Map<Integer, Integer> starRequirements = new HashMap<>();

    private final Map<Integer, Integer> stageStars = new HashMap<>();

    private final boolean[] stageUnlocked = new boolean[TOTAL_STAGE];

    public StageProgressManager(GameDatabaseService gameDb) {
        this.gameDb = gameDb;
        initStarRequirements();
        initStageUnlocks();
    }

    private void initStarRequirements() {
        starRequirements.put(1, 1000);
        starRequirements.put(2, 2000);
        starRequirements.put(3, 3000);
        starRequirements.put(4, 4000);
        starRequirements.put(5, 5000);
    }
    /** 잠금 배열 초기화 (1스테이지만 기본 오픈) */
    private void initStageUnlocks() {
        Arrays.fill(stageUnlocked, false);
        stageUnlocked[0] = true; // 스테이지 1은 항상 열려 있음
        rebuildStageUnlocks();
    }

    /** 스테이지별 3성 기준 점수 조회 */
    public int getRequiredScore(int stageId) {
        return starRequirements.getOrDefault(stageId, 1000);
    }

    /**
     * 별 개수 계산만 담당 (DB 반영 X)
     * 1★ : 무조건 클리어
     * 2★ : 시간 내 클리어
     * 3★ : 시간 내 + 무피격 + 점수 기준 달성
     */
    public int evaluateStars(int stageId, int timeLeft, int damageTaken, int score) {
        int stars = 1;

        if (timeLeft > 0) {
            stars = 2;
            int requiredScore = getRequiredScore(stageId);
            if (damageTaken == 0 && score >= requiredScore) {
                stars = 3;
            }
        }
        return stars;
    }

    /**
     * 해당 스테이지의 별 갱신 (기존보다 좋을 때만)
     * 세션이 있으면 DB 저장까지 수행
     */
    public boolean updateStageStars(AuthSession session, int stageId, int newStars) {
        int prev = stageStars.getOrDefault(stageId, 0);
        if (newStars <= prev) {
            return false; // 더 안 좋으면 갱신하지 않음
        }

        stageStars.put(stageId, newStars);

        if (session != null && session.isLoggedIn()) {
            gameDb.saveStageStars(session, stageStars);
        }
        rebuildStageUnlocks();
        return true;
    }

    /** DB에서 stageStars 불러오기 + 잠금 상태 재계산 */
    public void loadFromDb(AuthSession session) {
        if (session == null || !session.isLoggedIn()) return;

        Map<Integer, Integer> loaded = gameDb.loadStageStars(session);
        stageStars.clear();
        stageStars.putAll(loaded);

        rebuildStageUnlocks();
    }

    public int getStageStars(int stageId) {
        return stageStars.getOrDefault(stageId, 0);
    }

    public boolean isStageUnlocked(int stageIndex) {
        if (stageIndex < 0 || stageIndex >= stageUnlocked.length) {
            return false;
        }
        return stageUnlocked[stageIndex];
    }

    /** stageStars 기반으로 잠금 상태 재계산 */
    public void rebuildStageUnlocks() {
        // 1스테이지(인덱스 0)는 항상 열려 있음
        stageUnlocked[0] = true;

        for (int stageId = 2; stageId <= TOTAL_STAGE; stageId++) {
            int prevStars = getStageStars(stageId - 1);
            stageUnlocked[stageId - 1] = (prevStars >= 3);
        }
    }

    /** currentStageId를 클리어했을 때, 조건 만족하면 다음 스테이지 오픈 */
    public void unlockNextStageIfEligible(int currentStageId) {
        if (currentStageId >= TOTAL_STAGE) return;
        if (getStageStars(currentStageId) >= 3) {
            stageUnlocked[currentStageId] = true; // currentStageId의 다음 스테이지 인덱스
        }
    }
}
