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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;


public class DumpFileBitReader implements BitReader {
	private final InputStream is;
	private int nibble;
	private int nibblePtr;
	private final OutputStream os;
	private boolean logOn = true;
	
	public void setLogOn() {
		logOn = true;
	}
	
	public void setLogOff() {
		logOn = false;
	}
	
	public DumpFileBitReader(File f, File of) throws FileNotFoundException {
		is = new FileInputStream(f);
		if(of != null) os = new FileOutputStream(of); else os = null;
		nibble = 0;
		nibblePtr = 0;
	}
	
	public boolean getBit() throws IOException {
		nibble = nibble<<1;

		if(nibblePtr==0) {
			do {
				nibble = Integer.MAX_VALUE;
				int c = is.read();
				if(logOn && os != null && c != -1) os.write(c);
				
				/*if(c == -1) {
					throw new EOFException();
				}*/
				
				if(c>='0' && c<='9') nibble = c - '0';
				if(c>='A' && c<='F') nibble = c - 'A' + 10;
			} while(nibble == Integer.MAX_VALUE);
			
			nibblePtr=4;
		}

		nibblePtr--;
		return (nibble&8) == 0;   // == 0 pour l'inversion avec optocoupleur, != 0 sans inversion
	}

}
