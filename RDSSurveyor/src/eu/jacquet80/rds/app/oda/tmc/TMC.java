package eu.jacquet80.rds.app.oda.tmc;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

public class TMC {
	// TODO primary key, unique/not null constraints
	private static final String[] initStmts = {
		// 1 - Countries - COUNTRIES.DAT;
		"create cached table if not exists Countries(CID integer primary key, ECC varchar(2), CCD varchar(1), CNAME varchar(50));",
		"drop index if exists Countries_ECC_idx;",
		"drop index if exists Countries_CCD_idx;",
		"create index Countries_ECC_idx ON Countries (ECC);",
		"create index Countries_CCD_idx ON Countries (CCD);",
		// 2 - LocationDataSets - LOCATIONDATASETS.DAT;
		"create cached table if not exists LocationDataSets(CID integer, TABCD integer, DCOMMENT varchar(100), VERSION varchar(7), VERSIONDESCRIPTION varchar(100), unique(CID, TABCD));",
		"drop index if exists LocationDataSets_CID_TABCD_idx;",
		"create index LocationDataSets_CID_TABCD_idx on LocationDataSets (CID, TABCD);",
		"drop trigger if exists LocationDataSets_after_delete_trigger;",
		"create trigger LocationDataSets_after_delete_trigger after delete on LocationDataSets delete from Countries where CID not in (select CID from LocationDataSets);",
		// 3 - Locationcodes - LOCATIONCODES.DAT; skipped for now
		// 4 - Classes - CLASSES.DAT; skipped for now
		// 5 - Types - TYPES.DAT; skipped for now
		// 6 - Subtypes - SUBTYPES.DAT; skipped for now
		// 7 - Languages - LANGUAGES.DAT; skipped for now
		// 8 - EuroRoadNo - EUROROADNO.DAT; skipped for now;
		// 9 - Names - NAMES.DAT;
		"create cached table if not exists Names(CID integer, LID integer, NID integer, NAME varchar(100) not null, NCOMMENT varchar(100), unique(CID, NID), foreign key(CID) references Countries(CID) on delete cascade);",
		"drop index if exists Names_CID_NID_LID_idx;",
		"create index Names_CID_NID_LID_idx on Names (CID, NID, LID);",
		// 10 - NameTranslations - NAMETRANSLATIONS.DAT; skipped for now
		// 11 - SubtypeTranslations - SUBTYPETRANSLATIONS.DAT; skipped for now
		// 12 - ERNo_belongs_to_country - ERNO_BELONGS_TO_CO.DAT; skipped for now
		// 13 - AdministrativeAreas - ADMINISTRATIVEAREA.DAT;
		"create cached table if not exists AdministrativeAreas(CID integer, TABCD integer, LCD integer, CLASS varchar(1) not null, TCD integer not null, STCD integer not null, NID integer, POL_LCD integer, foreign key(CID, TABCD) references LocationDataSets(CID, TABCD) on delete cascade, unique(CID, TABCD, LCD));",
		"drop index if exists AdministrativeAreas_CID_TABCD_LCD_idx;",
		"create index AdministrativeAreas_CID_TABCD_LCD_idx on AdministrativeAreas (CID, TABCD, LCD);",
		// 14 - OtherAreas - OTHERAREAS.DAT;
		"create cached table if not exists OtherAreas(CID integer, TABCD integer, LCD integer, CLASS varchar(1) not null, TCD integer not null, STCD integer not null, NID integer, POL_LCD integer, foreign key(CID, TABCD) references LocationDataSets(CID, TABCD) on delete cascade, unique(CID, TABCD, LCD));",
		"drop index if exists OtherAreas_CID_TABCD_LCD_idx;",
		"create index OtherAreas_CID_TABCD_LCD_idx on OtherAreas (CID, TABCD, LCD);",
		// 15 - Roads - ROADS.DAT;
		"create cached table if not exists Roads(CID integer, TABCD integer, LCD integer, CLASS varchar(1) not null, TCD integer not null, STCD integer not null, ROADNUMBER varchar(10), RNID integer, N1ID integer, N2ID integer, POL_LCD integer, PES_LEV integer, RDID integer, foreign key(CID, TABCD) references LocationDataSets(CID, TABCD) on delete cascade, unique(CID, TABCD, LCD));",
		"drop index if exists Roads_CID_TABCD_LCD_idx;",
		"create index Roads_CID_TABCD_LCD_idx on Roads (CID, TABCD, LCD);",
		// 16 - Road_network_level_types - ROAD_NETWORK_LEVEL_TYPES.DAT; skipped for now
		// 17 - Segments - SEGMENTS.DAT;
		"create cached table if not exists Segments(CID integer, TABCD integer, LCD integer, CLASS varchar(1) not null, TCD integer not null, STCD integer not null, ROADNUMBER varchar(10), RNID integer, N1ID integer, N2ID integer, ROA_LCD integer, SEG_LCD integer, POL_LCD integer, RDID integer, foreign key(CID, TABCD) references LocationDataSets(CID, TABCD) on delete cascade, unique(CID, TABCD, LCD));",
		"drop index if exists Segments_CID_TABCD_LCD_idx;",
		"create index Segments_CID_TABCD_LCD_idx on Segments (CID, TABCD, LCD);",
		// 18 - Soffsets - SOFFSETS.DAT
		"create cached table if not exists Soffsets(CID integer, TABCD integer, LCD integer, NEG_OFF_LCD integer, POS_OFF_LCD integer, foreign key(CID, TABCD) references LocationDataSets(CID, TABCD) on delete cascade, unique(CID, TABCD, LCD));",
		"drop index if exists Soffsets_CID_TABCD_LCD_idx;",
		"create index Soffsets_CID_TABCD_LCD_idx on Soffsets (CID, TABCD, LCD);",
		// 19 - Seg_has_ERNo - SEG_HAS_ERNO.DAT; skipped for now
		// 20 - Points - POINTS.DAT;
		"create cached table if not exists Points(CID integer, TABCD integer, LCD integer, CLASS varchar(1) not null, TCD integer not null, STCD integer not null, JUNCTIONNUMBER varchar(10), RNID integer, N1ID integer, N2ID integer, POL_LCD integer, OTH_LCD integer, SEG_LCD integer, ROA_LCD integer, INPOS integer, INNEG integer, OUTPOS integer, OUTNEG integer, PRESENTPOS integer, PRESENTNEG integer, DIVERSIONPOS varchar(10), DIVERSIONNEG varchar(10), XCOORD decimal(8,5), YCOORD decimal(7,5), INTERRUPTSROAD integer, URBAN boolean not null, JNID integer, foreign key(CID, TABCD) references LocationDataSets(CID, TABCD) on delete cascade, unique(CID, TABCD, LCD));",
		"drop index if exists Points_CID_TABCD_LCD_idx;",
		"create index Points_CID_TABCD_LCD_idx on Points (CID, TABCD, LCD);",
		// 21 - Poffsets - POFFSETS.DAT
		"create cached table if not exists Poffsets(CID integer, TABCD integer, LCD integer, NEG_OFF_LCD integer, POS_OFF_LCD integer, foreign key(CID, TABCD) references LocationDataSets(CID, TABCD) on delete cascade, unique(CID, TABCD, LCD));",
		"drop index if exists Poffsets_CID_TABCD_LCD_idx;",
		"create index Poffsets_CID_TABCD_LCD_idx on Poffsets (CID, TABCD, LCD);"
		// 22 - Intersections - INTERSECTIONS.DAT; skipped for now
	};
	private static String dbUrl = null;
	private static Connection dbConnection = null;
	
