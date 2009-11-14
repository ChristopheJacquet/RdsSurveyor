/*
 RDS Surveyor -- RDS decoder, analyzer and monitor tool and library.
 For more information see
   http://www.jacquet80.eu/
   http://rds-surveyor.sourceforge.net/
 
 Copyright (c) 2009 Christophe Jacquet

 Permission is hereby granted, free of charge, to any person
 obtaining a copy of this software and associated documentation
 files (the "Software"), to deal in the Software without
 restriction, including without limitation the rights to use,
 copy, modify, merge, publish, distribute, sublicense, and/or sell
 copies of the Software, and to permit persons to whom the
 Software is furnished to do so, subject to the following
 conditions:

 The above copyright notice and this permission notice shall be
 included in all copies or substantial portions of the Software.

 THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 OTHER DEALINGS IN THE SOFTWARE.
*/

package eu.jacquet80.rds.input;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class TeeBitReader implements BitReader {
	private final BitReader reader;
	private final FileOutputStream writer;
	
	private int currentByte = 0;
	private int currentBitCount = 0;
	private int currentByteCount = 0;
	
	public TeeBitReader(BitReader reader, File of) throws IOException {
		this.reader = reader;
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
