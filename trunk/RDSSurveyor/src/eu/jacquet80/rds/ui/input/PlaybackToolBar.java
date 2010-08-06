package eu.jacquet80.rds.ui.input;

import java.awt.event.ActionEvent;
import java.util.concurrent.Semaphore;

import eu.jacquet80.rds.img.Image;
import eu.jacquet80.rds.log.DefaultLogMessageVisitor;
import eu.jacquet80.rds.log.Log;
import eu.jacquet80.rds.log.StationLost;

public class PlaybackToolBar extends InputToolBar {
	private static final long serialVersionUID = 8528017046827475742L;

	private final static String NEXT_BUTTON = "NEXT";
	
	private final Log log;
	
	private final Semaphore waitClick = new Semaphore(0);

	@Override
	protected void handleButtonAction(ActionEvent e) {
		if(e.getActionCommand() == NEXT_BUTTON) {
			waitClick.release();
		}
	}
	
	public PlaybackToolBar(Log log) {
		super("Playback", "");
		
		this.log = log;
		
		addButton(Image.FFWD, NEXT_BUTTON);
		
		log.addNewMessageListener(new DefaultLogMessageVisitor() {
			@Override
			public void visit(StationLost stationLost) {
				waitClick.acquireUninterruptibly();
			}
		});
	}		
}
