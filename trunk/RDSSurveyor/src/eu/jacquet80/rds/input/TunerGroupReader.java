package eu.jacquet80.rds.input;

public interface TunerGroupReader extends GroupReader {
	public boolean isStereo();

	public int setFrequency(int frequency);

	public int getFrequency();

	public int mute();

	public int unmute();

	public int getSignalStrength();
}
