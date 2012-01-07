package eu.jacquet80.rds.ui.input;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.util.Locale;

import javax.swing.JLabel;

import eu.jacquet80.rds.input.LiveAudioBitReader;

@SuppressWarnings("serial")

public class LiveAudioToolBar extends InputToolBar {

	private boolean active = true;
	
	@Override
	protected void handleButtonAction(ActionEvent e) {
		// no button
	}

	@Override
	public void unregister() {
		active = false;    // stop update thread
	}

	public LiveAudioToolBar(final LiveAudioBitReader reader) {
		final JLabel lblStatus = new JLabel("");
		lblStatus.setPreferredSize(new Dimension(100, 30));
		add(lblStatus);
		
		new Thread() {
			public void run() {
				while(active) {
					// update label
					double clockFrequency = reader.getClockFrequency();
					if(clockFrequency >= 0) {
						lblStatus.setText(String.format(Locale.US, "<html>Clock:<br>%.1f Hz</html>", clockFrequency));
					} else {
						lblStatus.setText("<html>Clock:<br>None</html>");
					}
					
					if(clockFrequency > 1187 && clockFrequency < 1188) {
						lblStatus.setForeground(BitStreamSynchronizerToolBar.GREEN_OK_COLOR);
					} else {
						lblStatus.setForeground(Color.RED);
					}
					
					try {
						sleep(1000);
					} catch (InterruptedException e) {}
				}
			};
		}.start();
	}
}
