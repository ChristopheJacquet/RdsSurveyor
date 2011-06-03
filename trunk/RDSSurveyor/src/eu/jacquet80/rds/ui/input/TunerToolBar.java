package eu.jacquet80.rds.ui.input;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.geom.Rectangle2D;

import javax.swing.JPanel;

import eu.jacquet80.rds.img.Image;
import eu.jacquet80.rds.input.TunerGroupReader;

public class TunerToolBar extends InputToolBar {
	private static final long serialVersionUID = -5257388808546986303L;

	private final TunerGroupReader reader;
	
	private final FrequencyDisplay freqDisplay = new FrequencyDisplay();
	
	private int frequency;
	private int signal;
	private final static int MAX_SIGNAL = 65535;
	
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
		frequency = reader.getFrequency();
		signal = reader.getSignalStrength();
		
		freqDisplay.repaint();
	}
	
	public TunerToolBar(TunerGroupReader reader) {
		super("Live", reader.getDeviceName());
		
		this.reader = reader;
		
		addButton(Image.RWND, RWND_BUTTON);
		addButton(Image.DOWN, DOWN_BUTTON);
		
		addSeparator();

		add(freqDisplay);
		
		addSeparator();
		
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


	private static final Color DISPLAY_BACKGROUND = Color.BLACK;
	private static final Color DISPLAY_FOREGROUND = Color.CYAN;
	private static final Color DISPLAY_FOREGROUND_DARK = DISPLAY_FOREGROUND.darker().darker();
	private static final Font DISPLAY_FREQUENCY_FONT = new Font("Sans", Font.BOLD, 14);
	
	private class FrequencyDisplay extends JPanel {
		private static final long serialVersionUID = 3109732979840091804L;
		
		
		public FrequencyDisplay() {
			setBackground(DISPLAY_BACKGROUND);
			setMinimumSize(new Dimension(300, 30));
			setPreferredSize(new Dimension(300, 30));
			setMaximumSize(new Dimension(300, 30));
		}
		
		@Override
		protected void paintComponent(Graphics g) {
			super.paintComponent(g);
			
			g.setColor(DISPLAY_FOREGROUND);
			
			g.setFont(DISPLAY_FREQUENCY_FONT);
			String freq = String.format("%.2f", frequency/1000f);
			Rectangle2D bounds = g.getFontMetrics().getStringBounds(freq, g);
			g.drawString(freq, (getWidth() - (int)bounds.getWidth())/2, (int)bounds.getHeight()+2);
			
			int w = getWidth() - 20;
			
			int barWidth = w * signal / MAX_SIGNAL;

			g.fillRect(10, getHeight() - 8, barWidth, 6);
			
			g.setColor(DISPLAY_FOREGROUND_DARK);
			g.fillRect(10 + barWidth, getHeight() - 8, w - barWidth, 6);
		}
		
	}
	
	@Override
	public void unregister() {
		// nothing to do, this toolbar is not registered as a log listener
	}
}
