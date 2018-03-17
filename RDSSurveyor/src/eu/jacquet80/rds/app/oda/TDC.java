package eu.jacquet80.rds.app.oda;

import java.io.PrintWriter;

import eu.jacquet80.rds.app.Application;
import eu.jacquet80.rds.core.RDS;
import eu.jacquet80.rds.log.RDSTime;

public abstract class TDC extends Application {
	private final String fullName;
	
	public TDC() {
		String tdcAppName = getTDCAppName();
		fullName = tdcAppName != null ? "TDC-" + tdcAppName : "TDC";
	}
	
	@Override
	public String getName() {
		return fullName;
	}

	@Override
	public void receiveGroup(PrintWriter console, int type, int version, int[] blocks,
			boolean[] blocksOk, RDSTime time) {
		if(blocksOk[1] && blocksOk[2] && blocksOk[3]) {
			int channel = blocks[1] & 0x1F;
			int[] octet = new int[] {
					(blocks[2] >> 8) & 0xFF, blocks[2] & 0xFF, 
					(blocks[3] >> 8) & 0xFF, blocks[3] & 0xFF};
			String str = 
					Character.toString(RDS.toChar(octet[0])) +
					Character.toString(RDS.toChar(octet[1])) +
					Character.toString(RDS.toChar(octet[2])) +
					Character.toString(RDS.toChar(octet[3]));
			console.printf("Channel %02d: \"%s\"  ", channel, str);
			
			String res = processTDCData(channel, octet);
			console.print(res);
		}
	}
	
	@SuppressWarnings("rawtypes")
	private static Class preferredTDCApp;
	
	public static boolean setPreferredTDCApp(String tdcApp) {
		if("CATRADIO".equals(tdcApp)) {
			preferredTDCApp = CatalunyaRadioTDC.class;
			return true;
		} else return false;
	}
	
	@SuppressWarnings("unchecked")
	public static TDC createPreferredTDCApp() {
		if(preferredTDCApp != null) {
			try {
				return (TDC) preferredTDCApp.getConstructor().newInstance();
			} catch (Exception e) {
				System.err.println("Something went bad when instantiating a TDC app. This is a bug.");
			}
		}
		
		return new DefaultTDC();
	}

	protected abstract String getTDCAppName();
	protected abstract String processTDCData(int channel, int[] contents);

}
