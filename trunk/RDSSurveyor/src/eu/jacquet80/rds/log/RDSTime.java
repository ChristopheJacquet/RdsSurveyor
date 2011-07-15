package eu.jacquet80.rds.log;

import java.util.Date;

public interface RDSTime {
	public Date getRealTime(RDSTime refStreamTime, Date refDate);
}
