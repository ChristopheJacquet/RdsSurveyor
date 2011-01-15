/*
 RDS Surveyor -- RDS decoder, analyzer and monitor tool and library.
 For more information see
   http://www.jacquet80.eu/
   http://rds-surveyor.sourceforge.net/
 
 Copyright (c) 2009, 2010 Christophe Jacquet

 This file is part of RDS Surveyor.

 RDS Surveyor is free software: you can redistribute it and/or modify
 it under the terms of the GNU Lesser Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 RDS Surveyor is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Lesser Public License for more details.

 You should have received a copy of the GNU Lesser Public License
 along with RDS Surveyor.  If not, see <http://www.gnu.org/licenses/>.

*/

package eu.jacquet80.rds.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.HashMap;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.GroupLayout;
import javax.swing.GroupLayout.ParallelGroup;
import javax.swing.GroupLayout.SequentialGroup;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import eu.jacquet80.rds.app.Application;
import eu.jacquet80.rds.core.RDS;
import eu.jacquet80.rds.core.TunedStation;
import eu.jacquet80.rds.log.ApplicationChanged;
import eu.jacquet80.rds.log.DefaultLogMessageVisitor;
import eu.jacquet80.rds.log.EndOfStream;
import eu.jacquet80.rds.log.Log;
import eu.jacquet80.rds.log.StationTuned;
import eu.jacquet80.rds.ui.app.AppPanel;
import eu.jacquet80.rds.ui.input.InputToolBar;

public class MainWindow extends JFrame {
	private static final long serialVersionUID = -5219617213305143171L;

	//private final Log log;
	private final EONTableModel eonTableModel = new EONTableModel();
	
	private final static Color BORDER_COLOR = new Color(180, 180, 180);
	
	private final JTextArea
			txtPS = new JTextArea(1, 8),
			txtPSName = new JTextArea(1, 8),
			txtPI = new JTextArea(1, 4),
			txtPTY = new JTextArea(1, 20),
			txtPTYN = new JTextArea(1, 8),
			txtDPTY = new JTextArea(1, 7),
			txtTraffic = new JTextArea(1, 5),
			txtCountry = new JTextArea(1, 20),
			txtLang = new JTextArea(1, 20),
			txtTime = new JTextArea(1, 40),
			txtAF = new JTextArea(3, 64),
			txtDynPS = new JTextArea(1, 80),
			//txtRT = new JTextArea(1, 64),
			txtCompressed = new JTextArea(1, 5),
			txtStereo = new JTextArea(1, 5),
			txtHead = new JTextArea(1, 5),
			txtPIN = new JTextArea(1, 12);
	private final JLabel txtRT = new JLabel("<html> </html>");
	private final GroupPanel groupStats = new GroupPanel();
	
	private final JTabbedPane tabbedPane = new JTabbedPane();
	
	private final JProgressBar barBLER = new JProgressBar(0, 100);
	
	private RTPanel pnlRT = new RTPanel();
	private ODAPanel pnlODA = new ODAPanel();
			
	private final JTextArea[] smallTxt = {txtPTY, txtPTYN, txtDPTY, txtTraffic, txtCountry, txtLang, txtTime, txtDynPS, txtPIN, txtCompressed, txtStereo, txtHead};
	private final JTextArea[] bigTxt = {txtPS, txtPSName, txtPI};
	private final JTable tblEON;
	private TunedStation station;
	private boolean streamFinished = false;
	
	private Map<Application, AppPanel> currentAppPanels = new HashMap<Application, AppPanel>();
	
	private void updateAppTabs() {
		if(station == null) return;
		for(Application app : station.getApplications()) {
			///System.err.println("tab for " + app.getName() + ": " + currentAppPanels.get(app));
			if(currentAppPanels.get(app) == null) {
				// application does not have a tab!
				AppPanel panel = AppPanel.forApp(app);
				if(panel != null) {
					currentAppPanels.put(app, panel);
					tabbedPane.addTab(app.getName(), panel);
					///System.err.println("add tab: " + app.getName());
				}
			}
		}
	}
	
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
	
