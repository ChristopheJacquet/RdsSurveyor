package eu.jacquet80.rds.app.oda.tmc;

import java.util.Map;

/** Describes a TMC location name. */
public class TMCName {
	/** The country ID used in TMC messages. */
	public int cid;
	/** The language ID for the name. */
	public int lid;
	/** The name ID by which locations refer to the name. */
	public int nid;
	/** The name. */
	public String name;
	public String nameComment;
	
	TMCName(String line, Map<String, Integer> fields) {
		String[] comp = TMC.colonPattern.split(line);
		this.cid = Integer.parseInt(comp[fields.get("CID")]);
		this.lid = Integer.parseInt(comp[fields.get("LID")]);
		this.nid = Integer.parseInt(comp[fields.get("NID")]);
		this.name = comp[fields.get("NAME")];
		if (comp.length > fields.get("NCOMMENT"))
			this.nameComment = comp[fields.get("NCOMMENT")];
		else
			this.nameComment = "";
	}
}
