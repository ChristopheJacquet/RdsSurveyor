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

import javax.swing.table.AbstractTableModel;

import eu.jacquet80.rds.core.Station;
import eu.jacquet80.rds.core.TunedStation;

public class EONTableModel extends AbstractTableModel {
	private static final long serialVersionUID = 7466888480643372873L;
	
	private static final String[] columnNames = {"PI", "PS*", "PTY", "Traffic", "AF"};
	
	private TunedStation station = null;
	
	public void setTunedStation(TunedStation station) {
		this.station = station;
	}

	public Class<?> getColumnClass(int columnIndex) {
		return String.class;
	}

	public int getColumnCount() {
		return columnNames.length;
	}

	public String getColumnName(int columnIndex) {
		return columnNames[columnIndex];
	}

	public int getRowCount() {
		return station != null ? station.getONcount() : 0;
	}

	public Object getValueAt(int rowIndex, int columnIndex) {
		Station on = station != null ? station.getONbyIndex(rowIndex) : null;
		if(on == null) return null;
		
		switch(columnIndex) {
		case 0: return String.format("%04X", on.getPI());
		case 1: return on.getStationName();
		case 2: return on.getPTY() + " (" + on.getPTYlabel() + ")";
		case 3: return on.trafficInfoString();
		case 4: return on.afsToString();
		default: return null;
		}
	}

	public boolean isCellEditable(int rowIndex, int columnIndex) {
		return false;
	}

}
