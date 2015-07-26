package eu.jacquet80.rds.app.oda.tmc;

import java.util.Map;

/** Describes a TMC ROAD location. */
public class Road extends TMCLocation {
	// CID, TABCD, LCD, CLASS, TCD, STCD, RNID, N1ID, N2ID inherited from TMCLocation
	
	/** The road number, e.g. M5, A4, 128, B49n, ST1027. */
	public String roadNumber;
	
	/** The road network level. */
	public int pesLev;
	
	Road(String line, Map<String, Integer> fields) {
		String[] comp = TMC.colonPattern.split(line);
		this.cid = Integer.parseInt(comp[fields.get("CID")]);
		this.tabcd = Integer.parseInt(comp[fields.get("TABCD")]);
		this.lcd = Integer.parseInt(comp[fields.get("LCD")]);
		this.category = LocationClass.forCode(comp[fields.get("CLASS")]);
		this.tcd = Integer.parseInt(comp[fields.get("TCD")]);
		this.stcd = Integer.parseInt(comp[fields.get("STCD")]);
		if ((fields.containsKey("ROADNUMBER")) && (comp.length > fields.get("ROADNUMBER")))
			this.roadNumber = comp[fields.get("ROADNUMBER")];
		if ((fields.containsKey("RNID")) && (comp.length > fields.get("RNID")) && (!"".equals(comp[fields.get("RNID")]))) {
			this.rnid = Integer.parseInt(comp[fields.get("RNID")]);
			this.roadName = TMC.getName(this.cid, this.rnid);
		}
		if ((fields.containsKey("N1ID")) && (comp.length > fields.get("N1ID")) && (!"".equals(comp[fields.get("N1ID")]))) {
			this.n1id = Integer.parseInt(comp[fields.get("N1ID")]);
			this.name1 = TMC.getName(this.cid, this.n1id);
		}
		if ((fields.containsKey("N2ID")) && (comp.length > fields.get("N2ID")) && (!"".equals(comp[fields.get("N2ID")]))) {
			this.n2id = Integer.parseInt(comp[fields.get("N2ID")]);
			this.name2 = TMC.getName(this.cid, this.n2id);
		}
		if ((fields.containsKey("POL_LCD")) && (comp.length > fields.get("POL_LCD")) && (!"".equals(comp[fields.get("POL_LCD")]))) {
			this.polLcd = Integer.parseInt(comp[fields.get("POL_LCD")]);
			this.area = TMC.getArea(this.cid, this.tabcd, this.polLcd);
		}
		if ((fields.containsKey("PES_LEV")) && (comp.length > fields.get("PES_LEV")) && (!"".equals(comp[fields.get("PES_LEV")])))
			this.pesLev = Integer.parseInt(comp[fields.get("PES_LEV")]);
	}
	
	@Override
	public String toString() {
		StringBuilder res = new StringBuilder(super.toString());
		if (!"".equals(this.roadNumber))
			res.append("Road number: " + this.roadNumber + "\n");
		
		return res.toString();
	}
	
	@Override
	public String html() {
		StringBuilder res = new StringBuilder(super.html());
		if (!"".equals(this.roadNumber))
			res.append("Road number: " + this.roadNumber + "<br>");
		
		return res.toString();
	}
}
