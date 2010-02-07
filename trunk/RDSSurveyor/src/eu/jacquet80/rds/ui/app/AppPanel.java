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
