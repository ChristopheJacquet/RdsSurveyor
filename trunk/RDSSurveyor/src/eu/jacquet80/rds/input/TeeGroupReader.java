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
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import eu.jacquet80.rds.input.group.FrequencyChangeEvent;
import eu.jacquet80.rds.input.group.GroupEvent;
import eu.jacquet80.rds.input.group.GroupReaderEvent;
import eu.jacquet80.rds.input.group.GroupReaderEventVisitor;
import eu.jacquet80.rds.input.group.StationChangeEvent;

public class TeeGroupReader implements GroupReader {
	private final PrintWriter writer;
	private final GroupReader reader;
	
	public TeeGroupReader(GroupReader reader, File of) throws IOException {
		this.reader = reader;
		writer = new PrintWriter(of);
		writer.println("% RDS hexgroups");
	}
	
	@Override
	public GroupReaderEvent getGroup() throws IOException, EndOfStream {
		GroupReaderEvent event = reader.getGroup();
		if(event == null) return null;		// propagate null event
		
		event.accept(new GroupReaderEventVisitor() {
			@Override
			public void visit(FrequencyChangeEvent freqChangeEvent) {
				writer.print("% Freq " + freqChangeEvent.frequency + ", date=" + 
						freqChangeEvent.getTime().toLongString());
			}
			
			@Override
			public void visit(GroupEvent groupEvent) {
				for(int i=0; i<4; i++) {
					if(groupEvent.blocks[i]>=0) writer.printf("%04X ", groupEvent.blocks[i]);
					else writer.print("---- ");
				}
				writer.print("@" + groupEvent.getTime().toLongString());
			}

			@Override
			public void visit(StationChangeEvent stationChangeEvent) {
				// do nothing
			}
		});

		writer.println();
		writer.flush();
		
		return event;
	}

}
