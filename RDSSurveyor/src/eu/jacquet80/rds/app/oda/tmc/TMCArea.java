package eu.jacquet80.rds.app.oda.tmc;

import java.sql.ResultSet;
import java.sql.SQLException;

/** Describes a TMC AREA location. */
public class TMCArea extends TMCLocation {

	/**
	 * @brief Creates a new {@code TMCArea} from a given record.
	 * 
	 * This constructor expects one argument, {@code rset}, which must be a result set obtained by
	 * querying one of the {@code AdministrativeAreas} or {@code OtherAreas} tables. Prior to calling
	 * the constructor, the cursor for {@code rset} must be set. The constructor will use the data
	 * from the record which the cursor points to.
	 * 
	 * @param rset The result set
	 * @throws SQLException
	 */
	TMCArea(ResultSet rset) throws SQLException {
		this.cid = rset.getInt("CID");
		this.tabcd = rset.getInt("TABCD");
		this.lcd = rset.getInt("LCD");
		this.category = LocationClass.forCode(rset.getString("CLASS"));
		this.tcd = rset.getInt("TCD");
		this.stcd = rset.getInt("STCD");
		int n1id = rset.getInt("NID");
		if (!rset.wasNull()) {
			this.n1id = n1id;
			this.name1 = TMC.getName(this.cid, this.n1id);
		}
		int polLcd = rset.getInt("POL_LCD");
		if (!rset.wasNull()) {
			this.polLcd = polLcd;
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
