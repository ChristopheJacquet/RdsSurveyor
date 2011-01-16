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

package eu.jacquet80.rds.app;

import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import eu.jacquet80.rds.core.RDS;
import eu.jacquet80.rds.core.TunedStation;

public class Paging extends Application {
	private LinkedList<Message> messages = new LinkedList<Message>();
	private Message currentMessage = null;
	private int lastIdx;
	
	private int lastB1 = -1;
	private int nextInterval = -1;
	private int currentInterval = -1;
	private int intervalBitNumber = -1;
	private int intervalResult = Integer.MIN_VALUE;
	
	private String tngd;
	
	private final DateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");

	public Paging(TunedStation station, String tngd) {
		setStation(station);
		this.tngd = tngd;
	}

	@Override
	public String getName() {
		return "Paging";
	}

	@Override
	public void receiveGroup(PrintWriter console, int type, int version, int[] blocks, boolean[] blocksOk, int bitTime) {
		Message newMessage = null;
		String time = "-";
		if(station != null) {
			Date d = station.getDateForBitTime(bitTime);
			if(d != null) time = timeFormat.format(d);
		}

		synchronized(this) {

			// Groups 7A used for RP
			if(type == 7 && version == 0 && blocksOk[2] && blocksOk[3]) {
				String addrStr = "" +
					decodeBCD(blocks[2], 3) + decodeBCD(blocks[2], 2) + "/" +
					decodeBCD(blocks[2], 1) + "" + decodeBCD(blocks[2], 0) + "" + decodeBCD(blocks[3], 3) + "" + decodeBCD(blocks[3], 2);

				int ab = (blocks[1]>>4) & 1;
				console.print("RP: flag=" + (char)('A' + ab) + ", ");
				if(currentMessage != null && currentMessage.getAB() != ab) {
					currentMessage.setComplete();
					currentMessage = null;
				}

				if((blocks[1] & 0x8) == 8) {
					int idx = blocks[1] & 0x7;
					if(idx == 0) {
						// address of an alpha message
						console.print("Alpha message: " + addrStr);

						newMessage = currentMessage = new Message(time, addrStr, MessageType.ALPHA, ab);
						lastIdx = 0;
					} else {
						if(currentMessage == null || currentMessage.getType() != MessageType.ALPHA) {
							// part of an alpha message with missed address
							newMessage = currentMessage = new Message(time, null, MessageType.ALPHA, ab);
							lastIdx = 0;
						}

						if(idx != 7) {  // check for missed parts only if the current part is not the last one
							lastIdx++; if(lastIdx>6) lastIdx=1;
							// missed parts? if yes, fill with "????"
							while(lastIdx != idx) {
								currentMessage.addText("????");
								lastIdx++; if(lastIdx>6) lastIdx=1;
							}
						}

						console.print("Alpha message: msg[" + idx + "]=\"");
						for(int i=2; i<=3; i++) {
							String part = RDS.toChar((blocks[i]>>8) & 0xFF) + "" + RDS.toChar(blocks[i] & 0xFF);
							console.print(part);
							currentMessage.addText(part);

						}
						console.print("\"");

						if(idx == 7) currentMessage.setComplete();
					}

				}
				if((blocks[1] & 0xC) == 4) console.print("18/15-digit numeric msg: " + addrStr);
				if((blocks[1] & 0xE) == 2) {
					console.print("10-digit msg " + (1+(blocks[1] & 1)) + "/2: ");
					if((blocks[1] & 1) == 0) {
						String msg = Character.toString(decodeBCD(blocks[3], 1)) + Character.toString(decodeBCD(blocks[3], 0));
						console.print(addrStr +
								", msg=" + msg + "...");

						newMessage = currentMessage = new Message(time, addrStr, MessageType.NUMERIC_10, ab);
						currentMessage.addText(msg);
					} else {
						String msg = decodeBCDWord(blocks[2]) + decodeBCDWord(blocks[3]);
						console.print("msg=..." + msg);

						if(currentMessage == null || currentMessage.getType() != MessageType.NUMERIC_10) {
							newMessage = currentMessage = new Message(time, null, MessageType.NUMERIC_10, ab);
							currentMessage.addText("...");
						}

						currentMessage.addText(msg);
					}
				}
				if((blocks[1] & 0xF) == 1) console.print("Part of func");
				if((blocks[1] & 0xF) == 0) {
					console.print("Beep: " + addrStr);
					newMessage = currentMessage = new Message(time, addrStr, MessageType.BEEP, ab);
				}


				// update the list of messages
				if(newMessage != null) messages.addLast(newMessage);    // add the new message
				if(currentMessage != null && currentMessage.isComplete()) currentMessage = null;  // a completed message is no longer current
			}

			fireChangeListeners();
		}


	}