	static BufferedReader openTMCFile(String name) throws IOException {
		BufferedReader br = new BufferedReader(new InputStreamReader(TMC.class.getResourceAsStream(name)));
		// skip first line
		br.readLine();
		return br;
	}

	/**
	 * @brief Opens a location table file and retrieves its fields.
	 * 
	 * @param file The location table file to open.
	 * @return A {@code BufferedReader} from which data can be read. The next call to its
	 * {@code read()} method will retrieve the first line of data.
	 * @throws IOException
	 */
	static BufferedReader openLTFile(File file) throws IOException {
		BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
		/*
		 * According to the original TMC spec, encoding for the LT is ISO-8859-1. However, by now
		 * TISA has certified location tables in UTF-8 encoding. Examples are Switzerland, Italy
		 * and Slovakia (all files), as well as Sweden (NAMES only). Hence we try to probe for the
		 * encoding actually used and open the file accordingly.
		 */
		try {
			String line = br.readLine();
			if (line.codePointAt(0) == 0xfeff)
				br = new BufferedReader(new InputStreamReader(new FileInputStream(file), Charset.forName("UTF-8")));
			else
				br = new BufferedReader(new InputStreamReader(new FileInputStream(file), Charset.forName("ISO-8859-1")));
		} catch (Exception e) {
			br = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
		}
		return br;
	}
	
	/**
	 * @brief Returns an array of the field names in a TMC file.
	 * 
	 * Leading byte-order markers will be stripped. Field names in the array will be ordered as in the file.
	 * 
	 * @param line The first line of the TMC file
	 * @return An array of field names.
	 */
	static String[] getFields(String line) {
		String[] comp = TMC.colonPattern.split(line);
		for (int i = 0; i < comp.length; i++) {
			/*
			 * Be sure to drop byte order markers from the first field.
			 * The code below will also catch the UTF-8 BOM (EF BB BF).
			 */
			if ((i == 0) && (comp[i].codePointAt(0) == 0xfeff))
				comp[i] = comp[i].substring(1, comp[i].length());
		}
		return comp;
	}
	
	
	/**
	 * @brief Returns the current database URL.
	 * 
	 * @return the dbUrl
	 */
	public static String getDbUrl() {
		return dbUrl;
	}

