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
import java.awt.Font;

import javax.swing.AbstractListModel;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;

import eu.jacquet80.rds.app.Application;
import eu.jacquet80.rds.app.InHouse;
import eu.jacquet80.rds.ui.MainWindow;

@SuppressWarnings("serial")
public class InHousePanel extends AppPanel {
	private final JList<String> list = new JList<String>();
	private final IHListModel listModel;
	
	private InHouse app = null;
	
	public InHousePanel() {
		super(new BorderLayout());
		listModel = new IHListModel();
		list.setModel(listModel);
		list.setLayoutOrientation(JList.VERTICAL_WRAP);
		Font f = new Font(MainWindow.MONOSPACED, Font.PLAIN, list.getFont().getSize());
		list.setFont(f);
		add(new JScrollPane(list), BorderLayout.CENTER);
	}
	
	public InHousePanel(Application appl) {
		this();
		setApplication(appl);
	}

	@Override
	public void setApplication(Application appl) {
		super.setApplication(appl);
		synchronized(this) {
			this.app = (InHouse)appl;
		}
	}

	@Override
	public void doNotifyChange() {
		for(ListDataListener l : listModel.getListDataListeners()) {
			l.intervalAdded(new ListDataEvent(this, ListDataEvent.INTERVAL_ADDED, app.getMessageCount()-1, app.getMessageCount()-1));
		}
		
	}
	
	private class IHListModel extends AbstractListModel<String> {
		@Override
		public int getSize() {
			synchronized(InHousePanel.this) {
				if(app != null) return app.getMessageCount();
				else return 0;
			}
		}
		
		@Override
		public String getElementAt(int index) {
			synchronized(InHousePanel.this) {
				if(app != null) return app.getMessage(index);
				else return "ERR";
			}
		}
	}
}
