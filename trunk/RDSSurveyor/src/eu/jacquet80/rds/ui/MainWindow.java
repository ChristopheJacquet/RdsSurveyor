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
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.util.Date;

import javax.swing.BoxLayout;
import javax.swing.GroupLayout;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.GroupLayout.ParallelGroup;
import javax.swing.GroupLayout.SequentialGroup;

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
	private final JTextArea
			txtPS = new JTextArea(1, 8),
			txtPSName = new JTextArea(1, 8),
			txtPI = new JTextArea(1, 4),
			txtPTY = new JTextArea(1, 20),
			txtPTYN = new JTextArea(1, 8),
			txtTraffic = new JTextArea(1, 5),
			txtCountry = new JTextArea(1, 20),
			txtLang = new JTextArea(1, 20),
			txtTime = new JTextArea(1, 30),
			txtRTa = new JTextArea(1, 64),
			txtRTb = new JTextArea(1, 64),
			txtAF = new JTextArea(3, 64),
			txtGroupStats = new JTextArea(1, 64);
	private final JTextArea[] smallTxt = {txtPTY, txtPTYN, txtTraffic, txtCountry, txtLang, txtTime, txtRTa, txtRTb};
	private final JTextArea[] bigTxt = {txtPS, txtPSName, txtPI};
	private final JTable tblEON;
	private TunedStation station;
	
	private static JPanel createArrangedPanel(Component[][] components) {
		JPanel panel = new JPanel();
		GroupLayout layout = new GroupLayout(panel);
		
		panel.setLayout(layout);
		layout.setAutoCreateGaps(true);
		layout.setAutoCreateContainerGaps(true);
		
		SequentialGroup horiz = layout.createSequentialGroup();
		for(int h = 0; h < components[0].length; h++) {
			ParallelGroup p = layout.createParallelGroup(GroupLayout.Alignment.LEADING);
			for(int v = 0; v < components.length; v++)
				p.addComponent(components[v][h]);
			horiz.addGroup(p);
		}
		layout.setHorizontalGroup(horiz);
		
		SequentialGroup vert = layout.createSequentialGroup();
		for(int v = 0; v < components.length; v++) {
			ParallelGroup p = layout.createParallelGroup(GroupLayout.Alignment.BASELINE);
			for(int h = 0; h < components[v].length; h++)
				p.addComponent(components[v][h]);
			vert.addGroup(p);
		}
		layout.setVerticalGroup(vert);
		
		return panel;
	}
	
	public MainWindow(Log log) {
		super("RDS Surveyor");
		
		this.log = log;
		
		final JTabbedPane tabbedPane = new JTabbedPane();
		
		setLayout(new BorderLayout());
		
		JPanel mainPanel = new JPanel();
		BoxLayout boxLayout = new BoxLayout(mainPanel, BoxLayout.PAGE_AXIS);
		mainPanel.setLayout(boxLayout);
		add(mainPanel, BorderLayout.NORTH);
		
		// Main panel
		final JLabel 
				lblPTY = new JLabel("PTY"),
				lblPTYN = new JLabel("PTYN"),
				lblTraffic = new JLabel("Traffic"),
				lblCountry = new JLabel("Country"),
				lblLang = new JLabel("Language"),
				lblTime = new JLabel("Time"),
				lblRTa = new JLabel("RTA"),
				lblRTb = new JLabel("RTB"),
				lblPS = new JLabel("PS"),
				lblPSName = new JLabel("Station name"),
				lblPI = new JLabel("PI"),
				lblAF = new JLabel("Alternative Frequencies"),
				lblGroupStats = new JLabel("Group statistics");
		
		
		mainPanel.add(createArrangedPanel(new Component[][] {
				{lblPS, lblPSName, lblPI},
				{txtPS, txtPSName, txtPI},
		}));

		mainPanel.add(createArrangedPanel(new Component[][] {
				{lblPTY, lblPTYN, lblTraffic},
				{txtPTY, txtPTYN, txtTraffic},
		}));
		
		mainPanel.add(createArrangedPanel(new Component[][] {
				{lblCountry, lblLang},
				{txtCountry, txtLang},
		}));
		
		mainPanel.add(createArrangedPanel(new Component[][] {
				{lblTime},
				{txtTime},
		}));
		
		mainPanel.add(createArrangedPanel(new Component[][] {
				{lblRTa, txtRTa},
				{lblRTb, txtRTb},
		}));

		mainPanel.add(createArrangedPanel(new Component[][] {
				{lblAF},
				{txtAF},
		}));

		mainPanel.add(createArrangedPanel(new Component[][] {
				{lblGroupStats},
				{txtGroupStats},
		}));

		
		final JPanel pnlEON = new JPanel(new BorderLayout());
		pnlEON.add(new JScrollPane(tblEON = new JTable(eonTableModel)), BorderLayout.CENTER);
		
		add(tabbedPane, BorderLayout.CENTER);
		
		for(JTextArea txt : smallTxt) {
			txt.setFont(new Font("monospaced", Font.PLAIN, txt.getFont().getSize()));
			txt.setEditable(false);
		}
		
		for(JTextArea txt : bigTxt) {
			txt.setFont(new Font("monospaced", Font.PLAIN, 20));
			txt.setEditable(false);
		}
		
		txtGroupStats.setLineWrap(true);
		txtGroupStats.setWrapStyleWord(true);
		
		txtAF.setLineWrap(true);
		txtAF.setWrapStyleWord(true);
		
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
					if(station != null) {
						txtPS.setText(station.getPS());
						txtPSName.setText(station.getStationName());
						txtPI.setText(String.format("%04X", station.getPI()));
						txtPTY.setText(Integer.toString(station.getPTY()) + " (" + station.getPTYlabel() + ")");
						txtPTYN.setText(station.getPTYN());
						txtRTa.setText(station.getRT(0));
						txtRTb.setText(station.getRT(1));
						txtTraffic.setText(station.trafficInfoString());
						if(station.whichRT() == 0) {
							lblRTa.setForeground(Color.RED);
							lblRTb.setForeground(Color.BLACK);
						} else if(station.whichRT() == 1) {
							lblRTa.setForeground(Color.BLACK);
							lblRTb.setForeground(Color.RED);							
						} else {
							lblRTa.setForeground(Color.BLACK);
							lblRTb.setForeground(Color.BLACK);
						}
						
						Date date = station.getDate();
						txtTime.setText(date != null ? date.toString() : "");
						txtAF.setText(station.afsToString());
						txtGroupStats.setText(station.groupStats());
					}
				}
				eonTableModel.fireTableDataChanged();
				Util.packColumns(tblEON);
			}
			
		});
		
		pack();
		
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	}
}
