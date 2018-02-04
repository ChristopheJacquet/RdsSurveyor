package eu.jacquet80.rds.app.oda.tmc;

import java.sql.ResultSet;
import java.sql.SQLException;

/** TMC information about a country. */
public class Country {
	/** The country ID used in TMC messages. */
	public int cid;
	/** The Extended Country Code (ECC) used in RDS 1A groups. */
	public String ecc;
	/** The Country Code (CC) used in the PI. */
	public String ccd;
	/** The name of the country. */
	public String country;

	/**
	 * @brief Creates a new {@code Country} from a given record.
	 * 
	 * This constructor expects one argument, {@code rset}, which must be a result set obtained by
	 * querying the {@code Countries} table. Prior to calling the constructor, the cursor for
	 * {@code rset} must be set. The constructor will use the data from the record which the cursor
	 * points to.
	 * 
	 * @param rset The result set
	 * @throws SQLException
	 */
	Country(ResultSet rset) throws SQLException {
		this.cid = rset.getInt("CID");
		this.ecc = rset.getString("ECC");
		// ECC is not mandatory and some countries omit it (e.g. Sweden v2.1)
		if (rset.wasNull())
			this.ecc = "";
		this.ccd = rset.getString("CCD");
		this.country = rset.getString("CNAME");
		if (rset.wasNull())
			this.country = "unknown#" + this.cid;
	}
}
