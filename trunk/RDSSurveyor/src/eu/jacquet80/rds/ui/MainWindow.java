package eu.jacquet80.rds.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;

import eu.jacquet80.rds.core.TunedStation;
import eu.jacquet80.rds.log.DefaultLogMessageVisitor;
import eu.jacquet80.rds.log.Log;
import eu.jacquet80.rds.log.StationTuned;

public class MainWindow extends JFrame {
	private static final long serialVersionUID = -5219617213305143171L;

	private final Log log;
	private final EONTableModel eonTableModel = new EONTableModel();
	private final JLabel lblPS = new JLabel();
	private final JTable tblEON;
	private TunedStation station;
	
	public MainWindow(Log log) {
		super("RDS Surveyor");
		
		this.log = log;
		
		setLayout(new BorderLayout());
		add(new JScrollPane(tblEON = new JTable(eonTableModel)), BorderLayout.CENTER);
		
		lblPS.setHorizontalAlignment(SwingConstants.CENTER);
		lblPS.setFont(new Font("monospaced", Font.PLAIN, 20));
		add(lblPS, BorderLayout.NORTH);
		
		setPreferredSize(new Dimension(1000, 800));
		
		log.addNewMessageListener(new DefaultLogMessageVisitor() {
			@Override
			public void visit(StationTuned stationTuned) {
				synchronized (MainWindow.this) {
					station = stationTuned.getStation();
					eonTableModel.setTunedStation(station);	
				}
			}
		});
		
		log.addGroupListener(new Runnable() {
			public void run() {
				synchronized(MainWindow.this) {
					lblPS.setText(station.getPS());
				}
				eonTableModel.fireTableDataChanged();
			}
			
		});
		
		pack();
	}
}
