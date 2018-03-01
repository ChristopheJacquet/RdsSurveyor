/*
 RDS Surveyor -- RDS decoder, analyzer and monitor tool and library.
 For more information see
   http://www.jacquet80.eu/
   http://rds-surveyor.sourceforge.net/
 
 Copyright (c) 2009, 2011 Christophe Jacquet

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

import javax.swing.JLabel;

import eu.jacquet80.rds.app.Application;
import eu.jacquet80.rds.app.oda.EN301700;

@SuppressWarnings("serial")
public class EN301700Panel extends AppPanel {
	private final JLabel lblData = new JLabel();
	private final EN301700 app;

	public EN301700Panel(Application app) {
		super(new BorderLayout());
		
		this.app = (EN301700)app;
		setApplication(app);
		
		add(lblData, BorderLayout.CENTER);
	}
	
	
	@Override
	protected void doNotifyChange() {
		int eid = app.getEnsembleId();
		int freqKHz = app.getFrequencyInKHz();
		String mode = app.getMode();
		if(eid != -1 && freqKHz != -1 && mode != null) {
			lblData.setText("<html>Cross-referenced DAB Ensemble table:<br><br><table border='0'>" +
					"<tr><td>Mode:</td><td>" + mode + "</td></tr>" +
					"<tr><td>Ensemble Id:</td><td>" + String.format("%04X", eid) + "</td></tr>" +
					"<tr><td>Frequency:</td><td>" + freqKHz + " kHz</td></tr>" +
					"</table></html>");
		} else lblData.setText("");
	}

}
