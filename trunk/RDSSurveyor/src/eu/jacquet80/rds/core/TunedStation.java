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

package eu.jacquet80.rds.core;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import eu.jacquet80.rds.app.Application;


public class TunedStation extends Station {
	// radiotext-related variables
	private char[] currentRT = new char[64];
	private int currentRTflags = 0;
	private List<String> rtMessages = new ArrayList<String>();
	private int latestRT = -1;
	
	private SortedMap<Integer, Station> otherNetworks;  // maps ON-PI -> OtherNetwork
	private int[][] groupStats = new int[17][2];
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
		
		// reset radiotext
		resetRT();
		
		synchronized(this) {
			otherNetworks = new TreeMap<Integer, Station>();
		}
		
		for(int i=0; i<16; i++)
			for(int j=0; j<2; j++)
				groupStats[i][j] = 0;
		
		date = null;
		
		for(int i=0; i<groupStats.length; i++)
			for(int j=0; j<2; j++)
				groupStats[i][j] = 0;
		totalBlocks = 0;
		totalBlocksOk = 0;
		
		applications = new Application[32];
		di = 0;
	}
	
	public String toString() {
		StringBuffer res = new StringBuffer();
		//System.out.println("pi=" + pi + ", ps=" + new String(ps) + ", time=" + timeOfLastPI);
		res.append(String.format("PI=%04X    Station name=\"%s\"    PS=\"%s\"    Time=%.3f", pi, getStationName(), new String(ps), (float)(timeOfLastPI / (1187.5f))));
		
		res.append(String.format("\nRT = \"%s\"", getRT()));

		synchronized(this) {
			for(Station on : otherNetworks.values()) res.append("\nON: ").append(on);
		}
		
		// AFs
		res.append("\n").append(afsToString());
		
		//res.append("\n\t\tquality = " + quality);
		
		res.append("\n" + groupStats());
		
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
	
	public String groupStats() {
		StringBuffer res = new StringBuffer();
		for(int i=0; i<16; i++)
			for(int j=0; j<2; j++)
				if(groupStats[i][j] > 0) res.append(String.format("%d%c: %d,   ", i, (char)('A' + j), groupStats[i][j]));
		res.append("U: " + groupStats[16][0]);
		return res.toString();
	}
	
	public int[][] numericGroupStats() {
		return groupStats;
	}

	public int getTimeOfLastPI() {
		return timeOfLastPI;
	}
	
	public void addGroupToStats(int type, int version, int nbOk) {
		groupStats[type][version]++;
		totalBlocks += 4;
		totalBlocksOk += nbOk;
	}
	
	public void addUnknownGroupToStats() {
		groupStats[16][0]++;
	}
	
	public void setRTChars(int ab, int position, char ... characters) {
		for(int i = 0; i < characters.length; i++) {
			if(currentRT[position * characters.length + i] != '\0' && characters[i] != currentRT[position * characters.length + i]) {
				// this is a new RT message: save the previous message...
				StringBuffer msg = new StringBuffer("[");
				for(int f=0; f<2; f++)
					if((currentRTflags & (1<<f)) != 0) msg.append((char)('A' + f));
				msg.append("] ").append(getRT());
				rtMessages.add(msg.toString());
				
				// ... and reset the message buffer
				resetRT();
				break;
			}
		}
		
		setChars(currentRT, position, characters);
		currentRTflags |= (1 << ab);   // set a bit corresponding to the current flag
		latestRT = ab;
		
		//System.out.println("\n*** RT=" + getRT() + ",   msgs=" + rtMessages + " ***");
	}
	
	private void resetRT() {
		Arrays.fill(currentRT, '\0');
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
	
	public Date getDate() {
		return date;
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
		if(latestRT < 0) return null;
		
		StringBuffer res = new StringBuffer();
		
		for(int j=0; j<64; j++) {
			if(currentRT[j] == 0x0D) break;
			if(currentRT[j] == 0) res.append(" ");
			else if(currentRT[j] >= 32) res.append(currentRT[j]);
			else res.append('<').append(Integer.toString(currentRT[j], 16)).append('>');
		}
		
		return res.toString().trim();
	}
	
	public int whichRT() {
		return latestRT;
	}
	
	public List<String> getRTMessages() {
		return rtMessages;
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
