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

import java.awt.LayoutManager;

import javax.swing.JPanel;

import eu.jacquet80.rds.app.Application;
import eu.jacquet80.rds.app.ChangeListener;
import eu.jacquet80.rds.app.InHouse;
import eu.jacquet80.rds.app.Paging;
import eu.jacquet80.rds.app.oda.AlertC;

public abstract class AppPanel extends JPanel implements ChangeListener {
	private static final long serialVersionUID = -6735379516008375660L;
	
	public void setApplication(Application app) {
		app.addChangeListener(this);
	}
	
	protected AppPanel(LayoutManager layout) {
		super(layout);
	}
	
	public void notifyChange() {
		repaint();
	}
	
	public static AppPanel forApp(Application app) {
		if(app instanceof Paging) return new PagingPanel(app);
		if(app instanceof AlertC) return new AlertCPanel(app);
		if(app instanceof InHouse) return new InHousePanel(app);
		return null;
	}
}
