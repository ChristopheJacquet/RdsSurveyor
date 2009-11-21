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

package eu.jacquet80.rds.core;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import eu.jacquet80.rds.oda.ODA;


public class TunedStation extends Station {
	private char[][] rt = new char[2][64];
	private Map<Integer, OtherNetwork> otherNetworks;  // maps ON-PI -> OtherNetwork
	private int[][] blockCount = new int[17][2];
	private Date date = null;
	private int timeOfLastPI = 0;
	private ODA[] odas = new ODA[32];
	private boolean usesRP = false;
	private int di = 0;

	
	public TunedStation(int pi) {
		reset(pi);
	}
	
	public TunedStation() {
		this(0);
	}

	
	protected void reset(int pi) {
		super.reset(pi);
		
		for(int i=0; i<2; i++) {
			Arrays.fill(rt[i], '?');
		}
		
		otherNetworks = new HashMap<Integer, OtherNetwork>();
		
		for(int i=0; i<16; i++)
			for(int j=0; j<2; j++)
				blockCount[i][j] = 0;
		
		date = null;
		
		for(int i=0; i<blockCount.length; i++)
			for(int j=0; j<2; j++)
				blockCount[i][j] = 0;
		
		odas = new ODA[32];
		usesRP = false;
		di = 0;
	}
	
	public String toString() {
		StringBuffer res = new StringBuffer();
		//System.out.println("pi=" + pi + ", ps=" + new String(ps) + ", time=" + timeOfLastPI);
		res.append(String.format("\tPI=%04X PS=\"%s\" Time=%.3f", pi, new String(ps), (float)(timeOfLastPI / (1187.5f))));
		
		for(int ab=0; ab<2; ab++) {
			for(int i=0; i<64; i++) {
				if(rt[ab][i] != '?') {
					res.append(String.format("\n\t\tRT %c = \"", (char)('a'+ab)));
					for(int j=0; j<64; j++) {
						if(rt[ab][j] == 0x0D) break;
						res.append(rt[ab][j]);
					}
					res.append('\"');
					break;
				}
			}
		}
		
		for(OtherNetwork on : otherNetworks.values()) res.append("\n\t\tON: ").append(on);
		
		// AFs
		res.append("\n\t\t").append(afsToString());
		
		//res.append("\n\t\tquality = " + quality);
		
		res.append("\n\t\t" + stats());
		
		if(date != null) res.append("\n\t\tLatest CT: " + date);
		
		res.append("\n\t\tDI: ")
				.append((di & 1) == 0 ? "Mono" : "Stereo").append(", ")
				.append((di & 2) == 0 ? "Not artificial head" : "Artificial head").append(", ")
				.append((di & 4) == 0 ? "Not compressed" : "Compressed").append(", ")
				.append((di & 8) == 0 ? "Static PTY" : "Dynamic PTY");
				
		
		return res.toString();
	}
	
	private String stats() {
		StringBuffer res = new StringBuffer();
		for(int i=0; i<16; i++)
			for(int j=0; j<2; j++)
				if(blockCount[i][j] > 0) res.append(String.format("%d%c: %d,   ", i, (char)('A' + j), blockCount[i][j]));
		res.append("U: " + blockCount[16][0]);
		return res.toString();
	}

	public int getTimeOfLastPI() {
		return timeOfLastPI;
	}
	
	public void pingPI(int time) {
		timeOfLastPI = time;
	}
	
	public void addGroupToStats(int type, int version) {
		blockCount[type][version]++;
	}
	
	public void addUnknownGroupToStats() {
		blockCount[16][0]++;
	}
	
	public void setUsesRP(boolean usesRP) {
		this.usesRP = usesRP;
	}
	
	public boolean isUsingRP() {
		return usesRP;
	}
	
	public void setRTChars(int ab, int position, char ... characters) {
		setChars(rt[ab], position, characters);
	}
	
	public void setODAforGroup(int type, int version, ODA oda) {
		odas[type<<1 | version] = oda;
	}
	
	public ODA getODAforGroup(int type, int version) {
		 return odas[type<<1 | version];
	}
	
	public void setDate(Date date) {
		this.date = date;
	}
	
	public void addON(OtherNetwork on) {
		otherNetworks.put(on.getPI(), on);
	}
	
	public OtherNetwork getON(int onpi) {
		return otherNetworks.get(onpi);
	}
	
	public void setDIbit(int pos, int val) {
		di &= 0xF ^ (1<<(3-pos));		// clear bit
		di |= val<<(3-pos);				// set it if needed
	}

}
