package eu.jacquet80.rds.app.oda.tmc;

import java.util.Map;

/** Describes offsets for a TMC POINT or SEGMENT location. */
public class TMCOffset {
	// Members are in the same order as location table columns
	/** The country ID used in TMC messages. */
	public int cid;
	/** The location table number (LTN). */
	public int tabcd;
	/** The location code. */
	public int lcd;
	/** Location code of the next location in negative direction. */
	public int negOffLcd = -1;
	/** Location code of the next location in positive direction. */
	public int posOffLcd = -1;

	TMCOffset(String line, Map<String, Integer> fields) {
		String[] comp = TMC.colonPattern.split(line);
		this.cid = Integer.parseInt(comp[fields.get("CID")]);
		this.tabcd = Integer.parseInt(comp[fields.get("TABCD")]);
		this.lcd = Integer.parseInt(comp[fields.get("LCD")]);
		// Check length as some offset tables have records where both offsets are NULL
		if ((comp.length > fields.get("NEG_OFF_LCD")) && (!"".equals(comp[fields.get("NEG_OFF_LCD")])))
			negOffLcd = Integer.parseInt(comp[fields.get("NEG_OFF_LCD")]);
		if ((comp.length > fields.get("POS_OFF_LCD")) && (!"".equals(comp[fields.get("POS_OFF_LCD")])))
			posOffLcd = Integer.parseInt(comp[fields.get("POS_OFF_LCD")]);
	}
}
