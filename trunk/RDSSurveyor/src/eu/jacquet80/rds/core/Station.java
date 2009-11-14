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
import java.util.HashSet;
import java.util.Set;


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
	
	protected void reset(int pi) {
		this.pi = pi;
		
		Arrays.fill(ps, '?');
		
		frequency = 0;
		afCount = 0;
		afs.clear();

	}
	
	protected void setChars(char[] text, int position, char ... characters) {
		for(int i=0; i<characters.length; i++)
			text[position * characters.length + i] = characters[i];
	}
	
	public void setPSChars(int position, char ... characters) {
		setChars(ps, position, characters);
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
	

	public int getPI() {
		return pi;
	}
}
