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
	
	public boolean equals(TMCLocation obj) {
		if (obj == null)
			return false;
		return ((this.cid == obj.cid)
				&& (this.tabcd == obj.tabcd)
				&& (this.lcd == obj.lcd));
	}
	
	/**
	 * @brief Returns the name of the area in which the location is situated.
	 * 
	 * If the location is within an area of a lower order than A10.x (town), the name of the town
	 * is returned.
	 * 
	 * This method is somewhat resilient to incomplete or corrupt data: if the area hierarchy is
	 * interrupted and no A10.x area is found, the name of the highest-order area is returned. If
	 * no area information is found at all, this method will return {@code null}.
	 * 
	 * @return The area name, or {@code null} if no area name can be found.
	 */
	public String getAreaName() {
		String ret = null; // higher-order administrative area name
		TMCArea a = area;
		if ((a != null) && (a.name1 != null)) {
			// when enclosing area is a town district, we want the town (A10.x or higher), not the district
			while ((a.tcd > 10) && (a.area != null))
				a = a.area;
			ret = a.name1.name;
		}
		return ret;
	}
	
	/**
	 * @brief Returns a string specifying the exact location in a road, in a user-friendly form.
	 * 
	 * The result of this method is intended to be used as a refinement to the results of
	 * {@link #getRoadNumber()} and/or {@link #getDetailedDisplayName(TMCLocation, String, String)}.
	 * A typical use case is to add junction names to the former, as in "A9 between Saronno and Turate".
	 * 
	 * How the locations are formatted is governed by two format strings, {@code format1} and {@code format2}.
	 * {@code format1} is used if a single location is present, i.e. {@code secondary} is {@code null} or matches
	 * the object for which this method is called. Otherwise {@format2} is used. This allows for the strings
	 * to be localized by simply passing the appropriate format strings to this function.
	 * 
	 * Typical examples for format strings are:<br/>
	 * {@code format1 = "at %s"}<br/>
	 * {@code format2 = "between %s and %s"}<br/>
	 * 
	 * Descendant classes can override this method, changing its behavior. Where this information has no
	 * meaning (e.g. for areas), this method should return NULL.
	 * 
	 * @param secondary The secondary location, if any.
	 * @param format1 The format string to use if a single location is present. This string must contain
	 * the {@code %s} placeholder exactly once. If {@code null}, {@code "%s"} will be used instead.
	 * @param format2 The format string to use if two locations are present. This string must contain
	 * the {@code %s} placeholder exactly twice. If {@code null}, {@code "%s – %s"} will be used instead.
	 * @return
	 */
	public String getDetailedDisplayName(TMCLocation secondary, String format1, String format2) {
		return null;
	}
	
	/**
	 * @brief Returns a name for the location which can be displayed to the user.
	 * 
	 * The display name, together with the road number (if any), identifies the location of the event.
	 * A display name can take one of the following forms:
	 * <ul>
	 * <li>{@code name1 - name2 (roadName)} (for roads with endpoints and a name)</li>
	 * <li>{@code name1 - name2} (for roads with endpoints but no road name)</li>
	 * <li>{@code roadName} (for roads with only a road name, e.g. ring roads)</li>
	 * <li>{@code roadName, areaName} (for roads with no road number, e.g. urban roads)</li>
	 * </ul>
	 * 
	 * Where endpoint names {@code name1} and {@code name2} are used, they are reordered to match
	 * the travel direction of affected traffic (opposite to the direction of queue growth).
	 * 
	 * @param secondary The secondary location. If supplied, this method will try to return the names
	 * for the lowest-order segment or road which spans both locations.
	 * @param direction The direction of queue growth (0 for positive), used to order names correctly.
	 * @param bidirectional Whether the message applies to one or both directions.
	 * @return A user-friendly string describing the location of the event.
	 */
	public String getDisplayName(TMCLocation secondary, int direction, boolean bidirectional) {
		if ((secondary == null) || (this.equals(secondary))) {
			String n1n2 = null; // endpoint names, ordered
			if ((name1 != null) && (name2 != null)) {
				if (bidirectional)
					/* bidirectional */
					n1n2 = String.format("%s ↔ %s", name2.name, name1.name);
				else if (direction != 0)
					/* negative */
					n1n2 = String.format("%s → %s", name1.name, name2.name);
				else
					/* positive */
					n1n2 = String.format("%s → %s", name2.name, name1.name);
			}
			
			String lname = null; // location name (roadName or name1)
			if (roadName != null)
				lname = roadName.name;
			else if ((name1 != null) && (name2 == null))
				lname = name1.name;
			
			String aname = getAreaName();
			
			if ((n1n2 != null) && (lname != null))
				return String.format("%s (%s)", n1n2, lname);
			else if (n1n2 != null)
				return n1n2;
			else if (lname != null) {
				if ((category == LocationClass.AREA)
						|| (aname == null)
						|| ((area.tcd > 9) && (getRoadNumber() != null)))
					return lname;
				else
					return String.format("%s, %s", lname, aname);
			}
		}
		TMCLocation loc = this.getEnclosingLocation(secondary);
		if (loc == null)
			return null;
		return loc.getDisplayName(null, direction, bidirectional);
	}
	
	/**
	 * @brief Returns the lowest-order segment or road which spans both locations.
	 * 
	 * @param secondary The secondary location. If {@code null} is supplied, this method returns
	 * the instance for which it was called.
	 * @return The lowest-order enclosing location, or {@code null} if none was found.
	 */
	public TMCLocation getEnclosingLocation(TMCLocation secondary) {
		if ((secondary == null) || (this.equals(secondary)))
			return this;
		else
			return null;
	}
	
	/**
	 * @brief Returns the coordinates of the first point of this Location.
	 *
	 * This is a dummy implementation which can be used for all subclasses which have no explicit
	 * or implicit coordinates. It will simply return {@code null}.
	 * 
	 * @return The coordinates (order is longitude, latitude) or {@code null}.
	 */
	public float[] getFirstCoordinates() {
		return null;
	}
	
	/**
	 * @brief Returns the coordinates of the last point of this Location.
	 *
	 * This is a dummy implementation which can be used for all subclasses which have no explicit
	 * or implicit coordinates. It will simply return {@code null}.
	 * 
	 * @return The coordinates (order is longitude, latitude) or {@code null}.
	 */
	public float[] getLastCoordinates() {
		return null;
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
	
	/**
	 * @brief Returns the road number for the location, if any.
	 * 
	 * @return The road number, or {@code null} if the location does not have a corresponding road number.
	 */
	public String getRoadNumber() {
		return null;
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