	/**
	 * @brief Sets the database URL.
	 * 
	 * @param dbUrl the dbUrl to set
	 */
	public static void setDbUrl(String dbUrl) {
		if (dbConnection != null)
			try {
				if (!dbConnection.isClosed())
					dbConnection.close();
			} catch (Exception e) {
				// NOP
			}

		TMC.dbUrl = dbUrl;
		try {
			dbConnection = DriverManager.getConnection(dbUrl);
			dbConnection.setAutoCommit(false);
		} catch (SQLException e) {
			dbUrl = null;
			e.printStackTrace(System.err);
		}
	}


	static Pattern colonPattern = Pattern.compile(";");

	private static Map<Integer, TMCEvent> EVENTS = new HashMap<Integer, TMCEvent>();
	static {
		try {
			BufferedReader br = openTMCFile("EL.DAT");
			String line;
			while((line = br.readLine()) != null) {
				TMCEvent event = new TMCEvent(line);
				EVENTS.put(event.code, event);
			}
		} catch (IOException e) {
			e.printStackTrace(System.err);
			System.exit(1);
		}
	}
	
	public static Map<Integer, SupplementaryInfo> SUPP_INFOS = new HashMap<Integer, SupplementaryInfo>();
	static {
		try {
			BufferedReader br = openTMCFile("SIL.DAT");
			String line;
			while((line = br.readLine()) != null) {
				SupplementaryInfo info = new SupplementaryInfo(line);
				SUPP_INFOS.put(info.code, info);
			}
		} catch (IOException e) {
			e.printStackTrace(System.err);
			System.exit(1);
		}
	}

	public static TMCEvent getEvent(int code) {
		TMCEvent r = EVENTS.get(code);
		if(r == null) {
			r = new TMCEvent(code + ";unknown#" + code + ";;;;;;D;1;;1;A50;");
		}
		return r;
	}
	
	private static Map<String, Country> COUNTRIES = new HashMap<String, Country>();

	public static Country getCountry(String cc, int ltn) {
		Country ret = COUNTRIES.get("ccd=" + cc + ";tabcd=" + ltn);
		if (ret == null) 
			try {
				PreparedStatement stmt = dbConnection.prepareStatement("select * from Countries where CCD = ? and CID in (select CID from LocationDataSets where TABCD = ?);");
				stmt.setString(1, cc);
				stmt.setInt(2, ltn);
				ResultSet rset = stmt.executeQuery();
				if (rset.next()) {
					Country country = new Country(rset);
					putCountry(cc, ltn, country);
					putCountry(country.cid, country);
					putCountry(country.ecc, country);
					return country;
				} else
					return null;
			} catch (SQLException e) {
				e.printStackTrace(System.err);
				return null;
			}
		return ret;
	}
	
	public static Country getCountry(int cid) {
		Country ret = COUNTRIES.get("cid=" + cid);
		if (ret == null) 
			try {
				PreparedStatement stmt = dbConnection.prepareStatement("select * from Countries where CID = ?");
				stmt.setInt(1, cid);
				ResultSet rset = stmt.executeQuery();
				if (rset.next()) {
					Country country = new Country(rset);
					putCountry(cid, country);
					putCountry(country.ecc, country);
					return country;
				} else
					return null;
			} catch (SQLException e) {
				e.printStackTrace(System.err);
				return null;
			}
		return ret;
	}
	
	public static Country getCountry(String ecc) {
		Country ret = COUNTRIES.get("ecc=" + ecc);
		if (ret == null) 
			try {
				PreparedStatement stmt = dbConnection.prepareStatement("select * from Countries where ECC = ?");
				stmt.setString(1, ecc);
				ResultSet rset = stmt.executeQuery();
				if (rset.next()) {
					Country country = new Country(rset);
					putCountry(ecc, country);
					putCountry(country.cid, country);
					return country;
				} else
					return null;
			} catch (SQLException e) {
				e.printStackTrace(System.err);
				return null;
			}
		return ret;
	}
	
	public static void putCountry(String cc, int ltn, Country country) {
		COUNTRIES.put("ccd=" + cc + ";tabcd=" + ltn, country);
	}
	
	public static void putCountry(int cid, Country country) {
		COUNTRIES.put("cid=" + cid, country);
	}
	
	public static void putCountry(String ecc, Country country) {
		COUNTRIES.put("ecc=" + ecc, country);
	}
	
	private static Map<String, LocationDataset> LOCATION_DATASETS = new HashMap<String, LocationDataset>();

