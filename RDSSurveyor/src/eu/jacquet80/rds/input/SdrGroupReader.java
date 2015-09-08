package eu.jacquet80.rds.input;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PrintStream;
import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import eu.jacquet80.rds.core.BitStreamSynchronizer;
import eu.jacquet80.rds.core.BitStreamSynchronizer.Status;
import eu.jacquet80.rds.input.group.FrequencyChangeEvent;
import eu.jacquet80.rds.input.group.GroupReaderEvent;
import eu.jacquet80.rds.log.RealTime;

public class SdrGroupReader extends TunerGroupReader {
	/** The sample rate at which we receive data from the tuner. */
	private static final int sampleRate = 250000;
	
	private final PipedInputStream syncIn;
	private final DataOutputStream tunerOut;
	private final BitStreamSynchronizer synchronizer;
	private boolean synced = false;
	private boolean newGroups;
	
	/* Read/write lock for frequency-related members, must be acquired prior to accessing them */
	private ReentrantReadWriteLock frequencyLock = new ReentrantReadWriteLock();
	/* Frequency in kHz */
	private int mFrequency;
	/* Whether the frequency has changed */
	private boolean mFrequencyChanged;
	
	/* Received signal strength in dBm. Always use getter method for this. */
	private Float mRssi = new Float(0);
	
	// TODO check which members we need
	private static final String dir, sep;
	private boolean audioCapable = false;
	private boolean audioPlaying = false;
	private final Semaphore resumePlaying = new Semaphore(0);

	public SdrGroupReader(PrintStream console, String filename) throws UnavailableInputMethod, IOException {
		File path = new File(filename);
		String absoluteLibPath = path.getAbsolutePath();
		String aFilename = path.getName();

		try {
			System.load(absoluteLibPath);
		} catch(UnsatisfiedLinkError e) {
			throw new UnavailableInputMethod(
					aFilename + ": cannot load library");
		}

		syncIn = new PipedInputStream();
		tunerOut = new DataOutputStream(new PipedOutputStream(syncIn));
		synchronizer = new BitStreamSynchronizer(console, new AudioBitReader(new DataInputStream(syncIn), sampleRate));
		
		synchronizer.addStatusChangeListener(new BitStreamSynchronizer.StatusChangeListener() {
			@Override
			public void report(Status status) {
				synced = (status == Status.SYNCED) ? true : false;
			}
		});

		if(open()) {
			System.out.println(
					aFilename + ": device found, using it!");
			setFrequency(87500);
		} else {
			throw new UnavailableInputMethod(
					aFilename + ": no device found");
		}
		
		/* TODO implement sound
		SoundPlayer p = new SoundPlayer();
		if(audioCapable) {
			p.start();
		}
		*/
	}

	@Override
	public boolean isStereo() {
		return false; // TODO implement sound
		//return data.stereo;
	}

	@Override
	public boolean isSynchronized() {
		return synced;
	}

	/**
	 * @brief Switches to a new frequency.
	 * 
	 * Note that this function will request a frequency change and return immediately. The actual
	 * frequency change typically happens after the function returns. After changing the frequency,
	 * the native tuner driver will call {@link #onFrequencyChanged(int)}. To determine if the
	 * frequency has been changed, call {@link #getFrequency()}.
	 * 
	 * @param frequency The new frequency, in kHz.
	 * @return If the request was accepted, the return value is the new frequency, in kHz. If the
	 * frequency cannot be changed at this time for whatever reason, the return value is zero. Note
	 * that even an accepted request does not guarantee that the frequency will be changed.
	 */
	@Override
	public native int setFrequency(int frequency);

	/**
	 * @brief Returns the current tuner frequency in kHz.
	 */
	@Override
	public int getFrequency() {
		int ret;
		frequencyLock.readLock().lock();
		try {
			ret = mFrequency;
		} finally {
			frequencyLock.readLock().unlock();
		}
		return ret;
	}

	@Override
	public int mute() {
		audioPlaying = false;
		return 0;
	}

