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
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;


public class DumpFileBitReader extends BitReader {
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
