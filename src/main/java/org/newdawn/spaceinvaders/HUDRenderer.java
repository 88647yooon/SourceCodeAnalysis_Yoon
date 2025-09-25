package org.newdawn.spaceinvaders;

import org.newdawn.spaceinvaders.entity.ShipEntity;

import java.awt.*;

public class HUDRenderer {
    private Game game;
    public HUDRenderer(Game game){
        this.game = game;
    }

    public void render(Graphics2D g){
        ShipEntity player = game.getPlayerShip();
        int x = 20, y = 20;
        int HeartSize = 20;
        //하트로 표시
        Image heart = SpriteStore.get().getSprite("sprites/heart.png").getImage();
        Image emptyheart = SpriteStore.get().getSprite("sprites/emptyheart.png").getImage();
        for(int i=0;i<player.getMaxHP(); i++){
            if(i < player.getCurrentHP()){
                //현재 HP 보다 작으면 ->빨간 하트
                g.drawImage(heart,x + (i*(HeartSize+10)),y,HeartSize,HeartSize,null);
            }
            else{
                // 나머지 -> 빈 하트
                g.drawImage(emptyheart,x + (i*(HeartSize+10)),y,HeartSize,HeartSize,null);
            }
        }

        /*
        g.setFont(new Font("Arial", Font.BOLD, 16));
        g.setColor(Color.BLACK);
        //나중에 동규가 스코어 추가하면서 추가할 예정
        //g.drawString("SCORE: " + game.getScore(), x, y+40);
        */

    }
}
