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

package eu.jacquet80.rds.ui.app;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.AbstractTableModel;

import eu.jacquet80.rds.app.Application;
import eu.jacquet80.rds.app.Paging;
import eu.jacquet80.rds.app.Paging.Message;
import eu.jacquet80.rds.ui.Util;

public class PagingPanel extends AppPanel {
	private static final long serialVersionUID = -1835993971276718953L;
	
	private Paging pagingApp;
	private final PagingTableModel tableModel;
	private final JTable table;
	private final JTextField txtInterval;
	private final JLabel lblTNGD;

	@Override
	public void setApplication(Application app) {
		super.setApplication(app);
		pagingApp = (Paging)app;
		lblTNGD.setText(pagingApp.getTNGD());
	}

	public PagingPanel() {
		super(new BorderLayout());
		tableModel = new PagingTableModel();
		table = new JTable(tableModel);
		table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		JPanel pnlTable = new JPanel(new BorderLayout());
		pnlTable.add(new JScrollPane(table), BorderLayout.CENTER);
		pnlTable.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		add(pnlTable, BorderLayout.CENTER);
		
		JPanel pnlTop = new JPanel();
		pnlTop.setLayout(new BoxLayout(pnlTop, BoxLayout.LINE_AXIS));
		pnlTop.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		add(pnlTop, BorderLayout.NORTH);
		
		txtInterval = new JTextField(); //, SwingConstants.CENTER);
		txtInterval.setEditable(false);
		txtInterval.setPreferredSize(new Dimension(50, 20));
		txtInterval.setMaximumSize(new Dimension(50, 20));
		txtInterval.setBorder(BorderFactory.createEtchedBorder());
		txtInterval.setHorizontalAlignment(JTextField.CENTER);
		
		lblTNGD = new JLabel();
		
		pnlTop.add(lblTNGD);
		pnlTop.add(Box.createHorizontalGlue());
		pnlTop.add(new JLabel("Interval: "));
		pnlTop.add(txtInterval);
	}
	
	public PagingPanel(Application app) {
		this();
		setApplication(app);
	}
	
	private class PagingTableModel extends AbstractTableModel {
		private static final long serialVersionUID = 4494094179345299991L;
		
		private final String[] columnNames = {"A/B", "Time", "Address", "Type", "Contents"};

		public int getColumnCount() {
			return columnNames.length;
		}

		public int getRowCount() {
			if(pagingApp == null) return 0;
			
			synchronized(pagingApp) {
				return pagingApp.getMessages().size();
			}
		}

		public Object getValueAt(int rowIndex, int columnIndex) {
			if(pagingApp == null) return "";
			
			synchronized(pagingApp) {
				Message m = pagingApp.getMessages().get(rowIndex);
				switch(columnIndex) {
				case 0: return (char)('A' + m.getAB());
				case 1: return m.getTime();
				case 2: return m.getAddress();
				case 3: return m.getType().toString();
				case 4: return m.getContents();
				default: return "ERR";
				}
			}
		}
		
		@Override
		public String getColumnName(int column) {
			if(column >=0 && column < columnNames.length)
				return columnNames[column];
			else return "";
		}
	}

	@Override
	public void notifyChange() {
		tableModel.fireTableDataChanged();
		Util.packColumns(table);
		
		String  interval;
		if(pagingApp.getInterval() >= 0) {
			interval = Integer.toString(pagingApp.getInterval());
			txtInterval.setBackground(Color.GREEN);
			txtInterval.setForeground(Color.BLACK);
		} else if(pagingApp.getInterval() == Integer.MIN_VALUE) {
			interval = "ERR";
			txtInterval.setBackground(Color.RED);
			txtInterval.setForeground(Color.WHITE);
		} else {
			interval = Integer.toString(- pagingApp.getInterval() - 1);
			txtInterval.setBackground(Color.YELLOW);
			txtInterval.setForeground(Color.BLACK);
		}
		
		txtInterval.setText(interval);
	}
}
