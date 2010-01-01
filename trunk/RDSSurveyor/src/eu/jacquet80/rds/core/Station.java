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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;


/**
 * Tries to implement javax.microedition.amms.control.tuner.RDSControl
 *
 */
public abstract class Station {
	protected int pi;
	protected char[] ps = new char[8];
	protected final Map<Integer, AFList> afs = new HashMap<Integer, AFList>();
	protected AFList currentAFList = null;
	private HashMap<String, Integer>[] psSegments;
	private String[] psPage;
	private String dynamicPSmessage;
	protected int pty = 0;
	protected char[] ptyn = null;
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


	
	@SuppressWarnings("unchecked")
	protected void reset(int pi) {
		this.pi = pi;
		
		Arrays.fill(ps, '?');
		
		afs.clear();
		currentAFList = null;

		psSegments = new HashMap[4];
		for(int i=0; i<4; i++) psSegments[i] = new HashMap<String, Integer>();
		
		psPage = new String[4];
		dynamicPSmessage = "";
	}
	
	protected void setChars(char[] text, int position, char ... characters) {
		for(int i=0; i<characters.length; i++)
			text[position * characters.length + i] = characters[i];
	}
	
	public void setPSChars(int position, char ... characters) {
		setChars(ps, position, characters);
		
		// update segment count
		String s = new String(characters);
		Integer i = psSegments[position].get(s);
		if(i == null) psSegments[position].put(s, 1);
		else psSegments[position].put(s, i+1);
		
		// de-page dynamic PS
		if(psPage[position] == null) {
			psPage[position] = s;
		} else if(! s.equals(psPage[position])) {
			// new page detected
			String page = "";
			for(int j=0; j<4; j++) {
				page += (psPage[j] == null ? "??" : psPage[j]);
				psPage[j] = null;  // reset segment for new page
			}
			dynamicPSmessage += page.trim() + " ";
			psPage[position] = s;
		}
	}
	
	public void setPTYNChars(int position, char ... characters) {
		if(ptyn == null) ptyn = new char[8];
		
		setChars(ptyn, position, characters);
	}
	
	protected void setPI(int pi) {
		this.pi = pi;
	}
	
	public void pingPI(int time) {
		timeOfLastPI = time;
	}

	protected static int channelToFrequency(int channel) {
		if(channel >= 0 && channel <= 204) return 875 + channel;
		else return 0;
	}
	
	protected static String frequencyToString(int freq) {
		if(freq == 875) return "Illegal";
		return String.format("%d.%d", freq/10, freq%10);
	}
	
	public String addAFPair(int a, int b) {
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
	
	public String afsToString() {
		StringBuffer res = new StringBuffer();
		for(AFList l : afs.values()) {
			if(l.getTransmitterFrequency() == 0) continue;
			res.append("AF ").append(l).append("  ");
		}
		return res.toString();
	}
	
	public String getPS() {
		return new String(ps);
	}
	
	public String getStationName() {
		String res = "";
		
		for(int i=0; i<4; i++) {
			String seg = "";
			int max = 0;
			for(Entry<String, Integer> e : psSegments[i].entrySet()) {
				if(e.getValue() > max) {
					max = e.getValue();
					seg = e.getKey();
				}
			}
			if(max>0) res += seg; else res += "??";
		}
		
		return res;
	}
	
	public String getDynamicPSmessage() {
		return dynamicPSmessage;
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
	
	public String getPTYN() {
		return ptyn == null ? "" : new String(ptyn);
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
		if(fA == transmitterFrequency) {  // method B
			method = 'B';
			if(fB != 0) afs.add(fB);
			return "Method B: " + Station.frequencyToString(transmitterFrequency) + " -> " + Station.frequencyToString(fB) + " (" + typeIfB + ")";
		} else if(fB == transmitterFrequency) {  // method B
			method = 'B';
			if(fA != 0) afs.add(fA);
			return "Method B: " + Station.frequencyToString(transmitterFrequency) + " -> " + Station.frequencyToString(fA) + " (" + typeIfB + ")";
		} else {  // method A
			if(transmitterFrequency != 0) method = 'A';
			String res = (method == 'A' ? "Method A: " : "Unknown method: ");
			if(fA != 0) {
				afs.add(fA);
				res += Station.frequencyToString(fA) + "  ";
			}
			if(fB != 0) {
				afs.add(fB);
				res += Station.frequencyToString(fB);
			}
			return res;
		}
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