	@Override
	public int unmute() {
		audioPlaying = true;
		resumePlaying.release();
		return 0;
	}

	@Override
	public boolean isAudioCapable() {
		return false; // TODO implement sound
		// return audioCapable;
	}

	@Override
	public boolean isPlayingAudio() {
		return false; // TODO implement sound
		// return audioPlaying;
	}
	
	/**
	 * @brief Returns the signal strength of the current station.
	 * 
	 * Signal strength is expressed as a value between 0 and 65535, which corresponds to a range
	 * from -30 dBm to +10 dBm. These values were chosen because the noise level is slightly above
	 * -30 dBm, whereas strong nearby transmitters with a minimum of block errors reach +10 dBm and
	 * more. It also places -10 dBm, which is the practical limit for RDS reception, in the middle
	 * of the range.
	 * 
	 * If the actual signal strength is outside the boundaries, the nearest boundary is returned.
	 * 
	 * The return value of this function can be calculated from signal strength as follows:
	 * signal = (rssi + 30) * 8192 / 5
	 * 
	 * Vice versa:
	 * rssi = signal * 5 / 8192 - 30;
	 * 
	 * @return Signal strength
	 */
	@Override
	public int getSignalStrength() {
		int signal = (int)((getRssi() + 30) * 8192 / 5);  // 8192 / 5 = 65536 / 40
		if (signal < 0)
			signal = 0;
		else if (signal > 0xFFFF)
			signal = 0xFFFF;
		
		return signal;
	}

	@Override
	public void tune(boolean up) {
		int freq = getFrequency() + (up ? 100 : -100);
		
		if(freq > 108000) freq = 87500;
		if(freq < 87500) freq = 108000;
		
		setFrequency(freq);
	}

	@Override
	public native boolean seek(boolean up);

	@Override
	public native String getDeviceName();

	@Override
	public boolean newGroups() {
		boolean ng = newGroups;
		newGroups = false;
		return ng;
	}

	@Override
	public GroupReaderEvent getGroup() throws IOException, EndOfStream {
		GroupReaderEvent ret = null;
		
		readTuner(); // this is here for legacy reasons, the method currently does nothing
		
		if (isFrequencyChanged()) {
			// if frequency has just been changed, must report an event
			return new FrequencyChangeEvent(new RealTime(), getFrequency());
		}
		
		ret = synchronizer.getGroup();
		
		if (ret != null) {
			newGroups = true;
		}
		return ret;
	}
	
	/**
	 * @brief Returns current signal strength in dBm.
	 * 
	 * This method synchronizes all member access and can thus be called from any thread.
	 */
	private float getRssi() {
		float ret;
		synchronized(mRssi) {
			ret = mRssi;
		}
		return ret;
	}
	
	/**
	 * @brief Whether the frequency has changed since the last call to this method.
	 * 
	 * Note that this method resets the internal flag every time it is called. That is, even if the
	 * result is {@true}, subsequent calls to this method will return {@code false} unless the
	 * frequency has been changed again since the last call.
	 * 
	 * Also, if this method returns {@code true}, the frequency may have changed more than once
	 * since the last call.
	 * 
	 * This method synchronizes all member access and can thus be called from any thread.
	 */
	private boolean isFrequencyChanged() {
		boolean res = false;
		frequencyLock.writeLock().lock();
		try {
			res = mFrequencyChanged;
			mFrequencyChanged = false;
		} finally {
			frequencyLock.writeLock().unlock();
		}
		return res;
	}
	
	/**
	 * @brief Called when the tuner frequency has been changed successfully
	 * 
	 * This method notifies the {@code SdrGroupReader} about a successful frequency change.
	 * Native tuner drivers must call it immediately after changing the tuner frequency.
	 * This method synchronizes all member access and can thus be called from any thread. 
	 * 
	 * @param frequency The new frequency, in kHz
	 */
	private void onFrequencyChanged(int frequency) {
		frequencyLock.writeLock().lock();
		try {
			this.mFrequency = frequency;
			this.mFrequencyChanged = true;
		} finally {
			frequencyLock.writeLock().unlock();
		}
	}
	
