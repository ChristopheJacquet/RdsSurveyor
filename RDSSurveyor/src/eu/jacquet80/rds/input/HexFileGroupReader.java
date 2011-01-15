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
import java.io.InputStreamReader;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import eu.jacquet80.rds.input.group.FrequencyChangeEvent;
import eu.jacquet80.rds.input.group.GroupEvent;
import eu.jacquet80.rds.input.group.GroupReaderEvent;

public class HexFileGroupReader implements GroupReader {
	private final static Pattern FIRST_NUMBER = Pattern.compile(".*\\D(\\d+)");
	private final BufferedReader br;
	private int bitTime = 0;
	
	public HexFileGroupReader(BufferedReader br) {
		this.br = br;
	}
	
	public HexFileGroupReader(URL url) throws IOException {
		this(new BufferedReader(new InputStreamReader(url.openStream())));
	}
	
	public HexFileGroupReader(File file) throws FileNotFoundException {
		this(new BufferedReader(new FileReader(file)));
	}
	
	public GroupReaderEvent getGroup() throws IOException, EndOfStream {
		GroupReaderEvent event;
		
		do {
			String line = br.readLine();
			if(line == null) throw new EndOfStream();

			int thisBitTime = bitTime;
			bitTime += 26;

			event = parseHexLine(line, thisBitTime);
		} while(event == null);
		
		return event;
	}
	
	static GroupReaderEvent parseHexLine(String line, int bitTime) throws IOException {
		if(line.startsWith("%")) {
			// Lines beginning with % are to be ignored, but may contain metadata
			if(line.startsWith("% Freq")) {
				// Frequency indicator metadata
				Matcher m = FIRST_NUMBER.matcher(line);
				int f = 0;
				if(m.matches()) f = Integer.parseInt(m.group(1));
				return new FrequencyChangeEvent(f);
			}
			
		    // ignore other lines beginning with '%'
			return null;
		}
		
		// lines beginning with < are specific to RDS Spy. Ignore them altogether
		if(line.startsWith("<")) return null;
		
		String[] components = line.trim().split("\\s+");
		if(components.length < 4) throw new IOException("Not enough blocks on line \"" + line + "\"");
		int[] res = new int[4];
		
		for(int i=0; i<4; i++) {
			String s = components[i];
			if("----".equals(s)) res[i] = -1;
			else res[i] = Integer.parseInt(s, 16);
		}
		

		return new GroupEvent(bitTime, res, false);
	}
}
