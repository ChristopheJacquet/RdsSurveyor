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
import java.io.FileOutputStream;
import java.io.IOException;

public class TeeBitReader extends BitReader {
	private final BitReader reader;
	private final FileOutputStream writer;
	
	private int currentByte = 0;
	private int currentBitCount = 0;
	private int currentByteCount = 0;
	
	public TeeBitReader(BitReader reader, File of) throws IOException {
		this.reader = reader;
		setParent(reader);
		writer = new FileOutputStream(of);
	}
	
	public boolean getBit() throws IOException {
		boolean bit = reader.getBit();
		currentByte <<= 1;
		if(bit) currentByte |= 1;
		currentBitCount++;
		if(currentBitCount == 8) {
			writer.write(currentByte);
			currentByte = 0;
			currentBitCount = 0;
			currentByteCount++;
			if(currentByteCount == 20) {
				writer.flush();
				currentByteCount = 0;
			}
		}
		return bit;
	}

	@Override
	protected void finalize() throws Throwable {
		writer.close();
	}
}
