package eu.jacquet80.rds.log;

import java.text.SimpleDateFormat;
import java.util.Date;

public class RealTime implements RDSTime {
	private final Date time;
	
	private final static SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("HH.mm.ss");
	private static final SimpleDateFormat LONG_TIME_FORMAT = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss.SSS");
	
	@Override
	public Date getRealTime(RDSTime refStreamTime, Date refDate) {
		// TODO Auto-generated method stub
		return null;
	}

	public RealTime(Date time) {
		this.time = time;
	}
	
	public RealTime() {
		this.time = new Date();
	}
	
	public String toString() {
		return TIME_FORMAT.format(this.time);
	}
	
	public String toLongString() {
		return LONG_TIME_FORMAT.format(this.time);
	}
}
