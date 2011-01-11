package eu.jacquet80.rds.app.oda;

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

	@Override
	public void receiveGroup(int type, int version, int[] blocks,
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
				int mode = (blocks[1] >> 2) & 3;
				
				switch(mode) {
				case 0: console.print("Unspecified mode, "); break;
				case 1: console.print("Mode I, "); break;
				case 2: console.print("Mode II or III, "); break;
				case 3: console.print("Mode IV, "); break;
				}
				
				// Frequency, coded as per DAB standard (EN 300401)
				int freq = blocks[2] | ((blocks[1] & 3)<<16);
				
				// Frequency in kHz is expressed in units of 16 kHz
				int freqKHz = 16 * freq;
				
				console.print(freqKHz + " kHz, ");
				
				console.printf("Ensemble ID=%04X", blocks[3]);
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
	}

	@Override
	public String getName() {
		return "Cross-reference to DAB";
	}

}
