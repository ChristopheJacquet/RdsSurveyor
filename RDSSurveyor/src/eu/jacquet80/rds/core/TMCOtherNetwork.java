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

import eu.jacquet80.rds.app.oda.AlertC;

/**
 * Encapsulates an Other Network (ON) obtained from TMC/AlertC Tuning Information.
 * 
 * TMC Tuning Information is equivalent to RDS EON in many ways, with a few additions. Four
 * different variants of transmitting Other Network information are available:
 * <ul>
 * <li>Variant 6: Program Identification (PI) code and two Alternate Frequencies (AFs)</li>
 * <li>Variant 7: Mapping of a Tuned Network (TN) frequency to a PI and AF</li>
 * <li>Variant 8: Two PI codes, which carry the same TMC service on all frequencies (AFI bit set)</li>
 * <li>Variant 9: PI code and Location Table Number (LTN), Message Geographical Scope (MGS) and
 * Service Identifier (SID) for the TMC service carried on all frequencies of that station (AFI
 * bit set)</li>
 * </ul>
 * 
 * With the exception of Variant 9, the Other Network carries the same TMC service as the current
 * station. Variant 9 implies that messages from the TMC service indicated may update messages
 * received from the current service, and vice versa.
 */
public class TMCOtherNetwork extends OtherNetwork {
	private int ltn = -1;
	private int mgs = 0;
	private int sid = -1;

	public TMCOtherNetwork(int pi) {
		super(pi);
	}
	
	/**
	 * @brief Returns the Location Table Number for this station.
	 * 
	 * @return The LTN, or -1 if it is the same as for the current station
	 */
	public int getLtn() {
		return ltn;
	}
	
	/**
	 * @brief Returns the Message Geographical Scope for this station.
	 * 
	 * @return The MGS, or 0 if it is the same as for the current station
	 */
	public int getMgs() {
		return mgs;
	}
	
	/**
	 * @brief Returns the Service Identifier for this station.
	 * 
	 * @return The SID, or -1 if it is the same as for the current station
	 */
	public int getSid() {
		return sid;
	}
	
	/**
	 * @brief Sets TMC service information for this station.
	 * 
	 * @param ltn Location Table Number
	 * @param mgs Message Geographical Scope
	 * @param sid Service Identifier
	 * 
	 * @return A textual representation of the service information
	 */
	public String setService(int ltn, int mgs, int sid) {
		this.ltn = ltn;
		this.mgs = mgs;
		this.sid = sid;
		return "LTN=" + ltn + ", MGS=" + AlertC.decodeMGS(mgs) + ", SID=" + sid;
	}

}
