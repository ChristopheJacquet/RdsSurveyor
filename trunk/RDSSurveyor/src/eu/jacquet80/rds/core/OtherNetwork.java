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
