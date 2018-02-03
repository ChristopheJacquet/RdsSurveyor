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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * Represents an Other Network, as transmitted in Extended Other Network (EON) information or by
 * some ODA applications.
 */
public class OtherNetwork extends Station {
	private Map<Integer, Set<Integer>> mappedAFs = Collections.synchronizedMap(new HashMap<Integer, Set<Integer>>());
	private Set<Integer> pseudoMethodAAFs = Collections.synchronizedSet(new HashSet<Integer>());
	
	/**
	 * @brief Adds a mapped frequency.
	 * 
	 * @param channel The tuned (current) frequency, represented as a channel number
	 * @param mappedChannel The mapped (other) frequency, represented as a channel number
	 * 
	 * @return A textual representation of the mapping
	 */
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
	
	/**
	 * @brief Adds a pair of alternate frequencies.
	 * 
	 * If the first frequency is a list length indicator, it will be ignored.
	 * 
	 * @param a The first alternate frequency, represented as a channel number
	 * @param b The second alternate frequency, represented as a channel number
	 * 
	 * @return A textual representation of the two frequencies
	 */
	@Override
	public synchronized String addAFPair(int a, int b) {
		StringBuffer res = new StringBuffer("Pseudo-method A: ");
		
		// some stations use list length indicators, others don't
		if(! isListLengthIndicator(a)) {
			int fA = channelToFrequency(a);
			pseudoMethodAAFs.add(fA);
			res.append(frequencyToString(fA)).append(", ");
		}
		
		
		int fB = channelToFrequency(b);
		pseudoMethodAAFs.add(fB);
		res.append(frequencyToString(fB));
		
		return res.toString();
	}
	
	@Override
	public String afsToString() {
		if(mappedAFs.size() == 0) {
			StringBuffer res = new StringBuffer();
			List<Integer> afs = new ArrayList<Integer>(pseudoMethodAAFs);
			Collections.sort(afs);
			
			for(int i = 0; i<afs.size(); i++) {
				res.append(frequencyToString(afs.get(i)));
				if(i < afs.size()-1) {
					res.append(' ');
				}
			}
			return res.toString();
		}
		
		String res = "Mapped: ";
		synchronized(mappedAFs) {
			for(Map.Entry<Integer, Set<Integer>> e : mappedAFs.entrySet()) {
				res += "[" + frequencyToString(e.getKey()) + " → ";
				for(Iterator<Integer> it = e.getValue().iterator(); it.hasNext(); ) {
					res += frequencyToString(it.next());
					if(it.hasNext()) res += ", ";
				}
				res += "] ";
			}
		}
		return res;
	}
	
	public Map<Integer, Set<Integer>> getMappedAFs() {
		return mappedAFs;
	}
	
	public Set<Integer> getPseudoMethodAAFs() {
		return pseudoMethodAAFs;
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
