package eu.jacquet80.rds.ui.input;

import java.awt.event.ActionEvent;
import java.util.Locale;

import javax.swing.JLabel;

import eu.jacquet80.rds.input.LiveAudioBitReader;

@SuppressWarnings("serial")

public class LiveAudioToolBar extends InputToolBar {

	@Override
	protected void handleButtonAction(ActionEvent e) {
		// TODO Auto-generated method stub

	}

	@Override
	public void unregister() {
		// TODO Auto-generated method stub

	}

	public LiveAudioToolBar(LiveAudioBitReader reader) {
		final JLabel lblStatus = new JLabel("");
		add(lblStatus);
		
		reader.addStatusListener(new LiveAudioBitReader.StatusListener() {
			@Override
			public void report(double clockFrequency) {
				lblStatus.setText(String.format(Locale.US, "Clock: %.1f Hz", clockFrequency));
			}
		});
	}
}
