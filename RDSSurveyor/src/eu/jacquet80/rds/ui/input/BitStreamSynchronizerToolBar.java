package eu.jacquet80.rds.ui.input;

import java.awt.event.ActionEvent;

import javax.swing.JLabel;

import eu.jacquet80.rds.core.BitStreamSynchronizer;
import eu.jacquet80.rds.core.BitStreamSynchronizer.Status;

@SuppressWarnings("serial")
public class BitStreamSynchronizerToolBar extends InputToolBar {

	public BitStreamSynchronizerToolBar(BitStreamSynchronizer bsd) {
		super();
		
		final JLabel lblStatus = new JLabel("");
		add(lblStatus);
		
		bsd.addStatusChangeListener(new BitStreamSynchronizer.StatusChangeListener() {
			
			@Override
			public void report(Status status) {
				lblStatus.setText(status.toString());
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
