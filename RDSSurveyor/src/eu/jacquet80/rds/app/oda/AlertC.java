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

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

import eu.jacquet80.rds.app.oda.tmc.SupplementaryInfo;
import eu.jacquet80.rds.app.oda.tmc.TMC;
import eu.jacquet80.rds.app.oda.tmc.TMCEvent;
import eu.jacquet80.rds.app.oda.tmc.TMCEvent.EventDurationType;
import eu.jacquet80.rds.app.oda.tmc.TMCEvent.EventNature;
import eu.jacquet80.rds.app.oda.tmc.TMCEvent.EventUrgency;
import eu.jacquet80.rds.app.oda.tmc.TMCLocation;
import eu.jacquet80.rds.app.oda.tmc.TMCPoint;
import eu.jacquet80.rds.core.RDS;
import eu.jacquet80.rds.core.TMCOtherNetwork;
import eu.jacquet80.rds.log.RDSTime;

public class AlertC extends ODA {
	public static final int AID = 0xCD46;
	
	private static int[] codesTd = {0, 1, 2, 3};
	private static int[] codesTa = {1, 2, 4, 8};
	private static int[] codesTw = {1, 2, 4, 8};
	private static int[] codesGap = {3, 5, 8, 11};
	
	// provider name
	private String[] providerName = {"????", "????"};
	
	// basic parameters
	private int cc = -1;			// country code (CC) from PID
	private int ltn = -1;			// location table number
	private int afi = -1;			// AF indicator
	private int mgs = -1;			// message geographical scope
	private int mode = 0;			// mode (basic or enhanced)
	private int sid = -1;			// Service ID
	
	private Map<Integer, TMCOtherNetwork> otherNetworks = Collections.synchronizedMap(new HashMap<Integer, TMCOtherNetwork>());
	private List<Message> messages = new ArrayList<Message>();
	private Comparator<Message> messageComparator = new DefaultComparator();
	private Message currentMessage;
	private Bitstream multiGroupBits;

	private int currentContIndex = -1;
	private int nextGroupExpected = -1;
	private int totalGroupsExpected = -1;
	
	private Set<String> onInfo = new HashSet<String>();
	
	public AlertC() {
	}

