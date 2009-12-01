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
import java.util.Set;
import java.util.Map.Entry;


/**
 * Tries to implement javax.microedition.amms.control.tuner.RDSControl
 *
 */
public abstract class Station {
	protected int pi;
	protected char[] ps = new char[8];
	protected Set<Integer> afs = new HashSet<Integer>();
	protected int frequency;
	protected int afCount;
	private int badPI = 0;
	private HashMap<String, Integer>[] psSegments;
	private String[] psPage;
	private String dynamicPSmessage;
	protected int pty = 0;
	
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
		
		frequency = 0;
		afCount = 0;
		afs.clear();

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
	
	protected boolean setPI(int pi) {
		// initially, we try to acquire PI
		if(this.pi == 0) {
			this.pi = pi;
			return true;
		}
		
		// after that, react to PI changes
		if(this.pi != pi) {
			badPI++;
			if(badPI > 3) {
				///reset(pi);
				return false;
			}
		} else badPI = 0;
		return true;
	}

	protected int channelToFrequency(int channel) {
		return 875 + channel;
	}
	
	protected String frequencyToString(int freq) {
		return String.format("%d.%d", freq/10, freq%10);
	}
	
	protected String addAFPair(int a, int b) {
		if(a >= 224 && a <= 249) {
			afCount = a - 224;
			frequency = channelToFrequency(b);
			return "AF: #" + afCount + ", freq=" + frequencyToString(frequency);
		} else {
			int freqA = 0, freqB = 0;
			if(a >= 0 && a <= 204) {
				freqA = channelToFrequency(a);
				afs.add(freqA);
			}
			if(b >= 0 && b <= 204) {
				freqB = channelToFrequency(b);
				afs.add(freqB);
			}
			return "AF: " + frequencyToString(freqA) + ", " + frequencyToString(freqB);
		}
	}
	
	protected String afsToString() {
		StringBuffer res = new StringBuffer();
		res.append("freq=").append(frequencyToString(frequency)).append(" AF# ").append(afCount).append(' ');
		for(int af : afs) res.append(frequencyToString(af)).append(' ');
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

	public String addMappedFreq(int channel, int mappedChannel) {
		// not implemented here, mapped frequencies only exist for other networks
		return null;
	}

}
