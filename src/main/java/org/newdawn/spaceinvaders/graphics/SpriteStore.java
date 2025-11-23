package org.newdawn.spaceinvaders.graphics;

import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.Transparency;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import javax.imageio.ImageIO;
import java.util.logging.Logger;

public class SpriteStore {

	private static final SpriteStore single = new SpriteStore();
    private static final Logger logger = Logger.getLogger(SpriteStore.class.getName());

	public static SpriteStore get() {
		return single;
	}
	

	private final HashMap<String, Sprite> sprites = new HashMap<>();
	

	public Sprite getSprite(String ref) {
        if (sprites.get(ref) != null) {
			return sprites.get(ref);
		}

        BufferedImage sourceImage = null;
		
		try {
            URL url = this.getClass().getClassLoader().getResource(ref);
            if (url == null) {
				fail("Can't find ref: "+ref);
			}
            sourceImage = ImageIO.read(url);
		} catch (IOException e) {
			fail("Failed to load: "+ref);
		}
		
        GraphicsConfiguration gc = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration();
		Image image = gc.createCompatibleImage(sourceImage.getWidth(),sourceImage.getHeight(),Transparency.BITMASK);
		
        image.getGraphics().drawImage(sourceImage,0,0,null);
		
        Sprite sprite = new Sprite(image);
		sprites.put(ref,sprite);

		return sprite;
	}
	
    private void fail(String message) {
		logger.warning(message);
        throw new IllegalStateException(message);
	}
}