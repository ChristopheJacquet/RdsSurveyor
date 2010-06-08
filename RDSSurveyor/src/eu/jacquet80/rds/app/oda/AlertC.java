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

import java.text.Format;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import eu.jacquet80.rds.core.OtherNetwork;
import eu.jacquet80.rds.core.RDS;

public class AlertC extends ODA {
	public static final int AID = 0xCD46;
	
	// provider name
	private String[] providerName = {"????", "????"};
	
	// basic parameters
	private int ltn = -1;			// location table number
	private int afi = -1;			// AF indicator
	private int mgs = -1;			// message geographical scope
	private int mode = 0;			// mode (basic or enhanced)
	private int sid = -1;			// Service ID
	
	private Map<Integer, OtherNetwork> otherNetworks = new HashMap<Integer, OtherNetwork>();
	private List<Message> messages = new ArrayList<Message>();
	private Message currentMessage;
	private Bitstream multiGroupBits;

	private int currentContIndex = -1;
	private int nextGroupExpected = -1;
	private int totalGroupsExpected = -1;
	
	public AlertC() {
	}

	@Override
	public void receiveGroup(int type, int version, int[] blocks, boolean[] blocksOk, int bitTime) {
		boolean messageJustCompleted = false;
		
		// in all cases, we need all blocks to proceed
		if(!blocksOk[2] || !blocksOk[3]) return;
		
		if(type == 3 && version == 0) {
			int var = (blocks[2]>>14) & 0x3;
			console.print("Sys.Info v=" + var+ ", ");
			
			if(var == 0) {
				ltn = (blocks[2]>>6) & 0x3F;
				afi = (blocks[2]>>5) & 1;
				mode = (blocks[2]>>4) & 1;
				mgs = blocks[2] & 0xF;
				
				/*
				int scopeI = (blocks[2]>>3) & 1;
				int scopeN = (blocks[2]>>2) & 1;
				int scopeR = (blocks[2]>>1) & 1;
				int scopeU = (blocks[2]) & 1;
				*/
				
				console.printf("LTN=%d, AFI=%d, Mode=%d, MGS=%s ", ltn, afi, mode, decodeMGS(mgs));
			} else if(var == 1) {
				int gap = (blocks[2]>>12) & 3;
				sid = (blocks[2]>>6) & 0x3F;
				int ta = (blocks[2]>>4) & 3;
				int tw = (blocks[2]>>2) & 3;
				int td = blocks[2] & 3;
				
				console.printf("SID=%d", sid);
				if(mode == 1) console.printf(", mode=1 (enhanced) => gap=%d, Ta=%d, Tw=%d, Td=%d", gap, ta, tw, td);
			}
		}
		
		else
		if(type == 8 && version == 0) {
			int x4 = (blocks[1] & 0x10)>>4;
			console.print("T=" + x4 + " ");
			
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
					currentMessage = new Message(dir, extent, event, location);
					
					// single-group message is complete
					currentMessage.complete = true;
					messageJustCompleted = true;
					
					// reset "expected" indicators
					currentContIndex = -1;
					nextGroupExpected = -1;
				}
				else {
					int idx = blocks[1] & 7;
					
					if(idx == 0 || idx == 7) {
						console.printf("non-standard message [F=0, CI=%d]: %04X-%04X", idx, blocks[2], blocks[3]);
					} else {

						console.print("multi-group [" + idx + "]: ");
						int first = (blocks[2]>>15) & 1;
						if(first == 1) {
							console.print("1st, ");

							int dir = (blocks[2]>>14) & 1;
							int extent = (blocks[2]>>11) & 7;
							int event = blocks[2] & 0x7FF;
							int location = blocks[3];
							console.print("DIR=" + dir + ", ext=" + extent + ", evt=" + event + ", loc=" + location);

							currentMessage = new Message(dir, extent, event, location);
							multiGroupBits = new Bitstream();
							currentContIndex = idx;
							nextGroupExpected = 2;
						} else {
							int second = (blocks[2]>>14) & 1;
							int remaining = (blocks[2]>>12) & 3;
							if(second == 1) {
								totalGroupsExpected = 2 + remaining;
								console.print("2nd ");
							} else console.print("later ");

							int groupNumber = totalGroupsExpected-remaining;

							if(nextGroupExpected >= 0) {
								console.print("(#" + groupNumber + "/" + totalGroupsExpected + 
										") [rem=" + remaining + "]");
								
								if(idx != currentContIndex) {
									console.printf(" ignoring, bad continuity index (was %d), probably missed groups", currentContIndex);
									currentContIndex = -1;
									nextGroupExpected = -1;
								} else if(groupNumber != nextGroupExpected) {
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
											} else {
												console.print("EOM");
												break;
											}
										}
									}
								}
								
								// message is complete if no remaining group
								if(remaining == 0) {
									currentMessage.complete = true;
									messageJustCompleted = true;
								}
								
							} else {  /* if nextGroupExpected = -1 */
								//console.printf("(#%d), ", groupNumber);
								if(currentContIndex == idx) {
									console.print("rem=" + remaining + ", ignoring repeated last group of multi-group message");
								} else {
									console.print("rem=" + remaining + ", ignoring (missed first group of multi-group message)");
								}
							}
						}
					}
				}
			} else {
				int addr = blocks[1] & 0xF;
				console.print("Tuning Info: ");
				
				OtherNetwork on = null;
				if(addr >= 6 && addr <= 9){
					on = otherNetworks.get(blocks[3]);
					if(on == null) on = new OtherNetwork(blocks[3]);
					otherNetworks.put(blocks[3], on);
				}

				
				switch(addr) {
				case 4: case 5:
					providerName[addr-4] = String.format("%c%c%c%c", RDS.toChar((blocks[2]>>8) & 0xFF), RDS.toChar(blocks[2] & 0xFF), RDS.toChar((blocks[3]>>8) & 0xFF), RDS.toChar(blocks[3] & 0xFF));
					console.printf("Prov.name[%d]=\"%s\" ", addr-4, providerName[addr-4]);
					break;
										
				case 6:
					int af1 = (blocks[2] >> 8) & 0xFF;
					int af2 = blocks[2] & 0xFF;
					
					console.printf("Other Network, ON.PI=%04X", blocks[3]);
					console.print(", ON." + on.addAFPair(af1, af2));
					break;
					
				case 8:
					console.printf("Other Networks, ON.PI=%04X, ON.PI=%04X", blocks[2], blocks[3]);
					break;
					
				case 9:
					console.printf("Other Network, ON.PI=%04X", blocks[3]);
					int ltn = (blocks[2]>>10) & 0x3F;
					int mgs = (blocks[2]>>6) & 0xF;
					int sid = blocks[2] & 0x3F;
					console.printf(", ON.LTN=" + ltn + ", ON.MGS=" + decodeMGS(mgs) + ", ON.SID=" + sid);
					break;
					
				default: console.print("addr=" + addr);
				}
			}
		}
		
		// if a message has just been completed, update the list of messages
		// accordingly
		if(messageJustCompleted) {
			// 1) first we need to remove any message overriden by the current one
			List<Message> messagesToRemove = new LinkedList<Message>();
			int oldUpdate = 0;
			for(Message m : messages) {
				if(currentMessage.overrides(m)) {
					messagesToRemove.add(m);
					oldUpdate = m.updateCount;
				}
			}
			
			for(Message msgToRemove : messagesToRemove) {
				messages.remove(msgToRemove);
			}
			
			// 2) second we just need to add the current message
			messages.add(currentMessage);
			
			currentMessage.updateCount = oldUpdate + 1;
			
			//System.out.println("*** Current TMC messages: ");
			//for(Message m : messages) System.out.println("\t" + m);
		}
		
		fireChangeListeners();
	}

	private static String decodeMGS(int mgs) {
		if(mgs < 0) return "";
		return
			((mgs&8) != 0 ? "I" : "") +
			((mgs&4) != 0 ? "N" : "") +
			((mgs&2) != 0 ? "R" : "") +
			((mgs&1) != 0 ? "U" : "");
	}
	
	public List<Message> getMessages() {
		return messages;
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
	
	public int getLTN() {
		return ltn;
	}
	
	public int getAFI() {
		return afi;
	}
	
	public String getMGS() {
		return decodeMGS(mgs);
	}
	
	public int getMode() {
		return mode;
	}
	
	public int getSID() {
		return sid;
	}
	
	private static class Bitstream {
		private long bits;
		private int count;
		
		public Bitstream() {
			bits = 0L;
		}
		
		public int peek(int count) {
			return (int) ( ( bits >> (this.count - count) ) & ( (1L << count) - 1 ) );
		}
		
		public int take(int count) {
			int res = peek(count);
			this.count -= count;
			bits &= (1L << this.count) - 1;   // remove the count leftmost bits
			///System.out.println(" *" + ((1L << this.count)-1) + "* ");
			return res;
		}
		
		public void add(long bits, int count) {
			this.count += count;
			this.bits <<= count;
			this.bits |= bits;
		}
		
		public int count() {
			return count;
		}
		
		@Override
		public String toString() {
			StringBuffer res = new StringBuffer();
			StringBuffer theBits = new StringBuffer(Long.toBinaryString(bits));
			res.append(count).append('/');
			for(int i=0; i<count-theBits.length(); i++)
				res.append('0');
			return res.append(theBits).toString();
		}
	}
	
	public static class Message {
		private final int direction;
		private final int extent;
		private final List<Integer> events;
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
		private boolean complete = false;
		private int updateCount = 0;
		
		public final static int[] labelSizes = {3, 3, 5, 5, 5, 8, 8, 8, 8, 11, 16, 16, 16, 16, 0, 0};
		
		public Message(int direction, int extent, int event, int location) {
			this.direction = direction;
			this.extent = extent;
			this.events = new ArrayList<Integer>(1);
			events.add(event);
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
			case 0: duration = value;
			break;
			
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
			
			case 9:		// additional event
				events.add(value);
				break;
				
			// TODO complete!
			}
		}
		
		public boolean overrides(Message m) {
			return
				(location == m.location || location == 65535) &&
				(direction == m.direction) &&
				hasAnEventFromTheSameUpdateClassAs(m) &&
				(!isForecastMessage() || duration == m.duration);
				// is forecast message => same duration
		}
		
		/**
		 * As per TMC/Alert-C standard, "contains an event that belongs to the
		 * same update class as any event (a multi-group message may have more
		 * than one event) in the existing message)"
		 * 
		 * @param m
		 * @return
		 */
		private boolean hasAnEventFromTheSameUpdateClassAs(Message m) {
			for(Integer u : events) {
				for(Integer v : m.events) {
					if(updateClasses[u] == updateClasses[v]) return true;
				}
			}
			return false;
		}
		
		/**
		 * Contains an event from one of the forecast update classes.
		 * 
		 * @return
		 */
		private boolean isForecastMessage() {
			for(Integer u : events) {
				int c = updateClasses[u];
				if(c >= 32 && c <= 39) return true;
			}
			return false;
		}
		
		@Override
		public String toString() {
			return location + ": " + events + "(" + updateCount + ")";
		}
		
		public int getLocation() {
			return location;
		}
		
		public List<Integer> getEvents() {
			return events;
		}
		
		public int getUpdateCount() {
			return updateCount;
		}
	}
	
	/* Table derived from work done by Tobias Lorenz
	 */
	private static byte[] updateClasses = {
		 0,  1,  1,  0,  0,  0,  0,  0,  0,  0,  0,  4,  3,  0,  0,  0, 
		 5,  0,  0,  0,  9,  0,  9,  9,  9,  9,  6,  6,  9,  0,  0,  0, 
		 0,  0,  0,  0,  9,  9,  0,  0,  9,  5,  5,  0,  0,  0,  0,  0, 
		 0,  0,  0,  5, 11, 11,  0,  2,  2,  2,  0,  0,  0, 12, 12, 12, 
		12,  0,  0,  0,  0,  0,  1,  1,  1,  1,  1,  1,  1,  0,  0,  0, 
		 0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0, 20,  0,  0,  0,  0, 
		 0,  0,  0,  0,  0,  1,  1,  1,  1,  1,  1,  2,  1,  1,  1,  1, 
		 1,  1,  2,  1,  1,  1,  1,  1,  1,  2,  1,  2,  1,  1,  1,  1, 
		 1,  1,  1,  1,  1,  1,  1,  1,  1,  1,  1,  1,  1,  3,  1,  1, 
		 0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0, 
		 0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0, 
		 0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0, 
		 0,  0,  0,  0,  0,  0,  0,  0, 20,  3,  3,  3,  3,  3,  3,  3, 
		 1,  3, 12,  4,  4,  3,  4,  1,  1,  1,  1,  1,  1,  1,  1,  1, 
		 1,  1,  1,  1,  1,  1,  1,  1,  1,  1,  1,  2,  1,  2,  1,  1, 
		 5,  5,  5,  5,  5,  5,  5, 20, 20, 20,  1,  1,  1,  1,  1,  1, 
		 1,  1,  1,  1,  1,  1,  1,  1,  1,  1,  1,  1,  1,  1,  2,  1, 
		 2,  0,  1, 20, 20, 20,  1,  1,  1,  1,  1,  1,  1,  1,  1,  1, 
		 1,  1,  1,  1,  1,  1,  1,  1,  1,  1,  2,  1,  2,  1,  1,  5, 
		 5,  5,  5,  5,  5,  5, 20, 20, 20,  1,  1,  1,  1,  1,  2,  1, 
		 2,  1,  1,  5,  5,  5,  5,  5,  5,  5, 20, 20, 20,  3,  3,  3, 
		 3,  3,  3,  3,  3,  3,  3,  3,  3,  3,  4, 24,  1,  1,  1,  3, 
		 1,  1,  1,  1,  1,  1,  1, 12,  1,  1,  1,  1,  1,  2,  1,  2, 
		 1,  5,  5,  5,  5,  5,  5, 20, 20, 20,  3,  1,  1,  1,  1,  1, 
		 0,  1,  0,  1, 20,  0, 20,  3,  3,  4,  4,  4,  4,  4,  0,  8, 
		 0,  5,  5,  9,  9,  9,  8,  7,  7,  7,  1,  1,  1,  1,  1,  1, 
		 1,  1,  1,  1,  1,  1,  1,  1,  1,  1,  1,  1,  1,  1,  2,  1, 
		 2,  1,  1, 20, 20, 20,  1,  1,  1,  1,  1,  1,  1,  1,  1,  1, 
		 1,  1,  1,  1,  1,  1,  1,  1,  1,  1,  2,  1,  2,  1,  1, 20, 
		20, 20,  7,  5,  9,  9,  9,  8,  8,  8,  7,  7,  7,  7,  7,  6, 
		 6,  6,  6,  6,  6,  7,  6,  6,  6,  6,  6,  6,  9,  9,  9,  1, 
		 1,  1,  1,  1,  5,  5,  5,  5,  5,  5,  5,  5,  5,  5,  5,  5, 
		 5,  5,  5,  5,  5,  5,  5,  5,  5,  1,  1,  1,  1,  1,  1,  1, 
		 1,  1,  1,  1,  1,  1,  1,  1,  1,  1,  1,  1,  1,  2,  1,  2, 
		 1,  1,  1,  1,  1,  1,  1,  2,  1,  2,  1,  1,  1,  1,  1,  1, 
		 1,  2,  1,  2,  1,  1,  1,  1,  1,  1,  1,  2,  1,  2,  1,  1, 
		 1,  1,  1,  1,  1,  1,  1,  1,  1,  1,  1,  1,  1,  1,  1,  1, 
		 1,  1,  1,  1,  2,  1,  2,  1,  1,  5,  5,  5,  1,  1,  1,  1, 
		 1,  2,  1,  2,  1,  1,  1,  1,  1,  1,  1,  2,  1,  2,  1,  1, 
		 5,  5,  1,  9,  9,  9,  5,  5,  8,  7,  6,  9,  9,  5,  5,  5, 
		 5,  5,  5,  5,  5,  5,  5, 10, 10, 10, 10,  1,  1,  1,  1,  1, 
		 1,  5,  5, 10,  9,  9,  9,  6,  5,  5,  5,  0,  0,  0,  0, 10, 
		 6,  7,  0,  0,  6,  0,  5,  5,  9,  2,  0,  0,  0,  0,  0,  0, 
		 0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0, 11, 11, 11, 
		11, 11, 13, 11, 11, 11,  1,  1,  1,  1,  1,  1,  1,  1,  1,  1, 
		 1,  1,  1,  1,  1,  1,  1,  1,  1,  1,  2,  1,  2,  1,  1,  5, 
		 5,  5,  5,  5,  5,  5,  5,  5,  5,  5,  5, 20, 20, 20,  1,  1, 
		 1,  1,  1,  1,  1,  1,  1,  1,  1,  1,  1,  1,  1,  1,  1,  1, 
		 1,  1,  2,  1,  2,  1,  1,  5,  5,  5,  5,  5, 20, 20, 20,  1, 
		 1,  1,  1,  1,  2,  1,  2,  1,  1,  5,  5,  5,  5,  5,  5,  5, 
		11, 11, 11, 11, 23, 11, 11, 11, 11, 11, 11, 11,  1,  1,  1, 11, 
		11, 11,  1,  1,  1, 11, 11, 11, 13,  1,  1,  1,  1,  1,  2,  1, 
		 2,  1,  1,  5,  5,  5,  5,  5, 20, 20, 20, 20, 20, 20, 20, 20, 
		20, 20, 20, 20, 13, 11, 11, 11, 13, 13, 13, 13, 13, 13, 13,  0, 
		 0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0, 
		 0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0, 
		 0,  0, 12, 12, 12, 12, 12, 12, 12, 12, 12, 12, 12, 12, 12, 12, 
		12, 12, 12, 12, 14, 12, 12, 12, 12, 12, 13, 13,  4,  5,  5, 12, 
		 1,  1,  1,  1,  1,  2,  1,  2,  1,  1,  9, 20, 20, 20, 12,  9, 
		12,  9, 12,  5, 12,  9, 12,  5,  5,  5,  5, 12,  5,  5, 20, 20, 
		20,  5, 20, 20, 20,  5, 20, 20, 20,  5, 12, 12, 12, 12, 12, 12, 
		12, 12, 12, 12,  5, 12,  5, 12, 12, 12, 12,  5, 12, 12, 12, 12, 
		12,  9, 12,  9, 12, 12, 12, 12, 12, 14, 14, 14, 14, 14, 14, 14, 
		14, 14, 14, 14, 14, 14, 14, 14, 14, 14, 14, 14, 14,  5,  5,  5, 
		14, 14, 12, 20, 20, 20, 12, 12, 12, 12,  4, 14, 14, 14, 14, 14, 
		14, 14, 14, 14, 14, 14,  0, 14, 14,  0, 14,  0,  0,  0, 14, 14, 
		14, 14, 14, 14, 14, 14, 14, 14, 14, 14,  4, 13, 13, 14, 14,  0, 
		 0, 14, 14, 14,  0,  0,  0, 15, 15, 15, 15, 15, 12,  0, 12,  0, 
		 0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0, 16, 16, 16, 
		16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 15, 15,  0,  0,  0, 
		 0,  0,  0,  0,  0,  0, 16, 15, 16,  0, 16,  0, 16,  0, 16, 16, 
		16, 16,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0, 
		 0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0, 14, 14,  0, 
		 0,  0, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16,  0,  0,  0, 
		 0,  0,  0,  0,  0,  0, 17, 17,  0,  0,  0,  0,  0,  0,  0,  0, 
		 0, 17, 17, 17, 17, 17,  0,  0,  0, 17, 17, 17,  9, 17, 17,  9, 
		 0, 17,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0, 
		 0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0, 
		 0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0, 
		 0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0, 
		 0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0, 
		 0,  0,  0,  0,  0, 16, 16, 16, 16, 16,  0, 16, 16, 16, 16,  0, 
		16, 16, 16,  0,  0,  0, 16, 16, 16, 16, 16, 16, 16, 16, 16,  0, 
		 0,  0,  0,  0,  9,  0,  0,  0,  0, 16,  9,  0, 16,  0,  0,  0, 
		 0, 16, 16,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0, 
		 0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0, 
		 0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0, 
		 0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0, 
		 0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0, 
		 0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0, 
		 0,  0,  0,  0,  0,  0,  0,  0,  0,  0, 18, 18, 18, 18, 18, 18, 
		18, 18, 18, 18, 18, 18, 18, 18, 18, 18, 18, 18, 18, 18, 18, 13, 
		13, 13, 13, 18, 19, 19, 19, 19, 19, 19, 13, 13, 13,  9, 20, 20, 
		20, 20, 20, 20, 19, 18, 19,  1, 18,  0,  0,  0,  0, 18, 18, 18, 
		18, 18, 18, 18, 18, 18, 18, 18, 18, 18, 18, 19, 19,  1,  1,  1, 
		 1,  1,  2,  1,  2,  1,  1,  9, 20, 20, 20,  1,  1,  1,  1,  1, 
		 2,  1,  2,  1,  1,  9, 20, 20, 20,  1,  1,  1,  1,  1,  2,  1, 
		 2,  1,  1,  9, 20, 20, 20,  9, 20, 20, 20,  9, 20, 20, 20,  9, 
		20, 20, 20,  1,  1,  1,  1,  1,  2,  1,  2,  1,  9, 20, 20, 20, 
		 1, 18,  1, 19, 19, 31, 31, 18, 18, 18, 18, 18, 18, 18,  0,  0, 
		 0, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 21, 
		21, 21, 21, 21, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 
		20, 21, 21, 21, 21, 21, 21, 21, 21, 21, 20, 20, 21, 21, 22, 22, 
		20, 21, 20, 21, 21, 20, 20, 21, 22, 21, 21, 21, 21, 21, 22, 20, 
		 0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0, 
		 0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0, 22, 
		22,  0,  0,  0, 12, 23, 23, 23, 23, 23, 23, 23, 23, 12, 23, 23, 
		12,  0,  0,  0,  0,  0,  0,  0, 21, 21,  0,  0,  0,  0,  0,  0, 
		 0,  0,  0, 24, 24, 24, 24, 24, 23, 24, 24, 24, 20, 20,  0,  0, 
		 0,  0,  0,  0,  0,  0,  0, 24, 24, 24, 24, 24, 20, 20, 20, 20, 
		20, 20, 24, 24, 24, 23, 24, 24, 23, 23, 24, 13,  0,  0,  0,  0, 
		 0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0, 
		 0,  0,  0,  0,  0,  0,  0,  0,  0, 25, 25, 25, 25, 25, 25,  1, 
		 1,  1,  1,  1,  2,  1,  2,  1,  1, 20, 20, 20,  1,  1,  1,  1, 
		 1,  2,  1,  2,  1,  1, 20, 20, 20, 25, 25, 25, 25, 25, 25, 25, 
		25, 25, 25, 25, 25, 25, 25, 25, 25, 25, 25, 26, 26,  0,  9, 28, 
		28, 26, 20,  0,  0, 26, 26,  0, 25, 25, 25, 25, 20, 20, 20, 26, 
		26, 26, 26, 25, 20, 20, 20, 25, 20, 26, 26, 27, 20, 20, 27, 27, 
		28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 22, 22, 28, 
		28, 28, 21, 31, 29, 29, 29, 29,  0, 29, 30, 31, 30,  0, 28,  0, 
		28, 28, 28, 28, 28, 28, 28, 27, 27, 29, 30, 29, 30,  0, 28,  0, 
		 0,  0, 28,  0, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 
		30, 30, 30, 29, 30, 30,  0,  0,  0,  9, 10, 10, 30, 30, 30, 30, 
		 0,  0,  0,  9,  9,  9,  9,  0,  0,  9,  9,  9,  0,  0,  6, 25, 
		 0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0, 
		 9,  0,  0,  0,  0,  0, 10, 10,  0,  0,  0,  0,  0,  9,  0,  0, 
		 0,  0,  0,  0,  0, 21, 21,  0,  0,  0,  0,  0,  4, 10, 13,  0, 
		 0, 19, 21, 22,  0,  0, 28, 30,  0,  0,  0,  0,  0,  0,  0, 31, 
	};
}