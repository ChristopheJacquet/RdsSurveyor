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

import eu.jacquet80.rds.RDSSurveyor;
import eu.jacquet80.rds.log.RDSTime;


/**
 * Tries to implement javax.microedition.amms.control.tuner.RDSControl
 *
 */
public abstract class Station {
	protected int pi;
	protected Text ps = new Text(8);
	protected final Map<Integer, AFList> afs = new HashMap<Integer, AFList>();
	protected AFList currentAFList = null;
	protected int pty = 0;
	protected Text ptyn = new Text(8);
	private boolean tp, ta;
	protected String pinText = "";
	protected RDSTime timeOfLastPI = null;
	
	/**
	 * A station uses (World/European) RDS or American RBDS. We use the default
	 * specified in the preferences (true for RBDS, false for standard RDS),
	 * which defaults to false (=RDS).
	 * If a station broadcasts an ECC, then we can set the rbds flag 
	 * appropriately: true for the USA, Canada and Mexico, false for any other
	 * country. 
	 */
	protected boolean rbds = RDSSurveyor.preferences.getBoolean(RDSSurveyor.PREF_RBDS, false);

	
	protected void reset(int pi) {
		this.pi = pi;
	
		ps.reset();
		
		afs.clear();
		currentAFList = null;
	}
	
	
	protected void setPI(int pi) {
		this.pi = pi;
	}
	
	public void pingPI(RDSTime time) {
		this.timeOfLastPI = time;
	}

	/**
	 * @brief Converts an RDS channel code to a frequency.
	 * 
	 * @param channel The RDS channel (an integer in the 0–205 range, 205 being the filler code)
	 * @return The frequency in multiples of 100 kHz, or -1 for the filler code, or 0 if an invalid
	 * code was supplied
	 */
	public static int channelToFrequency(int channel) {
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
	
	protected static boolean isListLengthIndicator(int a) {
		return a >= 224 && a <= 249;
	}
	
	public synchronized String addAFPair(int a, int b) {
		if(isListLengthIndicator(a)) {
			if(b >= 0 && b <= 205) {
				currentAFList = afs.get(b);
				if(currentAFList == null) {
					currentAFList = new AFList(b);
					afs.put(b, currentAFList);
				}
			
				return "AF: #" + (a-224) + ", freq=" + frequencyToString(currentAFList.getTransmitterFrequency());
			} else return "No AF information";
		} else {
			if(a >= 0 && a <= 205 && b >= 0 && b <= 205) {
				String res = currentAFList == null ? null : currentAFList.addPair(a, b);
				if(res == null) {
					// this means that the method addPair has determined that
					// the new AF pair cannot belong to the existing list
					// So create a new list
					currentAFList = new AFList(-1);
					res = currentAFList.addPair(a, b);
				}
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
			res.append(l);
			if(i < afs.size()) res.append("\n");
		}

		return res.toString();
	}

	public synchronized String afsToHTML(String font) {
		StringBuffer res = new StringBuffer("<html><body style='font-family: \"").append(font).append("\";'");
		int i = 0;
		for(AFList l : afs.values()) {
			i++;
			if(l.getTransmitterFrequency() == 0) continue;
			res.append(l.toHTML());
			if(i < afs.size()) res.append("<br>\n");
		}
		res.append("</body></html>");

		return res.toString();
	}

	
	public Text getPS() {
		return ps;
	}
	
	public String getStationName() {
		return ps.getMostFrequentOrPartialText();
	}
	

	/**
	 * This method tries to reconstruct a message transmitted using (non-
	 * standard) "dynamic PS".
	 * 
	 * <p>I have observed two main ways of transmitting "dynamic PS":</p>
	 * <ul>
	 *   <li>transmit successively full words, for instance: "YOU ARE ",
	 *   "TUNED TO", "RADIO 99",</li>
	 *   <li>scroll a message one letter at a time, for instance: "YOU ARE ",
	 *   "OU ARE T", "U ARE TU", " ARE TUN", "ARE TUNE", "RE TUNED", 
	 *   "E TUNED ", " TUNED T", "TUNED TO", "UNED TO ", "NED TO R", etc.</li>
	 * </ul>
	 * 
	 * <p>
	 * This methods identifies the type of transmission used, and tries to 
	 * reconstruct the original message.
	 * </p>
	 * 
	 * <p>
	 * Note that the method should work even if the two types are mixed in a
	 * given transmission. Note also that the method will work correctly only
	 * if reception is good. If there are many missing blocks, it will not
	 * make sense of the message. This is not a limitation of the method
	 * itself, rather, it is caused by of the abusive use of PS to transmit
	 * complex text, what PS is not designed for. 
	 * </p>
	 *  
	 * @return the reconstructed message, limited to 80 characters in length
	 */
	public String getDynamicPSmessage() {
		List<String> msg = ps.getPastMessages(true);
		
		// trivial case when there is no message
		if(msg.size() == 0) return "";
		
		StringBuffer res = new StringBuffer();
		String prev = null;
		for(int i=msg.size()-1; i>=0 && res.length() < 80; i--) {
			String current = msg.get(i);
			boolean done = false;
			if(prev != null && prev.length() == 8 && current.length() == 8) {
				// if the 7 rightmost characters of the current PS correspond
				// to the 7 leftmost characters of the "previous" PS (going 
				// backward in time), then the PS is scrolling one character
				// at a time, so we just add *the* leftmost character at the
				// start
				if(prev.substring(0, 6).equals(current.substring(1, 7))) {
					res.insert(0, current.charAt(0));
					done = true;
				} else if(prev.substring(0, 5).equals(current.subSequence(2, 7))) {
					res.insert(0, current.substring(0, 2));
					done = true;
				}
			} 
			
			if(! done) {
				// otherwise, the PS is not scrolling, it's just displaying a
				// succession of 8-character words/sentences
				res.insert(0, current.trim() + " ");
			}
			prev = current;
		}
		return res.toString();
	}
	

	/**
	 * Returns the PI code of the station.
	 * 
	 * @return the PI code
	 */
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
		return (rbds ? RDS.rbdsPtyLabels : RDS.rdsPtyLabels) [pty];
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
	
	public void setPIN(int pin) {
		int day = (pin>>11) & 0x1F;
		int hour = (pin>>6) & 0x1F;
		int min = pin & 0x3F;
		pinText = String.format("D=%d, H=%02d:%02d", day, hour, min);
	}
	
	public String getPINText() {
		return pinText;
	}
	
	public String getCallsign() {
		if(rbds) {
			return RBDSCallsigns.callSign(this.pi);
		} else {
			return null;
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
			if(transmitterFrequency != 0) {
				if(method == 'B') {
					// if the two frequencies are transmitted
					// if a transmitter frequency has previously been provided
					// if none of them corresponds to the transmitter frequency
					// and if method B had been identified...
					// then the only possible explanation is that a new B-list has
					// begun. So we return null, so that the caller creates a new AFList
					return null;
				} // else
				method = 'A';
			}
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
	
	public String toHTML() {
		StringBuffer res = new StringBuffer("<b>AF list, method ").append(method).append(", size ").append(afs.size()).append(":</b> ");
		res.append(Station.frequencyToString(transmitterFrequency)).append(" → ");
		for(int af : afs) {
			res.append(Station.frequencyToString(af)).append("  ");
		}
		return res.toString();
	}
}