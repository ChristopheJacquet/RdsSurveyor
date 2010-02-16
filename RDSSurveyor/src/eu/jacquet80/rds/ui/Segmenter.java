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
