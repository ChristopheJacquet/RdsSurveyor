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
import java.awt.Font;

import javax.swing.JLabel;
import javax.swing.SwingConstants;

import eu.jacquet80.rds.app.Application;
import eu.jacquet80.rds.app.oda.AlertC;

public class AlertCPanel extends AppPanel {
	private static final long serialVersionUID = 4835434126469108572L;
	private AlertC app;

	private final JLabel lblProviderName = new JLabel();
	
	public AlertCPanel() {
		super(new BorderLayout());
		
		lblProviderName.setHorizontalAlignment(SwingConstants.CENTER);
		lblProviderName.setFont(new Font("monospaced", Font.PLAIN, 20));
		add(lblProviderName, BorderLayout.NORTH);
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
	public void notifyChange() {
		lblProviderName.setText(app.getProviderName());
	}
}
