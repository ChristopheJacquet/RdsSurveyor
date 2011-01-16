/*
 RDS Surveyor -- RDS decoder, analyzer and monitor tool and library.
 For more information see
   http://www.jacquet80.eu/
   http://rds-surveyor.sourceforge.net/
 
 Copyright (c) 2009, 2011 Christophe Jacquet

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


package eu.jacquet80.rds.app.oda;

import java.io.PrintWriter;

/**
 * ETSI standard EN 301700 describes a way to cross-reference DAB (aka Eureka
 * 147) services from their FM/RDS counterparts. This is achieved through the
 * use of an ODA, of AID 0x0093 (dec 147).
 * 
 * @author Christophe Jacquet
 * @date 2011-01-11
 *
 */
public class EN301700 extends ODA {
	public static final int AID = 0x0093;
	
	@Override
	public int getAID() {
		return AID;
	}

	/** Frequency of DAB ensemble, in kHz (if not known, -1) */
	private int freqKHz = -1;
	
	/** DAB Ensemble ID (if not known, -1) */
	private int eid = -1;
	
	/** DAB mode (if not known, null) */
	private String mode = null;
	
	@Override
	public void receiveGroup(PrintWriter console, int type, int version, int[] blocks,
			boolean[] blocksOk, int bitTime) {
		// need to have the three blocks, otherwise abort here
		if(!(blocksOk[1] && blocksOk[2] && blocksOk[3])) return;
		
		if(type == 3) {
			console.print("No data in group 3A");
		} else {
			// E/S flag to differentiate between the Ensemble table
			// and the Service table
			int es = (blocks[1] >> 4) & 1;
			
			if(es == 0) {
				// Ensemble table
				
				// Note: decoding of the ensemble table has been checked
				// against BBC data (EId=E1.CE15, freq=225648 kHz)
				// See here: http://code.google.com/p/dab-epg/wiki/SimpleServiceInformation
				// And compare with RDS recordings made in 2010-2011 here:
				// http://www.band2dx.info/
				
				// Mode
				int numMode = (blocks[1] >> 2) & 3;
				
				switch(numMode) {
				case 0: mode = "Unspecified mode"; break;
				case 1: mode = "Mode I"; break;
				case 2: mode = "Mode II or III"; break;
				case 3: mode = "Mode IV"; break;
				}
				
				console.print(mode + ", ");
				
				// Frequency, coded as per DAB standard (EN 300401)
				int freq = blocks[2] | ((blocks[1] & 3)<<16);
				
				// Frequency in kHz is expressed in units of 16 kHz
				freqKHz = 16 * freq;
				
				// Ensemble ID
				eid = blocks[3];
				
				console.print(freqKHz + " kHz, ");
				
				console.printf("Ensemble ID=%04X", eid);
			} else {
				// Service table
				int variant = blocks[1] & 0xF;    // variant code
				
				console.print("v=" + variant + ", ");
				
				switch(variant) {
				case 0:
					console.printf("Ensemble ID=%04X, ", blocks[2]);
					break;
				default:
					console.print("Unhandled, ");
				}
				
				console.printf("Service ID=%04X", blocks[3]);
			}
		}
		
		fireChangeListeners();
	}

	@Override
	public String getName() {
		return "DAB X-Ref";
	}

	/**
	 * Returns the frequency of the DAB ensemble, in kHz, or -1 if the
	 * frequency is not known.
	 * 
	 * @return the frequency
	 */
	public int getFrequencyInKHz() {
		return freqKHz;
	}
	
	/**
	 * Returns the DAB mode, or null if it is not known.
	 * 
	 * @return a string describing the mode
	 */
	public String getMode() {
		return mode;
	}
	
	/**
	 * Returns the DAB ensemble Id, or -1 if it is not known.
	 * 
	 * @return the Ensemble Id (EId)
	 */
	public int getEnsembleId() {
		return eid;
	}
}
