package eu.jacquet80.rds.ui.input;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;

import javax.swing.JProgressBar;
import javax.swing.JTextField;

import eu.jacquet80.rds.img.Image;
import eu.jacquet80.rds.input.TunerGroupReader;

public class TunerToolBar extends InputToolBar {
	private static final long serialVersionUID = -5257388808546986303L;

	private final TunerGroupReader reader;
	
	private final JTextField txtFrequency = new JTextField();
	private final JProgressBar barSignal = new JProgressBar(0, 65535);
	
	private final static String 
		UP_BUTTON = "UP",
		DOWN_BUTTON = "DOWN",
		PLAY_BUTTON = "PLAY",
		PAUSE_BUTTON = "PAUSE",
		FFWD_BUTTON = "FFWD",
		RWND_BUTTON = "RWND";
	
	@Override
	protected void handleButtonAction(ActionEvent e) {
		if(e.getActionCommand() == FFWD_BUTTON) {
			reader.seek(true);
			update();
		} else if(e.getActionCommand() == RWND_BUTTON) {
			reader.seek(false);
			update();
		} else if(e.getActionCommand() == UP_BUTTON) {
			reader.tune(true);
			update();
		} else if(e.getActionCommand() == DOWN_BUTTON) {
			reader.tune(false);
			update();
		}
	}
	
	private synchronized void update() {
		txtFrequency.setText(Double.toString(reader.getFrequency() / 1000.));
		barSignal.setValue(reader.getSignalStrength());
	}
	
	public TunerToolBar(TunerGroupReader reader) {
		super("Live", reader.getDeviceName());
		
		this.reader = reader;
		
		addButton(Image.RWND, RWND_BUTTON);
		addButton(Image.DOWN, DOWN_BUTTON);
		
		txtFrequency.setEditable(false);
		txtFrequency.setFont(txtFrequency.getFont().deriveFont(Font.PLAIN, 30f));
		txtFrequency.setPreferredSize(new Dimension(100, 30));
		txtFrequency.setMaximumSize(new Dimension(100, Integer.MAX_VALUE));
		add(txtFrequency);
		
		barSignal.setMaximumSize(new Dimension(200, 20));		
		add(barSignal);
		
		addButton(Image.UP, UP_BUTTON);
		addButton(Image.FFWD, FFWD_BUTTON);
		
		update();
		
		new Thread() {
			public void run() {
				for(;;) {
					update();
					try {
						sleep(500);
					} catch (InterruptedException e) {}
				}
			};
		}.start();
	}

}
