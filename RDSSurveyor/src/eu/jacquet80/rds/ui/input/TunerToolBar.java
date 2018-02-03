/*
 RDS Surveyor -- RDS decoder, analyzer and monitor tool and library.
 For more information see
   http://www.jacquet80.eu/
   http://rds-surveyor.sourceforge.net/
 
 Copyright (c) 2009-2012 Christophe Jacquet

 This file is part of RDS Surveyor.

 RDS Surveyor is free software: you can redistribute it and/or modify
 it under the terms of the GNU Lesser Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 RDS Surveyor is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Lesser Public License for more details.

 You should have received a copy of the GNU Lesser Public License
 along with RDS Surveyor.  If not, see <http://www.gnu.org/licenses/>.

*/


package eu.jacquet80.rds.ui.input;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Rectangle2D;

import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import eu.jacquet80.rds.RDSSurveyor;
import eu.jacquet80.rds.img.Image;
import eu.jacquet80.rds.input.TunerGroupReader;

@SuppressWarnings("serial")
public class TunerToolBar extends InputToolBar {
	private final TunerGroupReader reader;
	
	private final FrequencyDisplay freqDisplay = new FrequencyDisplay();
	
	private int frequency;
	private int signal;
	private boolean synced;
	private boolean stereo; 
	private final static int MAX_SIGNAL = 65535;
	private boolean active = true;
	
	private final static String 
		UP_BUTTON = "UP",
		DOWN_BUTTON = "DOWN",
		FFWD_BUTTON = "FFWD",
		RWND_BUTTON = "RWND",
		TUNE_BUTTON = "TUNE",
		SPEAKER_BUTTON = "SPKR";
	
	private final JButton speakerButton;
	
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
		} else if(e.getActionCommand() == TUNE_BUTTON) {
			tune();
		} else if(e.getActionCommand() == SPEAKER_BUTTON) {
			toggleAudio();
		}
	}
	
	private void tune() {
		String res = JOptionPane.showInputDialog(null, "Enter frequency to tune to", Double.toString(frequency/1000.));
		if (res == null)
			return;
		try {
			double freq = Double.parseDouble(res);
			if(freq >= 87.5 && freq <= 108.) {
				reader.setFrequency((int)(freq * 1000));
			}
		} catch(NumberFormatException e) {}
		
		update();
	}
	
	private void toggleAudio() {
		if(reader.isPlayingAudio()) {
			reader.mute();
			speakerButton.setIcon(Image.SPEAKER_ON);
		} else {
			reader.unmute();
			speakerButton.setIcon(Image.SPEAKER_OFF);
		}
	}
	
	private synchronized void update() {
		frequency = reader.getFrequency();
		signal = reader.getSignalStrength();
		synced = reader.isSynchronized();
		stereo = reader.isStereo();
		
		freqDisplay.repaint();
		
		RDSSurveyor.preferences.putInt(RDSSurveyor.PREF_TUNER_FREQ, frequency);
	}
	
	public TunerToolBar(TunerGroupReader reader) {
		super("Live", reader.getDeviceName());
		
		this.reader = reader;
		
		reader.setFrequency(RDSSurveyor.preferences.getInt(RDSSurveyor.PREF_TUNER_FREQ, 105500));
		
		addButton(Image.RWND, RWND_BUTTON, KeyEvent.VK_DOWN);
		addButton(Image.DOWN, DOWN_BUTTON, KeyEvent.VK_LEFT);
		
		addSeparator();

		add(freqDisplay);
		
		addSeparator();
		
		addButton(Image.UP, UP_BUTTON, KeyEvent.VK_RIGHT);
		addButton(Image.FFWD, FFWD_BUTTON, KeyEvent.VK_UP);
		
		addSeparator();
		
		speakerButton = addButton(
				reader.isPlayingAudio() ? Image.SPEAKER_OFF : Image.SPEAKER_ON, 
				SPEAKER_BUTTON, KeyEvent.VK_A);
		speakerButton.setDisabledIcon(Image.SPEAKER_NO);
		if(!reader.isAudioCapable()) speakerButton.setEnabled(false);

		addSeparator();
		
		addButton(Image.DIAL, TUNE_BUTTON, KeyEvent.VK_T);
		
		freqDisplay.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent event) {
				if(event.getClickCount() >= 2) {
					tune();
				}
			}
		});
		
		update();
		
		new Thread() {
			public void run() {
				while(active) {
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
	private static final Font DISPLAY_INDICATORS_FONT = new Font("Sans", Font.BOLD, 10);
	
	private class FrequencyDisplay extends JPanel {
		private static final long serialVersionUID = 3109732979840091804L;
		
		
		public FrequencyDisplay() {
			setBackground(DISPLAY_BACKGROUND);
			setMinimumSize(new Dimension(300, 30));
			setPreferredSize(new Dimension(300, 30));
			setMaximumSize(new Dimension(300, 30));
		}
		
		@Override
		protected void paintComponent(Graphics g_) {
			super.paintComponent(g_);
			
			Graphics2D g = (Graphics2D) g_;
			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			
			g.setColor(DISPLAY_FOREGROUND);
			
			g.setFont(DISPLAY_FREQUENCY_FONT);
			String freq = String.format("%.1f", frequency/1000f);
			Rectangle2D bounds = g.getFontMetrics().getStringBounds(freq, g);
			g.drawString(freq, (getWidth() - (int)bounds.getWidth())/2, (int)bounds.getHeight()+2);
			
			int w = getWidth() - 20;
			
			int barWidth = w * signal / MAX_SIGNAL;

			g.fillRect(10, getHeight() - 8, barWidth, 6);
			
			g.setColor(DISPLAY_FOREGROUND_DARK);
			g.fillRect(10 + barWidth, getHeight() - 8, w - barWidth, 6);
			
			g.setFont(DISPLAY_INDICATORS_FONT);
			g.setColor(synced ? DISPLAY_FOREGROUND : DISPLAY_FOREGROUND_DARK);
			g.drawString("RDS", getWidth() - 10 - g.getFontMetrics().stringWidth("RDS"), 16);
			
			g.setColor(stereo ? DISPLAY_FOREGROUND : DISPLAY_FOREGROUND_DARK);
			g.drawString("STEREO", 10, 16);
		}
		
	}
	
	@Override
	public void unregister() {
		active = false;   // stop update thread
		// this toolbar is not registered as a log listener
	}
}
