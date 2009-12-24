/*
 RDS Surveyor -- RDS decoder, analyzer and monitor tool and library.
 For more information see
   http://www.jacquet80.eu/
   http://rds-surveyor.sourceforge.net/
 
 Copyright (c) 2009 Christophe Jacquet

 Permission is hereby granted, free of charge, to any person
 obtaining a copy of this software and associated documentation
 files (the "Software"), to deal in the Software without
 restriction, including without limitation the rights to use,
 copy, modify, merge, publish, distribute, sublicense, and/or sell
 copies of the Software, and to permit persons to whom the
 Software is furnished to do so, subject to the following
 conditions:

 The above copyright notice and this permission notice shall be
 included in all copies or substantial portions of the Software.

 THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 OTHER DEALINGS IN THE SOFTWARE.
*/

package eu.jacquet80.rds.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

import eu.jacquet80.rds.app.Application;
import eu.jacquet80.rds.core.TunedStation;
import eu.jacquet80.rds.log.ApplicationChanged;
import eu.jacquet80.rds.log.DefaultLogMessageVisitor;
import eu.jacquet80.rds.log.Log;
import eu.jacquet80.rds.log.StationTuned;
import eu.jacquet80.rds.ui.app.AppPanel;

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
		
		final JTabbedPane tabbedPane = new JTabbedPane();
		
		setLayout(new BorderLayout());
		
		final JPanel pnlEON = new JPanel(new BorderLayout());
		pnlEON.add(new JScrollPane(tblEON = new JTable(eonTableModel)), BorderLayout.CENTER);
		
		add(tabbedPane, BorderLayout.CENTER);
		
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
					
					// reset the tabs displayed
					SwingUtilities.invokeLater(new Runnable() {
						public void run() {
							tabbedPane.removeAll();
							tabbedPane.addTab("EON", pnlEON);
						}
					});
				}
			}
			
			@Override
			public void visit(ApplicationChanged appChanged) {
				final Application newApp = appChanged.getNewApplication();
				final AppPanel panel = AppPanel.forApp(newApp);
				if(panel == null) return;
				SwingUtilities.invokeLater(new Runnable() {
					public void run() {
						tabbedPane.addTab(newApp.getName(), panel);
					};
				});
			}
		});
		
		log.addGroupListener(new Runnable() {
			public void run() {
				synchronized(MainWindow.this) {
					if(station != null) lblPS.setText(station.getPS());
				}
				eonTableModel.fireTableDataChanged();
				Util.packColumns(tblEON);
			}
			
		});
		
		pack();
	}
}