	public static LocationDataset getLocationDataset(int cid, int tabcd) {
		LocationDataset ret = LOCATION_DATASETS.get(cid + ";" + tabcd);
		if (ret == null) 
			try {
				PreparedStatement stmt = dbConnection.prepareStatement("select * from LocationDataSets where CID = ? AND TABCD = ?");
				stmt.setInt(1, cid);
				stmt.setInt(2, tabcd);
				ResultSet rset = stmt.executeQuery();
				if (rset.next()) {
					LocationDataset lds = new LocationDataset(rset);
					putLocationDataset(cid, tabcd, lds);
					return lds;
				} else
					return null;
			} catch (SQLException e) {
				e.printStackTrace(System.err);
				return null;
			}
		return ret;
	}
	
	public static void putLocationDataset(int cid, int tabcd, LocationDataset locationDataset) {
		LOCATION_DATASETS.put(cid + ";" + tabcd, locationDataset);
	}
	
	private static Map<String, TMCName> NAMES = new HashMap<String, TMCName>();

	public static TMCName getName(int cid, int nid) {
		TMCName ret = NAMES.get(cid + ";" + nid);
		if (ret == null) 
			try {
				PreparedStatement stmt = dbConnection.prepareStatement("select * from Names where CID = ? AND NID = ? ORDER BY LID");
				stmt.setInt(1, cid);
				stmt.setInt(2, nid);
				ResultSet rset = stmt.executeQuery();
				if (rset.next()) {
					TMCName name = getName(rset.getInt("CID"), rset.getInt("LID"), rset.getInt("NID"));
					if (name == null)
						name = new TMCName(rset);
					putName(cid, nid, name);
					putName(cid, name.lid, nid, name);
					return name;
				} else
					return null;
			} catch (SQLException e) {
				e.printStackTrace(System.err);
				return null;
			}
		return ret;
	}

	public static TMCName getName(int cid, int lid, int nid) {
		TMCName ret = NAMES.get(cid + ";" + lid + ";" + nid);
		if (ret == null) 
			try {
				PreparedStatement stmt = dbConnection.prepareStatement("select * from Names where CID = ? AND LID = ? AND NID = ?");
				stmt.setInt(1, cid);
				stmt.setInt(2, lid);
				stmt.setInt(3, nid);
				ResultSet rset = stmt.executeQuery();
				if (rset.next()) {
					TMCName name = new TMCName(rset);
					putName(cid, lid, nid, name);
					return name;
				} else
					return null;
			} catch (SQLException e) {
				e.printStackTrace(System.err);
				return null;
			}
		return ret;
	}
	
	public static void putName(int cid, int nid, TMCName name) {
		NAMES.put(cid + ";" + nid, name);
	}
	
	public static void putName(int cid, int lid, int nid, TMCName name) {
		NAMES.put(cid + ";" + lid + ";" + nid, name);
	}
	
	private static Map<String, TMCLocation> LOCATIONS = new HashMap<String, TMCLocation>();

	public static TMCLocation getLocation(int cid, int tabcd, int lcd) {
		TMCLocation ret = LOCATIONS.get(cid + ";" + tabcd + ";" + lcd);
		if (ret == null) {
			ret = getArea(cid, tabcd, lcd);
			if (ret == null)
				ret = getRoad(cid, tabcd, lcd);
			if (ret == null)
				ret = getSegment(cid, tabcd, lcd);
			if (ret == null)
				ret = getPoint(cid, tabcd, lcd);
			if (ret != null)
				putLocation(cid, tabcd, lcd, ret);
		}
		return ret;
	}
	
	public static TMCLocation getLocation(String cc, int tabcd, int lcd) {
		Country country = getCountry(cc, tabcd);
		if (country == null)
			return null;
		TMCLocation ret = getLocation(country.cid, tabcd, lcd);
		return ret;
	}
	
	public static void putLocation(int cid, int tabcd, int lcd, TMCLocation location) {
		LOCATIONS.put(cid + ";" + tabcd + ";" + lcd, location);
	}
	
	private static Map<String, TMCArea> AREAS = new HashMap<String, TMCArea>();

	public static TMCArea getArea(int cid, int tabcd, int lcd) {
		TMCArea ret = AREAS.get(cid + ";" + tabcd + ";" + lcd);
		if (ret == null) 
			try {
				PreparedStatement stmt = dbConnection.prepareStatement("select * from AdministrativeAreas where CID = ? AND TABCD = ? AND LCD = ?");
				stmt.setInt(1, cid);
				stmt.setInt(2, tabcd);
				stmt.setInt(3, lcd);
				ResultSet rset = stmt.executeQuery();
				if (rset.next()) {
					TMCArea area = new TMCArea(rset);
					putArea(cid, tabcd, lcd, area);
					putLocation(cid, tabcd, lcd, area);
					return area;
				} else {
					stmt = dbConnection.prepareStatement("select * from OtherAreas where CID = ? AND TABCD = ? AND LCD = ?");
					stmt.setInt(1, cid);
					stmt.setInt(2, tabcd);
					stmt.setInt(3, lcd);
					rset = stmt.executeQuery();
					if (rset.next()) {
						TMCArea area = new TMCArea(rset);
						putArea(cid, tabcd, lcd, area);
						putLocation(cid, tabcd, lcd, area);
						return area;
					} else
						return null;
				}
			} catch (SQLException e) {
				e.printStackTrace(System.err);
				return null;
			}
		return ret;
	}
	
