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


package eu.jacquet80.rds.oda;

public class RTPlus extends ODA {
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
	public void receiveGroup(int type, int version, int[] blocks, boolean[] blocksOk) {
		// Main RT+ group handling
		if(version == 0 && type != 3) {
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
					System.out.print(ctype[i] + "/" + classNames[ctype[i]] + "@" + start[i] + ":" + len[i]);
					if(text != null)
						System.out.print(" = \"" + text + "\"");
					System.out.print("    ");
				}
			}
		}
	}

}
