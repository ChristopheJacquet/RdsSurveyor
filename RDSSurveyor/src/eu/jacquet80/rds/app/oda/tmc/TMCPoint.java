package eu.jacquet80.rds.app.oda.tmc;

import java.util.Map;

/** Describes a TMC POINT location. */
public class TMCPoint extends TMCLocation {
	// CID, TABCD, LCD, CLASS, TCD, STCD, RNID, N1ID, N2ID inherited from TMCLocation
	
	/** The junction number, typically used for motorway junctions.  */
	public String junctionNumber;
	
	/** The location code of the other (non-administrative) area in which the point is located. */
	public int othLcd = -1;
	/** Other (non-administrative) area in which the point is located. */
	public TMCArea othArea;
	
	/** The location code of the corresponding segment. */
	public int segLcd = -1;
	/** The corresponding segment. */
	public Segment segment;
	
	/** The location code of the corresponding road. */
	public int roaLcd = -1;
	/** The corresponding road. */
	public Road road;
	
	//** Whether it is possible to move towards the point in positive direction. */
	// INPOS TODO true, false or unknown
	
	//** Whether it is possible to move towards the point in negative direction. */
	// INNEG TODO true, false or unknown
	
	//** Whether it is possible to move away from the point in positive direction. */
	// OUTPOS TODO true, false or unknown
	
	//** Whether it is possible to move away from the point in negative direction. */
	// OUTNEG TODO true, false or unknown
	
	//** Whether the point is present in positive direction. */
	// PRESENTPOS TODO true, false or unknown
	
	//** Whether the point is present in negative direction. */
	// PRESENTNEG TODO true, false or unknown
	
	/** Number of the predefined diversion in positive direction. */
	public String diversionPos;
	
	/** Number of the predefined diversion in negative direction. */
	public String diversionNeg;
	
	/** The X (longitude) coordinate of the point. */
	public float xCoord;
	
	/** The Y (longitude) coordinate of the point. */
	public float yCoord;
	
	// INTERRUPTSROAD TODO next location after interruption in the road (?)
	
	/** Whether the point is in a built-up area. */
	public boolean urban;

	/** Location code of the next location in negative direction. */
	public int negOffLcd = -1;
	/** The next location in negative direction. */
	public TMCPoint negOffset = null;

	/** Location code of the next location in positive direction. */
	public int posOffLcd = -1;
	/** The next location in positive direction. */
	public TMCPoint posOffset = null;

