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
