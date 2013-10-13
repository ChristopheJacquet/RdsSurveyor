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
import eu.jacquet80.rds.app.oda.CatalunyaRadioTDC;
import eu.jacquet80.rds.app.oda.EN301700;
import eu.jacquet80.rds.app.oda.ITunesTagging;

public abstract class AppPanel extends JPanel implements ChangeListener {
	private static final long serialVersionUID = -6735379516008375660L;
	
	public void setApplication(Application app) {
		app.addChangeListener(this);
	}
	
	protected AppPanel(LayoutManager layout) {
		super(layout);
	}
	
	private long previousTime = 0;
	
	public final void notifyChange() {
		long currentTime = System.currentTimeMillis();
		if(currentTime - previousTime > 500) {  // refresh every 1s only
			previousTime = currentTime;
			doNotifyChange();
		}
	}
	
	protected abstract void doNotifyChange();

	public static AppPanel forApp(Application app) {
		if(app instanceof Paging) return new PagingPanel(app);
		if(app instanceof AlertC) return new AlertCPanel(app);
		if(app instanceof InHouse) return new InHousePanel(app);
		if(app instanceof EN301700) return new EN301700Panel(app);
		if(app instanceof CatalunyaRadioTDC) return new CatalunyaRadioTDCPanel(app);
		if(app instanceof ITunesTagging) return new ITunesTaggingPanel(app);
		return null;
	}
}
