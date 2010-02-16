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


package eu.jacquet80.rds.app.oda;

public class RTPlus extends ODA {
	public static int AID = 0x4BD7;
	
	private static String[] classNames = {
		"DUMMY_CLASS",
		"ITEM.TITLE",
		"ITEM.ALBUM",
		"ITEM.TRACKNUMBER",
		"ITEM.ARTIST",
		"ITEM.COMPOSITION",
		"ITEM.MOVEMENT",
		"ITEM.CONDUCTOR",
		"ITEM.COMPOSER",
		"ITEM.BAND",
		"ITEM.COMMENT",
		"ITEM.GENRE",
		"INFO.NEWS",
		"INFO.NEWS.LOCAL",
		"INFO.STOCKMARKET",
		"INFO.SPORT",
		"INFO.LOTTERY",
		"INFO.HOROSCOPE",
		"INFO.DAILY_DIVERSION",
		"INFO.HEALTH",
		"INFO.EVENT",
		"INFO.SCENE",
		"INFO.CINEMA",
		"INFO.TV",
		"INFO.DATE_TIME",
		"INFO.WEATHER",
		"INFO.TRAFFIC",
		"INFO.ALARM",
		"INFO.ADVERTISEMENT",
		"INFO.URL",
		"INFO.OTHER",
		"STATIONNAME.SHORT",
		"STATIONNAME.LONG",
		"PROGRAMME.NOW",
		"PROGRAMME.NEXT",
		"PROGRAMME.PART",
		"PROGRAMME.HOST",
		"PROGRAMME.EDITORIAL_STAFF",
		"PROGRAMME.FREQUENCY",
		"PROGRAMME.HOMEPAGE",
		"PROGRAMME.SUBCHANNEL",
		"PHONE.HOTLINE",
		"PHONE.STUDIO",
		"PHONE.OTHER",
		"SMS.STUDIO",
		"SMS.OTHER",
		"EMAIL.HOTLINE",
		"EMAIL.STUDIO",
		"EMAIL.OTHER",
		"MMS.OTHER",
		"CHAT",
		"CHAT.CENTRE",
		"VOTE.QUESTION",
		"VOTE.CENTRE",
		"RFU-54",
		"RFU-55",
		"PRIVATE-56",
		"PRIVATE-57",
		"PRIVATE-58",
		"PLACE",
		"APPOINTMENT",
		"IDENTIFIER",
		"PURCHASE",
		"GET_DATA"
	};

	
	@Override
	public String getName() {
		return "RT+";
	}

	@Override
	public void receiveGroup(int type, int version, int[] blocks, boolean[] blocksOk, int bitTime) {
		// Main RT+ group handling
		if(type == 3 && version == 0 && blocksOk[2]) {
			int ert = (blocks[2]>>13) & 1;
			console.print("Applies to " + (ert == 1 ? "eRT" : "RT") + ", ");
			
			int cb = (blocks[2]>>12) & 1;
			console.print((cb == 0 ? "NO " : "") + "template, ");
			
			int scb = (blocks[2]>>8) & 0xF;
			console.printf("SCB=%01X, ", scb);
			
			int tn = blocks[2] & 0xFF;
			console.printf("template #%02X", tn);
		}
		
		if(version == 0 && type != 3) {
			int running = (blocks[1]>>4) & 1;
			int toggle = (blocks[1]>>3) & 1;
			
			console.printf("Running=%d, Toggle=%d, ", running, toggle);
			
			int[] ctype = new int[] {0, 0}, start = new int[2], len = new int[2];
			
			if(blocksOk[2]) {
				ctype[0] = ((blocks[1] & 7)<<3) | ((blocks[2] >> 13) & 7);
				start[0] = (blocks[2] >> 7) & 0x3F;
				len[0] = (blocks[2] >> 1) & 0x3F;
			}
			
			if(blocksOk[2] && blocksOk[3]) {
				ctype[1] = ((blocks[2] & 1) << 5) | ((blocks[3]>>11) & 0x1F);
				start[1] = (blocks[3] >> 5) & 0x3F;
				len[1] = blocks[3] & 0x1F;
			}
			
			for(int i=0; i<2; i++) {
				if(ctype[i] != 0) {
					String rt = station == null ? null : station.getRT();
					String text = null;
					if(rt != null) {
						if(start[i] + len[i] <= rt.length()) {
							// beware, len is the _additional_ length!
							text = rt.substring(start[i], start[i] + len[i] + 1);
						}
					}
					console.print(ctype[i] + "/" + classNames[ctype[i]] + "@" + start[i] + ":" + len[i]);
					if(text != null)
						console.print(" = \"" + text + "\"");
					console.print("    ");
				}
			}
		}
	}

	@Override
	public int getAID() {
		return AID;
	}

}
