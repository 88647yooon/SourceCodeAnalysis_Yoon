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
        if(!waitingForKeyPress){
            for(Entity entity : new ArrayList<>(entities)){
                entity.move(delta);
            }
        }
        List<Entity> currentEntities = new ArrayList<>(entities);
        int size = currentEntities.size();

        for (int p = 0; p < size; p++) {
            for (int s = p + 1; s < size; s++) {
                Entity a = currentEntities.get(p);
                Entity b = currentEntities.get(s);

                // 이미 제거 예약된 엔티티끼리는 충돌 검사 생략 (선택 사항)
                if (removeList.contains(a) || removeList.contains(b)) continue;

                if (a.collidesWith(b)) {
                    a.collidedWith(b);
                    b.collidedWith(a);
                }
            }
        }

        //엔티티 제거
        if (!removeList.isEmpty()){
            entities.removeAll(removeList);
            removeList.clear();
        }

        //doLogic
        if(game.isLogicRequiredThisLoop()){
            for(Entity e : new ArrayList<>(entities)){
                e.doLogic();
            }
            game.resetLogicFlag();
        }
    }
}
