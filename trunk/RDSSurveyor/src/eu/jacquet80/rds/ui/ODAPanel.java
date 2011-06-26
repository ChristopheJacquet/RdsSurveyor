package eu.jacquet80.rds.ui;

import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;

import eu.jacquet80.rds.app.Application;
import eu.jacquet80.rds.core.TunedStation;

public class ODAPanel extends JPanel {
	private static final long serialVersionUID = 3861111734100852873L;

	private TunedStation station = null;
	private final List<Entry> entries = new ArrayList<Entry>();
	private final ODATableModel model = new ODATableModel();
	
	public ODAPanel() {
		super(new BorderLayout());
		add(new JScrollPane(new JTable(model)), BorderLayout.CENTER);
	}
	
	public synchronized void setStation(TunedStation station) {
		this.station = station;
		
		// new station => not yet registered ODAs
		entries.clear();
		model.fireTableDataChanged();
	}
	
	public synchronized void update() {
		if(station.getODAs().size() != entries.size()) {
			entries.clear();
			List<Integer> odas = new ArrayList<Integer>(station.getODAs());
			Collections.sort(odas);
			for(int o : odas) {
				entries.add(new Entry(o, station.getODAgroup(o), station.getODAapplication(o)));
			}
			model.fireTableDataChanged();
		}
	}
	
	private class ODATableModel extends AbstractTableModel {
		private static final long serialVersionUID = -3255005783796495910L;

		@Override
		public int getColumnCount() {
			return 3;
		}

		@Override
		public synchronized int getRowCount() {
			return entries.size();
		}

		@Override
		public Object getValueAt(int rowIndex, int columnIndex) {
			Entry e = entries.get(rowIndex);
			switch(columnIndex) {
			case 0: return e.aid;
			case 1: return e.group;
			case 2: return e.name;
			default: return "?";
			}
		}
		
		@Override
		public String getColumnName(int column) {
			switch(column) {
			case 0: return "AID";
			case 1: return "Group";
			case 2: return "Name";
			default: return "?";
			}
		}
		
	}
	
	private static class Entry {
		public final String aid;
		public final String group;
		public final String name;
		
		public Entry(int aid, int group, Application app) {
			this.aid = String.format("%04X", aid);
			if(group != 0x1F) {
				this.group = Integer.toString((group>>1) & 0xF) + (char)('A' + (group & 1));
			} else {
				this.group = "Encoder fault";
			}
			if(app != null) this.name = app.getName(); else this.name = "(Unknown)";
		}
	}
}
