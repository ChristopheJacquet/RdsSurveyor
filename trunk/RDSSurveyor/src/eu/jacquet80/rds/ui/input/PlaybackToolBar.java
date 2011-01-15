package eu.jacquet80.rds.ui.input;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.concurrent.Semaphore;

import javax.swing.JButton;
import javax.swing.JCheckBox;

import eu.jacquet80.rds.img.Image;
import eu.jacquet80.rds.log.DefaultLogMessageVisitor;
import eu.jacquet80.rds.log.EndOfStream;
import eu.jacquet80.rds.log.GroupReceived;
import eu.jacquet80.rds.log.Log;
import eu.jacquet80.rds.log.StationLost;

public class PlaybackToolBar extends InputToolBar {
	private static final long serialVersionUID = 8528017046827475742L;

	private final static String NEXT_BUTTON = "NEXT";
	
	private final Log log;
	
	private boolean realtime = true;
	private final Semaphore waitClick = new Semaphore(0);
	private long initialTime;
	private int nbGroups = 0;

	@Override
	protected void handleButtonAction(ActionEvent e) {
		if(e.getActionCommand() == NEXT_BUTTON) {
			waitClick.release();
		}
	}
	
	public PlaybackToolBar(Log log) {
		super("Playback", "");
		
		this.log = log;
		
		final JButton btnNext = addButton("Next station", Image.FFWD, NEXT_BUTTON);
		
		final JCheckBox chkRealtime = new JCheckBox("Simulate real time", true);
		chkRealtime.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent evt) {
				realtime = chkRealtime.isSelected();
			}
		});
		addSeparator(new Dimension(20, 0));
		add(chkRealtime);
		
		log.addNewMessageListener(new DefaultLogMessageVisitor() {
			@Override
			public void visit(StationLost stationLost) {
				waitClick.acquireUninterruptibly();
			}

			@Override
			public void visit(EndOfStream endOfStream) {
				btnNext.setEnabled(false);
			}
			
			@Override
			public void visit(GroupReceived groupReceived) {
				nbGroups++;
				
				if(! realtime) return;
				
				double toWait = initialTime + nbGroups * (104*1000/1187.5) - System.currentTimeMillis();
				
				if(toWait > 0) {
					try {
						Thread.sleep((long)toWait);
					} catch (InterruptedException e) {}
				}
			}
		});
		
		initialTime = System.currentTimeMillis();
	}		
}
