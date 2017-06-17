package eu.jacquet80.rds.input;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PrintStream;
import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.SourceDataLine;

import eu.jacquet80.rds.core.BitStreamSynchronizer;
import eu.jacquet80.rds.core.BitStreamSynchronizer.Status;
import eu.jacquet80.rds.input.group.FrequencyChangeEvent;
import eu.jacquet80.rds.input.group.GroupReaderEvent;
import eu.jacquet80.rds.log.RealTime;

public class SdrGroupReader extends TunerGroupReader {
	/** The sample rate at which we receive data from the tuner. */
	private static final int sampleRate = 128000;
	
	/** The sample rate for audio output. */
	private static final int outSampleRate = 48000;
	
	/* The stream from which demodulated audio data is read, linked to tunerOut */
	private final PipedInputStream syncIn;
	/* The stream to which the native plugin writes demodulated audio data. */
	private final DataOutputStream tunerOut;
	/* The audio bit reader which handles audio stream decoding and provides an audio input stream */
	private final AudioBitReader reader;
	private final BitStreamSynchronizer synchronizer;
	private final InputStream audioStream;
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
	
	private static final String dir, sep;
	private boolean audioCapable = false;
	private boolean audioPlaying = false;
	
	/* Whether audio was suspended (e.g. for a seek operation) and should resume once the operation
	 * is complete */
	private boolean wasPlaying = false;
	
	private final Semaphore resumePlaying = new Semaphore(0);


	public SdrGroupReader(PrintStream console, String filename) throws UnavailableInputMethod, IOException {
		syncIn = new PipedInputStream();
		tunerOut = new DataOutputStream(new PipedOutputStream(syncIn));
		reader = new AudioBitReader(new DataInputStream(syncIn), sampleRate);
		reader.setAudioSampleRate(outSampleRate);
		audioStream = reader.getAudioMirrorStream();
		synchronizer = new BitStreamSynchronizer(console, reader);
		
		synchronizer.addStatusChangeListener(new BitStreamSynchronizer.StatusChangeListener() {
			@Override
			public void report(Status status) {
				synced = (status == Status.SYNCED) ? true : false;
			}
		});

		File path = new File(filename);
		String absoluteLibPath = path.getAbsolutePath();
		String aFilename = path.getName();

		try {
			System.load(absoluteLibPath);
		} catch(UnsatisfiedLinkError e) {
			throw new UnavailableInputMethod(
					aFilename + ": cannot load library");
		}

		if(open()) {
			System.out.println(
					aFilename + ": device found, using it!");
			setFrequency(87500);
		} else {
			throw new UnavailableInputMethod(
					aFilename + ": no device found");
		}
		
		SoundPlayer p = new SoundPlayer();
		if(audioCapable) {
			p.start();
		}
	}

	@Override
	public boolean isStereo() {
		return false; // TODO implement stereo
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
		return audioCapable;
	}

	@Override
	public boolean isPlayingAudio() {
		return audioPlaying;
	}
	
	/**
	 * @brief Returns the signal strength of the current station.
	 * 
	 * Signal strength is expressed as a value between 0 and 65535, which corresponds to a range
	 * from -30 dBm to +45 dBm. This mapping was modeled after the Si470x driver, which obtains
	 * RSSI by reading from the chip's 0x0a register, which provides a 8-bit value (0-255). This is
	 * then multiplied by 873 (which would allow for a 0-75 input range without causing an
	 * overflow). Silicon Labs documentation (AN230) anecdotally indicates a practical range of 0
	 * to +45 dB, which is roughly congruent with the values reported by the RTL2832U.
	 * 
	 * If the actual signal strength is outside the boundaries, the nearest boundary is returned.
	 * 
	 * The return value of this function can be calculated from signal strength as follows:
	 * signal = rssi * 873
	 * 
	 * Vice versa:
	 * rssi = signal / 873;
	 * 
	 * @return Signal strength
	 */
	@Override
	public int getSignalStrength() {
		int signal = (int)((getRssi()) * 873);
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

	/**
	 * @brief Seek to the next good station in the specified direction.
	 * 
	 * This method returns immediately, the actual seek operation typically has not completed by
	 * that time.
	 * 
	 * @param up If {@code true}, seek up, else, seek down.
	 * 
	 * @return Always false
	 */
	@Override
	public boolean seek(boolean up) {
		wasPlaying = audioPlaying;
		
		if (wasPlaying)
			mute();
		return nativeSeek(up);
	}

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
	 * @brief JNI wrapper for the native call which performs the actual seek operation.
	 * 
	 * This method is called by {@link #seek(boolean)}. No other code should ever need to call this
	 * method directly.
	 * 
	 * @param up If {@code true}, seek up, else, seek down.
	 * 
	 * @return Always false
	 */
	private native boolean nativeSeek(boolean up);

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
			if (wasPlaying)
				unmute();
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
	 * supported (TODO) and RDS data is decoded from the audio stream returned by the SDR.
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


	private class SoundPlayer extends Thread {
		private SourceDataLine outLine;

		public SoundPlayer() {
			try {
				AudioFormat outFormat =  
						new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, outSampleRate, 16, 1, 2, outSampleRate, false);
				DataLine.Info outInfo = new DataLine.Info(SourceDataLine.class, outFormat, 4 * outSampleRate);
				outLine = (SourceDataLine) AudioSystem.getLine(outInfo);
				outLine.open(outFormat);
			} catch(Exception e) {
				System.out.println("SDR: could not open output line:");
				System.out.println("\t" + e);
				return;
			}
			
			if (sampleRate < outSampleRate) {
				System.out.println(String.format("SDR: sample rate must be %d or higher", outSampleRate));
				return;
			}
			
			audioCapable = true;
			audioPlaying = false;
			wasPlaying = true;
		}
		
		@Override
		public void run() {
			byte[] inData = new byte[sampleRate / 2];
			
			outLine.start();
			reader.startPlaying();
			
			// simple audio pass through
			while(true) {
				try {
					int len = audioStream.read(inData, 0, inData.length);
					outLine.write(inData, 0, len);
				} catch (IOException e) {
					e.printStackTrace();
				}
				
				if(! audioPlaying) {
					reader.stopPlaying();
					outLine.stop();
					outLine.flush();
					resumePlaying.acquireUninterruptibly();
					outLine.start();
					reader.startPlaying();
				}
			}
		}
	}
}
