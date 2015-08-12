package eu.jacquet80.rds.app.oda.tmc;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

public class TMC {
	// TODO index, primary key
	private static final String[] initStmts = {
		// 1 - Countries - COUNTRIES.DAT;
		"create table if not exists Countries(CID integer, ECC varchar(2), CCD varchar(1), CNAME varchar(50));",
		"drop index if exists Countries_ECC_idx;",
		"drop index if exists Countries_CID_idx;",
		"drop index if exists Countries_CCD_idx;",
		"create index Countries_ECC_idx ON Countries (ECC);",
		"create index Countries_CID_idx ON Countries (CID);",
		"create index Countries_CCD_idx ON Countries (CCD);",
		// 2 - LocationDataSets - LOCATIONDATASETS.DAT;
		"create table if not exists LocationDataSets(CID integer, TABCD integer, DCOMMENT varchar(100), VERSION varchar(7), VERSIONDESCRIPTION varchar(100));",
		"drop index if exists LocationDataSets_CID_TABCD_idx;",
		"create index LocationDataSets_CID_TABCD_idx on LocationDataSets (CID, TABCD);",
		// 3 - Locationcodes - LOCATIONCODES.DAT; skipped for now
		// 4 - Classes - CLASSES.DAT; skipped for now
		// 5 - Types - TYPES.DAT; skipped for now
		// 6 - Subtypes - SUBTYPES.DAT; skipped for now
		// 7 - Languages - LANGUAGES.DAT; skipped for now
		// 8 - EuroRoadNo - EUROROADNO-DAT; skipped for now;
		// 9 - Names - NAMES.DAT;
		"create table if not exists Names(CID integer, LID integer, NID integer, NAME varchar(100), NCOMMENT varchar(100));",
		"drop index if exists Names_CID_LID_NID_idx;",
		"create index Names_CID_LID_NID_idx on Names (CID, LID, NID);",
		// 10 - NameTranslations - NAMETRANSLATIONS.DAT; skipped for now
		// 11 - SubtypeTranslations - SUBTYPETRANSLATIONS.DAT; skipped for now
		// 12 - ERNo_belongs_to_country - ERNO_BELONGS_TO_CO.DAT; skipped for now
		// 13 - AdministrativeAreas - ADMINISTRATIVEAREA.DAT;
		"create table if not exists AdministrativeAreas(CID integer, TABCD integer, LCD integer, CLASS varchar(1), TCD integer, STCD integer, NID integer, POL_LCD integer);",
		"drop index if exists AdministrativeAreas_CID_TABCD_LCD_idx;",
		"create index AdministrativeAreas_CID_TABCD_LCD_idx on AdministrativeAreas (CID, TABCD, LCD);",
		// 14 - OtherAreas - OTHERAREAS.DAT;
		"create table if not exists OtherAreas(CID integer, TABCD integer, LCD integer, CLASS varchar(1), TCD integer, STCD integer, NID integer, POL_LCD integer);",
		"drop index if exists OtherAreas_CID_TABCD_LCD_idx;",
		"create index OtherAreas_CID_TABCD_LCD_idx on OtherAreas (CID, TABCD, LCD);",
		// 15 - Roads - ROADS.DAT;
		"create table if not exists Roads(CID integer, TABCD integer, LCD integer, CLASS varchar(1), TCD integer, STCD integer, ROADNUMBER varchar(10), RNID integer, N1ID integer, N2ID integer, POL_LCD integer, PES_LEV integer, RDID integer);",
		"drop index if exists Roads_CID_TABCD_LCD_idx;",
		"create index Roads_CID_TABCD_LCD_idx on Roads (CID, TABCD, LCD);",
		// 16 - Road_network_level_types - ROAD_NETWORK_LEVEL_TYPES.DAT; skipped for now
		// 17 - Segments - SEGMENTS.DAT;
		"create table if not exists Segments(CID integer, TABCD integer, LCD integer, CLASS varchar(1), TCD integer, STCD integer, ROADNUMBER varchar(10), RNID integer, N1ID integer, N2ID integer, ROA_LCD integer, SEG_LCD integer, POL_LCD integer, RDID integer);",
		"drop index if exists Segments_CID_TABCD_LCD_idx;",
		"create index Segments_CID_TABCD_LCD_idx on Segments (CID, TABCD, LCD);",
		// 18 - Soffsets - SOFFSETS.DAT
		"create table if not exists Soffsets(CID integer, TABCD integer, LCD integer, NEG_OFF_LCD integer, POS_OFF_LCD integer);",
		"drop index if exists Soffsets_CID_TABCD_LCD_idx;",
		"create index Soffsets_CID_TABCD_LCD_idx on Soffsets (CID, TABCD, LCD);",
		// 19 - Seg_has_ERNo - SEG_HAS_ERNO.DAT; skipped for now
		// 20 - Points - POINTS.DAT;
		"create table if not exists Points(CID integer, TABCD integer, LCD integer, CLASS varchar(1), TCD integer, STCD integer, JUNCTIONNUMBER varchar(10), RNID integer, N1ID integer, N2ID integer, POL_LCD integer, OTH_LCD integer, SEG_LCD integer, ROA_LCD integer, INPOS integer, INNEG integer, OUTPOS integer, OUTNEG integer, PRESENTPOS integer, PRESENTNEG integer, DIVERSIONPOS varchar(10), DIVERSIONNEG varchar(10), XCOORD decimal(8,5), YCOORD decimal(7,5), INTERRUPTSROAD integer, URBAN boolean, JNID integer);",
		"drop index if exists Points_CID_TABCD_LCD_idx;",
		"create index Points_CID_TABCD_LCD_idx on Points (CID, TABCD, LCD);",
		// 21 - Poffsets - POFFSETS.DAT
		"create table if not exists Poffsets(CID integer, TABCD integer, LCD integer, NEG_OFF_LCD integer, POS_OFF_LCD integer);",
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
	 * This method opens a location table in MFF exchange format and analyzes its first line for
	 * field names. The map passed as {@code fields} will receive an entry with the field names as
	 * keys and their numeric indices (the first field is referred to as zero). Existing entries in
	 * the map will be cleared. 
	 * 
	 * @param file The location table file to open.
	 * @param fields The map which will receive field indices.
	 * @return A {@code BufferedReader} from which data can be read. The next call to its
	 * {@code read()} method will retrieve the first line of data.
	 * @throws IOException
	 */
	static BufferedReader openLTFile(File file) throws IOException {
		BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
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
	
	static Map<String, Integer> getFieldsOld(String line) {
		Map<String, Integer> fields = new HashMap<String, Integer>();
		String[] comp = getFields(line);
		for (int i = 0; i < comp.length; i++) {
			fields.put(comp[i], i);
		}
		return fields;
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
		// TODO handle cases in which DB is already open (close current DB connection or prevent change)
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
		return COUNTRIES.get("ccd=" + cc + ";tabcd=" + ltn);
	}
	
	public static Country getCountry(int ecc) {
		return COUNTRIES.get("ecc=" + ecc);
	}
	
	private static Map<String, LocationDataset> LOCATION_DATASETS = new HashMap<String, LocationDataset>();

	public static LocationDataset getLocationDataset(int cid, int tabcd) {
		return LOCATION_DATASETS.get(cid + ";" + tabcd);
	}
	
	private static Map<String, TMCName> NAMES = new HashMap<String, TMCName>();

	public static TMCName getName(int cid, int nid) {
		return NAMES.get(cid + ";" + nid);
	}

	public static TMCName getName(int cid, int lid, int nid) {
		return NAMES.get(cid + ";" + lid + ";" + nid);
	}
	
	private static Map<String, TMCLocation> LOCATIONS = new HashMap<String, TMCLocation>();

	public static TMCLocation getLocation(int cid, int tabcd, int lcd) {
		return LOCATIONS.get(cid + ";" + tabcd + ";" + lcd);
	}
	
	public static TMCLocation getLocation(String cc, int tabcd, int lcd) {
		Country country = getCountry(cc, tabcd);
		if (country == null)
			return null;
		return LOCATIONS.get(country.cid + ";" + tabcd + ";" + lcd);
	}
	
	private static Map<String, TMCArea> AREAS = new HashMap<String, TMCArea>();

	public static TMCArea getArea(int cid, int tabcd, int lcd) {
		return AREAS.get(cid + ";" + tabcd + ";" + lcd);
	}
	
	private static Map<String, Road> ROADS = new HashMap<String, Road>();

	public static Road getRoad(int cid, int tabcd, int lcd) {
		return ROADS.get(cid + ";" + tabcd + ";" + lcd);
	}
	
	private static Map<String, Segment> SEGMENTS = new HashMap<String, Segment>();

	public static Segment getSegment(int cid, int tabcd, int lcd) {
		return SEGMENTS.get(cid + ";" + tabcd + ";" + lcd);
	}
	
	private static Map<String, TMCPoint> POINTS = new HashMap<String, TMCPoint>();

	public static TMCPoint getPoint(int cid, int tabcd, int lcd) {
		return POINTS.get(cid + ";" + tabcd + ";" + lcd);
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
	}

	public static void readLocationTablesFromDir(File path) {
		File file;
		Map<String, Integer> fields = new HashMap<String, Integer>();
		
		// 1 - COUNTRIES.DAT;
		file = new File(path.getAbsolutePath() + File.separator + "COUNTRIES.DAT");
		if (file.exists()) {
			importTable("Countries", file);
			try {
				BufferedReader br = openLTFile(file);
				String line = br.readLine();
				fields = getFieldsOld(line);
				while((line = br.readLine()) != null)
					if (line.length() > 0) {
						Country country = new Country(line, fields);
						if (!"".equals(country.ecc))
							COUNTRIES.put("ecc=" + country.ecc, country);
						COUNTRIES.put("cid=" + country.cid, country);
					}
			} catch (IOException e) {
				e.printStackTrace(System.err);
				System.exit(1);
			}
		}

		// 2 - LOCATIONDATASETS.DAT;
		file = new File(path.getAbsolutePath() + File.separator + "LOCATIONDATASETS.DAT");
		if (file.exists()) {
			importTable("LocationDataSets", file);
			try {
				BufferedReader br = openLTFile(file);
				String line = br.readLine();
				fields = getFieldsOld(line);
				while((line = br.readLine()) != null)
					if (line.length() > 0) {
						LocationDataset ds = new LocationDataset(line, fields);
						LOCATION_DATASETS.put(ds.cid + ";" + ds.tabcd, ds);

						// Add entry to COUNTRIES so the country can be found using CC + LTN
						Country country = COUNTRIES.get("cid=" + ds.cid);
						if (country != null) {
							COUNTRIES.put("ccd=" + country.ccd + ";tabcd=" + ds.tabcd, country);
						}
					}
			} catch (IOException e) {
				e.printStackTrace(System.err);
				System.exit(1);
			}
		}
		
		// 3 - LOCATIONCODES.DAT; skipped for now
		// 4 - CLASSES.DAT; skipped for now
		// 5 - TYPES.DAT; skipped for now
		// 6 - SUBTYPES.DAT; skipped for now
		// 7 - LANGUAGES.DAT; skipped for now
		// 8 - EUROROADNO-DAT; skipped for now;

		// 9 - NAMES.DAT;
		file = new File(path.getAbsolutePath() + File.separator + "NAMES.DAT");
		if (file.exists()) {
			importTable("Names", file);
			try {
				BufferedReader br = openLTFile(file);
				String line = br.readLine();
				fields = getFieldsOld(line);
				while((line = br.readLine()) != null)
					if (line.length() > 0) {
						TMCName name = new TMCName(line, fields);
						NAMES.put(name.cid + ";" + name.lid + ";" + name.nid, name);

						// add the first name found as the default name (which can be found without a LID)
						if (NAMES.get(name.cid + ";" + name.nid) == null)
							NAMES.put(name.cid + ";" + name.nid, name);
					}
			} catch (IOException e) {
				e.printStackTrace(System.err);
				System.exit(1);
			}
		}
		
		// 10 - NAMETRANSLATIONS.DAT; skipped for now
		// 11 - SUBTYPETRANSLATIONS.DAT; skipped for now
		// 12 - ERNO_BELONGS_TO_CO.DAT; skipped for now

		// 13 - ADMINISTRATIVEAREA.DAT;
		file = new File(path.getAbsolutePath() + File.separator + "ADMINISTRATIVEAREA.DAT");
		if (file.exists()) {
			importTable("AdministrativeAreas", file);
			try {
				BufferedReader br = openLTFile(file);
				String line = br.readLine();
				fields = getFieldsOld(line);
				while((line = br.readLine()) != null)
					if (line.length() > 0) {
						TMCArea area = new TMCArea(line, fields);
						AREAS.put(area.cid + ";" + area.tabcd + ";" + area.lcd, area);
						LOCATIONS.put(area.cid + ";" + area.tabcd + ";" + area.lcd, area);
					}
			} catch (IOException e) {
				e.printStackTrace(System.err);
				System.exit(1);
			}
		}

		// 14 - OTHERAREAS.DAT;
		file = new File(path.getAbsolutePath() + File.separator + "OTHERAREAS.DAT");
		if (file.exists()) {
			importTable("OtherAreas", file);
			try {
				BufferedReader br = openLTFile(file);
				String line = br.readLine();
				fields = getFieldsOld(line);
				while((line = br.readLine()) != null)
					if (line.length() > 0) {
						TMCArea area = new TMCArea(line, fields);
						AREAS.put(area.cid + ";" + area.tabcd + ";" + area.lcd, area);
						LOCATIONS.put(area.cid + ";" + area.tabcd + ";" + area.lcd, area);
					}
			} catch (IOException e) {
				e.printStackTrace(System.err);
				System.exit(1);
			}
		}

		// 15 - ROADS.DAT;
		file = new File(path.getAbsolutePath() + File.separator + "ROADS.DAT");
		if (file.exists()) {
			importTable("Roads", file);
			try {
				BufferedReader br = openLTFile(file);
				String line = br.readLine();
				fields = getFieldsOld(line);
				while((line = br.readLine()) != null)
					if (line.length() > 0) {
						Road road = new Road(line, fields);
						ROADS.put(road.cid + ";" + road.tabcd + ";" + road.lcd, road);
						LOCATIONS.put(road.cid + ";" + road.tabcd + ";" + road.lcd, road);
					}
			} catch (IOException e) {
				e.printStackTrace(System.err);
				System.exit(1);
			}
		}
		
		// 16 - ROAD_NETWORK_LEVEL_TYPES.DAT; skipped for now

		// 17 - SEGMENTS.DAT;
		file = new File(path.getAbsolutePath() + File.separator + "SEGMENTS.DAT");
		if (file.exists()) {
			importTable("Segments", file);
			try {
				BufferedReader br = openLTFile(file);
				String line = br.readLine();
				fields = getFieldsOld(line);
				while((line = br.readLine()) != null)
					if (line.length() > 0) {
						Segment segment = new Segment(line, fields);
						SEGMENTS.put(segment.cid + ";" + segment.tabcd + ";" + segment.lcd, segment);
						LOCATIONS.put(segment.cid + ";" + segment.tabcd + ";" + segment.lcd, segment);
					}
			} catch (IOException e) {
				e.printStackTrace(System.err);
				System.exit(1);
			}
		}

		// 18 - SOFFSETS.DAT
		file = new File(path.getAbsolutePath() + File.separator + "SOFFSETS.DAT");
		if (file.exists()) {
			importTable("Soffsets", file);
			try {
				BufferedReader br = openLTFile(file);
				String line = br.readLine();
				fields = getFieldsOld(line);
				while((line = br.readLine()) != null)
					if (line.length() > 0) {
						TMCOffset offset = new TMCOffset(line, fields);
						Segment segment = getSegment(offset.cid, offset.tabcd, offset.lcd);
						if (segment != null)
							segment.setOffset(offset);
					}
			} catch (IOException e) {
				e.printStackTrace(System.err);
				System.exit(1);
			}
		}
		
		// 19 - SEG_HAS_ERNO.DAT; skipped for now

		// 20 - POINTS.DAT;
		file = new File(path.getAbsolutePath() + File.separator + "POINTS.DAT");
		if (file.exists()) {
			importTable("Points", file);
			try {
				BufferedReader br = openLTFile(file);
				String line = br.readLine();
				fields = getFieldsOld(line);
				while((line = br.readLine()) != null)
					if (line.length() > 0) {
						TMCPoint point = new TMCPoint(line, fields);
						POINTS.put(point.cid + ";" + point.tabcd + ";" + point.lcd, point);
						LOCATIONS.put(point.cid + ";" + point.tabcd + ";" + point.lcd, point);
					}
			} catch (IOException e) {
				e.printStackTrace(System.err);
				System.exit(1);
			}
		}
		
		// 21 - POFFSETS.DAT
		file = new File(path.getAbsolutePath() + File.separator + "POFFSETS.DAT");
		if (file.exists()) {
			importTable("Poffsets", file);
			try {
				BufferedReader br = openLTFile(file);
				String line = br.readLine();
				fields = getFieldsOld(line);
				while((line = br.readLine()) != null)
				if (line.length() > 0) {
					TMCOffset offset = new TMCOffset(line, fields);
					TMCPoint point = getPoint(offset.cid, offset.tabcd, offset.lcd);
					if (point != null)
						point.setOffset(offset);
				}
			} catch (IOException e) {
				e.printStackTrace(System.err);
				System.exit(1);
			}
		}
		
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
		System.out.println(String.format("Processing table %s from file %s", table, file.getAbsolutePath())); // TODO remove
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
						stmt.executeUpdate();
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
		}
	}
}
