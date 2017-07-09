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

package eu.jacquet80.rds.ui.app;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Desktop;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.URISyntaxException;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.text.DefaultCaret;
import javax.swing.text.html.HTMLDocument;

import eu.jacquet80.rds.app.Application;
import eu.jacquet80.rds.app.oda.AlertC;
import eu.jacquet80.rds.app.oda.AlertC.Message;
import eu.jacquet80.rds.app.oda.tmc.TMC;
import eu.jacquet80.rds.ui.MainWindow;
import eu.jacquet80.rds.ui.Util;

@SuppressWarnings("serial")
public class AlertCPanel extends AppPanel {
	private AlertC app;
	private final MessageTableModel model = new MessageTableModel();
	private final JTable tblList;
	private boolean freezeDetails = false;
	private int latestSelectedLocation = -1;

	private final JLabel 
		lblProviderName = new JLabel(),
		lblLTN = new JLabel(),
		lblMGS = new JLabel(),
		lblAFI = new JLabel(),
		lblMode = new JLabel(),
		lblSID = new JLabel(),
		lblMessageCount = new JLabel();
	
	private final JButton btnON = new JButton("Other Networks");
	
	private JLabel[] fields = {lblProviderName, lblLTN, lblMGS, lblAFI, lblMode, lblSID, lblMessageCount};
	private Component[] infoComponents = {
			new JLabel("Provider:"), lblProviderName,
			Box.createHorizontalStrut(20),
			new JLabel("LTN:"), lblLTN,
			Box.createHorizontalStrut(20),
			new JLabel("MGS:"), lblMGS,
			Box.createHorizontalStrut(20),
			new JLabel("AFI:"), lblAFI,
			Box.createHorizontalStrut(20),
			new JLabel("Mode:"), lblMode,
			Box.createHorizontalStrut(20),
			new JLabel("SID:"), lblSID,
			Box.createHorizontalStrut(20),
			new JLabel("Messages:"), lblMessageCount,
			Box.createHorizontalStrut(40),
			btnON,
	};
	
	public AlertCPanel() {
		super(new BorderLayout());
		
		for(JLabel f : fields) {
			f.setHorizontalAlignment(SwingConstants.CENTER);
			f.setFont(new Font(MainWindow.MONOSPACED, Font.PLAIN, 20));
		}
		
		JPanel pnlInfo = new JPanel(new FlowLayout());
		
		for(Component c : infoComponents) {
			pnlInfo.add(c);
		}
		
		add(pnlInfo, BorderLayout.NORTH);
		
		final JPanel pnlMain = new JPanel(new GridLayout(1, 2, 6, 6));
		
		tblList = new JTable(model);
		final JEditorPane txtDetails = new JEditorPane("text/html", null);
		txtDetails.setEditable(false);
		String bodyRule = "body { font-family: Sans-Serif; }";
		((HTMLDocument) txtDetails.getDocument()).getStyleSheet().addRule(bodyRule);
		((DefaultCaret) txtDetails.getCaret()).setUpdatePolicy(DefaultCaret.NEVER_UPDATE);
		txtDetails.addHyperlinkListener(new HyperlinkListener() {

			@Override
			public void hyperlinkUpdate(HyperlinkEvent event) {
				if (event.getEventType()==HyperlinkEvent.EventType.ACTIVATED)
					try {
						Desktop.getDesktop().browse(event.getURL().toURI());
					} catch (URISyntaxException e) {
						// bad URI
					} catch (IOException e) {
						System.err.println("Failed to launch browser.");
					}
			}
			
		});
		
		pnlMain.add(new JScrollPane(tblList));
		final JScrollPane scrDetails = new JScrollPane(txtDetails);
		pnlMain.add(scrDetails);
		
		tblList.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		tblList.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
			
			@Override
			public void valueChanged(ListSelectionEvent evt) {
				if (freezeDetails)
					return;
				int row = tblList.getSelectedRow();
				if(row >= 0) {
					Message msg = app.getMessages().get(row);
					latestSelectedLocation = msg.getLcid();
					txtDetails.setText(msg.html());
				} else {
					txtDetails.setText("");
				}
			}
		});
		
		add(pnlMain, BorderLayout.CENTER);
		
		btnON.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent evt) {
				StringBuilder sb = new StringBuilder();
				for(String s : app.getONInfo()) {
					sb.append(s).append('\n');
				}
				
				if(sb.length() == 0) {
					sb.append("No information.");
				}
				
				JOptionPane.showMessageDialog(null, sb, "Other Networks", JOptionPane.PLAIN_MESSAGE);
			}
		});
	}
	
	public AlertCPanel(Application app) {
		this();
		setApplication(app);
	}

	@Override
	public void setApplication(Application app) {
		super.setApplication(app);
		this.app = (AlertC)app;
	}
	
	@Override
	public void doNotifyChange() {
		lblProviderName.setText(app.getProviderName());
		lblLTN.setText(app.getLTN() >= 0 ? Integer.toString(app.getLTN()) : "");
		lblMGS.setText(app.getMGS());
		lblAFI.setText(app.getAFI() >= 0 ? Integer.toString(app.getAFI()) : "");
		lblMode.setText(Integer.toString(app.getMode()));
		lblSID.setText(app.getSID() >= 0 ? Integer.toString(app.getSID()) : "");
		lblMessageCount.setText(Integer.toString(app.getMessages().size()));

		freezeDetails = true;
		model.fireTableDataChanged();
		
		// try to restore selection
		int selectedRow = model.getRowForLocation(latestSelectedLocation);
		if(selectedRow >= 0 && selectedRow < model.getRowCount()) {
			tblList.getSelectionModel().setSelectionInterval(selectedRow, selectedRow);
		}
		Util.packColumns(tblList);
		freezeDetails = false;
	}
	
	private class MessageTableModel extends AbstractTableModel {
		@Override
		public int getColumnCount() {
			return 3;
		}
		
		@Override
		public String getColumnName(int column) {
			switch(column) {
			case 0: return "Location";
			case 1: return "Events";
			case 2: return "Updates";
			default: return "ERR";
			}
		}
		
		@Override
		public boolean isCellEditable(int row, int column) {
			return false;
		}
		
		@Override
		public Object getValueAt(int row, int column) {
			AlertC.Message msg = app.getMessages().get(row);
			if(msg == null) return null;
			
			switch(column) {
			case 0:
				String ret = msg.getShortDisplayName();
				if (ret == null)
					ret = "#" + msg.getLcid();
				return ret;
			case 1:
				StringBuffer buf = new StringBuffer();
				for(int event : msg.getEvents()) {
					if(buf.length() > 0) buf.append(" / ");
					buf.append(TMC.getEvent(event).text);
				}
				return buf.toString();
			case 2: return msg.getUpdateCount();
			default: return null;
			}
		}
		
		@Override
		public int getRowCount() {
			return app.getMessages().size();
		}
		
		private int getRowForLocation(int location) {
			int row = 0;
			for(AlertC.Message m : app.getMessages()) {
				if(m.getLcid() == location) {
					return row;
				}
				row++;
			}
			
			return -1;
		}
	}
}
