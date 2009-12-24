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
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.SortedMap;
import java.util.TreeMap;

import eu.jacquet80.rds.app.Application;


public class TunedStation extends Station {
	private char[][] rt = new char[2][64];
	private char[] latestRT = null;
	private SortedMap<Integer, Station> otherNetworks;  // maps ON-PI -> OtherNetwork
	private int[][] blockCount = new int[17][2];
	private Date date = null;
	private Application[] applications = new Application[32];
	private int di = 0;
	private int totalBlocks, totalBlocksOk;
	private int ecc, language;
	private int dateBitTime = -1;
	
	
	public TunedStation(int pi, int time) {
		reset(pi);
		pingPI(time);
	}
	
	public TunedStation(int time) {
		this(0, time);
	}

	
	protected void reset(int pi) {
		super.reset(pi);
		
		for(int i=0; i<2; i++) {
			Arrays.fill(rt[i], '?');
		}
		
		synchronized(this) {
			otherNetworks = new TreeMap<Integer, Station>();
		}
		
		for(int i=0; i<16; i++)
			for(int j=0; j<2; j++)
				blockCount[i][j] = 0;
		
		date = null;
		
		for(int i=0; i<blockCount.length; i++)
			for(int j=0; j<2; j++)
				blockCount[i][j] = 0;
		totalBlocks = 0;
		totalBlocksOk = 0;
		
		applications = new Application[32];
		di = 0;
	}
	
	public String toString() {
		StringBuffer res = new StringBuffer();
		//System.out.println("pi=" + pi + ", ps=" + new String(ps) + ", time=" + timeOfLastPI);
		res.append(String.format("PI=%04X    Station name=\"%s\"    PS=\"%s\"    Time=%.3f", pi, getStationName(), new String(ps), (float)(timeOfLastPI / (1187.5f))));
		
		for(int ab=0; ab<2; ab++) {
			for(int i=0; i<64; i++) {
				if(rt[ab][i] != '?') {
					res.append(String.format("\nRT %c = \"", (char)('a'+ab)));
					for(int j=0; j<64; j++) {
						if(rt[ab][j] == 0x0D) break;
						res.append(rt[ab][j] >= 32 ? rt[ab][j] : "\\x" + ((int)rt[ab][j]));
					}
					res.append('\"');
					break;
				}
			}
		}
		
		synchronized(this) {
			for(Station on : otherNetworks.values()) res.append("\nON: ").append(on);
		}
		
		// AFs
		res.append("\n").append(afsToString());
		
		//res.append("\n\t\tquality = " + quality);
		
		res.append("\n" + stats());
		
		if(date != null) res.append("\nLatest CT: " + date);
		
		res.append("\nPTY: " + pty + " -> " + ptyLabels[pty]);
		if(ptyn != null) res.append(", PTYN=" + new String(ptyn));
		
		res.append("\nDI: ")
				.append((di & 1) == 0 ? "Mono" : "Stereo").append(", ")
				.append((di & 2) == 0 ? "Not artificial head" : "Artificial head").append(", ")
				.append((di & 4) == 0 ? "Not compressed" : "Compressed").append(", ")
				.append((di & 8) == 0 ? "Static PTY" : "Dynamic PTY")
				.append("\n");
		
		if(ecc != 0) {
			res.append("Country: " + RDS.getISOCountryCode((pi>>12)&0xF, ecc)).append("\n");
		}
		
		if(language < RDS.languages.length) res.append("Language: ").append(RDS.languages[language][0]).append("\n");
				
		
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
	
	public void addGroupToStats(int type, int version, int nbOk) {
		blockCount[type][version]++;
		totalBlocks += 4;
		totalBlocksOk += nbOk;
	}
	
	public void addUnknownGroupToStats() {
		blockCount[16][0]++;
	}
	
	public void setRTChars(int ab, int position, char ... characters) {
		setChars(rt[ab], position, characters);
		latestRT = rt[ab];
	}
	
	public void setApplicationForGroup(int type, int version, Application app) {
		applications[(type<<1) | version] = app;
	}
	
	public Application getApplicationForGroup(int type, int version) {
		 return applications[(type<<1) | version];
	}
	
	public void setDate(Date date, int bitTime) {
		this.date = date;
		this.dateBitTime = bitTime;
	}
	
	public synchronized void addON(Station on) {
		otherNetworks.put(on.getPI(), on);
	}
	
	public synchronized Station getON(int onpi) {
		return otherNetworks.get(onpi);
	}
	
	public synchronized int getONcount() {
		return otherNetworks.size();
	}
	
	public synchronized Station getONbyIndex(int idx) {
		int i = 0;
		for(Station s : otherNetworks.values()) {
			if(i == idx) return s;
			i++;
		}
		return null;
	}
	
	public void setDIbit(int pos, int val) {
		di &= 0xF ^ (1<<(3-pos));		// clear bit
		di |= val<<(3-pos);				// set it if needed
	}
	
	public String getRT() {
		if(latestRT == null) return null;
		else return new String(latestRT).split("\n")[0];
	}

	public int getTotalBlocks() {
		return totalBlocks;
	}
	
	public int getTotalBlocksOk() {
		return totalBlocksOk;
	}
	
	public void setECC(int ecc) {
		this.ecc = ecc;
	}
	
	public void setLanguage(int lang) {
		this.language = lang;
	}
	
	public Date getDateForBitTime(int bitTime) {
		if(date == null) return null;
		Calendar c = new GregorianCalendar();
		c.setTime(date);
		c.add(Calendar.SECOND, (int)((bitTime - dateBitTime) / 1187.5f));
		return c.getTime();
	}
}
