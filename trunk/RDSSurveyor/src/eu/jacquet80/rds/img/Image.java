package eu.jacquet80.rds.img;

import java.awt.Toolkit;

import javax.swing.Icon;
import javax.swing.ImageIcon;

public class Image {
	public static final Icon 
		UP = iconForName("up.png"),
		DOWN = iconForName("down.png"),
		PLAY = iconForName("play.png"),
		PAUSE = iconForName("pause.png"),
		FFWD = iconForName("ffwd.png"),
		RWND = iconForName("rwnd.png"),
		OPEN = iconForName("open.png"),
		USBKEY = iconForName("usbkey.png"),
		MICROPHONE = iconForName("microphone.png"),
		NETWORK = iconForName("network.png"),
		SPEAKER_ON = iconForName("speaker_on.png"),
		SPEAKER_OFF = iconForName("speaker_off.png"),
		SPEAKER_NO = iconForName("speaker_no.png"),
		DIAL = iconForName("dial.png");
	
	private static Icon iconForName(String name) {
		return new ImageIcon(Image.class.getResource(name));
	}
	
	public static java.awt.Image ICON = Toolkit.getDefaultToolkit().getImage(Image.class.getResource("rds.png"));
}
