package org.newdawn.spaceinvaders.manager;

import org.newdawn.spaceinvaders.Game;
import org.newdawn.spaceinvaders.entity.base.Entity;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class entitymanager {
    private final Game game;
    private static final List<Entity> entities = new ArrayList<Entity>();
    private static final List<Entity> removeList = new ArrayList<>();

    public entitymanager(Game game){
        this.game = game;
    }

    public List<Entity> getEntities() {
        return entities;
    }
    public static List<Entity> getMutableEntities(){
        return entities; }
    public static void addEntity(Entity entity){
        entities.add(entity);
    }

    public void removeEntity(Entity entity){
        removeList.add(entity);
    }

    public static void clearEntity(){
        entities.clear();
        removeList.clear();
    }

    public void update(long delta, boolean skipMovement){
        if(!skipMovement){
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
    public void removeAllHostages() {
        Iterator<Entity> it = entities.iterator();
        while (it.hasNext()) {
            Entity e = it.next();
            if (e instanceof org.newdawn.spaceinvaders.entity.enemy.HostageEntity) {
                it.remove();
            }
        }
    }

}
