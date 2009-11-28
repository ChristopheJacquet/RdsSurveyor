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

package eu.jacquet80.rds.oda;

import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;

import eu.jacquet80.rds.core.TunedStation;

public abstract class ODA {
	private static Map<Integer, Class<? extends ODA>> odas = new HashMap<Integer, Class<? extends ODA>>();
	protected TunedStation station = null;
	protected PrintStream console = System.out;
	
	public abstract void receiveGroup(int type, int version, int[] blocks, boolean[] blocksOk);
	public abstract String getName();
	
	public void setStation(TunedStation station) {
		this.station = station;
	}
	
	public void setConsole(PrintStream console) {
		this.console = console;
	}
	
	private static void register(int aid, Class<? extends ODA> oda) {
		odas.put(aid, oda);
	}
	
	public static ODA forAID(int aid) {
		try {
			Class<? extends ODA> theClass = odas.get(aid);
			if(theClass != null) return theClass.newInstance();
			else return null;
		} catch (InstantiationException e) {
			return null;
		} catch (IllegalAccessException e) {
			return null;
		}
	}
	
	static {
		register(0x4BD7, RTPlus.class);
		register(0xCD46, AlertC.class);
	}
}
