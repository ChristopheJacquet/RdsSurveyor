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

package eu.jacquet80.rds.app;

import java.io.PrintStream;
import java.util.LinkedList;

import eu.jacquet80.rds.core.TunedStation;

public abstract class Application {
	protected TunedStation station = null;
	protected PrintStream console = System.out;
	private LinkedList<ChangeListener> changeListeners = new LinkedList<ChangeListener>();

	public void setStation(TunedStation station) {
		this.station = station;
	}
	
	public void setConsole(PrintStream console) {
		this.console = console;
	}
	
	public abstract void receiveGroup(int type, int version, int[] blocks, boolean[] blocksOk, int bitTime);
	public abstract String getName();

	public void addChangeListener(ChangeListener l) {
		changeListeners.addLast(l);
	}
	
	protected void fireChangeListeners() {
		for(ChangeListener l : changeListeners) {
			l.notifyChange();
		}
	}
}
