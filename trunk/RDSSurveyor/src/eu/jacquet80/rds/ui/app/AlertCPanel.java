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
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;

import javax.swing.Box;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import eu.jacquet80.rds.app.Application;
import eu.jacquet80.rds.app.oda.AlertC;

public class AlertCPanel extends AppPanel {
	private static final long serialVersionUID = 4835434126469108572L;
	private AlertC app;

	private final JLabel 
		lblProviderName = new JLabel(),
		lblLTN = new JLabel(),
		lblMGS = new JLabel(),
		lblAFI = new JLabel(),
		lblMode = new JLabel(),
		lblSID = new JLabel();
	
	private JLabel[] fields = {lblProviderName, lblLTN, lblMGS, lblAFI, lblMode, lblSID};
	private Component[] infoComponents = {
			new JLabel("Provider"), lblProviderName,
			Box.createHorizontalStrut(20),
			new JLabel("LTN"), lblLTN,
			Box.createHorizontalStrut(20),
			new JLabel("MGS"), lblMGS,
			Box.createHorizontalStrut(20),
			new JLabel("AFI"), lblAFI,
			Box.createHorizontalStrut(20),
			new JLabel("Mode"), lblMode,
			Box.createHorizontalStrut(20),
			new JLabel("SID"), lblSID
	};
	
	public AlertCPanel() {
		super(new BorderLayout());
		
		for(JLabel f : fields) {
			f.setHorizontalAlignment(SwingConstants.CENTER);
			f.setFont(new Font("monospaced", Font.PLAIN, 20));
		}
		
		JPanel pnlInfo = new JPanel(new FlowLayout());
		
		for(Component c : infoComponents) {
			pnlInfo.add(c);
		}
		
		add(pnlInfo, BorderLayout.NORTH);
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
	}
}
