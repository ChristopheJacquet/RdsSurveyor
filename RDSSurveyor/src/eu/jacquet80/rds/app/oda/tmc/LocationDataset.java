package eu.jacquet80.rds.app.oda.tmc;

import java.sql.ResultSet;
import java.sql.SQLException;

/** Describes a TMC Location Table. */
public class LocationDataset {
	/** The country ID used in TMC messages. */
	public int cid;
	/** The location table number (LTN). */
	public int tabcd;
	public String dComment;
	/** The version of the location table. */
	public String version;
	/** A description of the location table version. */
	public String versionDescription;
	
	/**
	 * @brief Creates a new {@code LocationDataset} from a given record.
	 * 
	 * This constructor expects one argument, {@code rset}, which must be a result set obtained by
	 * querying the {@code LocationDataSets} table. Prior to calling the constructor, the cursor for
	 * {@code rset} must be set. The constructor will use the data from the record which the cursor
	 * points to.
	 * 
	 * @param rset The result set
	 * @throws SQLException
	 */
	LocationDataset(ResultSet rset) throws SQLException {
		this.cid = rset.getInt("CID");
		this.tabcd = rset.getInt("TABCD");
		this.version = rset.getString("VERSION");
		this.dComment = rset.getString("DCOMMENT");
		this.versionDescription = rset.getString("VERSIONDESCRIPTION");
		if (rset.wasNull())
			this.versionDescription = "";
	}
}
