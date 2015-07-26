package eu.jacquet80.rds.app.oda.tmc;

/** Abstract base class for TMC locations. */
public abstract class TMCLocation {
	/** The country ID used in TMC messages. */
	public int cid;
	/** The location table number (LTN). */
	public int tabcd;
	/** The location code. */
	public int lcd;
	/** 
	 * The location category, i.e. the geometry type (AREA, LINE or POINT).
	 *
	 * The column name is {@code CLASS} but this is a reserved word in Java, hence the term
	 * category (which is used interchangeably) was chosen.
	 */
	public LocationClass category;
	/** The location type (e.g. junction), meaningful only together with {@code category}. */
	public int tcd;
	/** The location subtype (e.g. roundabout), meaningful only together with {@code category} and {@code tcd}. */
	public int stcd;
	/** The name ID (NID) of the road name. */
	public int rnid = -1;
	/** The road name. */
	public TMCName roadName;
	/** The name ID (NID) of the first (or only) name. */
	public int n1id = -1;
	/** The first (or only) name. */
	public TMCName name1;
	/** The name ID (NID) of the second name. */
	public int n2id = -1;
	/** The second name. */
	public TMCName name2;
	/** The location code of the enclosing administrative area. */
	public int polLcd = -1;
	/** The enclosing administrative area. */
	public TMCArea area;
	
	
	public static enum LocationClass {
		AREA, LINE, POINT;
		
		static LocationClass forCode(String s) {
			if("A".equals(s)) {
				return AREA;
			} else if("L".equals(s)) {
				return LINE;
			} else {
				return POINT;
			}
		}
		
		@Override
		public String toString() {
			if (this.equals(AREA))
				return "A";
			else if (this.equals(LINE))
				return "L";
			else
				return "P";
		}
	}
	
	/**
	 * @brief Returns the location at the given offset in the given direction from the current one.
	 *
	 * This is a dummy implementation which can be used for all subclasses for which extents have
	 * no meaning. It will simply return the same location.
	 * 
	 * @param extent The extent (i.e. number of steps) as indicated in the TMC message.
	 * @param direction The direction as indicated in the TMC message (0 = positive, 1 = negative).
	 */
	public TMCLocation getOffset(int extent, int direction) {
		return this;
	}
	
	@Override
	public String toString() {
		StringBuilder res = new StringBuilder("CID: ").append(cid);
		res.append(", TABCD: " + this.tabcd);
		res.append(", LCD: " + this.lcd);
		res.append(", Type: " + this.category.toString() + this.tcd + "." + this.stcd);
		res.append("\n");
		if (this.rnid >= 0) {
			res.append("Road name: ");
			if (this.roadName != null)
				res.append(this.roadName.name);
			else
				res.append("Unknown");
			res.append(" (" + this.rnid + ")\n");
		}
		if (this.n1id >= 0) {
			if (this.name1 != null)
				res.append(this.name1.name);
			else
				res.append("Unknown");
			res.append(" (" + this.n1id + ")");
		}
		if (this.n2id >= 0) {
			res.append(" - ");
			if (this.name2 != null)
				res.append(this.name2.name);
			else
				res.append("Unknown");
			res.append(" (" + this.n2id + ")");
		}
		if ((this.n1id >= 0) || (this.n2id >= 0))
			res.append("\n");
		if (this.area != null)
			res.append("\nAdministrative area: " + this.area.toString() + "\n\n");
		
		return res.toString();			
	}
	
	public String html() {
		StringBuilder res = new StringBuilder("CID: ").append(cid);
		res.append(", TABCD: " + this.tabcd);
		res.append(", LCD: " + this.lcd);
		res.append(", Type: " + this.category.toString() + this.tcd + "." + this.stcd);
		res.append("<br>");
		if (this.rnid >= 0) {
			res.append("Road name: ");
			if (this.roadName != null)
				res.append(this.roadName.name);
			else
				res.append("Unknown");
			res.append(" (" + this.rnid + ")<br>");
		}
		if (this.n1id >= 0) {
			if (this.name1 != null)
				res.append(this.name1.name);
			else
				res.append("Unknown");
			res.append(" (" + this.n1id + ")");
		}
		if (this.n2id >= 0) {
			res.append(" - ");
			if (this.name2 != null)
				res.append(this.name2.name);
			else
				res.append("Unknown");
			res.append(" (" + this.n2id + ")");
		}
		if ((this.n1id >= 0) || (this.n2id >= 0))
			res.append("<br>");
		if (this.area != null)
			res.append("<blockquote>Administrative area: " + this.area.html() + "</blockquote>");
		
		return res.toString();			
	}
	
}
