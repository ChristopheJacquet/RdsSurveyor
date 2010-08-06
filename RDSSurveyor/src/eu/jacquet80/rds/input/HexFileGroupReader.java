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

import eu.jacquet80.rds.input.group.FrequencyChangeEvent;
import eu.jacquet80.rds.input.group.GroupEvent;
import eu.jacquet80.rds.input.group.GroupReaderEvent;

public class HexFileGroupReader implements GroupReader {
	private final BufferedReader br;
	private int bitTime = 0;
	
	public HexFileGroupReader(File file) throws FileNotFoundException {
		br = new BufferedReader(new FileReader(file));
	}
	
	
	public GroupReaderEvent getGroup() throws IOException, EndOfStream {
		String line;
		boolean ok;
		
		do {
			ok = true;
			line = br.readLine();
			///System.out.println("[Read: " + line + "] ");
			if(line == null) throw new EndOfStream();
			if(line.startsWith("%")) {
				if(line.startsWith("% Freq")) {
					return new FrequencyChangeEvent(0);
					// TODO FIXME: parse frequency here
				} else ok = false;
			}
		} while(! ok);    // ignore lines beginning with '%' (metadata and possibly comments)
		
		///System.out.println("OK");
		
		String[] components = line.trim().split("\\s+");
		if(components.length < 4) throw new IOException("Not enough blocks on line \"" + line + "\"");
		int[] res = new int[4];
		
		for(int i=0; i<4; i++) {
			String s = components[components.length-4+i];
			if("----".equals(s)) res[i] = -1;
			else res[i] = Integer.parseInt(s, 16);
		}
		
		int thisBitTime = bitTime;
		bitTime += 26;
		return new GroupEvent(thisBitTime, res, false);
	}
}
