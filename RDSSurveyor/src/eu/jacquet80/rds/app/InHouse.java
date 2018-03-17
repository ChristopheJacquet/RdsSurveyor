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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import eu.jacquet80.rds.core.RDS;
import eu.jacquet80.rds.log.RDSTime;

public class InHouse extends Application {
	private final List<Message> messages = new ArrayList<Message>();
	private final Map<Message, MessageMetadata> metadata =
			new HashMap<Message, MessageMetadata>();
	
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
				if(metadata.containsKey(m)) {
					metadata.get(m).addOccurrence();
				} else {
					messages.add(m);
					metadata.put(m, new MessageMetadata());
				}
			}
			console.print(m.getDump());
		}
	}
	
	public synchronized int getMessageCount() {
		return messages.size();
	}
	
	public synchronized String getMessage(int index) {
		Message m = messages.get(index);
		return m.getHTMLDump() + " [" + metadata.get(m).getCount() + "]";
	}

	private static class Message {
		private final int w1, w2, w3;
		private final String contents;
		private final String contentsHTML;
		
		public Message(int w1, int w2, int w3) {
			this.w1 = w1;
			this.w2 = w2;
			this.w3 = w3;
			
			contents = 
				character((w2 >> 8) & 0xFF, false) + character(w2 & 0xFF, false) +
				character((w3 >> 8) & 0xFF, false) + character(w3 & 0xFF, false);
			contentsHTML = 
				character((w2 >> 8) & 0xFF, true) + character(w2 & 0xFF, true) +
				character((w3 >> 8) & 0xFF, true) + character(w3 & 0xFF, true);
		}
		
		private String character(int v, boolean html) {
			if(v >= 32 && v<=255) return Character.toString(RDS.toChar(v));
			else return html ? "<font color=#7777FF>.</font>" : ".";
		}
		
		public String getHTMLDump() {
			return "<html>" + 
					String.format("%02X/%04X-%04X", w1, w2, w3) +
					" (" + contentsHTML + ")</html>";
		}
		
		public String getDump() {
			return String.format("%02X/%04X-%04X", w1, w2, w3) + " (" + contents + ")";
		}		
		
		@Override
		public boolean equals(Object obj) {
			if(! (obj instanceof Message)) return false;
			Message m = (Message)obj;
			return this.w1 == m.w1 && this.w2 == m.w2 && this.w3 == m.w3;
		}
		
		@Override
		public int hashCode() {
			return this.w1 ^ ((this.w2<<16) | this.w3);
		}
	}
	
	private static class MessageMetadata {
		private int count = 1;
		
		public void addOccurrence() {
			count++;
		}
		
		public int getCount() {
			return count;
		}
	}
}