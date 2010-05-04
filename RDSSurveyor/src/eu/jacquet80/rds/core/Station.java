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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * Tries to implement javax.microedition.amms.control.tuner.RDSControl
 *
 */
public abstract class Station {
	protected int pi;
	protected Text ps = new Text(8, false);
	protected final Map<Integer, AFList> afs = new HashMap<Integer, AFList>();
	protected AFList currentAFList = null;
	protected int pty = 0;
	protected Text ptyn = new Text(8, false);
	private boolean tp, ta;
	protected int timeOfLastPI = 0;
	
	protected static String[] ptyLabels = {
		"No programme type or undefined",
		"News",
		"Current Affairs",
		"Information",
		"Sport",
		"Education",
		"Drama",
		"Culture",
		"Science",
		"Varied",
		"Pop Music",
		"Rock Music",
		"Easy Listening Music",
		"Light classical",
		"Serious classical",
		"Other Music",
		"Weather",
		"Finance",
		"Children's programmes",
		"Social Affairs",
		"Religion",
		"Phone In",
		"Travel",
		"Leisure",
		"Jazz Music",
		"Country Music",
		"National Music",
		"Oldies Music",
		"Folk Music",
		"Documentary",
		"Alarm Test",
		"Alarm"
	};


	
	protected void reset(int pi) {
		this.pi = pi;
	
		ps.reset();
		
		afs.clear();
		currentAFList = null;
	}
	
	
	protected void setPI(int pi) {
		this.pi = pi;
	}
	
	public void pingPI(int time) {
		timeOfLastPI = time;
	}

	protected static int channelToFrequency(int channel) {
		if(channel >= 0 && channel <= 204) return 875 + channel;
		else if(channel == 205) return -1;		// -1 = filler code
		else return 0;
	}
	
	protected static String frequencyToString(int freq) {
		if(freq == 875) return "Illegal";
		else if(freq == 0) return "Unhandled";
		else if(freq == -1) return "None";
		return String.format("%d.%d", freq/10, freq%10);
	}
	
	public synchronized String addAFPair(int a, int b) {
		if(a >= 224 && a <= 249) {
			if(b >= 0 && b <= 205) {
				currentAFList = afs.get(b);
				if(currentAFList == null) {
					currentAFList = new AFList(b);
					afs.put(b, currentAFList);
				}
			
				return "AF: #" + (a-224) + ", freq=" + frequencyToString(currentAFList.getTransmitterFrequency());
			} else return "No AF information";
		} else {
			if(currentAFList == null) {
				currentAFList = new AFList(-1);
			}
			if(a >= 0 && a <= 205 && b >= 0 && b <= 205) {
				String res = currentAFList.addPair(a, b);
				return "AF: " + res;
			} else return "Unhandled AF pair: " + a + ", " + b;
		}
	}
	
	public synchronized String afsToString() {
		StringBuffer res = new StringBuffer();
		int i = 0;
		for(AFList l : afs.values()) {
			i++;
			if(l.getTransmitterFrequency() == 0) continue;
			res.append("AF ").append(l);
			if(i < afs.size()) res.append("\n");
		}

		return res.toString();
	}
	
	public Text getPS() {
		return ps;
	}
	
	public String getStationName() {
		return ps.getMostFrequentText();
	}
	

	public String getDynamicPSmessage() {
		List<String> msg = ps.getPastMessages();
		
		StringBuffer res = new StringBuffer();
		for(int i=9; i>=0; i--) {
			if(msg.size() > i) res.append(msg.get(msg.size() - 1 - i).trim()).append(' ');
		}
		return res.toString();
	}
	

	public int getPI() {
		return pi;
	}
	
	
	public void setPTY(int pty) {
		this.pty = pty;
	}
	
	public int getPTY() {
		return pty;
	}
	
	public Text getPTYN() {
		return ptyn;
	}
	
	public String getPTYlabel() {
		return ptyLabels[pty];
	}

	public String addMappedFreq(int channel, int mappedChannel) {
		// not implemented here, mapped frequencies only exist for other networks
		return null;
	}
	
	public void setTP(boolean tp) {
		this.tp = tp;
	}
	
	public void setTA(boolean ta) {
		this.ta = ta;
	}
	
	public boolean getTP() {
		return tp;
	}
	
	public boolean getTA() {
		return ta;
	}

	
	public String trafficInfoString() {
		if(tp) {
			return "TP" + (ta ? " + TA" : "");
		} else {
			if(ta) return "ON with TP";
			else return "";
		}
	}
}

class AFList {
	private final int transmitterFrequency;
	private final Set<Integer> afs = new HashSet<Integer>(24);
	private char method = '?';
	
	public AFList(int transmitterFrequency) {
		this.transmitterFrequency = Station.channelToFrequency(transmitterFrequency);
	}
	
	public int getTransmitterFrequency() {
		return transmitterFrequency;
	}
	
	public String addPair(int a, int b) {
		int fA = Station.channelToFrequency(a);
		int fB = Station.channelToFrequency(b);
		String typeIfB = fA < fB ? "same" : "variant"; 
		if(fA == transmitterFrequency && fA > 0) {  // method B
			method = 'B';
			if(fB > 0) afs.add(fB);
			return "Method B: " + Station.frequencyToString(transmitterFrequency) + " -> " + Station.frequencyToString(fB) + " (" + typeIfB + ")";
		} else if(fB == transmitterFrequency && fB > 0) {  // method B
			method = 'B';
			if(fA > 0) afs.add(fA);
			return "Method B: " + Station.frequencyToString(transmitterFrequency) + " -> " + Station.frequencyToString(fA) + " (" + typeIfB + ")";
		} else if(fA > 0 || fB > 0){  // method A
			if(transmitterFrequency != 0) method = 'A';
			String res = (method == 'A' ? "Method A: " : "Unknown method: ");
			if(fA > 0) {
				afs.add(fA);
				res += Station.frequencyToString(fA) + "  ";
			}
			if(fB > 0) {
				afs.add(fB);
				res += Station.frequencyToString(fB);
			}
			return res;
		} else return "No info";
	}
	
	public String toString() {
		StringBuffer res = new StringBuffer("List[").append(method).append(", sz=").append(afs.size()).append("]: ");
		res.append(Station.frequencyToString(transmitterFrequency)).append(" -> ");
		for(int af : afs) {
			res.append(Station.frequencyToString(af)).append("  ");
		}
		return res.toString();
	}
}