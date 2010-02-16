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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

public class HexFileGroupReader implements GroupReader {
	private final BufferedReader br;
	
	public HexFileGroupReader(File file) throws FileNotFoundException {
		br = new BufferedReader(new FileReader(file));
	}
	
	
	public int[] getGroup() throws IOException {
		String line = br.readLine();
		if(line == null) throw new IOException("End of file");
		
		String[] components = line.split("\\s");
		if(components.length < 4) throw new IOException("Not enough blocks on line \"" + line + "\"");
		int[] res = new int[4];
		
		for(int i=0; i<4; i++) {
			res[i] = Integer.parseInt(components[components.length-4+i], 16);
		}
		
		return res;
	}

}
