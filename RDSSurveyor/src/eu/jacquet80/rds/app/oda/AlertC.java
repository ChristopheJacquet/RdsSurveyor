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
	private Message currentMessage;
	private Bitstream multiGroupBits;
	private int currentContIndex = -1;
	private int nextGroupExpected = -1;
	private int totalGroupsExpected = -1;
	
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
						
						// TODO find a message to possibly update
						currentMessage = new Message(dir, extent, event, location);
						multiGroupBits = new Bitstream();
						currentContIndex = idx;
						nextGroupExpected = 2;
					} else {
						int second = (blocks[2]>>14) & 1;
						int remaining = (blocks[2]>>12) & 3;
						if(second == 1) {
							totalGroupsExpected = 2 + remaining;
						} else console.print("later");
						
						int groupNumber = totalGroupsExpected-remaining;
						
						console.print("#" + groupNumber + "/" + totalGroupsExpected + 
								" [rem=" + remaining + "]");
						
						if(groupNumber != nextGroupExpected) {
							console.print(" ignoring, next expected is #" + nextGroupExpected);
						} else {
							nextGroupExpected++;
							if(nextGroupExpected > totalGroupsExpected) nextGroupExpected = -1;
							
							multiGroupBits.add(blocks[2] & 0xFFF, 12);
							multiGroupBits.add(blocks[3], 16);
							
							console.print("  ");
							
							console.print(" [" + multiGroupBits + "] ");
							
							while(multiGroupBits.count() >= 4) {
								int label = multiGroupBits.peek(4);
								if(multiGroupBits.count() < 4 + Message.labelSizes[label]) {
									break;
								} else {
									multiGroupBits.take(4);
									int value = multiGroupBits.take(Message.labelSizes[label]);
									if(!(label == 0 && value == 0)) {
										console.print(label + "->" + value + ", ");
										currentMessage.addField(label, value);
									}
								}
							}
							
						}
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
	
	private static class Bitstream {
		private long bits;
		private int count;
		
		public Bitstream() {
			bits = 0L;
		}
		
		public int peek(int count) {
			return (int) ( ( bits >> (this.count - count) ) & ( (1 << count) - 1 ) );
		}
		
		public int take(int count) {
			int res = peek(count);
			this.count -= count;
			return res;
		}
		
		public void add(int bits, int count) {
			this.count += count;
			this.bits <<= count;
			this.bits |= bits;
		}
		
		public int count() {
			return count;
		}
		
		@Override
		public String toString() {
			return count + "/" + Long.toBinaryString(bits);
		}
	}
	
	private static class Message {
		private final int direction;
		private final int extent;
		private final int event;
		private final int location;
		private int diversion = 0;
		private int duration = 0;
		private int urgency = 0;
		private boolean directional = true;  // TODO default
		private boolean dynamic = true;      // TODO default
		private boolean spoken = false;      // TODO default
		private int steps = 0;               // TODO default
		private int length = 0;
		private int speed = 0;
		private int quantifier = 0;
		private int suppInfo = 0;
		
		public final static int[] labelSizes = {3, 3, 5, 5, 5, 8, 8, 8, 8, 11, 16, 16, 16, 16, 0, 0};
		
		public Message(int direction, int extent, int event, int location) {
			this.direction = direction;
			this.extent = extent;
			this.event = event;
			this.location = location;
		}
		
		public Message(int direction, int extent, int event, int location, int diversion, int duration) {
			this(direction, extent, event, location);
			this.diversion = diversion;
			this.duration = duration;
		}
		
		public void addField(int label, int value) {
			switch(label) {
			// duration
			case 0: duration = value; break;
			
			// control code
			case 1:
				switch(value) {
				case 0: urgency++; break;
				case 1: urgency--; break;
				case 2: directional = !directional; break;
				case 3: dynamic = !dynamic; break;
				case 4: spoken = !spoken; break;
				case 5: diversion = 1; break;
				case 6: steps += 8; break;
				case 7: steps += 16; break;
				}
				break;

			
			case 2:
				if(value == 0) length = 100;
				else if(value <= 10) length = value;
				else if(value <= 15) length = 10 + (value-10)*2;
				else length = 20 + (value-15)*5;
				break;
			
			case 3:
				speed = 5*value;
				break;
			
			case 4:
			case 5:
				quantifier = value;
				break;
				
			case 6:
				suppInfo = value;
				break;
				
			// TODO complete!
			}
		}
	}
}