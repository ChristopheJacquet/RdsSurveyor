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

import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

public class SyncBinaryFileBitReader implements BitReader {
	private final InputStream isr;
	private int oct;
	private int octPtr;
	private int bytePtr;
	
	public SyncBinaryFileBitReader(File f) throws FileNotFoundException {
		isr = new FileInputStream(f);
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
