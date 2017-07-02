package eu.jacquet80.rds.app.oda.tmc;

import java.sql.ResultSet;
import java.sql.SQLException;

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

	/**
	 * @brief Creates a new {@code Segment} from a given record.
	 * 
	 * This constructor expects two arguments, {@code rset} and {@code offsets}. {@code rset} must
	 * be a result set obtained by querying the {@code Segments} table. {@code offsets} must be a
	 * result set obtained by querying the {@code Soffsets} table. It can be null, in which case
	 * the segment will have no offsets in either direction and no extents can be resolved for this
	 * segment. Prior to calling the constructor, the cursor for both result sets must be set. The
	 * constructor will use the data from the records which the cursors point to.
	 * 
	 * @param rset The result set containing the segment.
	 * @param offsets The result set containing the offset. This argument can be {@code null}.
	 * @throws SQLException
	 */
	public Segment(ResultSet rset, ResultSet offsets) throws SQLException {
		this.cid = rset.getInt("CID");
		this.tabcd = rset.getInt("TABCD");
		this.lcd = rset.getInt("LCD");
		this.category = LocationClass.forCode(rset.getString("CLASS"));
		this.tcd = rset.getInt("TCD");
		this.stcd = rset.getInt("STCD");
		this.roadNumber = rset.getString("ROADNUMBER");
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
		int polLcd = rset.getInt("POL_LCD");
		if (!rset.wasNull()) {
			this.polLcd = polLcd;
			this.area = TMC.getArea(this.cid, this.tabcd, this.polLcd);
		}
		
		if (offsets != null)
			try {
				this.negOffLcd = offsets.getInt("NEG_OFF_LCD");
				this.posOffLcd = offsets.getInt("POS_OFF_LCD");
			} catch (SQLException e) {
				// NOP
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
	
	public Segment getNegOffset() {
		if ((this.negOffset == null) && (this.negOffLcd != -1))
			this.negOffset = TMC.getSegment(this.cid, this.tabcd, this.negOffLcd);
		return this.negOffset;
	}
	
	public Segment getPosOffset() {
		if ((this.posOffset == null) && (this.posOffLcd != -1))
			this.posOffset = TMC.getSegment(this.cid, this.tabcd, this.posOffLcd);
		return this.posOffset;
	}
	
	/**
	 * @brief Returns the coordinates of the first point of this Location.
	 *
	 * For a ROAD or SEGMENT location, this method determines if the location is further divided
	 * into segments. In that case, the first segment is identified (the only segment whose
	 * negative offset location is empty) and the coordinates of its first point are returned.
	 * 
	 * If the location is not segmented, its first point is identified (the only point whose
	 * negative offset location AND interruptsRoad are empty) and its coordinates are returned.
	 * 
	 * @return The coordinates (order is longitude, latitude) or {@code null}.
	 */
	@Override
	public float[] getFirstCoordinates() {
		TMCPoint point = TMC.getFirstPoint(this.cid, this.tabcd, this.lcd);
		if (point != null)
			return point.getFirstCoordinates();
		Segment segment = TMC.getFirstSegment(this.cid, this.tabcd, this.lcd);
		if (segment != null)
			return segment.getFirstCoordinates();
		return null;
	}
	
	/**
	 * @brief Returns the coordinates of the last point of this Location.
	 *
	 * For a ROAD or SEGMENT location, this method determines if the location is further divided
	 * into segments. In that case, the last segment is identified (the only segment whose
	 * positive offset location is empty) and the coordinates of its last point are returned.
	 * 
	 * If the location is not segmented, its last point is identified (the only point whose
	 * positive offset location AND interruptsRoad are empty) and its coordinates are returned.
	 * 
	 * @return The coordinates (order is longitude, latitude) or {@code null}.
	 */
	@Override
	public float[] getLastCoordinates() {
		TMCPoint point = TMC.getLastPoint(this.cid, this.tabcd, this.lcd);
		if (point != null)
			return point.getLastCoordinates();
		Segment segment = TMC.getLastSegment(this.cid, this.tabcd, this.lcd);
		if (segment != null)
			return segment.getLastCoordinates();
		return null;
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
			if ((direction == 0) && (ret.getPosOffset() != null))
				ret = ret.getPosOffset();
			else if ((direction != 0) && (ret.getNegOffset() != null))
				ret = ret.getNegOffset();
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

	/**
	 * @brief Whether this location is the direct or indirect child of another location.
	 * 
	 * {@code Segment} has a reference to an area, a segment and a road. If any of these is
	 * non-null, then the instance is a child of that location (and any other location that the
	 * parent location is a child of).
	 * 
	 * @param location The potential parent location.
	 * @return True if this location is a child of {@code location}, false if not.
	 */
	@Override
	public boolean isChildOf(TMCLocation location) {
		if (location.equals(area) || location.equals(road) || location.equals(segment))
			return true;
		if ((area != null) && area.isChildOf(location))
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
