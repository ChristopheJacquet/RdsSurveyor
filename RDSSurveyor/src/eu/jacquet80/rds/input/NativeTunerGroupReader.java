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

import eu.jacquet80.rds.input.group.FrequencyChangeEvent;
import eu.jacquet80.rds.input.group.GroupEvent;
import eu.jacquet80.rds.input.group.GroupReaderEvent;
import eu.jacquet80.rds.log.RealTime;

public class NativeTunerGroupReader extends TunerGroupReader {
	private boolean newGroups;
	private TunerData data = new TunerData();
	private static final String dir, sep;

	
	@Override
	public boolean isStereo() {
		return data.stereo;
	}

	@Override
	public native int setFrequency(int frequency);

	@Override
	public int getFrequency() {
		//System.out.println("*** " + data.frequency);
		return data.frequency;
	}

	@Override
	public int mute() {
		return 0;
	}

	@Override
	public int unmute() {
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
		String aFilename = path.getAbsolutePath();
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