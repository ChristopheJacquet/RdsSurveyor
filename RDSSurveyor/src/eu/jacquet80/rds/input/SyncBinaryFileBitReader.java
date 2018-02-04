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

import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

public class SyncBinaryFileBitReader extends BitReader {
	private final InputStream isr;
	private int oct;
	private int octPtr;
	private int bytePtr;
	
	public SyncBinaryFileBitReader(InputStream isr) throws FileNotFoundException {
		this.isr = isr;
		try {
			// read two bytes at the start
			isr.read(); isr.read(); 
		} catch (IOException e) {
			e.printStackTrace();
		}
		oct = 0;
		octPtr = 0;
		bytePtr = 3;
	}
	
	public SyncBinaryFileBitReader(File f) throws FileNotFoundException {
		this(new FileInputStream(f));
	}
	
	
	public boolean getBit() throws IOException {
		oct = oct<<1;

		if(octPtr==0) {
			oct = isr.read();
			bytePtr = (bytePtr + 1) % 4;
			if(oct == -1) {
				throw new EOFException();
			}
			octPtr=8;
		}

		octPtr--;
		if(bytePtr == 3 && octPtr == 6) octPtr = 0;
		//System.err.print(((oct & 128)>>7) + "(" + octPtr + ")");
		return (oct&128) != 0;
	}


}
