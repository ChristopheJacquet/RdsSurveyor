package eu.jacquet80.rds.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.util.List;

import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import eu.jacquet80.rds.app.Application;
import eu.jacquet80.rds.app.oda.RTPlus;
import eu.jacquet80.rds.core.TunedStation;

public class RTPanel extends JPanel {
	private static final long serialVersionUID = -8026586811311768637L;
	
	private final JList messageList = new JList();
	private TunedStation station = null;
	
	public RTPanel() {
		super(new BorderLayout());
		
		messageList.setFont(new Font("monospaced", Font.PLAIN, messageList.getFont().getSize()));
		messageList.setSelectionBackground(new Color(255, 255, 200));
		messageList.setSelectionForeground(Color.BLACK);
		
		add(new JScrollPane(messageList), BorderLayout.CENTER);
	}
	
	public void update() {
		synchronized(this) {
			String[] msg = station.getRT().getPastMessages(true).toArray(new String[]{});
			String[] data = new String[msg.length];
			
			// is there the RT+ ODA?
			List<Application> apps = station.getApplications();
			RTPlus rtplus = null;
			for(Application a : apps) {
				if(a instanceof RTPlus) {
					rtplus = (RTPlus) a;
				}
			}
			
			for(int i=0; i<msg.length; i++) {
				StringBuilder r = new StringBuilder("<html>").append(msg[i].replaceAll("\\s", "&nbsp;"));
				
				if(rtplus != null) {
					r.append("<br><font color=green>" + rtplus.getHistoryForIndex(i, msg[i])).append("</font><br>");
				}
				
				r.append("</html>");
				data[msg.length - 1 - i] = r.toString();
			}
			
			messageList.setListData(data);
		}
		messageList.repaint();
	}
	
	public synchronized void setStation(TunedStation station) {
		this.station = station;
	}

}
