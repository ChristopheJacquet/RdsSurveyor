package eu.jacquet80.rds.app.oda.tmc;

import java.util.Map;

/** Describes a TMC Location Table. */
public class LocationDataset {
	/** The country ID used in TMC messages. */
	public int cid;
	/** The location table number (LTN). */
	public int tabcd;
	public String dComment;
	/** The version of the location table. */
	public String version;
	/** A description of the location table version. */
	public String versionDescription;
	
	LocationDataset(String line, Map<String, Integer> fields) {
		String[] comp = TMC.colonPattern.split(line);
		this.cid = Integer.parseInt(comp[fields.get("CID")]);
		this.tabcd = Integer.parseInt(comp[fields.get("TABCD")]);
		this.version = comp[fields.get("VERSION")];
		if ((fields.containsKey("DCOMMENT") && (comp.length > fields.get("DCOMMENT"))))
			this.dComment = comp[fields.get("DCOMMENT")];
		if ((fields.containsKey("VERSIONDESCRIPTION") && (comp.length > fields.get("VERSIONDESCRIPTION"))))
			this.versionDescription = comp[fields.get("VERSIONDESCRIPTION")];
		else
			this.versionDescription = "";
	}

}
