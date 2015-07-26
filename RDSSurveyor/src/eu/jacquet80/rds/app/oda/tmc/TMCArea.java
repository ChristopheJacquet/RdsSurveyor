package eu.jacquet80.rds.app.oda.tmc;

import java.util.Map;

import eu.jacquet80.rds.app.oda.tmc.TMCLocation.LocationClass;

/** Describes a TMC AREA location. */
public class TMCArea extends TMCLocation {

	TMCArea(String line, Map<String, Integer> fields) {
		String[] comp = TMC.colonPattern.split(line);
		this.cid = Integer.parseInt(comp[fields.get("CID")]);
		this.tabcd = Integer.parseInt(comp[fields.get("TABCD")]);
		this.lcd = Integer.parseInt(comp[fields.get("LCD")]);
		this.category = LocationClass.forCode(comp[fields.get("CLASS")]);
		this.tcd = Integer.parseInt(comp[fields.get("TCD")]);
		this.stcd = Integer.parseInt(comp[fields.get("STCD")]);
		if ((fields.containsKey("NID")) && (comp.length > fields.get("NID")) && (!"".equals(comp[fields.get("NID")]))) {
			this.n1id = Integer.parseInt(comp[fields.get("NID")]);
			this.name1 = TMC.getName(this.cid, this.n1id);
		}
		if ((fields.containsKey("POL_LCD")) && (comp.length > fields.get("POL_LCD")) && (!"".equals(comp[fields.get("POL_LCD")]))) {
			this.polLcd = Integer.parseInt(comp[fields.get("POL_LCD")]);
			this.area = TMC.getArea(this.cid, this.tabcd, this.polLcd);
		}
	}
	
	@Override
	public String toString() {
		StringBuilder res = new StringBuilder("");
		if (this.n1id >= 0) {
			if (this.name1 != null)
				res.append(this.name1.name);
			else
				res.append("Unknown");
		}
		res.append(" (");
		res.append("CID: " + cid);
		res.append(", TABCD: " + this.tabcd);
		res.append(", LCD: " + this.lcd);
		res.append(", Type: " + this.category.toString() + this.tcd + "." + this.stcd);
		res.append(", NID: " + this.n1id);
		res.append(")");
		res.append("\n");
		
		return res.toString();			
	}
	
	@Override
	public String html() {
		StringBuilder res = new StringBuilder("");
		if (this.n1id >= 0) {
			if (this.name1 != null)
				res.append(this.name1.name);
			else
				res.append("Unknown");
		}
		res.append(" (");
		res.append("CID: " + cid);
		res.append(", TABCD: " + this.tabcd);
		res.append(", LCD: " + this.lcd);
		res.append(", Type: " + this.category.toString() + this.tcd + "." + this.stcd);
		res.append(", NID: " + this.n1id);
		res.append(")");
		res.append("<br>");
		
		return res.toString();			
	}
}
