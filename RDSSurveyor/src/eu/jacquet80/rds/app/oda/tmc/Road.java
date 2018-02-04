package eu.jacquet80.rds.app.oda.tmc;

import java.sql.ResultSet;
import java.sql.SQLException;

/** Describes a TMC ROAD location. */
public class Road extends TMCLocation {
	// CID, TABCD, LCD, CLASS, TCD, STCD, RNID, N1ID, N2ID inherited from TMCLocation
	
	/** The road number, e.g. M5, A4, 128, B49n, ST1027. */
	public String roadNumber;
	
	/** The road network level. */
	public int pesLev;
	
	/**
	 * @brief Creates a new {@code Road} from a given record.
	 * 
	 * This constructor expects one argument, {@code rset}, which must be a result set obtained by
	 * querying the {@code Roads} table. Prior to calling the constructor, the cursor for
	 * {@code rset} must be set. The constructor will use the data from the record which the cursor
	 * points to.
	 * 
	 * @param rset The result set
	 * @throws SQLException
	 */
	Road(ResultSet rset) throws SQLException {
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
		int polLcd = rset.getInt("POL_LCD");
		if (!rset.wasNull()) {
			this.polLcd = polLcd;
			this.area = TMC.getArea(this.cid, this.tabcd, this.polLcd);
		}
		this.pesLev = rset.getInt("PES_LEV");
	}
	
	@Override
	public String toString() {
		StringBuilder res = new StringBuilder(super.toString());
		if (!"".equals(this.roadNumber))
			res.append("Road number: " + this.roadNumber + "\n");
		
		return res.toString();
	}
	
	@Override
	public TMCLocation getEnclosingLocation(TMCLocation secondary) {
		TMCLocation ret = super.getEnclosingLocation(secondary);
		if (ret != null)
			return ret;
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
		return ret;
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
	
	@Override
	public String getRoadNumber() {
		if (!"".equals(this.roadNumber))
			return this.roadNumber;
		else
			return null;
	}

	@Override
	public String html() {
		StringBuilder res = new StringBuilder(super.html());
		if (!"".equals(this.roadNumber))
			res.append("Road number: " + this.roadNumber + "<br>");
		
		return res.toString();
	}
}
