package eu.jacquet80.rds.app.oda;

public class DefaultTDC extends TDC {

	@Override
	protected String getTDCAppName() {
		return null;
	}

	@Override
	protected String processTDCData(int channel, int[] contents) {
		return "";
	}

}
