package eu.jacquet80.rds.input;

public abstract class TunerGroupReader extends GroupReader {
	public abstract boolean isStereo();
	
	public abstract boolean isSynchronized();

	public abstract int setFrequency(int frequency);

	public abstract int getFrequency();

	public abstract int mute();

	public abstract int unmute();
	
	public abstract boolean isAudioCapable();
	
	public abstract boolean isPlayingAudio();

	public abstract int getSignalStrength();
	
	public abstract void tune(boolean up);
	
	public abstract boolean seek(boolean up);
	
	public abstract String getDeviceName();
	
	public abstract boolean newGroups();
}
