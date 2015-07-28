package eu.jacquet80.rds.app.oda.tmc;

import java.util.Map;

/** Describes a TMC SEGMENT location. */
public class Segment extends TMCLocation {
	// CID, TABCD, LCD, CLASS, TCD, STCD, RNID, N1ID, N2ID inherited from TMCLocation
	
	/** The road number, e.g. M5, A4, 128, B49n, ST1027. */
	public String roadNumber;
	
	/** The location code of the corresponding road. */
	public int roaLcd = -1;
	/** The corresponding road. */
	public Road road;
	
	/** Order 1 segment location code (type L3.x). */
	public int segLcd = -1;
	/** Order 1 segment (type L3.x). */
	public Segment segment;
	
	/** Location code of the next location in negative direction. */
	public int negOffLcd = -1;
	/** The next location in negative direction. */
	public Segment negOffset = null;

	/** Location code of the next location in positive direction. */
	public int posOffLcd = -1;
	/** The next location in positive direction. */
	public Segment posOffset = null;

	Segment(String line, Map<String, Integer> fields) {
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
		if ((fields.containsKey("ROA_LCD")) && (comp.length > fields.get("ROA_LCD")) && (!"".equals(comp[fields.get("ROA_LCD")]))) {
			this.roaLcd = Integer.parseInt(comp[fields.get("ROA_LCD")]);
			this.road = TMC.getRoad(this.cid, this.tabcd, this.roaLcd);
		}
		if ((fields.containsKey("SEG_LCD")) && (comp.length > fields.get("SEG_LCD")) && (!"".equals(comp[fields.get("SEG_LCD")]))) {
			this.segLcd = Integer.parseInt(comp[fields.get("SEG_LCD")]);
			this.segment = TMC.getSegment(this.cid, this.tabcd, this.segLcd);
		}
		if ((fields.containsKey("POL_LCD")) && (comp.length > fields.get("POL_LCD")) && (!"".equals(comp[fields.get("POL_LCD")]))) {
			this.polLcd = Integer.parseInt(comp[fields.get("POL_LCD")]);
			this.area = TMC.getArea(this.cid, this.tabcd, this.polLcd);
		}
	}
	
	@Override
	public TMCLocation getEnclosingLocation(TMCLocation secondary) {
		TMCLocation ret = super.getEnclosingLocation(secondary);
		if (ret != null)
			return ret;
		
		// try to match against parent of secondary location
		if (secondary instanceof TMCPoint) {
			TMCPoint point = (TMCPoint) secondary;
			if (point.road != null) {
				ret = this.getEnclosingLocation(point.road);
				if (ret != null)
					return ret;
			}
			if (point.segment != null)
				ret = this.getEnclosingLocation(point.segment);
		} else if (secondary instanceof Segment) {
			Segment segment = (Segment) secondary;
			if (segment.road != null) {
				ret = this.getEnclosingLocation(segment.road);
				if (ret != null)
					return ret;
			}
			if (segment.segment != null)
				ret = this.getEnclosingLocation(segment.segment);
		}
		if (ret != null)
			return ret;
		
		// secondary location has no matching parent, try matching own parents
		if (segment != null) {
			ret = segment.getEnclosingLocation(secondary);
			if (ret != null)
				return ret;
		}
		if (road != null)
			ret = road.getEnclosingLocation(secondary);
		return ret;
	}
	
	/**
	 * @brief Returns the location at the given offset in the given direction from the current one.
	 * 
	 * Offset is implemented as a linked list, i.e. each location has a reference to its immediate
	 * neighbors in either direction. The last location in the list has no further neighbors in
	 * the respective direction. The extent of a valid TMC message will never exceed the boundaries
	 * of the list. If this method is nonetheless called with an invalid extent, the last location
	 * in the linked list is returned to ensure that this method always returns a valid location.
	 *
	 * @param extent The extent (i.e. number of steps) as indicated in the TMC message.
	 * @param direction The direction as indicated in the TMC message (0 = positive, 1 = negative).
	 */
	@Override
	public Segment getOffset(int extent, int direction) {
		Segment ret = this;
		for (int i = 1; i <= extent; i++)
			if ((direction == 0) && (ret.posOffset != null))
				ret = ret.posOffset;
			else if ((direction != 0) && (ret.negOffset != null))
				ret = ret.negOffset;
		return ret;
	}
	
	@Override
	public String getRoadNumber() {
		String ret = null;
		if (this.road != null)
			ret = this.road.getRoadNumber();
		if ((ret == null) && (this.segment != null))
			ret = this.segment.getRoadNumber();
		if ((ret == null) && (!"".equals(this.roadNumber)))
			ret = this.roadNumber;
		return ret;
	}

	public void setOffset(TMCOffset offset) {
		this.negOffLcd = offset.negOffLcd;
		this.negOffset = TMC.getSegment(this.cid, this.tabcd, this.negOffLcd);
		this.posOffLcd = offset.posOffLcd;
		this.posOffset = TMC.getSegment(this.cid, this.tabcd, this.posOffLcd);
	}
	
	@Override
	public String toString() {
		StringBuilder res = new StringBuilder(super.toString());
		if (!"".equals(this.roadNumber))
			res.append("Road number: " + this.roadNumber + "\n");
		if (this.segment != null)
			res.append("\nSegment: " + this.segment.toString() + "\n\n");
		if (this.road != null)
			res.append("\nRoad: " + this.road.toString() + "\n\n");
		
		return res.toString();
	}
	
	@Override
	public String html() {
		StringBuilder res = new StringBuilder(super.html());
		if (!"".equals(this.roadNumber))
			res.append("Road number: " + this.roadNumber + "<br>");
		if (this.segment != null)
			res.append("<blockquote>Segment: " + this.segment.html() + "</blockquote>");
		if (this.road != null)
			res.append("<blockquote>Road: " + this.road.html() + "</blockquote>");
		
		return res.toString();
	}
}
