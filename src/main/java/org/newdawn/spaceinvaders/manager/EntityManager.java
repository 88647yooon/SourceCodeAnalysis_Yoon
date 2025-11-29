package org.newdawn.spaceinvaders.manager;

import org.newdawn.spaceinvaders.Game;
import org.newdawn.spaceinvaders.entity.base.Entity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class EntityManager {
    private final Game game;
    private final List<Entity> entities = new ArrayList<>();
    private final List<Entity> removeList = new ArrayList<>();

    public EntityManager(Game game){ this.game = game; }

    public void addEntity(Entity entity){ entities.add(entity); }
    public void removeEntity(Entity entity){ removeList.add(entity); }

    public void clearEntity(){
        entities.clear();
        removeList.clear();
    }

    public List<Entity> getEntities() { return Collections.unmodifiableList(entities); }
    public List<Entity> getMutableEntities(){ return entities; }

    public void removeEntitiesByClass(Class<? extends Entity> clazz) {
        for (Entity e : entities) {
            if (clazz.isInstance(e)) {
                removeEntity(e); // 제거 리스트에 등록 (다음 프레임에 안전하게 삭제됨)
            }
        }
    }

    public void update(long delta, boolean waitingForKeyPress){
        // 1. 엔티티 이동
        if(!waitingForKeyPress){
            moveAllEntities(delta);
        }

        // 2. 충돌 체크
        checkCollisions();

        // 3. 엔티티 제거
        processRemovals();

        // 4. 추가 로직 (doLogic)
        processGameLogic();
    }
    private void moveAllEntities(long delta) {

        for(Entity entity : new ArrayList<>(entities)){
            entity.move(delta);
        }
    }
    private void checkCollisions() {
        List<Entity> currentEntities = new ArrayList<>(entities);
        int size = currentEntities.size();

        for (int p = 0; p < size; p++) {
            for (int s = p + 1; s < size; s++) {
                Entity a = currentEntities.get(p);
                Entity b = currentEntities.get(s);

                resolveCollisionPair(a, b);
            }
        }
    }
    private void resolveCollisionPair(Entity a, Entity b) {
        if (removeList.contains(a) || removeList.contains(b)) return;

        if (a.collidesWith(b)) {
            a.collidedWith(b);
            b.collidedWith(a);
        }
    }

    private void processRemovals() {
        if (!removeList.isEmpty()){
            entities.removeAll(removeList);
            removeList.clear();
        }
    }

    private void processGameLogic() {
        if(game.isLogicRequiredThisLoop()){
            for(Entity e : new ArrayList<>(entities)){
                e.doLogic();
            }
            game.resetLogicFlag();
        }
    }
}
