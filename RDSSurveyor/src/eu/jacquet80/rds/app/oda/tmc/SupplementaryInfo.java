package eu.jacquet80.rds.app.oda.tmc;

public class SupplementaryInfo {
	public final int code;
	public final String text;

	SupplementaryInfo(String line) {
		String[] comp = TMC.colonPattern.split(line);
		this.code = Integer.parseInt(comp[1]);
		this.text = comp[2];
	}
	
	@Override
	public String toString() {
		return "[" + this.code + "] " + this.text;
	}
}