	@Override
	public void receiveGroup(PrintWriter console, int type, int version, int[] blocks, boolean[] blocksOk, RDSTime time) {
		boolean messageJustCompleted = false;
		
		// in all cases, we need all blocks to proceed
		if(!blocksOk[2] || !blocksOk[3]) return;
		
		// get CC so that we can decode locations
		if (blocksOk[0])
			cc = blocks[0] >> 12;
		
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
				int gap = codesGap[(blocks[2]>>12) & 3];
				sid = (blocks[2]>>6) & 0x3F;
				int ta = codesTa[(blocks[2]>>4) & 3];
				int tw = codesTw[(blocks[2]>>2) & 3];
				int td = codesTd[blocks[2] & 3];
				
				console.printf("SID=%d", sid);
				if(mode == 0) console.printf(", mode=0 (basic) => min gap=%d groups", gap);
				else if(mode == 1) console.printf(", mode=1 (enhanced) => min gap=%d groups, Ta=%ds, Tw=%ds, Td=%ds", gap, ta, tw, td);
			}
		}
		
		else
		if(type == 8 && version == 0) {
			int x4 = (blocks[1] & 0x10)>>4;
			console.print("T=" + x4 + " ");
			
			if(x4 == 0) {
				Date date = station.getRealTimeForStreamTime(time);
				if (date == null)
					date = new Date();
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
					currentMessage = new Message(dir, extent, event, location, cc, ltn, sid, date, station.getTimeZone(), div == 1, dp);
					
					// single-group message is complete
					currentMessage.complete();
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
							console.print("dir=" + dir + ", ext=" + extent + ", evt=" + event + ", loc=" + location);

							currentMessage = new Message(dir, extent, event, location, cc, ltn, sid, date, station.getTimeZone());
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
									
									if ((second == 1) && (currentMessage.interroad)) {
										/* 
										 * Second group of an INTER-ROAD message.
										 * Free bits are preceded by 16 bits location code.
										 * Early documents on TMC mention the EUROAD format with
										 * three DP bits after the location code (with free format
										 * bits starting at Z8), but later documents explicitly
										 * state that free format bits start at Z11, immediately
										 * after the location code. 
										 */
										currentMessage.setLocation(multiGroupBits.take(16));
									}

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
									currentMessage.complete();
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
				
				TMCOtherNetwork on = null;
				if (addr >= 6 && addr <= 9)
					synchronized(otherNetworks) {
						on = otherNetworks.get(blocks[3]);
						if (on == null)
							on = new TMCOtherNetwork(blocks[3]);
						otherNetworks.put(blocks[3], on);
					}
				
				String newOnInfo = null;

				
				switch(addr) {
				case 4: case 5:
					providerName[addr-4] = String.format("%c%c%c%c", RDS.toChar((blocks[2]>>8) & 0xFF), RDS.toChar(blocks[2] & 0xFF), RDS.toChar((blocks[3]>>8) & 0xFF), RDS.toChar(blocks[3] & 0xFF));
					console.printf("Prov.name[%d]=\"%s\" ", addr-4, providerName[addr-4]);
					break;
										
				case 6:
					int af1 = (blocks[2] >> 8) & 0xFF;
					int af2 = blocks[2] & 0xFF;
					
					newOnInfo = String.format("ON.PI=%04X", blocks[3]);
					newOnInfo += ", ON.AF=(" + on.addAFPair(af1, af2) + ")";
					break;
					
				case 7:
					newOnInfo = String.format("ON.PI=%04X", blocks[3]);
					newOnInfo += ", ON.AF=(" + on.addMappedFreq((blocks[2]>>8) & 0xFF, blocks[2] & 0xFF) + ")";
					break;
					
				case 8:
					newOnInfo = String.format("ON.PI=%04X, ON.PI=%04X", blocks[2], blocks[3]);
					synchronized(otherNetworks) {
						on = otherNetworks.get(blocks[2]);
						if (on == null)
							on = new TMCOtherNetwork(blocks[2]);
						otherNetworks.put(blocks[2], on);
					}
					break;
					
				case 9:
					newOnInfo = String.format("ON.PI=%04X", blocks[3]);
					int ltn = (blocks[2]>>10) & 0x3F;
					int mgs = (blocks[2]>>6) & 0xF;
					int sid = blocks[2] & 0x3F;
					newOnInfo += " (" + on.setService(ltn, mgs, sid) + ")";
					break;
					
				default: console.print("addr=" + addr);
				}
				
				if(newOnInfo != null) {
					console.print("Other Network, " + newOnInfo);
					onInfo.add(newOnInfo);
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
			// (unless it is a cancellation message)
			if(! currentMessage.isCancellation()) {
				messages.add(currentMessage);
				Collections.sort(messages, messageComparator);
			}
			
			currentMessage.updateCount = oldUpdate + 1;
			
			//System.out.println("*** Current TMC messages: ");
			//for(Message m : messages) System.out.println("\t" + m);
			
			// 3) display it
			console.println();
			console.print(currentMessage);
		}
		
		fireChangeListeners();
	}

	public static String decodeMGS(int mgs) {
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
	
	public Map<Integer, TMCOtherNetwork> getOtherNetworks() {
		return otherNetworks;
	}
	
	public int getSID() {
		return sid;
	}
	
	public Set<String> getONInfo() {
		return onInfo;
	}
	
	/**
	 * @brief Sets a new comparator, which will be used to sort the list of messages.
	 * 
	 * Setting a new comparator causes the list to be sorted with the new comparator and registered
	 * change listeners to fire.
	 * 
	 * @param comparator The new comparator
	 */
	public void setComparator(Comparator<Message> comparator) {
		if (comparator != messageComparator) {
			messageComparator = comparator;
			Collections.sort(messages, messageComparator);
			fireChangeListeners();
		}
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
			StringBuilder res = new StringBuilder();
			StringBuilder theBits = new StringBuilder(Long.toBinaryString(bits));
			res.append(count).append('/');
			for(int i=0; i<count-theBits.length(); i++)
				res.append('0');
			return res.append(theBits).toString();
		}
	}
	
	/**
	 * @brief Represents a TMC message.
	 * 
	 * A TMC message is a single report about an event at a particular location. It contains one or
	 * more event codes (description of "what"), a primary location and an optional secondary location
	 * (description of "where").
	 */
	public static class Message {
		/** Flags to denote an INTER-ROAD location code */
		public static final int LOCATION_INTER_ROAD = 0xFC00;
		/** Special location code for messages intended for all listeners */
		public static final int LOCATION_ALL_LISTENERS = 65533;
		/** Special location code for silent location (do not present a location) */
		public static final int LOCATION_SILENT = 65534;
		/** 
		 * Special location code for location-independent update/cancellation of messages.
		 * When comparing locations, this code will match any location.
		 */
		public static final int LOCATION_INDEPENDENT = 65535;
		
		// basic information
		/** Direction of queue growth (0 for positive, 1 for negative). */
		private final int direction;
		/** The geographic extent of the event, expressed as a number of steps from 0 to 31. */
		private int extent;		
		// extent, affected by 1.6 and 1.7   (= number of steps, see ISO 81419-1, par. 5.5.2 a: 31 steps max
		/** The time at which the message was received. */
		private Date date = null;
		/** The time zone to be used for persistence times based on "midnight". */
		private TimeZone timeZone;
		/** The country code of the service that sent the message (from RDS PI). */
		private final int cc;
		/** The Location Table Number (LTN) of the service that sent the message. */
		private int ltn;
		/** The Service ID (SID). */
		private int sid;
		/** Whether the message has an INTER-ROAD location, i.e. uses a foreign location table */
		private final boolean interroad;
		/** The country code of the location. */
		private int fcc = -1;
		/** The Foreign Location Table Number (LTN), i.e. the LTN for the location. */
		private int fltn = -1;
		/** The raw location code. */
		private int location;
		/** The resolved location, if the location is contained in a previously loaded TMC location table. */
		private TMCLocation locationInfo;
		/** Whether the default directionality of the message should be reversed. */
		private boolean reversedDirectionality = false;
		/** Whether the event affects both directions. */
		private boolean bidirectional = true;
		/** The coordinates of the event. */
		private float[] coords = null;
		/** The auxiliary coordinates of the event. */
		private float[] auxCoords = null;

		private boolean reversedDurationType = false;
		
		private int duration = 0;		// 0- duration (0 if not set)
		private int startTime = -1;		// 7- start time
		private int stopTime = -1;		// 8- stop time
		// 13- cross linkage to source

		/** Number of levels by which to increase or decrease default urgency. */
		private int increasedUrgency = 0;
		/** The urgency of the message. */
		private EventUrgency urgency;
		
		/*
		 * The TMC event which was followed by the duration.
		 * 
		 * Duration and expiration of a message is represented by a duration code. How this code
		 * maps to actual times is governed by the event nature and duration type. The latter two
		 * values are specific to an event and may differ between multiple events of a multi-event
		 * message. In this case, the nature and duration type of the last event received before the
		 * duration field apply (the last event may be the first group event).
		 */
		private AlertC.Event eventForDuration = null;
		
		private boolean spoken = false;      // TODO default   // 1.4
		private boolean diversion = false;							   // 1.5
		
		private List<InformationBlock> informationBlocks = new ArrayList<AlertC.InformationBlock>();
		private InformationBlock currentInformationBlock;
		
		private boolean complete = false;
		private int updateCount = 0;
		
		public final static int[] labelSizes = {3, 3, 5, 5, 5, 8, 8, 8, 8, 11, 16, 16, 16, 16, 0, 0};
		
		private void addInformationBlock(int eventCode) {
			this.currentInformationBlock = new InformationBlock(this, eventCode);
			this.informationBlocks.add(currentInformationBlock);			
		}

		private void addInformationBlock() {
			this.currentInformationBlock = new InformationBlock(this);
			this.informationBlocks.add(currentInformationBlock);			
		}

		private String formatTime(int time) {
			if(time >= 0 && time <= 95) {
				return String.format("%02d:%02d", time / 4, (time % 4) * 15);
			} else if(time >= 96 && time <= 200) {
				return String.format("midnight + %d days, %d hours", (time-96) / 24, (time-96) % 24);
			} else if(time >= 201 && time <= 231) {
				return "day " + (time - 200) + " this month";
			} else if(time >= 232 && time <= 255) {
				return String.format("%02d/%02d", 15 + 16*((time-231)%2),(time-231)/2);
			} else {
				return "INVALID";
			}
		}
		
		/**
		 * @brief Creates a new TMC/ALERT-C message.
		 * 
		 * This constructor is intended for multi-group messages, which have no fixed fields for
		 * diversion and duration. Instead, default values (duration 0, no diversion) are assumed
		 * unless one of the optional fields in the subsequent messages of the group sets a
		 * different value.
		 * 
		 * @param direction
		 * @param extent
		 * @param eventCode
		 * @param location
		 * @param cc
		 * @param ltn
		 * @param sid
		 */
		public Message(int direction, int extent, int eventCode, int location, int cc, int ltn, int sid, Date date, TimeZone tz) {
			this.direction = direction;
			this.extent = extent;
			this.date = date;
			this.timeZone = tz;
			this.cc = cc;
			this.ltn = ltn;
			this.sid = sid;
			if ((location < LOCATION_INTER_ROAD) || (location >= LOCATION_ALL_LISTENERS)) {
				interroad = false;
				fcc = cc;
				fltn = ltn;
			} else {
				interroad = true;
				fcc = (location >> 6) & 0xF;
				fltn = location & 0x3F;
			}
			this.setLocation(location);
			addInformationBlock(eventCode);
		}
		
		/**
		 * @brief Creates a new TMC/ALERT-C message.
		 * 
		 * This constructor is intended for single-group messages, which have fields for diversion and duration.
		 * 
		 * @param direction
		 * @param extent
		 * @param eventCode
		 * @param location
		 * @param cc
		 * @param ltn
		 * @param sid
		 * @param diversion
		 * @param duration
		 */
		public Message(int direction, int extent, int eventCode, int location, int cc, int ltn, int sid, Date date, TimeZone tz, boolean diversion, int duration) {
			this(direction, extent, eventCode, location, cc, ltn, sid, date, tz);
			this.diversion = diversion;
			this.duration = duration;
			this.eventForDuration = this.currentInformationBlock.currentEvent;
		}
		
		public void addField(int label, int value) {
			switch(label) {
			// duration
			case 0:
				duration = value;
				this.eventForDuration = this.currentInformationBlock.currentEvent;
				break;
			
			// control code
			case 1:
				switch(value) {
				case 0: increasedUrgency++; break;
				case 1: increasedUrgency--; break;
				case 2: reversedDirectionality = true; break;
				case 3: reversedDurationType = true; break;
				case 4: spoken = !spoken; break;
				case 5: diversion = true; break;
				case 6: extent += 8; break;			// extent == number of steps
				case 7: extent += 16; break;		// extent == number of steps
				}
				break;

			
			case 2:
				if(value == 0) currentInformationBlock.length = 1000;
				else if(value <= 10) currentInformationBlock.length = value;
				else if(value <= 15) currentInformationBlock.length = 10 + (value-10)*2;
				else currentInformationBlock.length = 20 + (value-15)*5;
				break;
			
			case 3:
				currentInformationBlock.speed = 5*value;
				break;
			
			case 4:
			case 5:
				currentInformationBlock.currentEvent.quantifier = value;
				break;
				
			case 6:
				SupplementaryInfo si = TMC.SUPP_INFOS.get(value);
				if (si != null)
					currentInformationBlock.currentEvent.suppInfo.add(si);
				break;
				
			case 7:
				startTime = value;
				break;
				
			case 8:
				stopTime = value;
				break;
			
			case 9:		// additional event
				currentInformationBlock.addEvent(value);
				break;
				
			case 11:
				//addInformationBlock();
				// TODO the spec says we should maybe create a new information block
				// also, a 11 may be followed only by a 6 (sup info)
				currentInformationBlock.setDestination(value);
				break;
				
			case 10:
				currentInformationBlock.diversionRoute.add(value);
				break;
				
			case 13:
				if(currentInformationBlock.currentEvent != null) {
					currentInformationBlock.currentEvent.sourceLocation = value;
				}
				break;
				
			case 14:
				addInformationBlock();
				break;
				
			}
		}
		
		/**
		 * Used to indicate that this message is complete. 
		 */
		public void complete() {
			this.bidirectional = true;
			for(InformationBlock ib : informationBlocks) {
				for(Event e : ib.events) {
					this.bidirectional &= e.tmcEvent.bidirectional;
					if (urgency == null)
						urgency = e.urgency;
					else
						urgency = EventUrgency.max(urgency, e.urgency);
				}
			}
			
			if(reversedDirectionality) this.bidirectional = !this.bidirectional;
			
			if (increasedUrgency > 0)
				for (int i = 0; i < increasedUrgency; i++)
					urgency.next();
			else if (increasedUrgency < 0)
				for (int i = 0; i > increasedUrgency; i--)
					urgency.prev();
			
			/*
			 * TODO: what if we have a multi-event message with different duration types and no
			 * duration set explicitly? As per the spec, duration would be assumed to be 0, which
			 * means the event is expected to last for an unspecified time. Even then, persistence
			 * depends on duration type (15 mins vs. 1 hour) - which event's duration type should
			 * we use? (Nature is not relevant for persistence.)
			 */
			if (eventForDuration == null)
				eventForDuration = this.informationBlocks.get(0).events.get(0);
			
			if (reversedDurationType)
				eventForDuration.invertDurationType(); 
			
			this.complete = true;
		}
		
		/**
		 * @brief Whether this message overrides another message.
		 * 
		 * A message overrides another if the following are true:
		 * <ul>
		 * <li>Both messages refer to the same location, from the same location table for the same
		 * country</li>
		 * <li>The directions of both messages match</li>
		 * <li>At least one update class is shared by events from both messages</li>
		 * <li>For forecast messages, the forecast duration matches</li>
		 * <li>The new message is complete</li>
		 * </ul>
		 * 
		 * Special rules apply to the {@link #LOCATION_INDEPENDENT} location code, which matches
		 * any location code for the purpose of message updating. A regular (non-INTER-ROAD)
		 * message with this location code will replace all otherwise matching messages, including
		 * those referring to an INTER-ROAD location. An INTER-ROAD message with this location code
		 * will only replace messages referring to locations from the same location table and
		 * country, even if they are non-INTER-ROAD messages.
		 * 
		 * These checks are implemented here.
		 * 
		 * In addition to the above, both messages must either originate from the same TMC service
		 * (identified by CC, LTN and SID) or from two different TMC services which explicitly
		 * allow overriding each other's messages via variant 9 tuning information messages. This
		 * check is not implemented here; callers who receive messages from multiple services must
		 * implement this check separately.
		 * 
		 * @param m A previously received message
		 * 
		 * @return True if the message can be overridden (save for the aforementioned service constraints not being met), false if not
		 */
		public boolean overrides(Message m) {
			return
				complete &&
				hasLocationMatching(m) && 
				(direction == m.direction) &&
				hasAnEventFromTheSameUpdateClassAs(m) &&
				((!isForecastMessage() && !m.isForecastMessage()) || duration == m.duration);
				// is forecast message => same duration
		}
		
		/**
		 * As per TMC/Alert-C standard, "contains an event that belongs to the
		 * same update class as any event (a multi-group message may have more
		 * than one event) in the existing message)".
		 * 
		 * If the class for which this method is called has event 2047 (null message), this method
		 * will also return true. However, the opposite is not true: if the message passed as
		 * {@code m} has event 2047, it will be treated like any other event.
		 * 
		 * @param m The message to override in case of a match
		 * 
		 * @return
		 */
		private boolean hasAnEventFromTheSameUpdateClassAs(Message m) {
			for(InformationBlock ib : this.informationBlocks) {
				for(Event e : ib.events) {
					if (e.tmcEvent.code == 2047)
						return true;
					for(InformationBlock mib : m.informationBlocks) {
						for(Event me : mib.events) {
							if(me.tmcEvent.updateClass == e.tmcEvent.updateClass) {
								return true;
							}
						}
					}
				}
			}
			return false;
		}
		
		/**
		 * @brief Whether the location of this message matches that of another for the purpose of message updating
		 * 
		 * @param m The message to override in case of a match
		 * 
		 * @return
		 */
		private boolean hasLocationMatching(Message m) {
			if ((location == LOCATION_INDEPENDENT) && (!interroad))
					return true;
			/* 
			 * Messages received before we had full service info may have -1 as CC and/or LTN
			 * (FCC and FLTN only if they are not INTER-ROAD messages).
			 * We consider these equal to the main CC/LTN of the service.
			 */
			int mfcc = m.fcc;
			int mfltn = m.fltn;
			if (!interroad) {
				if (mfcc < 0)
					mfcc = fcc;
				if (mfltn < 0)
					mfltn = fltn;
			}
			return ((fcc == mfcc) && (fltn == mfltn) &&
					((location == m.location) || (location == LOCATION_INDEPENDENT)));
		}
		
		/**
		 * Contains an event from one of the forecast update classes.
		 * 
		 * @return
		 */
		private boolean isForecastMessage() {
			for(InformationBlock ib : informationBlocks) {
				for(Event e : ib.events) {
					int c = e.tmcEvent.updateClass;
					if(c >= 32 && c <= 39) return true;
				}
			}
			return false;
		}
		
		/**
		 * Tells whether it is a cancellation message.
		 * @return
		 */
		public boolean isCancellation() {
			if(this.informationBlocks.size() > 0) {
				InformationBlock ib1 = this.informationBlocks.get(0);
				if(ib1.events.size() > 0) {
					Event e1 = ib1.events.get(0);
					return e1.tmcEvent.isCancellation();
				}
			}
			
			return false; // if empty event
		}
		
		@Override
		public String toString() {
			if(! complete) {
				return "Incomplete!";
			}
			StringBuilder res = new StringBuilder("");
			if (locationInfo != null) {
				String tmp = this.getRoadNumber();
				if (tmp != null)
					res.append(tmp);
				String name = this.getDisplayName();
				if (name != null) {
					if (tmp != null)
						res.append(" ");
					else
						tmp = name;
					res.append(name);
				}
				TMCLocation secondary = locationInfo.getOffset(this.extent, this.direction);
				name = locationInfo.getDetailedDisplayName(secondary, "at %s", "between %s and %s");
				if (name != null) {
					if (tmp != null)
						res.append(", ");
					else
						tmp = name;
					res.append(name);
				}
				res.append("\n");
			}
			if (interroad)
				res.append("Inter-Road, ");
			res.append("CC: ").append(String.format("%X", fcc));
			res.append(", LTN: ").append(fltn);
			res.append(", Location: ").append(location);
			res.append(", extent=" + this.extent);
			res.append(", ").append(this.bidirectional ? "bi" : "mono").append("directional");
			res.append(", growth direction ").append(this.direction == 0 ? "+" : "-");
			res.append('\n');
			res.append("urgency=").append(urgency);
			res.append(", nature=").append(eventForDuration.nature);
			res.append(", durationType=").append(eventForDuration.durationType);
			res.append(", duration=" + this.duration);
			if(this.diversion) res.append(", diversion advised");
			if(startTime != -1) res.append(", start=").append(formatTime(startTime));
			if(stopTime != -1) res.append(", stop=").append(formatTime(stopTime));
			res.append('\n');
			res.append("received=").append(date);
			res.append(", expires=").append(this.getPersistence());
			res.append('\n');
			if (locationInfo != null) {
				// http://www.openstreetmap.org/?mlat=60.31092&mlon=25.03073#map=9/60.31092/25.03073&layers=Q
				// http://www.openstreetmap.org/directions?engine=mapquest_car&route=48.071%2C11.482%3B45.486%2C9.129
				float[] c = this.getCoordinates();
				if ((c != null) && (c.length >= 2)) {
					if (c.length >= 4)
						res.append("Link: http://www.openstreetmap.org/directions?engine=mapquest_car&route=" 
								+ c[3]
								+ "%2C" 
								+ c[2]
								+ "%3B" 
								+ c[1]
								+ "%2C" 
								+ c[0]
								+ "\n");
					else
						res.append("Link: http://www.openstreetmap.org/?mlat=" 
								+ c[1]
								+ "&mlon=" 
								+ c[0]
								+ "#map=9/" 
								+ c[1]
								+ "/" 
								+ c[0]
								+ "&layers=Q\n");
				}
			}
			for(InformationBlock ib : informationBlocks) {
				res.append(ib);
			}
			if (locationInfo != null) {
				res.append("-------------\n").append(locationInfo).append("\n");
				TMCLocation secondary = locationInfo.getOffset(this.extent, this.direction);
				if (secondary != locationInfo)
					res.append("-------------\nExtent:\n").append(secondary);
			}
			
			return res.toString();
		}
		
		public String html() {
			if(! complete) {
				return "<html><b>Incomplete!</b><html>";
			}
			StringBuilder res = new StringBuilder("<html>");
			if (locationInfo != null) {
				res.append("<b>");
				String tmp = this.getRoadNumber();
				if (tmp != null)
					res.append(tmp);
				String name = this.getDisplayName();
				if (name != null) {
					if (tmp != null)
						res.append(" ");
					else
						tmp = name;
					res.append(name);
				}
				TMCLocation secondary = locationInfo.getOffset(this.extent, this.direction);
				name = locationInfo.getDetailedDisplayName(secondary, "at %s", "between %s and %s");
				if (name != null) {
					if (tmp != null)
						res.append(", ");
					else
						tmp = name;
					res.append(name);
				}
				res.append("</b>");
				res.append("<br/>");
			}
			if (interroad)
				res.append("Inter-Road, ");
			res.append("CC: ").append(String.format("%X", fcc));
			res.append(", LTN: ").append(fltn);
			res.append(", Location: ").append(location);
			res.append(", extent=" + this.extent);
			res.append(", ").append(this.bidirectional ? "bi" : "mono").append("directional");
			res.append(", growth direction ").append(this.direction == 0 ? "+" : "-");
			res.append("<br/>");
			res.append("urgency=").append(urgency);
			res.append(", nature=").append(eventForDuration.nature);
			res.append(", durationType=").append(eventForDuration.durationType);
			res.append(", duration=" + this.duration);
			if(this.diversion) res.append(", diversion advised");
			if(startTime != -1) res.append("<br><font color='#330000'>start=").append(formatTime(startTime)).append("</font>");
			if(stopTime != -1) res.append("<br><font color='#003300'>stop=").append(formatTime(stopTime)).append("</font>");
			res.append("<br/>");
			res.append("received=").append(date);
			res.append(", expires=").append(this.getPersistence());
			res.append("<br>");
			if (locationInfo != null) {
				// http://www.openstreetmap.org/?mlat=60.31092&mlon=25.03073#map=9/60.31092/25.03073&layers=Q
				// http://www.openstreetmap.org/directions?engine=mapquest_car&route=48.071%2C11.482%3B45.486%2C9.129
				float[] c = this.getCoordinates();
				if ((c != null) && (c.length >= 2)) {
					if (c.length >= 4) {
						res.append("<a href=\"http://www.openstreetmap.org/directions?engine=mapquest_car&route=" 
								+ c[3]
								+ "%2C" 
								+ c[2]
								+ "%3B" 
								+ c[1]
								+ "%2C" 
								+ c[0]
								+ "\">");
						res.append("http://www.openstreetmap.org/directions?engine=mapquest_car&route=" 
								+ c[3]
								+ "%2C" 
								+ c[2]
								+ "%3B" 
								+ c[1]
								+ "%2C" 
								+ c[0]
								+ " ");
						res.append("</a>");
					} else {
						res.append("<a href=\"http://www.openstreetmap.org/?mlat=" 
								+ c[1]
								+ "&mlon=" 
								+ c[0]
								+ "#map=9/" 
								+ c[1]
								+ "/" 
								+ c[0]
								+ "&layers=Q\">");
						res.append("http://www.openstreetmap.org/?mlat=" 
								+ c[1]
								+ "&mlon=" 
								+ c[0]
								+ "#map=9/" 
								+ c[1]
								+ "/" 
								+ c[0]
								+ "&layers=Q");
						res.append("</a>");
					}
				}
			}
			for(InformationBlock ib : informationBlocks) {
				res.append(ib.html());
			}
			if (locationInfo != null) {
				res.append("<hr>").append(locationInfo.html());
				TMCLocation secondary = locationInfo.getOffset(this.extent, this.direction);
				if (secondary != locationInfo)
					res.append("<hr>Extent:<br>").append(secondary.html());
			}
			res.append("</html>");
			
			return res.toString();			
		}
		
		
		/**
		 * @brief Returns the name of the area surrounding the location.
		 * 
		 * For formatting of the area name, see
		 * {@link eu.jacquet80.rds.app.oda.tmc.TMCLocation#getAreaName()}.
		 * 
		 * @return The area name, or {@code null} if the location of the message could not be
		 * resolved.
		 */
		public String getAreaName() {
			if (locationInfo == null)
				return null;
			return locationInfo.getAreaName();
		}
		
		/**
		 * @brief Returns the auxiliary coordinates of the message location.
		 * 
		 * Auxiliary coordinates are needed in cases in which {@link #getCoordinates()} returns a
		 * two-element array, i.e. a single point which carries no direction information.
		 * 
		 * For all other types of locations, this function returns the same result as
		 * {@link #getCoordinates()}.
		 * 
		 * Apart from obtaining direction information, they can also be used to infer the road
		 * which is affected by the message. While the design of TMC considers the road to be
		 * unambiguously identified by the primary coordinates, along with the road name or number,
		 * matching these values to map material may be difficult in practice: due to precision
		 * constraints, TMC coordinates can never be expected to precisely match those on map data,
		 * road names may have alternate spellings (via Verdi vs. via Giuseppe Verdi vs. via Verdi
		 * Giuseppe) and even road numbers may differ (most notoriously, German location tables
		 * prefix district roads in Bavaria with the letter K, which is not part of their official
		 * number and thus likely to not match the numbers used in maps).
		 * 
		 * Auxiliary coordinates for single-point locations are generally obtained by "stretching"
		 * the location beyond that single point to its immediate neighbors. For example, if a
		 * segment contains points 1–2–3 (in that order) and an event is signalled for point 2,
		 * this function will return the coordinates of point 1 and 3. The order of the points
		 * depends on the direction of the event, similar to primary and secondary locations in a
		 * TMC message: the first pair of coordinates refers to a point which is ahead of the
		 * obstacle, the second pair refers to a point which is behind the end of the queue. In
		 * other words, drivers traveling through the location in the affected direction will first
		 * pass the second pair of coordinates, then the first.
		 * 
		 * If the location does not have neighbors in both directions, the location itself is used
		 * in place of any missing neighbors.
		 * 
		 * @return The auxiliary coordinates of the message location (see description).
		 */
		public float[] getAuxCoordinates() {
			if (auxCoords == null) {
				float[] c = getCoordinates();
				if ((c == null) || (c.length == 4))
					auxCoords = c;
				// TODO get coordinates
			}
			if ((auxCoords == null) || (auxCoords.length == 0))
				return null;
			else
				return auxCoords.clone();
		}
		
		
		/**
		 * @brief Returns the coordinates of the message location.
		 * 
		 * The coordinates can take the following form:
		 * <ul>
		 * <li>{@code null} if no coordinates can be established (e.g. location of type AREA or
		 * unknown location code)</li>
		 * <li>A two-element array, representing longitude and latitude, for single-point locations
		 * (location of type POINT and extent 0)</li>
		 * <li>A four-element array, representing longitude and latitude of the primary location,
		 * followed by those of the secondary location, for multi-point locations (location of type
		 * POINT and nonzero extent, or location of type SEGMENT or ROAD)</li>
		 * </ul>
		 * 
		 * If the message location is of type POINT, this function will simply return its
		 * coordinates and, if present, those of the secondary location.
		 * 
		 * Message locations of type SEGMENT or ROAD will be "translated" into a pair of points in
		 * the following way: For a SEGMENT with zero extent or a ROAD, its start and end points
		 * are determined. For a SEGMENT with a nonzero extent, the outer end point of each segment
		 * (pointing away from the other segment) is used. Then the coordinates of these two points
		 * are returned.
		 * 
		 * Directionality is always preserved: queue growth is from the first to the second pair of
		 * coordinates. In other words, drivers traveling through the location in the affected
		 * direction will first pass the second pair of coordinates, then the first.
		 * 
		 * @return The coordinates of the message location (see description).
		 */
		public float[] getCoordinates() {
			float[] c1, c2;
			// TODO do we need to deal with extent changes?
			if (locationInfo == null)
				return null;
			if (coords == null) {
				if (direction == 0)
					c1 = locationInfo.getFirstCoordinates();
				else
					c1 = locationInfo.getLastCoordinates();
				if ((c1 == null) || (c1.length < 2))
					coords = new float[] {};
				else {
					TMCLocation secondary = locationInfo.getOffset(this.extent, this.direction);
					if ((locationInfo.equals(secondary)) && (locationInfo instanceof TMCPoint))
						coords = c1;
					else {
						if (direction == 0)
							c2 = secondary.getLastCoordinates();
						else
							c2 = secondary.getFirstCoordinates();
						if ((c2 == null) || (c2.length < 2))
							coords = c1;
						else
							coords = new float[] {c1[0], c1[1], c2[0], c2[1]};
					}
				}
			}
			if ((coords == null) || (coords.length == 0))
				return null;
			else
				return coords.clone();
		}
		
		
		/**
		 * @brief Returns the direction of the message.
		 * 
		 * In most cases, the return value should be evaluated together with
		 * {@link #isBidirectional()}.
		 * 
		 * @return 0 for positive, 1 for negative
		 */
		public int getDirection() {
			return direction;
		}
		
		
		/**
		 * @brief Returns a name for the location which can be displayed to the user.
		 * 
		 * The display name, together with the road number (if any), identifies the location of the event.
		 * For formatting of the display name, see
		 * {@link eu.jacquet80.rds.app.oda.tmc.TMCLocation#getDisplayName(TMCLocation, int)}.
		 * 
		 * @return A user-friendly string describing the location of the event, or {@code null} if
		 * the location of the message could not be resolved.
		 */
		public String getDisplayName() {
			if (locationInfo == null)
				return null;
			TMCLocation secondary = locationInfo.getOffset(this.extent, this.direction);
			return locationInfo.getDisplayName(secondary, this.direction, this.bidirectional);
		}
		
		/**
		 * @brief Returns the PID country code for this event (single hex digit).
		 * 
		 * This is the country code of the service which sent this event. For an INTER-ROAD
		 * message, the country code of the location may differ and can be retrieved using
		 * {@link #getForeignCountryCode()}.
		 * 
		 * @return the country code, or -1 if unknown
		 */
		public int getCountryCode() {
			return cc;
		}
		
		/**
		 * @brief Returns the country code for the location of this event (single hex digit).
		 * 
		 * For an INTER-ROAD message, the country code of the service which sent this event may
		 * differ and can be retrieved using {@link #getCountryCode()}.
		 * 
		 * @return the country code, or -1 if unknown
		 */
		public int getForeignCountryCode() {
			return fcc;
		}
		
		/**
		 * @brief Returns the time until which receivers should store the message.
		 * 
		 * Persistence is determined by three factors: the date and time at which the message was
		 * last received, its duration type.
		 * 
		 * @return The date and time at which the message expires.
		 */
		public Date getPersistence() {
			/* 15 min, 30 min, 1 h, 2 h, 3 h, 4 h and 24 h in milliseconds, respectively */
			long MS_15_MIN  = 900000;
			long MS_30_MIN = 1800000;
			long MS_1_H    = 3600000;
			long MS_2_H    = 7200000;
			long MS_3_H   = 10800000;
			long MS_4_H   = 14400000;
			long MS_24_H =  86400000;
			
			/* Calculate midnight (end of current day) in current time zone */
			Calendar cal = new GregorianCalendar(this.timeZone);
			cal.setTime(this.date);
			cal.setLenient(true);
			cal.set(Calendar.HOUR_OF_DAY, 24);
			cal.set(Calendar.MINUTE, 0);
			cal.set(Calendar.SECOND, 0);
			cal.set(Calendar.MILLISECOND, 0);
			Date midnight = cal.getTime();
			
			switch (duration) {
			case 0:
				if (eventForDuration.durationType == EventDurationType.DYNAMIC)
					return new Date(date.getTime() + MS_15_MIN);
				else
					return new Date(date.getTime() + MS_1_H);
			case 1:
				if (eventForDuration.durationType == EventDurationType.DYNAMIC)
					return new Date(date.getTime() + MS_15_MIN);
				else
					return new Date(date.getTime() + MS_2_H);
			case 2:
				if (eventForDuration.durationType == EventDurationType.DYNAMIC)
					return new Date(date.getTime() + MS_30_MIN);
				else
					return new Date(midnight.getTime());
			case 3:
				if (eventForDuration.durationType == EventDurationType.DYNAMIC)
					return new Date(date.getTime() + MS_1_H);
				else
					return new Date(midnight.getTime() + MS_24_H);
			case 4:
				if (eventForDuration.durationType == EventDurationType.DYNAMIC)
					return new Date(date.getTime() + MS_2_H);
				else
					return new Date(midnight.getTime() + MS_24_H);
			case 5:
				if (eventForDuration.durationType == EventDurationType.DYNAMIC)
					return new Date(date.getTime() + MS_3_H);
				else
					return new Date(midnight.getTime() + MS_24_H);
			case 6:
				if (eventForDuration.durationType == EventDurationType.DYNAMIC)
					return new Date(date.getTime() + MS_4_H);
				else
					return new Date(midnight.getTime() + MS_24_H);
			case 7:
				if (eventForDuration.durationType == EventDurationType.DYNAMIC)
					return new Date(midnight.getTime());
				else
					return new Date(midnight.getTime() + MS_24_H);
			}
			return null;
		}
		
		/**
		 * @brief Returns the junction number of the primary location, if any.
		 * 
		 * @return The junction number, or {@code null} if the primary location is not a
		 * {@link eu.jacquet80.rds.app.oda.tmc.TMCPoint}, has no junction number or could not be
		 * resolved.
		 */
		public String getPrimaryJunctionNumber() {
			if (locationInfo == null)
				return null;
			if (!(locationInfo instanceof TMCPoint))
				return null;
			TMCPoint loc = (TMCPoint) locationInfo;
			if ((loc.junctionNumber != null) && !loc.junctionNumber.isEmpty())
				return loc.junctionNumber;
			else
				return null;
		}
		
		/**
		 * @brief Returns the name of the primary location, if any.
		 * 
		 * @return The name of the primary location, or {@code null} if the primary location is not
		 * a {@link eu.jacquet80.rds.app.oda.tmc.TMCPoint}, has no name or could not be resolved.
		 */
		public String getPrimaryName() {
			if (locationInfo == null)
				return null;
			if (!(locationInfo instanceof TMCPoint))
				return null;
			if ((locationInfo.name1.name == null) || (locationInfo.name1.name.isEmpty()))
				return null;
			else
				return locationInfo.name1.name;
		}
		
		/**
		 * @brief Returns the road number for the message, if any.
		 * 
		 * @return The road number, or {@code null} if the message does not have a corresponding road number,
		 * or if the location of the message could not be resolved.
		 */
		public String getRoadNumber() {
			if (locationInfo == null)
				return null;
			return locationInfo.getRoadNumber();
		}
		
		/**
		 * @brief Returns the junction number of the secondary location, if any.
		 * 
		 * @return The junction number, or {@code null} if the message has no secondary location,
		 * the secondary location is not a {@link eu.jacquet80.rds.app.oda.tmc.TMCPoint}, has no
		 * junction number or could not be resolved.
		 */
		public String getSecondaryJunctionNumber() {
			if (locationInfo == null)
				return null;
			TMCLocation secondary = locationInfo.getOffset(this.extent, this.direction);
			if ((secondary == null) || (locationInfo.equals(secondary)))
				return null;
			if (!(secondary instanceof TMCPoint))
				return null;
			TMCPoint loc = (TMCPoint) secondary;
			if ((loc.junctionNumber != null) && !loc.junctionNumber.isEmpty())
				return loc.junctionNumber;
			else
				return null;
		}
		
		/**
		 * @brief Returns the name of the secondary location, if any.
		 * 
		 * @return The name of the secondary location, or {@code null} if the message has no secondary
		 * location, the secondary location is not a {@link eu.jacquet80.rds.app.oda.tmc.TMCPoint},
		 * has no name or could not be resolved.
		 */
		public String getSecondaryName() {
			if (locationInfo == null)
				return null;
			TMCLocation secondary = locationInfo.getOffset(this.extent, this.direction);
			if ((secondary == null) || (locationInfo.equals(secondary)))
				return null;
			if (!(secondary instanceof TMCPoint))
				return null;
			if ((secondary.name1.name == null) || (secondary.name1.name.isEmpty()))
				return null;
			else
				return secondary.name1.name;
		}
		
		/**
		 * @brief Returns a short display name for the location.
		 * 
		 * The short display name is intended for use in list views. It identifies the approximate
		 * location of the event. It can take one of the following forms (the first non-empty item
		 * is returned):
		 * <ol>
		 * <li>Road number</li>
		 * <li>Area name (for roads without a number)</li>
		 * <li>The value returned by {@link #getDisplayName()}</li>
		 * </ol>
		 * 
		 * @return The short display name, or {@code null} if the location of the message could not
		 * be resolved.
		 */
		public String getShortDisplayName() {
			if (locationInfo == null)
				return null;
			String ret = locationInfo.getRoadNumber();
			if (ret == null)
				ret = locationInfo.getAreaName();
			if (ret == null)
				ret = getDisplayName();
			return ret;
		}
		
		public int getSid() {
			return sid;
		}
		
		public int getLocation() {
			return location;
		}
		
		/**
		 * @brief Returns the Location Table Number (LTN) for the location of this event.
		 * 
		 * This is the LTN of the location table in which the location is defined. For an
		 * INTER-ROAD message, the LTN of the service which sent this event may differ and can be
		 * retrieved using {@link #getLocationTableNumber()}.
		 */
		public int getForeignLocationTableNumber() {
			return fltn;
		}
		
		/**
		 * @brief Returns the Location Table Number (LTN) for this event.
		 * 
		 * This is the LTN of the service which sent this event. For an INTER-ROAD message, the
		 * location may refer to a different location table, which can be retrieved using
		 * {@link #getForeignLocationTableNumber()}.
		 */
		public int getLocationTableNumber() {
			return ltn;
		}
		
		/**
		 * @brief Returns a list of all {@link InformationBlock}s in this message.
		 */
		public List<InformationBlock> getInformationBlocks() {
			List<InformationBlock> result = new ArrayList<InformationBlock>();
			
			for(InformationBlock ib : informationBlocks)
				result.add(ib);
			
			return result;
		}

		public List<Integer> getEvents() {
			List<Integer> result = new ArrayList<Integer>();
			
			for(InformationBlock ib : informationBlocks) {
				for(Event e : ib.events) {
					result.add(e.tmcEvent.code);
				}
			}
			
			return result;
		}
		
		/**
		 * @brief Returns the date and time at which this message was received.
		 */
		public Date getTimestamp() {
			return (Date) date.clone();
		}
		
		public int getUpdateCount() {
			return updateCount;
		}
		
		/**
		 * @brief Whether the message has a secondary location.
		 * 
		 * If {@code checkValidity} is false, this method will simply look at the extent and return
		 * true if the extent is greater than zero, false otherwise.
		 * 
		 * If {@code checkValidity} is true, this method will try to resolve the secondary location
		 * and return true only if the combination of primary location, direction and extent
		 * resolve to a valid location which is different from the primary location. It will also
		 * return false if no location table is present to resolve the primary location.
		 * 
		 * @param checkValidity
		 * 
		 * @return
		 */
		public boolean hasSecondaryLocation(boolean checkValidity) {
			if (extent <= 0)
				return false;
			if (!checkValidity)
				return true;
			TMCLocation secondary = locationInfo.getOffset(this.extent, this.direction);
			if ((secondary == null) || (locationInfo.equals(secondary)))
				return false;
			return true;
		}
		
		/**
		 * @brief Returns whether the message affects both directions.
		 * 
		 * To get the direction, call {@link #getDirection()}.
		 */
		public boolean isBidirectional() {
			return bidirectional;
		}
		
		/**
		 * @brief Returns whether a diversion route is available.
		 */
		public boolean isDiversionAvailable() {
			return diversion;
		}
		
		public void setUpdateCount(int newCount) {
			updateCount = newCount;
		}
		
		/**
		 * @brief Returns whether message information has been fully resolved.
		 * 
		 * This method currently checks for a valid CC and LTN and a fully resolved location.
		 */
		public boolean isFullyResolved() {
			return ((cc >= 0) && (ltn >= 0) && (sid >= 0)
					&& ((locationInfo != null) || (location >= LOCATION_ALL_LISTENERS)));
		}
		
		/**
		 * @brief Sets the LocationTable Number (LTN) and Service Identifier (SID).
		 * 
		 * This sets the LTN and SID for the originating service. This method is intended for
		 * situations in which the LTN and/or SID have not yet been received since the last station
		 * change, but cached values are available.
		 * 
		 * Attempting to change a valid LTN or SID to a different value will result in an
		 * {@link IllegalArgumentException}. Changing an LTN or SID to the same value is a no-op.
		 * 
		 * @param ltn The Location Table Number (LTN)
		 * @param sid The Service Identifier (SID)
		 */
		public void setService(int ltn, int sid) {
			if ((this.ltn == -1 || this.ltn == ltn)
					&& (this.sid == -1 || this.sid == sid)) {
				this.ltn = ltn;
				if (!interroad)
					this.fltn = ltn;
				this.sid = sid;
			} else
				throw new IllegalArgumentException("LTN and SID cannot be changed after being set");
		}

		private void setLocation(int location) {
			this.location = location;
			this.locationInfo = TMC.getLocation(String.format("%X", fcc), fltn, location);
		}
	}
	
	/* Table derived from work done by Tobias Lorenz,
	 * see http://sourceforge.net/projects/radiodatasystem/
	 * Used with permission.
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

	
	public static class InformationBlock {
		/** The enclosing TMC message */
		private Message message;
		
		private int length = -1;
		private int speed = -1;
		private int destination = -1;
		private TMCLocation destinationInfo = null;

		private final List<Event> events = new ArrayList<Event>();
		private Event currentEvent;
		
		private final List<Integer> diversionRoute = new ArrayList<Integer>();

		public InformationBlock(Message message, int eventCode) {
			this.message = message;
			addEvent(eventCode);
		}
		
		public InformationBlock(Message message) {
			this.message = message;
			this.currentEvent = null;
		}
		
		/**
		 * @brief Returns the destination information.
		 * 
		 * @return The location for the destination, or null if no destination is specified or if
		 * the location code cannot be resolved.
		 */
		public TMCLocation getDestination() {
			return destinationInfo;
		}
		
		/**
		 * @brief Returns the locations which make up the diversion route.
		 * 
		 * @return A list of locations. An empty list is returned if no diversion is specified or
		 * if one or more location codes cannot be resolved.
		 */
		public List<TMCLocation> getDiversion() {
			List<TMCLocation> res = new LinkedList<TMCLocation>();
			for (int lcid : diversionRoute) {
				TMCLocation location = TMC.getLocation(String.format("%X", message.fcc), message.fltn, lcid);
				if (location == null)
					return new LinkedList<TMCLocation>();
				res.add(location);
			}
			return res;
		}

		/**
		 * @brief Returns a list of all events in this information block.
		 */
		public List<Event> getEvents() {
			List<Event> result = new ArrayList<Event>();
			
			for(Event e : events) {
				result.add(e);
			}
			
			return result;
		}
		
		/**
		 * @brief Returns the length of the route affected.
		 * 
		 * @return The length of the route affected in km, -1 if unknown. Return values greater
		 * than 100 are to be interpreted as "100 km or more" rather than literally.
		 */
		public int getLength() {
			return length;
		}
		
		/**
		 * @brief Returns the speed limit.
		 * 
		 * @return The speed limit in km/h, or -1 if unknown.
		 */
		public int getSpeed() {
			return speed;
		}
		
		private void addEvent(int eventCode) {
			this.currentEvent = new Event(eventCode);
			events.add(this.currentEvent);		
		}
		
		/**
		 * @param destination the destination to set
		 */
		private void setDestination(int destination) {
			this.destination = destination;
			this.destinationInfo = TMC.getLocation(String.format("%X", message.fcc), message.fltn, destination);
		}

		@Override
		public String toString() {
			StringBuilder res = new StringBuilder();
			res.append("-------------\n");
			if (destination != -1) {
				res.append("For destination: " + destination + "  ");
				TMCLocation location = getDestination();
				if (location != null)
					res.append(location).append("\n");
			}
			
			if(length != -1) {
				String lengthKM;
				if(length > 100) lengthKM = "> 100 km";
				else lengthKM = " = " + length + " km";				
				res.append("length ").append(lengthKM).append("  ");
			}
			
			if(speed != -1) res.append("speed limit = ").append(speed).append(" km/h");
			if(length != -1 || speed != -1 || destination != -1) res.append('\n');
			for(Event e : events) {
				res.append(e).append('\n');
			}
			
			if(diversionRoute.size() > 0) res.append("Diversion route: " + diversionRoute).append('\n');
			if (diversionRoute.size() > 0) {
				res.append("Diversion route: " + diversionRoute).append("\n");
				for (int lcid : diversionRoute) {
					res.append("#").append(lcid);
					TMCLocation location = TMC.getLocation(String.format("%X", message.fcc), message.fltn, lcid);
					if (location != null)
						res.append(location).append("\n");
				}
			}
			
			return res.toString();
		}
		
		public String html() {
			StringBuilder res = new StringBuilder();
			res.append("<hr>");
			if (destination != -1) {
				res.append("For destination: " + destination + "  ");
				TMCLocation location = getDestination();
				if (location != null)
					res.append("<blockquote>").append(location.html()).append("</blockquote>");
			}
			
			if(length != -1) {
				String lengthKM;
				if(length > 100) lengthKM = "> 100 km";
				else lengthKM = " = " + length + " km";				
				res.append("length ").append(lengthKM).append("  ");
			}
			
			if(speed != -1) res.append("speed limit = ").append(speed).append(" km/h");
			if(length != -1 || speed != -1 || destination != -1) res.append("<br>");
			for(Event e : events) {
				res.append(e.html()).append("<br>");
			}
			
			if (diversionRoute.size() > 0) {
				res.append("Diversion route: " + diversionRoute).append("<br><ul>");
				for (int lcid : diversionRoute) {
					res.append("<li>").append(lcid);
					TMCLocation location = TMC.getLocation(String.format("%X", message.fcc), message.fltn, lcid);
					if (location != null)
						res.append("<blockquote>").append(location.html()).append("</blockquote>");
				}
				res.append("</ul>");
			}
			
			return res.toString();			
		}
	}
	
	public static class Event {
		private TMCEvent tmcEvent;
		private EventUrgency urgency;
		private EventNature nature;
		private EventDurationType durationType;
		private int sourceLocation = -1;

		private int quantifier = -1;
		private List<SupplementaryInfo> suppInfo = new ArrayList<SupplementaryInfo>();

		public Event(int eventCode) {
			this.tmcEvent = TMC.getEvent(eventCode);
			
			if(this.tmcEvent== null) {
				System.err.println("No such TMC event: " + eventCode);
			}
			
			this.urgency = this.tmcEvent.urgency;
			this.nature = this.tmcEvent.nature;
			this.durationType = this.tmcEvent.durationType;
		}
		

		/**
		 * @brief Returns the supplementary information phrases associated with this event.
		 */
		public List<SupplementaryInfo> getSupplementaryInfo() {
			List<SupplementaryInfo> result = new ArrayList<SupplementaryInfo>();
			
			for(SupplementaryInfo si : suppInfo)
				result.add(si);
			
			return result;
		}


		/**
		 * @brief Generates a formatted event description.
		 * 
		 * If the event has a quantifier, this method returns the quantifier string for the event
		 * with the quantifier parsed and inserted. Otherwise, the generic description is returned.
		 */
		public String getText() {
			String text;
			if(quantifier != -1) {
				text = tmcEvent.textQ.replace("$Q", tmcEvent.formatQuantifier(quantifier));
			} else {
				text = tmcEvent.text;
			}
			return text;
		}


		@Override
		public String toString() {
			String text = getText();
			StringBuffer res = new StringBuffer("[").append(tmcEvent.code).append("] ").append(text);
			res.append(", urgency=").append(urgency);
			res.append(", nature=").append(nature);
			res.append(", durationType=").append(durationType);
			if(this.sourceLocation != -1) res.append(", source problem at ").append(this.sourceLocation);
			if(this.quantifier != -1) res.append(" (Q=").append(quantifier).append(')');
			if(this.suppInfo.size() > 0) res.append("\nSupplementary information: ").append(this.suppInfo);
			return res.toString();
		}
		
		public String html() {
			String text = getText();
			StringBuffer res = new StringBuffer("[").append(tmcEvent.code).append("] <b>").append(text).append("</b/>");
			res.append(", urgency=").append(urgency);
			res.append(", nature=").append(nature);
			res.append(", durationType=").append(durationType);
			if(this.sourceLocation != -1) res.append(", source problem at ").append(this.sourceLocation);
			if(this.suppInfo.size() > 0) {
				res.append("<font color='#555555'><br>Supplementary information: <br>");
				for(SupplementaryInfo si : suppInfo) {
					res.append("- " + si  +"<br>");
				}
				res.append("</font>");
			}
			return res.toString();
		}


		private void invertDurationType() {
			if (this.durationType == EventDurationType.DYNAMIC)
				this.durationType = EventDurationType.LONGER_LASTING;
			else
				this.durationType = EventDurationType.DYNAMIC;
		}
	}
	
	/**
	 * @brief The default comparator for sorting TMC messages.
	 * 
	 * This comparator compares events, using the following items of information in the order
	 * shown, until a difference is found:
	 * <ol>
	 * <li>Road numbers</li>
	 * <li>Area names</li>
	 * <li>Location IDs</li>
	 * <li>Extents</li>
	 * </ol>
	 * 
	 * Road numbers and area names are sorted lexicographically. Null values are placed at the end,
	 * two null values are considered equal (causing the next items in the above list to be
	 * examined). Location IDs and extents are sorted numerically. 
	 */
	public static class DefaultComparator implements Comparator<Message> {
		@Override
		public int compare(Message lhs, Message rhs) {
			int res = 0;
			/* First compare by a road numbers (if only one location has a road number, it is first) */
			String lrn = lhs.getRoadNumber();
			String rrn = rhs.getRoadNumber();
			if ((lrn != null) && (rrn != null)) {
				res = lrn.compareTo(rrn);
				if (res != 0)
					return res;
			} else if (lrn != null)
				return -1;
			else if (rrn != null)
				return 1;
			
			/* Then compare by area names (if only one location has an area name, it is first) */
			String lan = lhs.getAreaName();
			String ran = rhs.getAreaName();
			if ((lan != null) && (ran != null)) {
				res = lan.compareTo(ran);
				if (res != 0)
					return res;
			} else if (lan != null)
				return -1;
			else if (ran != null)
				return 1;
			
			/* Then compare by primary location codes */
			res = lhs.location - rhs.location;
			if (res != 0)
				return res;
			
			/* Finally compare by extent */
			return lhs.extent - rhs.extent;
		}
	}
}