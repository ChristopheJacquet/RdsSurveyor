/*
 RDS Surveyor -- RDS decoder, analyzer and monitor tool and library.
 For more information see
   http://www.jacquet80.eu/
   http://rds-surveyor.sourceforge.net/
 
 Copyright (c) 2009, 2011 Christophe Jacquet

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


package eu.jacquet80.rds.app.oda;

import java.io.PrintWriter;
import java.util.Date;
import java.util.Vector;

import eu.jacquet80.rds.log.RDSTime;

/**
 * iTunes Tagging ODA
 * 
 * @author Christophe Jacquet
 * @date 2013-10-11
 *
 */
public class ITunesTagging extends ODA {
	public static final int AID = 0xC3B0;
	
	private static class Tag {
		private final Date datetime;
		private final long id;
		
		private Tag(Date datetime, long id) {
			this.datetime = datetime;
			this.id = id;
		}
		
		@Override
		public String toString() {
			String d = datetime == null ? "?" : datetime.toString();
			return d + ", id=0x" + Long.toHexString(id); 
		}
	}
	
	private long currentId = -1;
	
	private final Vector<Tag> tags = new Vector<Tag>();
	
	public Vector<Tag> getTagList() {
		return tags;
	}
	
	@Override
	public int getAID() {
		return AID;
	}

	@Override
	public void receiveGroup(PrintWriter console, int type, int version,
			int[] blocks, boolean[] blocksOk, RDSTime time) {
		
		if(type != 3 && blocksOk[1] && blocksOk[2] && blocksOk[3]) {
			long id = ((long)(blocks[1] & 0x1F))<<32 | ((long)blocks[2])<<16 | blocks[3];
			console.printf("ID %010X", id);
			
			if(id != currentId) {
				Date datetime = station.getRealTimeForStreamTime(time);
				
				tags.add(new Tag(datetime, id));
				fireChangeListeners();
				
				currentId = id;
			}
			
			
		}
	}
	
	@Override
	public String getName() {
		return "iTunes tagging";
	}
}