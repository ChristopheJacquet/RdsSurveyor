package eu.jacquet80.rds.ui.input;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.concurrent.Semaphore;

import javax.swing.JButton;
import javax.swing.JCheckBox;

import eu.jacquet80.rds.RDSSurveyor;
import eu.jacquet80.rds.img.Image;
import eu.jacquet80.rds.log.DefaultLogMessageVisitor;
import eu.jacquet80.rds.log.EndOfStream;
import eu.jacquet80.rds.log.GroupReceived;
import eu.jacquet80.rds.log.Log;
import eu.jacquet80.rds.log.LogMessageVisitor;
import eu.jacquet80.rds.log.StationLost;

@SuppressWarnings("serial")
public class PlaybackToolBar extends InputToolBar {
	private final static String NEXT_BUTTON = "NEXT";
	private final JButton btnNext = addButton("Next station", Image.FFWD, NEXT_BUTTON);
	
	private final Log log;
	
	private boolean realtime;
	private final Semaphore waitClick = new Semaphore(0);
	private long initialTime;
	private int nbGroups = 0;
	private boolean newStream = false;
	
	private final LogMessageVisitor visitor;

	@Override
	protected void handleButtonAction(ActionEvent e) {
		if(e.getActionCommand() == NEXT_BUTTON) {
			btnNext.setEnabled(false);
			waitClick.release();
		}
	}
	
	public PlaybackToolBar(Log log) {
		super("Playback", "");
		
		this.log = log;
		
		btnNext.setEnabled(false);
		
		realtime = RDSSurveyor.preferences.getBoolean(RDSSurveyor.PREF_REALTIME, true);
		
		final JCheckBox chkRealtime = new JCheckBox("Simulate real time", realtime);
		
		chkRealtime.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent evt) {
				realtime = chkRealtime.isSelected();
				RDSSurveyor.preferences.putBoolean(RDSSurveyor.PREF_REALTIME, realtime);
			}
		});
		addSeparator(new Dimension(20, 0));
		add(chkRealtime);
		
		visitor = new DefaultLogMessageVisitor() {
			@Override
			public void visit(StationLost stationLost) {
				if(! newStream && !stationLost.isLastInStream()) {
					btnNext.setEnabled(true);
					waitClick.acquireUninterruptibly();
				} else {
					newStream = false;
				}
				initialTime = System.currentTimeMillis();
				nbGroups = 0;
			}

			
			@Override
			public void visit(EndOfStream endOfStream) {
				newStream = true;
				// If end of stream is reached, then if a new stream is fed into the system,
				// a station lost event will first be generated. But this one
				// must not wait for user input. That's what newStream is for.
			}
			
			
			@Override
			public void visit(GroupReceived groupReceived) {
				newStream = false;  // if group received, not new stream any longer
				nbGroups++;
				
				if(! realtime) return;
				
				double toWait = initialTime + nbGroups * (104*1000/1187.5) - System.currentTimeMillis();
				
				if(toWait > 0) {
					try {
						Thread.sleep((long)toWait);
					} catch (InterruptedException e) {
						System.err.println("Thread interrupted in PlaybackToolBar");
					}
				}
			}
		};
		
		log.addNewMessageListener(visitor);
		
		initialTime = System.currentTimeMillis();
	}

	@Override
	public void unregister() {
		log.removeNewMessageListener(visitor);
	}		
}
