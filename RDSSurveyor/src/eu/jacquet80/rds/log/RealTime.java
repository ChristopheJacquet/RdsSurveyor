package eu.jacquet80.rds.log;

import java.text.SimpleDateFormat;
import java.util.Date;

public class RealTime implements RDSTime {
	private final Date time;
	
	private final static SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("HH.mm.ss");
	private static final SimpleDateFormat LONG_TIME_FORMAT = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss.SSS");
	
	/**
	 * @brief Converts a {@code RealTime} instance to a {@code Date}.
	 * 
	 * @param refStreamTime Not used, can be {@code null}
	 * @param refDate Not used, can be {@code null}
	 * @return The exact time which corresponds to this instance
	 */
	@Override
	public Date getRealTime(RDSTime refStreamTime, Date refDate) {
		return time;
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
