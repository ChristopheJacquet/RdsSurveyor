package eu.jacquet80.rds.app.oda.tmc;

import java.io.BufferedReader;
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

	
	static Pattern colonPattern = Pattern.compile(";");

	public static Map<Integer, TMCEvent> EVENTS = new HashMap<Integer, TMCEvent>();
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
}
