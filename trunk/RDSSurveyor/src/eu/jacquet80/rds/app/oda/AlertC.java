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

package eu.jacquet80.rds.app.oda;

import java.util.HashMap;
import java.util.Map;

import eu.jacquet80.rds.core.OtherNetwork;
import eu.jacquet80.rds.core.RDS;
import eu.jacquet80.rds.core.Station;

public class AlertC extends ODA {
	public static final int AID = 0xCD46;
	
	private String[] providerName = {"????", "????"};
	private Map<Integer, OtherNetwork> otherNetworks = new HashMap<Integer, OtherNetwork>();
	
	public AlertC() {
	}

	@Override
	public void receiveGroup(int type, int version, int[] blocks, boolean[] blocksOk, int bitTime) {
		// in all cases, we need all blocks to proceed
		if(!blocksOk[2] || !blocksOk[3]) return;
		
		if(type == 3 && version == 0) {
			int var = (blocks[2]>>14) & 0x3;
			console.print("Sys.Info v=" + var+ ", ");
			
			if(var == 0) {
				int ltn = (blocks[2]>>6) & 0x3F;
				int afi = (blocks[2]>>5) & 1;
				int mode = (blocks[2]>>4) & 1;
				int scopeI = (blocks[2]>>3) & 1;
				int scopeN = (blocks[2]>>2) & 1;
				int scopeR = (blocks[2]>>1) & 1;
				int scopeU = (blocks[2]) & 1;
				
				console.printf("LTN=%d, AFI=%d, Mode=%d, MGS[scope]=%c%c%c%c ",
						ltn, afi, mode,
						scopeI==1 ? 'I' : ' ',
						scopeN==1 ? 'N' : ' ',
						scopeR==1 ? 'R' : ' ',
						scopeU==1 ? 'U' : ' ');
			} else if(var == 1) {
				int gap = (blocks[2]>>12) & 3;
				int sid = (blocks[2]>>6) & 0x3F;
				int ta = (blocks[2]>>4) & 3;
				int tw = (blocks[2]>>2) & 3;
				int td = blocks[2] & 3;
				
				console.printf("Gap=%d, SID=%d, for mode 1: Ta=%d, Tw=%d, Td=%d", gap, sid, ta, tw, td);
			}
		}
		
		else
		if(version == 0) {
			int x4 = (blocks[1] & 0x10)>>4;
			console.print("X4=" + x4 + " ");
			
			if(x4 == 0) {
				int single_group = (blocks[1] & 0x8)>>3;
				if(single_group == 1) {
					console.print("single-group: ");
					int dp = blocks[1] & 7;
					int div = (blocks[2]>>15) & 1;
					int dir = (blocks[2]>>14) & 1;
					int extent = (blocks[2]>>11) & 7;
					int event = blocks[2] & 0x7FF;
					int location = blocks[3];
					console.print("DP=" + dp + ", DIV=" + div + ", DIR=" + dir + ", ext=" + extent + ", evt=" + event + ", loc=" + location);
				}
				else {
					int idx = blocks[1] & 7;
					console.print("multi-group [" + idx + "]: ");
					int first = (blocks[2]>>15) & 1;
					if(first == 1) {
						console.print("1st, ");
						
						int dir = (blocks[2]>>14) & 1;
						int extent = (blocks[2]>>11) & 7;
						int event = blocks[2] & 0x7FF;
						int location = blocks[3];
						console.print("DIR=" + dir + ", ext=" + extent + ", evt=" + event + ", loc=" + location);

					} else {
						int second = (blocks[2]>>14) & 1;
						int remaining = (blocks[2]>>12) & 3;
						if(second == 1) {
							console.print("2nd");
						} else console.print("later");
						console.print("[rem=" + remaining + "]");
					}
				}
			} else {
				int addr = blocks[1] & 0xF;
				console.print("Tuning Info: ");
				switch(addr) {
				case 4: case 5:
					providerName[addr-4] = String.format("%c%c%c%c", RDS.toChar((blocks[2]>>8) & 0xFF), RDS.toChar(blocks[2] & 0xFF), RDS.toChar((blocks[3]>>8) & 0xFF), RDS.toChar(blocks[3] & 0xFF));
					console.printf("Prov.name[%d]=\"%s\" ", addr-4, providerName[addr-4]);
					break;
					
				case 6:
					int af1 = (blocks[2] >> 8) & 0xFF;
					int af2 = blocks[2] & 0xFF;
					Station on = otherNetworks.get(blocks[3]);
					if(on == null) on = new OtherNetwork(blocks[3]);
					console.printf("Other Network, ON.PI=%04X", blocks[3]);
					console.print(", ON." + on.addAFPair(af1, af2));
					break;
					
				default: console.print("addr=" + addr);
				}
			}
		}
		
		fireChangeListeners();
	}

	@Override
	public String getName() {
		return "TMC/Alert-C";
	}
	
	@Override
	public int getAID() {
		return AID;
	}
	
	public String getProviderName() {
		return providerName[0] + providerName[1];
	}
}