	public static void putArea(int cid, int tabcd, int lcd, TMCArea area) {
		AREAS.put(cid + ";" + tabcd + ";" + lcd, area);
	}

	private static Map<String, Road> ROADS = new HashMap<String, Road>();

	public static Road getRoad(int cid, int tabcd, int lcd) {
		Road ret = ROADS.get(cid + ";" + tabcd + ";" + lcd);
		if (ret == null) 
			try {
				PreparedStatement stmt = dbConnection.prepareStatement("select * from Roads where CID = ? AND TABCD = ? AND LCD = ?");
				stmt.setInt(1, cid);
				stmt.setInt(2, tabcd);
				stmt.setInt(3, lcd);
				ResultSet rset = stmt.executeQuery();
				if (rset.next()) {
					Road road = new Road(rset);
					putRoad(cid, tabcd, lcd, road);
					putLocation(cid, tabcd, lcd, road);
					return road;
				} else
					return null;
			} catch (SQLException e) {
				e.printStackTrace(System.err);
				return null;
			}
		return ret;
	}
	
	public static void putRoad(int cid, int tabcd, int lcd, Road road) {
		ROADS.put(cid + ";" + tabcd + ";" + lcd, road);
	}
	
	private static Map<String, Segment> SEGMENTS = new HashMap<String, Segment>();

	public static Segment getSegment(int cid, int tabcd, int lcd) {
		Segment ret = SEGMENTS.get(cid + ";" + tabcd + ";" + lcd);
		if (ret == null) 
			try {
				PreparedStatement stmt = dbConnection.prepareStatement("select * from Segments where CID = ? AND TABCD = ? AND LCD = ?");
				stmt.setInt(1, cid);
				stmt.setInt(2, tabcd);
				stmt.setInt(3, lcd);
				ResultSet rset = stmt.executeQuery();
				if (rset.next()) {
					stmt = dbConnection.prepareStatement("select * from Soffsets where CID = ? AND TABCD = ? AND LCD = ?");
					stmt.setInt(1, cid);
					stmt.setInt(2, tabcd);
					stmt.setInt(3, lcd);
					ResultSet offsets = stmt.executeQuery();
					Segment segment;
					if (offsets.next())
						segment = new Segment(rset, offsets);
					else
						segment = new Segment(rset, null);
					putSegment(cid, tabcd, lcd, segment);
					putLocation(cid, tabcd, lcd, segment);
					return segment;
				} else
					return null;
			} catch (SQLException e) {
				e.printStackTrace(System.err);
				return null;
			}
		return ret;
	}
	
	public static void putSegment(int cid, int tabcd, int lcd, Segment segment) {
		SEGMENTS.put(cid + ";" + tabcd + ";" + lcd, segment);
	}
	
	private static Map<String, TMCPoint> POINTS = new HashMap<String, TMCPoint>();

	public static TMCPoint getPoint(int cid, int tabcd, int lcd) {
		TMCPoint ret = POINTS.get(cid + ";" + tabcd + ";" + lcd);
		if (ret == null) 
			try {
				PreparedStatement stmt = dbConnection.prepareStatement("select * from Points where CID = ? AND TABCD = ? AND LCD = ?");
				stmt.setInt(1, cid);
				stmt.setInt(2, tabcd);
				stmt.setInt(3, lcd);
				ResultSet rset = stmt.executeQuery();
				TMCPoint point;
				if (rset.next()) {
					stmt = dbConnection.prepareStatement("select * from Poffsets where CID = ? AND TABCD = ? AND LCD = ?");
					stmt.setInt(1, cid);
					stmt.setInt(2, tabcd);
					stmt.setInt(3, lcd);
					ResultSet offsets = stmt.executeQuery();
					if (offsets.next())
						point = new TMCPoint(rset, offsets);
					else
						point = new TMCPoint(rset, null);
					putPoint(cid, tabcd, lcd, point);
					putLocation(cid, tabcd, lcd, point);
					return point;
				} else
					return null;
			} catch (SQLException e) {
				e.printStackTrace(System.err);
				return null;
			}
		return ret;
	}
	
	public static void putPoint(int cid, int tabcd, int lcd, TMCPoint point) {
		POINTS.put(cid + ";" + tabcd + ";" + lcd, point);
	}

