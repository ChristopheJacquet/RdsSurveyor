package eu.jacquet80.rds.app.oda.tmc;

import java.sql.ResultSet;
import java.sql.SQLException;

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
	
	/**
	 * If a road is interrupted, the points bordering on the interruption have only one neighbor
	 * each. Their {@code interruptsRoad} members then refer to each other.
	 * 
	 * Note that a road interrupted in that way is not necessarily segmented, and that segments
	 * can likewise be interrupted without being split up in lower-level segments.
	 */
	public int interruptsRoad = -1;
	
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

	/**
	 * @brief Creates a new {@code TMCPoint} from a given record.
	 * 
	 * This constructor expects two arguments, {@code rset} and {@code offsets}. {@code rset} must
	 * be a result set obtained by querying the {@code Points} table. {@code offsets} must be a
	 * result set obtained by querying the {@code Poffsets} table. It can be null, in which case
	 * the segment will have no offsets in either direction and no extents can be resolved for this
	 * segment. Prior to calling the constructor, the cursor for both result sets must be set. The
	 * constructor will use the data from the records which the cursors point to.
	 * 
	 * @param rset The result set containing the point.
	 * @param offsets The result set containing the offset. This argument can be {@code null}.
	 * @throws SQLException
	 */
	TMCPoint(ResultSet rset, ResultSet offsets) throws SQLException {
		this.cid = rset.getInt("CID");
		this.tabcd = rset.getInt("TABCD");
		this.lcd = rset.getInt("LCD");
		this.category = LocationClass.forCode(rset.getString("CLASS"));
		this.tcd = rset.getInt("TCD");
		this.stcd = rset.getInt("STCD");
		this.junctionNumber = rset.getString("JUNCTIONNUMBER");
		if (rset.wasNull())
			this.junctionNumber = null;
		int rnid = rset.getInt("RNID");
		if (!rset.wasNull()) {
			this.rnid = rnid;
			this.roadName = TMC.getName(this.cid, this.rnid);
		}
		int n1id = rset.getInt("N1ID");
		if (!rset.wasNull()) {
			this.n1id = n1id;
			this.name1 = TMC.getName(this.cid, this.n1id);
		}
		int n2id = rset.getInt("N2ID");
		if (!rset.wasNull()) {
			this.n2id = n2id;
			this.name2 = TMC.getName(this.cid, this.n2id);
		}
		int polLcd = rset.getInt("POL_LCD");
		if (!rset.wasNull()) {
			this.polLcd = polLcd;
			this.area = TMC.getArea(this.cid, this.tabcd, this.polLcd);
		}
		int othLcd = rset.getInt("OTH_LCD");
		if (!rset.wasNull()) {
			this.othLcd = othLcd;
			this.othArea = TMC.getArea(this.cid, this.tabcd, this.othLcd);
		}
		int roaLcd = rset.getInt("ROA_LCD");
		if (!rset.wasNull()) {
			this.roaLcd = roaLcd;
			this.road = TMC.getRoad(this.cid, this.tabcd, this.roaLcd);
		}
		int segLcd = rset.getInt("SEG_LCD");
		if (!rset.wasNull()) {
			this.segLcd = segLcd;
			this.segment = TMC.getSegment(this.cid, this.tabcd, this.segLcd);
		}

		// INPOS TODO
		// INNEG TODO
		// OUTPOS TODO
		// OUTNEG TODO
		// PRESENTPOS TODO
		// PRESENTNEG TODO
		
		this.diversionPos = rset.getString("DIVERSIONPOS");
		this.diversionNeg = rset.getString("DIVERSIONNEG");
		this.xCoord = rset.getFloat("XCOORD");
		this.yCoord = rset.getFloat("YCOORD");
		
		int interruptsRoad = rset.getInt("INTERRUPTSROAD");
		if (!rset.wasNull()) {
			this.interruptsRoad = interruptsRoad;
		}
		
		this.urban = rset.getBoolean("URBAN");
		
		if (offsets != null)
			try {
				this.negOffLcd = offsets.getInt("NEG_OFF_LCD");
				this.posOffLcd = offsets.getInt("POS_OFF_LCD");
			} catch (SQLException e) {
				// NOP
			}
	}
	
	@Override
	public String getDetailedDisplayName(TMCLocation secondary, String format1, String format2) {
		String fmt1 = format1;
		String fmt2 = format2;
		TMCPoint sec = null;
		if ((!this.equals(secondary)) && (secondary instanceof TMCPoint))
			sec = (TMCPoint) secondary;
		if ((fmt1 == null) || (fmt1.isEmpty()))
			fmt1 = "%s";
		if ((fmt2 == null) || (fmt2.isEmpty()))
			fmt1 = "%s – %s";
		if (sec != null)
			return String.format(fmt2, sec.getJunctionDisplayName(), this.getJunctionDisplayName());
		else
			return String.format(fmt1, this.getJunctionDisplayName());
	}

	@Override
	public String getDisplayName(TMCLocation secondary, int direction, boolean bidirectional) {
		if ((secondary == null) || (this.equals(secondary))) {
			if (segment != null)
				return segment.getDisplayName(null, direction, bidirectional);
			else if (road != null)
				return road.getDisplayName(null, direction, bidirectional);
			else
				return null;
		} else
			return super.getDisplayName(secondary, direction, bidirectional);
	}
	
	@Override
	public TMCLocation getEnclosingLocation(TMCLocation secondary) {
		TMCLocation ret = super.getEnclosingLocation(secondary);
		if (ret != null)
			return ret;
		if (segment != null) {
			ret = segment.getEnclosingLocation(secondary);
			if (ret != null)
				return ret;
		}
		if (road != null)
			ret = road.getEnclosingLocation(secondary);
		return ret;
	}
	
	public TMCPoint getNegOffset() {
		if ((this.negOffset == null) && (this.negOffLcd != -1))
			this.negOffset = TMC.getPoint(this.cid, this.tabcd, this.negOffLcd);
		return this.negOffset;
	}
	
	public TMCPoint getPosOffset() {
		if ((this.posOffset == null) && (this.posOffLcd != -1))
			this.posOffset = TMC.getPoint(this.cid, this.tabcd, this.posOffLcd);
		return this.posOffset;
	}
	
	/**
	 * @brief Returns the coordinates of the first point of this Location.
	 *
	 * For a POINT location, this method will simply return its coordinates.
	 * 
	 * @return The coordinates (order is longitude, latitude) or {@code null}.
	 */
	@Override
	public float[] getFirstCoordinates() {
		return new float[] {xCoord, yCoord};
	}
	
	/**
	 * @brief Returns the coordinates of the last point of this Location.
	 *
	 * For a POINT location, this method will simply return its coordinates.
	 * 
	 * @return The coordinates (order is longitude, latitude) or {@code null}.
	 */
	@Override
	public float[] getLastCoordinates() {
		return new float[] {xCoord, yCoord};
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
			if ((direction == 0) && (ret.getPosOffset() != null))
				ret = ret.getPosOffset();
			else if ((direction != 0) && (ret.getNegOffset() != null))
				ret = ret.getNegOffset();
		return ret;
	}
	
	/**
	 * @brief Returns a label for the junction.
	 * 
	 * The label contains the name of the junction and its number, if any. It may take the following forms,
	 * depending on which members hold non-empty values:
	 * <ul>
	 * <li>name (number)</li>
	 * <li>name</li>
	 * <li>number</li>
	 * </ul>
	 * @return A formatted junction label.
	 */
	public String getJunctionDisplayName() {
		if ((name1 != null) && (!name1.name.isEmpty()))
			if ((junctionNumber != null) && (!junctionNumber.isEmpty()))
				return String.format("%s (%s)", name1.name, junctionNumber);
			else
				return name1.name;
		else
			if (junctionNumber.isEmpty())
				return null;
			else
				return junctionNumber;
	}
	
	@Override
	public String getRoadNumber() {
		String ret = null;
		if (this.road != null)
			ret = this.road.getRoadNumber();
		if ((ret == null) && (this.segment != null))
			ret = this.segment.getRoadNumber();
		return ret;
	}

	/**
	 * @brief Whether this location is the direct or indirect child of another location.
	 * 
	 * {@code TMCPoint} has a reference to an area, an other area, a segment and a road. If any of
	 * these is non-null, then the instance is a child of that location (and any other location
	 * that the parent location is a child of).
	 * 
	 * @param location The potential parent location.
	 * @return True if this location is a child of {@code location}, false if not.
	 */
	@Override
	public boolean isChildOf(TMCLocation location) {
		if (location.equals(area) || location.equals(othArea) || location.equals(road) || location.equals(segment))
			return true;
		if ((area != null) && area.isChildOf(location))
			return true;
		if ((othArea != null) && othArea.isChildOf(location))
			return true;
		if ((road != null) && road.isChildOf(location))
			return true;
		if ((segment != null) && segment.isChildOf(location))
			return true;
		return false;
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