	public MainWindow(Log log, InputToolBar toolbar) {
		super("RDS Surveyor");
		
		setLayout(new BorderLayout());
		
		JPanel globalPanel = new JPanel(new BorderLayout());
		add(globalPanel, BorderLayout.CENTER);
		
		if(toolbar != null) add(toolbar, BorderLayout.NORTH);
		
		JPanel mainPanel = new JPanel();
		BoxLayout boxLayout = new BoxLayout(mainPanel, BoxLayout.PAGE_AXIS);
		mainPanel.setLayout(boxLayout);
		globalPanel.add(mainPanel, BorderLayout.NORTH);
		
		// Main panel
		final JLabel 
				lblPTY = new JLabel("PTY"),
				lblPTYN = new JLabel("PTYN"),
				lblTraffic = new JLabel("Traffic"),
				lblCountry = new JLabel("Country"),
				lblLang = new JLabel("Language"),
				lblTime = new JLabel("Time"),
				lblPS = new JLabel("PS"),
				lblPSName = new JLabel("Station name"),
				lblPI = new JLabel("PI"),
				lblRT = new JLabel("RT"),
				lblGroupStats = new JLabel("Group statistics"),
				lblDynPS = new JLabel("Dynamic PS"),
				lblDPTY = new JLabel("PTY kind"),
				lblCompressed = new JLabel("Compressed"),
				lblHead = new JLabel("Artificial head"),
				lblStereo = new JLabel("Sound"),
				lblBLER = new JLabel("BLER"),
				lblPIN = new JLabel("PIN");
		
		
		mainPanel.add(createArrangedPanel(new Component[][] {
				{lblPS, lblPSName, lblPI, lblBLER},
				{txtPS, txtPSName, txtPI, barBLER},
		}));

		mainPanel.add(createArrangedPanel(new Component[][] {
				{lblDynPS},
				{txtDynPS},
		}));
		
		mainPanel.add(createArrangedPanel(new Component[][] {
				{lblPTY, lblDPTY, lblPTYN, lblTraffic},
				{txtPTY, txtDPTY, txtPTYN, txtTraffic},
		}));
		
		mainPanel.add(createArrangedPanel(new Component[][] {
				{lblCountry, lblLang, lblStereo, lblCompressed, lblHead},
				{txtCountry, txtLang, txtStereo, txtCompressed, txtHead},
		}));
		
		mainPanel.add(createArrangedPanel(new Component[][] {
				{lblTime, lblPIN},
				{txtTime, txtPIN},
		}));
		
		mainPanel.add(createArrangedPanel(new Component[][] {
				{lblRT},
				{txtRT},
		}));

		mainPanel.add(createArrangedPanel(new Component[][] {
				{lblGroupStats},
				{groupStats},
		}));

		
		final JPanel pnlEON = new JPanel(new BorderLayout());
		pnlEON.add(new JScrollPane(tblEON = new JTable(eonTableModel)), BorderLayout.CENTER);
		
		final JPanel pnlAF = new JPanel(new BorderLayout());
		pnlAF.add(new JScrollPane(txtAF, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED));
		
		globalPanel.add(tabbedPane, BorderLayout.CENTER);
		
		for(JTextArea txt : smallTxt) {
			txt.setFont(new Font("monospaced", Font.PLAIN, txt.getFont().getSize()));
			txt.setEditable(false);
			txt.setBorder(BorderFactory.createCompoundBorder(
					BorderFactory.createLineBorder(BORDER_COLOR, 1),
					BorderFactory.createLineBorder(Color.WHITE, 2)));
		}
		
		for(JTextArea txt : bigTxt) {
			txt.setFont(new Font("monospaced", Font.PLAIN, 20));
			txt.setEditable(false);
			txt.setBorder(BorderFactory.createCompoundBorder(
					BorderFactory.createLineBorder(BORDER_COLOR, 1),
					BorderFactory.createLineBorder(Color.WHITE, 2)));
		}
				
		txtAF.setLineWrap(true);
		txtAF.setWrapStyleWord(true);
		
		txtRT.setFont(new Font("monospaced", Font.PLAIN, txtRT.getFont().getSize()));
		txtRT.setBackground(Color.WHITE);
		txtRT.setOpaque(true);
		txtRT.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createLineBorder(BORDER_COLOR, 1),
				BorderFactory.createLineBorder(Color.WHITE, 2)));
		
		setPreferredSize(new Dimension(1000, 800));
		
		log.addNewMessageListener(new DefaultLogMessageVisitor() {
			@Override
			public void visit(StationTuned stationTuned) {
				synchronized(MainWindow.this) {
					station = stationTuned.getStation();
					eonTableModel.setTunedStation(station);
					pnlRT.setStation(station);
					pnlODA.setStation(station);
				}
					
				// reset the tabs displayed
				try {
					SwingUtilities.invokeAndWait(new Runnable() {
						public void run() {
							tabbedPane.removeAll();
							tabbedPane.addTab("EON", pnlEON);
							tabbedPane.addTab("RT", pnlRT);
							tabbedPane.addTab("AF", pnlAF);
							tabbedPane.addTab("ODA", pnlODA);
							currentAppPanels.clear();
							updateAppTabs();
						}
					});
					repaint();
				} catch (Exception e) {
					e.printStackTrace();
				}

			}
			
			@Override
			public void visit(ApplicationChanged appChanged) {
				/*final Application newApp = appChanged.getNewApplication();
				final AppPanel panel = AppPanel.forApp(newApp);
				if(panel == null) return;*/
				//currentAppPanels.put(newApp, panel);
				try {
					SwingUtilities.invokeAndWait(new Runnable() {
						public void run() {
							updateAppTabs();
						}
					});
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			
			@Override
			public void visit(EndOfStream endOfStream) {
				synchronized(MainWindow.this) {
					streamFinished = true;
				}
			}
		});
		
		new Thread(new Runnable() {
			public void run() {
				while(true) {
					try {
						Thread.sleep(500);
					} catch (InterruptedException e) {}

					synchronized(MainWindow.this) {
						if(station != null) {
							SwingUtilities.invokeLater(new Runnable() {
								public void run() {
									barBLER.setValue((int)(100. * station.getBLER()));
									
									int pi = station.getPI();
									txtPS.setText(station.getPS().getLatestCompleteOrPartialText());
									txtPSName.setText(station.getStationName());
									txtDynPS.setText(station.getDynamicPSmessage());

									txtPI.setText(String.format("%04X", pi));
									txtPTY.setText(Integer.toString(station.getPTY()) + " (" + station.getPTYlabel() + ")");
									txtPTYN.setText(station.getPTYN().toString());

									// Radiotext
									String rt = station.getRT().toStringWithHighlight();
									if(rt == null) {
										lblRT.setText("Current radiotext");
										txtRT.setText("<html> </html>");  // one space to set component height
									} else {
										lblRT.setText("Current radiotext [" + ((char)('A' + station.getRT().getFlags())) + "]");
										txtRT.setText(rt);
									}
									pnlRT.update();
									pnlODA.update();

									// Country & language
									{
										int ecc = station.getECC();
										if(pi != 0 && ecc != 0)
											txtCountry.setText(RDS.getISOCountryCode((pi>>12) & 0xF, ecc));
										else txtCountry.setText("");

										int lang = station.getLanguage();
										if(lang > 0 && lang < RDS.languages.length)
											txtLang.setText(RDS.languages[lang][0]);
										else txtLang.setText("");
									}

									txtTraffic.setText(station.trafficInfoString());

									txtTime.setText(station.getDateTime());
									txtPIN.setText(station.getPINText());
									txtAF.setText(station.afsToString());
									groupStats.update(station.numericGroupStats());

									eonTableModel.fireTableDataChanged();
									Util.packColumns(tblEON);
									
									// DI info
									txtStereo.setText(station.getStereo() ? "Stereo" : "Mono");
									txtHead.setText(station.getArtificialHead() ? "Yes" : "No");
									txtCompressed.setText(station.getCompressed() ? "Yes" : "No");
									txtDPTY.setText(station.getDPTY() ? "Dynamic" : "Static");

								};
							});

							// does not work here groupStats.update(station.numericGroupStats());
							repaint();
						}
						
						if(streamFinished) {
							// when stream finishes, update all app panels
							// one last time before exiting
							for(AppPanel p : currentAppPanels.values()) {
								p.notifyChange();
							}
							return;
						}
					}
				}
			}

		}).start();
		
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		pack();
	}

}

