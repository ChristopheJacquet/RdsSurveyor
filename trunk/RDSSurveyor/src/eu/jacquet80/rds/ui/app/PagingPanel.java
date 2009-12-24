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

import javax.swing.JScrollPane;
import javax.swing.JTable;
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

	@Override
	public void setApplication(Application app) {
		super.setApplication(app);
		pagingApp = (Paging)app;
	}

	public PagingPanel() {
		super(new BorderLayout());
		tableModel = new PagingTableModel();
		table = new JTable(tableModel);
		table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		add(new JScrollPane(table), BorderLayout.CENTER);
	}
	
	public PagingPanel(Application app) {
		this();
		setApplication(app);
	}
	
	private class PagingTableModel extends AbstractTableModel {
		private static final long serialVersionUID = 4494094179345299991L;
		
		private final String[] columnNames = {"Time", "Address", "Type", "Contents"};

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
				case 0: return m.getTime();
				case 1: return m.getAddress();
				case 2: return m.getType().toString();
				case 3: return m.getContents();
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

	}
}
