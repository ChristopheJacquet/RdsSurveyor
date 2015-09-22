/*
 RDS Surveyor -- RDS decoder, analyzer and monitor tool and library.
 For more information see
   http://www.jacquet80.eu/
   http://rds-surveyor.sourceforge.net/
 
 Copyright (c) 2009-2012 Christophe Jacquet

 This file is part of RDS Surveyor.

 RDS Surveyor is free software: you can redistribute it and/or modify
 it under the terms of the GNU Lesser Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 RDS Surveyor is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Lesser Public License for more details.

 You should have received a copy of the GNU Lesser Public License
 along with RDS Surveyor.  If not, see <http://www.gnu.org/licenses/>.

*/


package eu.jacquet80.rds.input;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.Semaphore;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.Line;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.TargetDataLine;

import eu.jacquet80.rds.input.group.FrequencyChangeEvent;
import eu.jacquet80.rds.input.group.GroupEvent;
import eu.jacquet80.rds.input.group.GroupReaderEvent;
import eu.jacquet80.rds.log.RealTime;

public class NativeTunerGroupReader extends TunerGroupReader {
	private boolean newGroups;
	private TunerData data = new TunerData();
	private static final String dir, sep;
	private boolean audioCapable = false;
	private boolean audioPlaying = false;
	private final Semaphore resumePlaying = new Semaphore(0);

	
	@Override
	public boolean isStereo() {
		return data.stereo;
	}

	@Override
	public native int setFrequency(int frequency);

	@Override
	public int getFrequency() {
		return data.frequency;
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
	public int getSignalStrength() {
		return data.rssi;
	}

	@Override
	public void tune(boolean up) {
		int freq = data.frequency + (up ? 100 : -100);
		
		if(freq > 108000) freq = 87500;
		if(freq < 87500) freq = 108000;
		
		data.frequency = setFrequency(freq);
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
	public GroupReaderEvent getGroup() throws IOException {
		int oldFreq = data.frequency;
		
		readTuner();
		
		if(data.frequency != oldFreq) {
			// if frequency has just been changed, must report an event
			return new FrequencyChangeEvent(new RealTime(), data.frequency);
		}
		
		if(!data.groupReady) return null;
		
		int[] res = new int[4];
		for(int i=0; i<4; i++) {
			if(data.err[i] > 0) res[i] = -1;
			else res[i] = data.block[i] & 0xFFFF;
		}
		
		newGroups = true;
		return new GroupEvent(new RealTime(), res, false);
	}
	
	public NativeTunerGroupReader(String filename) throws UnavailableInputMethod {
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
			data.frequency = 87500;
		} else {
			throw new UnavailableInputMethod(
					aFilename + ": no device found");
		}
		
		SoundPlayer p = new SoundPlayer();
		if(audioCapable) {
			p.start();
		}
	}
	

	private native int readTuner();
	private native boolean open();
	
	public static void main(String[] args) throws IOException, UnavailableInputMethod {
		String path = dir + sep + "si470x.dylib";
		NativeTunerGroupReader r = new NativeTunerGroupReader(path);
		//System.out.println("Tuned to: " + r.setFrequency(95400));
		while(true)	{
			System.out.println(r.getGroup());
		}
	}

	@Override
	public boolean isSynchronized() {
		return data.rdsSynchronized;
	}
	
	static {
		dir = System.getProperty("user.dir");
		sep = System.getProperty("file.separator");
	}


	private final static String[] okVendors = {
		"SILICON",
		"www.rding.cn",
		"ADS"
	};
	
	private final static String[] okNames = {
		"FM Radio",
		"Radio",
		"ADS"
	};

	
	private class SoundPlayer extends Thread {
		private Mixer mixer = null;
		private TargetDataLine inLine;
		private SourceDataLine outLine;

		public SoundPlayer() {
			StringBuffer audioDevices = new StringBuffer();
			
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
				
				audioDevices.append('"').append(vendor).append("\"/\"")
					.append(name).append("\"   ");
			}
			
			if(mixer == null) {
				System.out.println("Native tuner: not found a recognized audio device.");
				System.out.println("Audio devices: " + audioDevices);
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


	@Override
	public boolean isAudioCapable() {
		return audioCapable;
	}

	@Override
	public boolean isPlayingAudio() {
		return audioPlaying;
	}
}

class TunerData {
	public short[] block = {-1, -1, -1, -1};
	public short[] err = {9, 9, 9, 9};
	public boolean groupReady;
	public boolean rdsSynchronized;
	public boolean stereo;
	
	/** Received Signal Strength Indicator, 0..65535 */
	public int rssi;
	
	/** Frequency in kHz */
	public int frequency;
}