	public static void readLocationTables(File path) {
		// read and execute SQL initialization script
		for (String stmtSql: initStmts) {
			try {
				PreparedStatement stmt = dbConnection.prepareStatement(stmtSql);
				stmt.execute();
				dbConnection.commit();
			} catch (SQLException e) {
				e.printStackTrace(System.err);
				return;
			}	
		}

		readLocationTablesFromDir(path);
		for (File file: path.listFiles())
			if (file.isDirectory())
				readLocationTablesFromDir(file);
		
		if (!dbUrl.startsWith("jdbc:hsqldb:mem:")) {
			// if database is not an in-memory DB, close database to compact files on disk, then reopen it
			try {
				PreparedStatement stmt = dbConnection.prepareStatement("shutdown compact;");
				stmt.execute();
				dbConnection.commit();
			} catch (SQLException e) {
				e.printStackTrace(System.err);
				return;
			}	
			String url = getDbUrl();
			setDbUrl(url);
		}
	}
	
	/**
	 * @brief Compares two version strings and determines which one is newer.
	 * 
	 * String comparison is done by breaking down both strings into groups of numeric and non-numeric
	 * characters and comparing group by group, starting with the first. When the two groups are
	 * equal, the following groups from each string are compared. The following rules apply:
	 * <ul>
	 * <li>{@code null} is less than a string.</li>
	 * <li>Two numeric groups are compared as numbers.</li>
	 * <li>If at least one of the groups is non-numeric, they are compared as strings.</li>
	 * <li>Empty groups are considered less than non-empty groups.</li>
	 * </ul>
	 * 
	 * Note that dots are treated as non-numeric groups, not as decimal separators. Thus
	 * {@code 42.10 &gt; 42.9}.
	 * 
	 * @param v1
	 * @param v2
	 * @return The result of the comparison {@code v1 >= v2}.
	 */
	public static boolean isSameOrNewerVersion(String v1, String v2) {
		if (v1 == null)
			return (v2 == null);
		ArrayList<String> arr1 = new ArrayList<String>();
		ArrayList<String> arr2 = new ArrayList<String>();
		for (int i = 0; i < v1.length(); i++) {
			if ((i == 0) || (Character.isDigit(v1.charAt(i)) != Character.isDigit(v1.charAt(i - 1))))
				arr1.add("");
			arr1.set(arr1.size() - 1, arr1.get(arr1.size() - 1) + v1.charAt(i));
		}
		for (int i = 0; i < v2.length(); i++) {
			if ((i == 0) || (Character.isDigit(v2.charAt(i)) != Character.isDigit(v2.charAt(i - 1))))
				arr2.add("");
			arr2.set(arr2.size() - 1, arr2.get(arr2.size() - 1) + v2.charAt(i));
		}
		for (int i = 0; i <= arr1.size(); i++) {
			boolean numComp; // whether to do a numeric comparison
			int cmpres;      // + for arr1[i] > arr2[i], - for arr1[i] < arr2[i], 0 for arr1[i] == arr2[i]  
			if ((i < arr1.size()) && (i >= arr2.size()))
				return true;
			if ((i == arr1.size()) && (i == arr2.size()))
				return true;
			if ((i == arr1.size()) && (i < arr2.size()))
				return false;
			if (arr1.get(i).isEmpty())
				return arr2.get(i).isEmpty();
			if (arr2.get(i).isEmpty())
				return true;
			try {
				numComp = (((Integer.parseInt(arr1.get(i)) != 0) || (arr1.get(i).charAt(0) == '0'))
					&& ((Integer.parseInt(arr2.get(i)) != 0) || (arr2.get(i).charAt(0) == '0')));
			} catch (NumberFormatException e) {
				numComp = false;
			}
			if (numComp)
				cmpres = Integer.parseInt(arr1.get(i)) - Integer.parseInt(arr2.get(i));
			else
				cmpres = arr1.get(i).compareTo(arr2.get(i));
			if (cmpres > 0)
				return true;
			else if (cmpres < 0)
				return false;
		}
		// This should never happen, but Eclipse will nag about a missing return value if I omit the last return statement.
		System.err.println(String.format("wtf? Comparison of %s and %s produced no result, assuming true",
				Arrays.toString(arr1.toArray()),
				Arrays.toString(arr2.toArray())));
		return true;
	}
	
