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
