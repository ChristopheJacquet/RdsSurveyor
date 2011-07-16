package eu.jacquet80.rds.log;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Date;

public class SequentialTime implements RDSTime {

	private final int groupTime;
	
	private final static NumberFormat FORMAT = new DecimalFormat("0000");
	
	@Override
	public Date getRealTime(RDSTime refStreamTime, Date refDate) {
		// TODO Auto-generated method stub
		return null;
	}

	public SequentialTime(int groupTime) {
		this.groupTime = groupTime;
	}
	
	public String toString() {
		return FORMAT.format(groupTime % 10000);
	}

	@Override
	public String toLongString() {
		return toString();
	}
}