	TMCPoint(String line, Map<String, Integer> fields) {
		String[] comp = TMC.colonPattern.split(line);
		this.cid = Integer.parseInt(comp[fields.get("CID")]);
		this.tabcd = Integer.parseInt(comp[fields.get("TABCD")]);
		this.lcd = Integer.parseInt(comp[fields.get("LCD")]);
		this.category = LocationClass.forCode(comp[fields.get("CLASS")]);
		this.tcd = Integer.parseInt(comp[fields.get("TCD")]);
		this.stcd = Integer.parseInt(comp[fields.get("STCD")]);
		if ((fields.containsKey("JUNCTIONNUMBER")) && (comp.length > fields.get("JUNCTIONNUMBER")))
			this.junctionNumber = comp[fields.get("JUNCTIONNUMBER")];
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
		if ((fields.containsKey("OTH_LCD")) && (comp.length > fields.get("OTH_LCD")) && (!"".equals(comp[fields.get("OTH_LCD")]))) {
			this.othLcd = Integer.parseInt(comp[fields.get("OTH_LCD")]);
			this.othArea = TMC.getArea(this.cid, this.tabcd, this.othLcd);
		}
		if ((fields.containsKey("ROA_LCD")) && (comp.length > fields.get("ROA_LCD")) && (!"".equals(comp[fields.get("ROA_LCD")]))) {
			this.roaLcd = Integer.parseInt(comp[fields.get("ROA_LCD")]);
			this.road = TMC.getRoad(this.cid, this.tabcd, this.roaLcd);
		}
		if ((fields.containsKey("SEG_LCD")) && (comp.length > fields.get("SEG_LCD")) && (!"".equals(comp[fields.get("SEG_LCD")]))) {
			this.segLcd = Integer.parseInt(comp[fields.get("SEG_LCD")]);
			this.segment = TMC.getSegment(this.cid, this.tabcd, this.segLcd);
		}

		// INPOS TODO
		// INNEG TODO
		// OUTPOS TODO
		// OUTNEG TODO
		// PRESENTPOS TODO
		// PRESENTNEG TODO
		
		if ((fields.containsKey("DIVERSIONPOS")) && (comp.length > fields.get("DIVERSIONPOS")))
			this.diversionPos = comp[fields.get("DIVERSIONPOS")];
		if ((fields.containsKey("DIVERSIONNEG")) && (comp.length > fields.get("DIVERSIONNEG")))
			this.diversionNeg = comp[fields.get("DIVERSIONNEG")];
		this.xCoord = Integer.parseInt(comp[fields.get("XCOORD")]) / 100000.0f;
		this.yCoord = Integer.parseInt(comp[fields.get("YCOORD")]) / 100000.0f;
		
		// INTERRUPTSROAD TODO
		
		this.urban = Integer.parseInt(comp[fields.get("URBAN")]) == 0 ? false : true;
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
	public TMCPoint getOffset(int extent, int direction) {
		TMCPoint ret = this;
		for (int i = 1; i <= extent; i++)
			if ((direction == 0) && (ret.posOffset != null))
				ret = ret.posOffset;
			else if ((direction != 0) && (ret.negOffset != null))
				ret = ret.negOffset;
		return ret;
	}
	
	public void setOffset(TMCOffset offset) {
		this.negOffLcd = offset.negOffLcd;
		this.negOffset = TMC.getPoint(this.cid, this.tabcd, this.negOffLcd);
		this.posOffLcd = offset.posOffLcd;
		this.posOffset = TMC.getPoint(this.cid, this.tabcd, this.posOffLcd);
	}
	
	@Override
	public String toString() {
		StringBuilder res = new StringBuilder(super.toString());
		if (this.othArea != null)
			res.append("\nOther area: " + this.othArea.toString() + "\n\n");
		if (!"".equals(this.junctionNumber))
			res.append("Junction number: " + this.junctionNumber + "\n");
		if (this.segment != null)
			res.append("\nSegment: " + this.segment.toString() + "\n\n");
		if (this.road != null)
			res.append("\nRoad: " + this.road.toString() + "\n\n");
		res.append("Lon: " + this.xCoord + ", Lat: " + this.yCoord + "\n");
		res.append("Link: http://www.openstreetmap.org/?mlat=" + yCoord + "&mlon=" + xCoord + "#map=9/" + yCoord + "/" + xCoord + "&layers=Q");
		
		return res.toString();
	}
	
	@Override
	public String html() {
		StringBuilder res = new StringBuilder(super.html());
		if (this.othArea != null)
			res.append("<blockquote>Other area: " + this.othArea.html() + "</blockquote>");
		if (!"".equals(this.junctionNumber))
			res.append("Junction number: " + this.junctionNumber + "<br>");
		res.append("Urban: " + (this.urban ? "Yes" : "No") + "<br>");
		if (this.segment != null)
			res.append("<blockquote>Segment: " + this.segment.html() + "</blockquote>");
		if (this.road != null)
			res.append("<blockquote>Road: " + this.road.html() + "</blockquote>");
		res.append("Lon: " + this.xCoord + ", Lat: " + this.yCoord + "<br>");
		res.append("<a href=\"http://www.openstreetmap.org/?mlat=" + yCoord + "&mlon=" + xCoord + "#map=9/" + yCoord + "/" + xCoord + "&layers=Q\">");
		res.append("http://www.openstreetmap.org/?mlat=" + yCoord + "&mlon=" + xCoord + "#map=9/" + yCoord + "/" + xCoord + "&layers=Q");
		res.append("</a>");
		
		return res.toString();
	}
}
