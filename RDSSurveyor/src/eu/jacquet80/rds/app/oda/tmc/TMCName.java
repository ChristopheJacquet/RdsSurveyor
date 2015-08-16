package eu.jacquet80.rds.app.oda.tmc;

import java.sql.ResultSet;
import java.sql.SQLException;

/** Describes a TMC location name. */
public class TMCName {
	/** The country ID used in TMC messages. */
	public int cid;
	/** The language ID for the name. */
	public int lid;
	/** The name ID by which locations refer to the name. */
	public int nid;
	/** The name. */
	public String name;
	public String nameComment;
	
	/**
	 * @brief Creates a new {@code TMCName} from a given record.
	 * 
	 * This constructor expects one argument, {@code rset}, which must be a result set obtained by
	 * querying the {@code Names} table. Prior to calling the constructor, the cursor for
	 * {@code rset} must be set. The constructor will use the data from the record which the cursor
	 * points to.
	 * 
	 * @param rset The result set
	 * @throws SQLException
	 */
	TMCName(ResultSet rset) throws SQLException {
		this.cid = rset.getInt("CID");
		this.lid = rset.getInt("LID");
		this.nid = rset.getInt("NID");
		this.name = rset.getString("NAME");
		this.nameComment = rset.getString("NCOMMENT");
		if (rset.wasNull())
			this.nameComment = "";
	}
}
