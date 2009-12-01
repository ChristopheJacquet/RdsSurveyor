package eu.jacquet80.rds.ui;

import java.io.PrintStream;

import eu.jacquet80.rds.core.TunedStation;
import eu.jacquet80.rds.log.ClockTime;
import eu.jacquet80.rds.log.EONReturn;
import eu.jacquet80.rds.log.EONSwitch;
import eu.jacquet80.rds.log.Log;
import eu.jacquet80.rds.log.LogMessageVisitor;
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
	
	private class Visitor implements LogMessageVisitor {
		public void visit(ClockTime clockTime) {
		}

		public void visit(EONReturn eonReturn) {
		}

		public void visit(EONSwitch eonSwitch) {
		}

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