	/**
	 * @brief Determines if the location data set at {@code path} needs to be imported in the database,
	 * and removes older versions of the data set.
	 * 
	 * A location data set will be imported into the database if the database does not yet contain a
	 * data set with the same CID and TABCD, or if it contains an older version. In the latter case,
	 * this method will delete all data associated with the older version.
	 * 
	 * If the folder at {@code path} does not hold a valid location data set (specifically, if its
	 * {@code LOCATIONDATASETS.DAT} file is not found), the result will be {@code false}. 
	 * 
	 * @param path The path to the folder which holds the files of the new data set
	 * @return {@code true} if an import is needed, {@code false} if not.
	 */
	public static boolean prepareDataSetUpdate(File path) {
		boolean ret = false;
		String version = null;
		File file = new File(path.getAbsolutePath() + File.separator + "LOCATIONDATASETS.DAT");
		PreparedStatement stmt = null;
		int cid = -1;
		int tabcd = -1;
		if (file.exists()) {
			try {
				BufferedReader br = openLTFile(file);
				String line = br.readLine();
				String[] fields = getFields(line);
				line = br.readLine();
				String[] values = TMC.colonPattern.split(line);
				for (int i = 0; i < fields.length; i++) {
					if (fields[i].equals("CID"))
						cid = Integer.parseInt(values[i]);
					else if (fields[i].equals("TABCD"))
						tabcd = Integer.parseInt(values[i]);
					else if (fields[i].equals("VERSION"))
						version = values[i];
				}
				if ((cid == -1) || (tabcd == -1))
					throw new IllegalArgumentException();
				stmt = dbConnection.prepareStatement("select * from LocationDataSets where CID = ? and TABCD = ?;");
				stmt.setInt(1, cid);
				stmt.setInt(2, tabcd);
				ResultSet rset = stmt.executeQuery();
				if (!rset.next()) {
					System.out.println(String.format("Location data set in %s is not in DB yet, importing", path.getAbsolutePath()));
					ret = true;
				} else if (isSameOrNewerVersion(version, rset.getString("VERSION"))) {
					System.out.println(String.format("Location data set in %s is newer than DB or same age, importing", path.getAbsolutePath()));
					ret = true;
				} else {
					System.out.println(String.format("Location data set in %s is older than DB, skipping", path.getAbsolutePath()));
					return false;
				}

				if (ret) {
					stmt = dbConnection.prepareStatement("delete from LocationDataSets where CID = ? and TABCD = ?;");
					stmt.setInt(1, cid);
					stmt.setInt(2, tabcd);
					stmt.executeUpdate();
					dbConnection.commit();
				}
				return ret;
			} catch (Exception e) {
				System.out.println(String.format("File %s is invalid, skipping", file.getAbsolutePath()));
				return false;
			}
		} else {
			System.out.println(String.format("No LOCATIONDATASETS.DAT in %s, skipping", path.getAbsolutePath()));
			return false;
		}
	}

	public static void readLocationTablesFromDir(File path) {
		File file;
		
		if (!prepareDataSetUpdate(path)) {
			return;
		}
		
		// 1 - COUNTRIES.DAT;
		file = new File(path.getAbsolutePath() + File.separator + "COUNTRIES.DAT");
		importTable("Countries", file);

		// 2 - LOCATIONDATASETS.DAT;
		file = new File(path.getAbsolutePath() + File.separator + "LOCATIONDATASETS.DAT");
		importTable("LocationDataSets", file);
		
		// 3 - LOCATIONCODES.DAT; skipped for now
		// 4 - CLASSES.DAT; skipped for now
		// 5 - TYPES.DAT; skipped for now
		// 6 - SUBTYPES.DAT; skipped for now
		// 7 - LANGUAGES.DAT; skipped for now
		// 8 - EUROROADNO-DAT; skipped for now;

		// 9 - NAMES.DAT;
		file = new File(path.getAbsolutePath() + File.separator + "NAMES.DAT");
		importTable("Names", file);
		
		// 10 - NAMETRANSLATIONS.DAT; skipped for now
		// 11 - SUBTYPETRANSLATIONS.DAT; skipped for now
		// 12 - ERNO_BELONGS_TO_CO.DAT; skipped for now

		// 13 - ADMINISTRATIVEAREA.DAT;
		file = new File(path.getAbsolutePath() + File.separator + "ADMINISTRATIVEAREA.DAT");
		importTable("AdministrativeAreas", file);

		// 14 - OTHERAREAS.DAT;
		file = new File(path.getAbsolutePath() + File.separator + "OTHERAREAS.DAT");
		importTable("OtherAreas", file);

		// 15 - ROADS.DAT;
		file = new File(path.getAbsolutePath() + File.separator + "ROADS.DAT");
		importTable("Roads", file);
		
		// 16 - ROAD_NETWORK_LEVEL_TYPES.DAT; skipped for now

		// 17 - SEGMENTS.DAT;
		file = new File(path.getAbsolutePath() + File.separator + "SEGMENTS.DAT");
		importTable("Segments", file);

		// 18 - SOFFSETS.DAT
		file = new File(path.getAbsolutePath() + File.separator + "SOFFSETS.DAT");
		importTable("Soffsets", file);
		
		// 19 - SEG_HAS_ERNO.DAT; skipped for now

		// 20 - POINTS.DAT;
		file = new File(path.getAbsolutePath() + File.separator + "POINTS.DAT");
		importTable("Points", file);
		
		// 21 - POFFSETS.DAT
		file = new File(path.getAbsolutePath() + File.separator + "POFFSETS.DAT");
		importTable("Poffsets", file);
		
		// 22 - INTERSECTIONS.DAT; skipped for now
	}
	
