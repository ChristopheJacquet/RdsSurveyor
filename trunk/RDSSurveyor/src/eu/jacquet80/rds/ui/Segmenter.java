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

package eu.jacquet80.rds.ui;

import java.io.PrintStream;

import eu.jacquet80.rds.core.TunedStation;
import eu.jacquet80.rds.log.DefaultLogMessageVisitor;
import eu.jacquet80.rds.log.Log;
import eu.jacquet80.rds.log.StationLost;
import eu.jacquet80.rds.log.StationTuned;

public class Segmenter {
	private final PrintStream console;
	private final Visitor visitor = new Visitor();
	private int startTime = 0;
	
	public Segmenter(PrintStream console) {
		this.console = console;
	}
	
	public void registerAtLog(Log log) {
		log.addNewMessageListener(visitor);
	}
	
	private class Visitor extends DefaultLogMessageVisitor {
		public void visit(StationLost stationLost) {
			int endTime = stationLost.getBitTime();
			TunedStation station = stationLost.getStation();
			console.println((int)(startTime/1187.5f) + "\t " + (int)((endTime-startTime)/1187.5f) + "\t " + 
					String.format("%04X", station.getPI()) + "\t " + 
					String.format("%10s", station.getTotalBlocksOk() + "/" + station.getTotalBlocks()) + "\t " +
					station.getPS() + "\t " + station.getStationName() + "\t " + station.getDynamicPSmessage());
		}

		public void visit(StationTuned stationTuned) {
			startTime = stationTuned.getBitTime();
		}
		
	}
}