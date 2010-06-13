package eu.jacquet80.rds.app.oda;

public class AlertCwithAlertPlus extends AlertC {
	public static final int AID = 0x4B02; // Alert-C with Alert-Plus
	// I have no info on this, I only have France Inter samples from 2000 that made use of this AID

	@Override
	public String getName() {
		return "TMC/Alert-C with Alert-Plus";
	}
	
	@Override
	public int getAID() {
		return AID;
	}
}