class GroupPanel extends JPanel {
	private static final long serialVersionUID = -8242758630674812962L;
	private final JTextField[][] txtGroup = new JTextField[17][2];
	
	public GroupPanel() {
		GridLayout layout = new GridLayout(3, 16);
		setLayout(layout);
		
		add(new JLabel(""));
		for(int i=0; i<16; i++) add(new JLabel(Integer.toString(i), JLabel.CENTER));
		add(new JLabel("Unk", JLabel.CENTER));
		for(int j=0; j<2; j++) {
			add(new JLabel(Character.toString((char)('A' + j))));
			for(int i=0; i<17; i++) {
				txtGroup[i][j] = new JTextField();
				txtGroup[i][j].setHorizontalAlignment(JTextField.CENTER);
				txtGroup[i][j].setEditable(false);
				txtGroup[i][j].setBorder(BorderFactory.createEtchedBorder());
				//txtGroup[i][j].setPreferredSize(preferredSize)
				if(!(i == 16 && j == 1)) add(txtGroup[i][j]);
			}
		}
		
		layout.setHgap(5);
		layout.setVgap(5);
	}
	
	public void update(int[][] blockCount) {
		for(int i=0; i<17; i++)
			for(int j=0; j<2; j++) {
				txtGroup[i][j].setText(Integer.toString(blockCount[i][j]));
				if(blockCount[i][j] == 0) txtGroup[i][j].setBackground(Color.GRAY);
				else txtGroup[i][j].setBackground(Color.GREEN);
			}
	}
}