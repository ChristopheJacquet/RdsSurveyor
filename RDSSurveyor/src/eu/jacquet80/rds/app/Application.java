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
