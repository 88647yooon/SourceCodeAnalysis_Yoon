package org.newdawn.spaceinvaders.entity;

import java.awt.Graphics2D;

public interface BossAttackStrategy {
    /**
     * 패턴의 로직을 수행
     * boss 보스 엔티티 본체
     * delta 지난 프렘임과의 시간차
     */
    void update(BossEntity boss,long delta);

    /**
     * 패턴 특유의 그래픽을 그림
     * g그래픽 컨텍스트
     * boss 보스 엔티티 본체
     */
    void draw(Graphics2D g,BossEntity boss);
}
