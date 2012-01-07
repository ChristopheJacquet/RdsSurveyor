package eu.jacquet80.rds.ui.input;

import java.awt.Color;
import java.awt.event.ActionEvent;

import javax.swing.JLabel;

import eu.jacquet80.rds.core.BitStreamSynchronizer;
import eu.jacquet80.rds.core.BitStreamSynchronizer.Status;

@SuppressWarnings("serial")
public class BitStreamSynchronizerToolBar extends InputToolBar {
	final static Color GREEN_OK_COLOR = new Color(0, 140, 0);

	public BitStreamSynchronizerToolBar(BitStreamSynchronizer bsd) {
		super();
		
		final JLabel lblStatus = new JLabel("");
		add(lblStatus);
		
		bsd.addStatusChangeListener(new BitStreamSynchronizer.StatusChangeListener() {
			
			@Override
			public void report(Status status) {
				String text;
				Color color;
				
				switch(status) {
				case SYNCED:
					text = "<html><br>Synced</html>";
					color = GREEN_OK_COLOR;
					break;
					
				default:
				case NOT_SYNCED:
					text = "<html>Not<br>Synced</html>";
					color = Color.RED;
					break;
				}
				lblStatus.setText(text);
				lblStatus.setForeground(color);
			}
		});
	}
	
	@Override
	protected void handleButtonAction(ActionEvent e) {
		// TODO Auto-generated method stub

	}

	@Override
	public void unregister() {
		// TODO Auto-generated method stub

	}

}
