package org.newdawn.spaceinvaders.entity.boss;

import java.awt.Graphics2D;
import java.util.Arrays;
import java.util.List;

public class BossStrategyComposite implements BossAttackStrategy{
    private final List<BossAttackStrategy> strategies;

    public BossStrategyComposite(BossAttackStrategy... strategies){
        this.strategies = Arrays.asList(strategies);
    }

    @Override
    public void update(BossEntity boss, long delta){
        for(BossAttackStrategy s: strategies){
            s.update(boss,delta);
        }
    }

    @Override
    public void draw(Graphics2D g,BossEntity boss){
        for(BossAttackStrategy s: strategies){
            s.draw(g,boss);
        }
    }
}