	/**
	 * @brief Returns the SQL types for the specified columns of the specified table.
	 * 
	 * @param table The table name for which to return column types
	 * @param fields An array containing the field names
	 * @return An array of {@link java.sql.Types} constants representing the SQL types. The order
	 * matches that of {@code fields}. If one of the values in {@code fields} does not correspond
	 * to a column of the table specified by {@code table}, its type is {@link java.sql.Types.NULL}.
	 * @throws SQLException
	 */
	static int[] getColumnTypes(String table, String[] fields) throws SQLException {
		int[] res = new int[fields.length];
		PreparedStatement stmt = dbConnection.prepareStatement(String.format("select * from %s limit 1;", table));
		ResultSet rs = stmt.executeQuery();
		for (int i = 0; i < fields.length; i++) {
			try {
				res[i] = rs.getMetaData().getColumnType(rs.findColumn(fields[i]));
			} catch (SQLException e) {
				System.err.println(String.format("Could not determine type for column %s:", fields[i]));
				e.printStackTrace(System.err);
				res[i] = Types.NULL;
			}
		}
		return res;
	}
	
	/** @brief Imports a new location table file into the database.
	 * 
	 * @param table The table in which the record will be stored
	 * @param file The file to import (a file from a LT in exchange format)
	 */
	static void importTable(String table, File file) {
		PreparedStatement stmt = null;
		Boolean hasConstraintViolations = false;
		System.out.println(String.format("Processing table %s from file %s", table, file.getAbsolutePath()));
		if (file.exists()) {
			try {
				BufferedReader br = openLTFile(file);
				String line = br.readLine();
				String[] fields = getFields(line);
				int[] types = getColumnTypes(table, fields);
				String[] values;
				StringBuilder stmtBuilder = new StringBuilder("insert into ");
				stmtBuilder.append(table);
				stmtBuilder.append(" (");
				for (int i = 0; i < fields.length; i++) {
					if (stmtBuilder.charAt(stmtBuilder.length() - 1) != '(')
						stmtBuilder.append(", ");
					stmtBuilder.append(fields[i]);
				}
				stmtBuilder.append(") VALUES (");
				for (int i = 0; i < fields.length; i++) {
					if (stmtBuilder.charAt(stmtBuilder.length() - 1) != '(')
						stmtBuilder.append(", ");
					stmtBuilder.append("?");
				}
				stmtBuilder.append(");");
				stmt = dbConnection.prepareStatement(stmtBuilder.toString());
				while((line = br.readLine()) != null)
					if (line.length() > 0) {
						stmt.clearParameters();
						values = TMC.colonPattern.split(line);
						for (int i = 0; i < fields.length; i++) {
							if ((i >= values.length) || values[i].isEmpty())
								stmt.setNull(i + 1, types[i]);
							else
								switch (types[i]) {
									case Types.BOOLEAN:
										stmt.setBoolean(i + 1, Boolean.parseBoolean(values[i]));
										break;
									case Types.DECIMAL:
										/* Special case: this is the only type which requires conversion.
										 * DECIMAL is currently used only for two columns, Points.XCOORD and Points.YCOORD.
										 * Both are represented as 1/100000s of a degree in the source files and are
										 * converted to degrees on import.
										 * If further DECIMAL columns are introduced in the DB at a later stage, extra
										 * logic may be needed here.
										 */
										stmt.setFloat(i + 1, Integer.parseInt(values[i]) / 100000.0f);
										break;
									case Types.INTEGER:
										stmt.setInt(i + 1, Integer.parseInt(values[i]));
										break;
									case Types.VARCHAR:
										stmt.setString(i + 1, values[i]);
										break;
									default:
										System.err.println(String.format("Unknown type for parameter %d (%s.%s), type %d", i, table, fields[i], types[i]));
										stmt.setNull(i + 1, types[i]);
								}
						}
						
						try {
							stmt.executeUpdate();
						} catch (SQLIntegrityConstraintViolationException e) {
							hasConstraintViolations = true;
						}
					}
				dbConnection.commit();
			} catch (IOException e) {
				e.printStackTrace(System.err);
				return;
			} catch (SQLException e) {
				if (stmt != null)
					System.err.println(String.format("Error executing: %s", stmt.toString()));
				e.printStackTrace(System.err);
				return;
			}
			if (hasConstraintViolations)
				System.err.println(String.format("Some records from %s were skipped due to integrity constraint violations.", file.getAbsolutePath()));
		}
	}
}
