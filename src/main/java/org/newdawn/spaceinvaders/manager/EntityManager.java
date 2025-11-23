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



    public void update(long delta, boolean waitingForKeyPress){
        if(!waitingForKeyPress){
            for(Entity entity : new ArrayList<>(entities)){
                entity.move(delta);
            }
        }
        //엔티티 충돌
        int size = entities.size();
        for (int p = 0; p < size; p++) {
            for (int s = p + 1; s < size; s++) {
                Entity a = entities.get(p);
                Entity b = entities.get(s);
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
            for(Entity e : entities){
                e.doLogic();
            }
            game.resetLogicFlag();
        }
    }
}
