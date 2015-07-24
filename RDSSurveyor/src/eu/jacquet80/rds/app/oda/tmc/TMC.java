package eu.jacquet80.rds.app.oda.tmc;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

public class TMC {
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
	static BufferedReader openLTFile(File file, Map<String, Integer> fields) throws IOException {
		BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
		// get field order from first line
		fields.clear();
		String line = br.readLine();
		String[] comp = TMC.colonPattern.split(line);
		for (int i = 0; i < comp.length; i++) {
			/*
			 * Be sure to drop byte order markers from the first field.
			 * The code below will also catch the UTF-8 BOM (EF BB BF).
			 */
			if ((i == 0) && (comp[i].codePointAt(0) == 0xfeff))
				comp[i] = comp[i].substring(1, comp[i].length());
			fields.put(comp[i], i);
		}
		return br;
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
		readLocationTablesFromDir(path);
		for (File file: path.listFiles())
			if (file.isDirectory())
				readLocationTablesFromDir(file);
	}

	public static void readLocationTablesFromDir(File path) {
		File file;
		Map<String, Integer> fields = new HashMap<String, Integer>();

		// COUNTRIES.DAT;
		file = new File(path.getAbsolutePath() + File.separator + "COUNTRIES.DAT");
		if (file.exists()) {
			try {
				BufferedReader br = openLTFile(file, fields);
				String line;
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

		// LOCATIONDATASETS.DAT;
		file = new File(path.getAbsolutePath() + File.separator + "LOCATIONDATASETS.DAT");
		if (file.exists()) {
			try {
				BufferedReader br = openLTFile(file, fields);
				String line;
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

		// NAMES.DAT;
		file = new File(path.getAbsolutePath() + File.separator + "NAMES.DAT");
		if (file.exists()) {
			try {
				BufferedReader br = openLTFile(file, fields);
				String line;
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

		// TODO ADMINISTRATIVEAREA.DAT, OTHERAREAS.DAT

		// ROADS.DAT;
		file = new File(path.getAbsolutePath() + File.separator + "ROADS.DAT");
		if (file.exists()) {
			try {
				BufferedReader br = openLTFile(file, fields);
				String line;
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

		// SEGMENTS.DAT;
		file = new File(path.getAbsolutePath() + File.separator + "SEGMENTS.DAT");
		if (file.exists()) {
			try {
				BufferedReader br = openLTFile(file, fields);
				String line;
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

		// SOFFSETS.DAT
		file = new File(path.getAbsolutePath() + File.separator + "SOFFSETS.DAT");
		if (file.exists()) {
			try {
				BufferedReader br = openLTFile(file, fields);
				String line;
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

		// POINTS.DAT;
		file = new File(path.getAbsolutePath() + File.separator + "POINTS.DAT");
		if (file.exists()) {
			try {
				BufferedReader br = openLTFile(file, fields);
				String line;
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
		
		// POFFSETS.DAT
		file = new File(path.getAbsolutePath() + File.separator + "POFFSETS.DAT");
		if (file.exists()) {
			try {
				BufferedReader br = openLTFile(file, fields);
				String line;
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
	}
}
