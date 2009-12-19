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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;


public class OtherNetwork extends Station {
	private Map<Integer, Set<Integer>> mappedAFs = new HashMap<Integer, Set<Integer>>();
	
	@Override
	public String addMappedFreq(int channel, int mappedChannel) {
		int freq = channelToFrequency(channel);
		int mappedFreq = channelToFrequency(mappedChannel);
		
		// get the list of AFs mapped to the frequency "freq"
		Set<Integer> listOfMappedFreqs = mappedAFs.get(freq);

		// if no such list, create it
		if(listOfMappedFreqs == null) {
			listOfMappedFreqs = new HashSet<Integer>();
			mappedAFs.put(freq, listOfMappedFreqs);
		}
		
		// add the new mapped frequency
		listOfMappedFreqs.add(mappedFreq);
		
		return frequencyToString(freq) + " -> " + frequencyToString(mappedFreq);
	}
	
	@Override
	public String afsToString() {
		if(mappedAFs.size() == 0)
			return super.afsToString();
		
		String res = "Mapped AFs: ";
		for(Map.Entry<Integer, Set<Integer>> e : mappedAFs.entrySet()) {
			res += "[" + frequencyToString(e.getKey()) + " -> ";
			for(Iterator<Integer> it = e.getValue().iterator(); it.hasNext(); ) {
				res += frequencyToString(it.next());
				if(it.hasNext()) res += ", ";
			}
			res += "] ";
		}
		return res;
	}
	
	public String toString() {
		StringBuffer res = new StringBuffer();
		res.append(String.format("\tPI=%04X PS*=\"%s\" ", pi, getStationName())).append(afsToString());
		return res.toString();
	}
	
	public OtherNetwork(int pi) {
		reset(pi);
	}
}
