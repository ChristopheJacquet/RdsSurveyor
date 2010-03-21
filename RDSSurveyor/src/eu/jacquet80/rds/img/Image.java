package eu.jacquet80.rds.img;

import javax.swing.Icon;
import javax.swing.ImageIcon;

public class Image {
	public static final Icon 
		UP = iconForName("up.png"),
		DOWN = iconForName("down.png"),
		PLAY = iconForName("play.png"),
		PAUSE = iconForName("pause.png"),
		FFWD = iconForName("ffwd.png"),
		RWND = iconForName("rwnd.png");
	
	private static Icon iconForName(String name) {
		return new ImageIcon(Image.class.getResource(name));
	}
	
}
