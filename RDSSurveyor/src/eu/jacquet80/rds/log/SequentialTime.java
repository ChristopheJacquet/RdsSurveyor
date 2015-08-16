package eu.jacquet80.rds.log;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Date;

/**
 * Internally, SequentialTime is implemented as a counter of bits received. Given the data rate
 * of 1187.5 bit/s (+/- 0.125 bit/s), the time elapsed between two class instances can be
 * determined.
 */
public class SequentialTime implements RDSTime {

	private final int groupTime;
	
	private final static NumberFormat FORMAT = new DecimalFormat("0000");

	/**
	 * @brief Converts a {@code SequentialTime} instance to a {@code Date}.
	 * 
	 * @param refStreamTime A {@code SequentialTime} which corresponds to {@code refDate}.
	 * @param refDate The exact time which corresponds to {@code refStreamTime}.
	 * @return The exact time which corresponds to this instance. If any of the arguments is
	 * {@code null}, or if {@code refStreamTime} is not a {@code SequentialTime} instance, the
	 * return value is {@code null}.
	 */
	@Override
	public Date getRealTime(RDSTime refStreamTime, Date refDate) {
		if ((refDate == null) || (!(refStreamTime instanceof SequentialTime)))
			return null;
		/* determine seconds elapsed since reference timestamp */
		int offsetMillis = (int) ((groupTime - ((SequentialTime) refStreamTime).groupTime) * 1000 / 1187.5);
		return new Date(refDate.getTime() + offsetMillis);
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
