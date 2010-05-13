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

public class V4LTunerGroupReader implements TunerGroupReader {
	// See V4L2 Spec, section 4.11 <http://v4l2spec.bytesex.org/spec/x7607.htm>
	
	private final static String LIB_NAME = "v4ltuner";
	
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
    private native synchronized int open(String device);
    private native synchronized int close();
    private native synchronized boolean hasRDS();
    private native synchronized byte[] getRDSData();

	
	@Override
	public int[] getGroup() throws IOException {
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

				blockOffset = data[2] & 0x3;
				if(blockOffset != i) System.out.println("<SLIP got " + blockOffset + ", expecting " + i + ">");
			} while(blockOffset != i);
			
			res[i] = (data[0] & 0xFF) | ((data[1] & 0xFF) << 8);
			
			if((data[2] & 0xC0) != 0) res[i] = -1;
		}
		
		return res;
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
}
