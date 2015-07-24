package eu.jacquet80.rds.app.oda.tmc;

import java.util.Map;

/** TMC information about a country. */
public class Country {
	/** The country ID used in TMC messages. */
	public int cid;
	/** The Extended Country Code (ECC) used in RDS 1A groups. */
	public String ecc;
	/** The Country Code (CC) used in the PI. */
	public String ccd;
	/** The name of the country. */
	public String country;

	Country(String line, Map<String, Integer> fields) {
		String[] comp = TMC.colonPattern.split(line);
		this.cid = Integer.parseInt(comp[fields.get("CID")]);
		// ECC is not mandatory and some countries omit it (e.g. Sweden v2.1)
		if ((fields.containsKey("ECC")) && (comp.length > fields.get("ECC")))
			this.ecc = comp[fields.get("ECC")];
		else
			this.ecc = "";
		this.ccd = comp[fields.get("CCD")];
		if ((fields.containsKey("CNAME")) && (comp.length > fields.get("CNAME")))
			this.country = comp[fields.get("CNAME")];
		else
			this.country = "unknown#" + cid;
	}
}
