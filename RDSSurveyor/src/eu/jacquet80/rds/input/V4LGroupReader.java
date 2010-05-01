/*
 RDS Surveyor -- RDS decoder, analyzer and monitor tool and library.
 For more information see
   http://www.jacquet80.eu/
   http://rds-surveyor.sourceforge.net/
 
 Copyright (c) 2009, 2010 Christophe Jacquet

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
import java.io.FileInputStream;
import java.io.IOException;

public class V4LGroupReader implements GroupReader {
	// See V4L2 Spec, section 4.11 <http://v4l2spec.bytesex.org/spec/x7607.htm>
	
	private final FileInputStream reader;
	private final byte[] recvBuffer = new byte[3];
	
	public V4LGroupReader(File f) throws IOException {
		reader = new FileInputStream(f);
		
		/*
		boolean gotD = false;
		byte[] buf = new byte[3];
		do {
			if(reader.read(buf) != 3) throw new IOException("Could not read from Video4Linux radio device.");
			if((buf[2] & 3) == 3) gotD = true;
		} while(! gotD);
		*/
	}
	
	@Override
	public int[] getGroup() throws IOException {
		int[] res = new int[4];
		boolean error;

		// read 4 blocks of offsets 0, 1, 2, 3
		for(int i=0; i<4; i++) {
			int numRead;
			int blockOffset;
			do {
				do {
					numRead = reader.read(recvBuffer);
				} while(numRead != 3);
				blockOffset = recvBuffer[2] & 0x3;
				if(blockOffset != i) System.out.println("<SLIP got " + blockOffset + ", expecting " + i + ">");
			} while(blockOffset != i);
			
			res[i] = (recvBuffer[0] & 0xFF) | ((recvBuffer[1] & 0xFF) << 8);
			
			if((recvBuffer[2] & 0xC0) != 0) res[i] = -1;
			
			/*
			error =
				(recvBuffer[2] & 0x80) != 0 || 
				(recvBuffer[5] & 0x80) != 0 || 
				(recvBuffer[8] & 0x80) != 0 || 
				(recvBuffer[11] & 0x80) != 0;
				*/
			//error = false;
			
			/*
			System.out.printf("ERR: %02X %02X %02X %02X\n", recvBuffer[2], recvBuffer[5], recvBuffer[8], recvBuffer[11]);
			System.out.flush();
			*/
			
		}
		
		return res;
	}

}