	/**
	 * @brief Called when the signal strength has changed.
	 * 
	 * This method notifies the {@code SdrGroupReader} about a change in signal strength. Native
	 * tuner drivers must call it upon detecting a change. While multiple calls for the same RSSI
	 * are permissible, drivers should for performance reasons avoid calling this method unless the
	 * RSSI has actually changed. This method synchronizes all member access and can thus be called
	 * from any thread.
	 * 
	 * @param rssi
	 */
	private void onRssiChanged(float rssi) {
		synchronized(mRssi) {
			mRssi = rssi;
		}
	}
	
	/**
	 * @brief Reads data from the tuner and stores it internally.
	 * 
	 * For {@code SdrGroupReader}, this method does nothing.
	 * 
	 * Other implementations use it to retrieve RDS data, stereo flags, and RSSI from the tuner,
	 * requiring it to be called before the respective methods. This makes sense for a tuner but
	 * not for a SDR: RSSI is retrieved through an event handler method, stereo is currently not
	 * relevant due to lack of audio support (TODO) and RDS data is decoded from the audio stream
	 * returned by the SDR.
	 *  
	 * @return 0
	 */
	private int readTuner() {
		return 0;
	}
	
	private native boolean open();

	static {
		dir = System.getProperty("user.dir");
		sep = System.getProperty("file.separator");
	}


	/* TODO implement sound
	private final static String[] okVendors = { // TODO fix these
		"SILICON",
		"www.rding.cn"
	};
	
	private final static String[] okNames = { // TODO fix these
		"FM Radio",
		"Radio"
	};

	
	private class SoundPlayer extends Thread {
		private Mixer mixer = null;
		private TargetDataLine inLine;
		private SourceDataLine outLine;

		public SoundPlayer() {
			
			for(Mixer.Info mixInfo : AudioSystem.getMixerInfo()) {
				String vendor = mixInfo.getVendor();
				if(vendor != null) vendor = vendor.split(" ")[0];
				
				String name = mixInfo.getName();
				if(name != null) name = name.split(" ")[0];
				
				if(Arrays.asList(okVendors).contains(vendor) ||
						Arrays.asList(okNames).contains(name)) {

					System.out.println("Trying to use audio device: '" + mixInfo.getVendor() + 
							"', '" + mixInfo.getName() + "'");
					
					mixer = AudioSystem.getMixer(mixInfo);
					if(mixer != null) break;
				}
			}
			
			if(mixer == null) {
				System.out.println("Native tuner: not found audio device.");
				return;
			}
			
			try {
				Line.Info[] linesInfo = mixer.getTargetLineInfo();
				inLine = (TargetDataLine) mixer.getLine(linesInfo[0]);
				AudioFormat inFormat =  
						new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, 96000, 8, 1, 1, 96000, false);
				inLine.open(inFormat);

				AudioFormat outFormat =  
						new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, 48000, 16, 1, 2, 48000, false);
				DataLine.Info outInfo = new DataLine.Info(SourceDataLine.class, outFormat, 4*48000);
				outLine = (SourceDataLine) AudioSystem.getLine(outInfo);
				outLine.open(outFormat);
			} catch(Exception e) {
				System.out.println("Native tuner: audio device, but could not open lines:");
				System.out.println("\t" + e);
				return;
			}
			
			System.out.println("Both USB stick audio and sound card audio configured successfully");
		
			audioCapable = true;
			audioPlaying = true;
		}
		
		@Override
		public void run() {
			byte[] data = new byte[24000];
			
			inLine.start();
			outLine.start();
			
			// simple audio pass through
			while(true) {
				inLine.read(data, 0, data.length);
				outLine.write(data, 0, data.length);
				
				if(! audioPlaying) {
					inLine.stop();
					outLine.stop();
					inLine.flush();
					outLine.flush();
					resumePlaying.acquireUninterruptibly();
					inLine.start();
					outLine.start();
				}
			}
		}
	}
	*/
}
