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

package eu.jacquet80.rds.app;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import eu.jacquet80.rds.core.RDS;
import eu.jacquet80.rds.log.RDSTime;

public class InHouse extends Application {
	private final List<Message> messages = new ArrayList<Message>();
	
	public InHouse() {
	}
	
	@Override
	public String getName() {
		return "IH";
	}

	@Override
	public void receiveGroup(PrintWriter console, int type, int version, int[] blocks,
			boolean[] blocksOk, RDSTime time) {
		if(blocksOk[1] && blocksOk[2] && blocksOk[3]) {
			Message m = new Message(blocks[1] & 0x1F, blocks[2], blocks[3]);
			synchronized(this) {
				messages.add(m);
			}
			console.print(m.getDump());
		}
	}
	
	public synchronized int getMessageCount() {
		return messages.size();
	}
	
	public synchronized String getMessage(int index) {
		return messages.get(index).getDump();
	}

	public static class Message {
		private final int w1, w2, w3;
		private final String contents;
		
		public Message(int w1, int w2, int w3) {
			this.w1 = w1;
			this.w2 = w2;
			this.w3 = w3;
			
			contents = 
				Character.toString(character((w2 >> 8) & 0xFF)) +
				Character.toString(character(w2 & 0xFF)) +
				Character.toString(character((w3 >> 8) & 0xFF)) +
				Character.toString(character(w3 & 0xFF));
		}
		
		private char character(int v) {
			if(v >= 32 && v<=255) return RDS.toChar(v); else return '.';
		}
		
		public String getDump() {
			return String.format("%02X/%04X-%04X", w1, w2, w3) + " (" + contents + ")";
		}
	}
}