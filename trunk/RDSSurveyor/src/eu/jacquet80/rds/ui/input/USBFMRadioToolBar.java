package eu.jacquet80.rds.ui.input;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JProgressBar;
import javax.swing.JTextField;

import eu.jacquet80.rds.img.Image;
import eu.jacquet80.rds.input.USBFMRadioGroupReader;

public class USBFMRadioToolBar extends InputToolBar {
	private static final long serialVersionUID = -5257388808546986303L;

	private final USBFMRadioGroupReader reader;
	
	private final JTextField txtFrequency = new JTextField();
	private final JProgressBar barSignal = new JProgressBar(0, 63);
	
	private final static String 
		UP_BUTTON = "UP",
		DOWN_BUTTON = "DOWN",
		PLAY_BUTTON = "PLAY",
		PAUSE_BUTTON = "PAUSE",
		FFWD_BUTTON = "FFWD",
		RWND_BUTTON = "RWND";
	
	private ActionListener buttonListener = new ActionListener() {
		@Override
		public void actionPerformed(ActionEvent e) {
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
	};
	
	private synchronized void update() {
		txtFrequency.setText(Double.toString(reader.getFrequency() / 1000.));
		barSignal.setValue(reader.getSignal());
	}
	
	public USBFMRadioToolBar(USBFMRadioGroupReader reader) {
		super("Live", "USBFMRADIO");
		
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
	
	private void addButton(Icon icon, String command) {
		JButton button = new JButton(icon);
		button.setActionCommand(command);
		button.addActionListener(buttonListener);
		add(button);
	}
}
