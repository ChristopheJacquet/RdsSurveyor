/*
 RDS Surveyor -- RDS decoder, analyzer and monitor tool and library.
 For more information see
   http://www.jacquet80.eu/
   http://rds-surveyor.sourceforge.net/

 Copyright 2010 Christophe Jacquet
 Copyright 2010 Dominique Matz

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

import java.io.IOException;

import eu.jacquet80.rds.input.group.FrequencyChangeEvent;
import eu.jacquet80.rds.input.group.GroupEvent;
import eu.jacquet80.rds.input.group.GroupReaderEvent;
import eu.jacquet80.rds.log.RealTime;

public class V4LTunerGroupReader extends TunerGroupReader {
	// See V4L2 Spec, section 4.11 <http://v4l2spec.bytesex.org/spec/x7607.htm>
	
	private final static String LIB_NAME = "v4ltuner";
	private boolean newGroups;
	private int oldFreq = 0;	// if !=0, means that the frequency has just been adjusted
	
	public V4LTunerGroupReader(String device) {
		int res = open(device);
		if(res != 1) throw new RuntimeException("Device " + device + " failed to open: " +res);
		if(! hasRDS()) throw new RuntimeException("Device " + device + " correctly opened, but it is not RDS-capable.");
	}
	
	public native synchronized boolean isStereo();
	public native synchronized int setFrequency(int frequency);
	public native synchronized int getFrequency();
	public native synchronized int mute();
	public native synchronized int unmute();
	public native synchronized int getSignalStrength();
	public native synchronized void hwSeek(boolean up);
    private native synchronized int open(String device);
    private native synchronized int close();
    private native synchronized boolean hasRDS();
    private native byte[] getRDSData();

	
	@Override
	public GroupReaderEvent getGroup() throws IOException {
		int newFreq = getFrequency();
		if(newFreq != oldFreq) {
			// if frequency has just been changed, must report an event
			oldFreq = newFreq;
			return new FrequencyChangeEvent(new RealTime(), newFreq);
		}
		
		int[] res = new int[4];
		byte[] data;
		
		// read 4 blocks of offsets 0, 1, 2, 3
		for(int i=0; i<4; i++) {
			int blockOffset;
			do {
				data = getRDSData();
				
				if(data.length != 3) {
					//System.out.println("WARNING: read " + data.length + " bytes");
					return null;
				}

				blockOffset = data[2] & 0x7;
				// special handling of block C'
				if(blockOffset == 4) blockOffset = 2;
				if(blockOffset != i) System.out.println("<SLIP got " + blockOffset + ", expecting " + i + ">");
			} while(blockOffset != i);
			
			res[i] = (data[0] & 0xFF) | ((data[1] & 0xFF) << 8);
			
			if((data[2] & 0xC0) != 0) res[i] = -1;
		}
		
		newGroups = true;
		return new GroupEvent(new RealTime(), res, false);
	}

	static {
		try {
			System.loadLibrary(LIB_NAME);
		} catch (UnsatisfiedLinkError e) {
			System.err.println("Can not find file "
					+ System.mapLibraryName(LIB_NAME));
			throw e;
		}
	}


	@Override
	protected void finalize() throws Throwable {
		close();
	}
	
	@Override
	public String getDeviceName() {
		return "Video4Linux";
	}

	/*
	@Override
	public void seek(boolean up) {
		hwSeek(up);
	}
	*/
	
	
	@Override
	public boolean seek(boolean up) {
		hwSeek(up);
		return true;
		/*
		int steps = 0;
		// 87500 to 108000 => 410 50kHz steps
		do {
			tune(up);
			steps++;
		} while(getSignalStrength() < 30000 && steps<410);
		*/
	}
	

	@Override
	public void tune(boolean up) {
		//System.out.println("tune " + up);
		int freq = getFrequency();
		//System.out.println("starting from: " + freq);
		
		freq += up ? 100 : -100;
		
		if(freq > 108000) freq = 87500;
		if(freq < 87500) freq = 108000;
		
		setFrequency(freq);
		//System.out.println("tuned");
	}

	@Override
	public boolean newGroups() {
		boolean ng = newGroups;
		newGroups = false;
		return ng;
	}

	@Override
	public boolean isSynchronized() {
		return true;
	}

	@Override
	public boolean isAudioCapable() {
		return false;
	}

	@Override
	public boolean isPlayingAudio() {
		return false;
	}
}