	public List<Message> getMessages() {
		return messages;
	}

	private static char decodeBCD(int bcd, int pos) {
		int val =  ((bcd>>(pos*4)) & 0xF);
		if(val < 10) return (char)('0' + val);
		else if(val == 10) return ' ';
		else return '!';
	}

	private static String decodeBCDWord(int word) {
		String res = "";
		for(int i=3; i>=0; i--) res += decodeBCD(word, i);
		return res;
	}

	public static class Message {
		private final String addr;
		private final MessageType type;
		private String mesg;
		private boolean complete = false;
		private final String time;
		private final int ab;

		public Message(String time, String addr, MessageType type, int ab) {
			this.time = time;
			this.addr = addr;
			this.type = type;
			this.ab = ab;
			mesg = "";
		}

		public int getAB() {
			return ab;
		}

		public void addText(String text) {
			this.mesg += text;
		}

		public void setComplete() {
			this.complete = true;
		}

		public boolean isComplete() {
			return complete;
		}

		public MessageType getType() {
			return type;
		}

		public String getAddress() {
			return addr == null ? "Unknown" : addr;
		}

		public String getContents() {
			return mesg;
		}
		
		public String getTime() {
			return time;
		}
	}

	public static enum MessageType {
		ALPHA("alpha"), NUMERIC_10("10-digit"), BEEP("beep");

		private MessageType(String caption) {
			this.caption = caption;
		}

		public String toString() {
			return caption;
		}

		private final String caption;
	}

	public synchronized String syncInfo(int b1, int b0) {
		int oldIntervalResult = intervalResult;
		
		String res = "Unexpected termination (#" + intervalBitNumber + ", b1=" + b1 + ", b0=" + b0 + ")";
		
		if(lastB1 == 0  && b1 == 1 && b0 == 0) {
			res = "SOI (#1)";
			if(nextInterval != -1) {
				res += " Int?=" + nextInterval;
				intervalResult = -1-nextInterval;
			} else intervalResult = Integer.MIN_VALUE;
			currentInterval = 0;
			intervalBitNumber = 2;
		}
		
		else if(intervalBitNumber == 2 && b1 == 0) {
			intervalResult = Integer.MIN_VALUE;
			fireChangeListeners();
			return "Err, B1 should be 1 for 1A #2 in interval";
		}
		
		else if(intervalBitNumber > 2 && b1 == 1) {
			intervalResult = Integer.MIN_VALUE;
			fireChangeListeners();
			return "Err, B1 should be 0 for 1A #" + intervalBitNumber + " in interval";
		}
		
		else if(intervalBitNumber >= 2 && intervalBitNumber <= 5) {
			currentInterval = (currentInterval << 1) | b0;
			res = "b0=" + b0 + " (#" + intervalBitNumber + ")";
			intervalBitNumber++;
		}
		
		else if(intervalBitNumber > 5) {
			res = "#" + intervalBitNumber + ", Int=" + currentInterval;
			nextInterval = (currentInterval + 1) % 10;
			intervalResult = currentInterval;
			intervalBitNumber++;
		}
		
		lastB1 = b1;
		
		if(oldIntervalResult != intervalResult) fireChangeListeners();
		
		return res;
	}
	
	public synchronized String fullMinute() {
		intervalResult = nextInterval = 0; 
		currentInterval = 0;
		intervalBitNumber = 2;
		lastB1 = 1;
		fireChangeListeners();
		return "SOI (#1) Int=" + nextInterval;
	}

	public synchronized int getInterval() {
		return intervalResult;
	}

	public void setTNGD(String tngd) {
		this.tngd = tngd;
	}
	
	public String getTNGD() {
		return tngd;
	}
}
