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

package eu.jacquet80.rds.app;

import java.io.PrintStream;
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
	
	private final DateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");

	public Paging(TunedStation station, PrintStream console) {
		setStation(station);
		setConsole(console);
	}

	@Override
	public String getName() {
		return "Paging";
	}

	@Override
	public void receiveGroup(int type, int version, int[] blocks, boolean[] blocksOk, int bitTime) {
		Message newMessage = null;
		String time = "-";
		if(station != null) {
			Date d = station.getDateForBitTime(bitTime);
			if(d != null) time = timeFormat.format(d);
		}

		synchronized(this) {

			// Groups 7A used for RP
			if(type == 7 && version == 0 && blocksOk[2] && blocksOk[3]) {
				String addrStr = "addr=" + 
				decodeBCD(blocks[2], 3) + decodeBCD(blocks[2], 2) + "/" +
				decodeBCD(blocks[2], 1) + "" + decodeBCD(blocks[2], 0) + "" + decodeBCD(blocks[3], 3) + "" + decodeBCD(blocks[3], 2);

				console.print("RP: flag=" + (char)('A' + ((blocks[1]>>5) & 1)) + ", ");

				if((blocks[1] & 0x8) == 8) {
					int idx = blocks[1] & 0x7;
					if(idx == 0) {
						// address of an alpha message
						console.print("Alpha message: " + addrStr);

						newMessage = currentMessage = new Message(time, addrStr, MessageType.ALPHA);
						lastIdx = 0;
					} else {
						if(currentMessage == null || currentMessage.getType() != MessageType.ALPHA) {
							// part of an alpha message with missed address
							newMessage = currentMessage = new Message(time, null, MessageType.ALPHA);
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
						String msg = Integer.toString(decodeBCD(blocks[3], 1)) + Integer.toString(decodeBCD(blocks[3], 0));
						console.print(addrStr +
								", msg=" + msg + "...");

						newMessage = currentMessage = new Message(time, addrStr, MessageType.NUMERIC_10);
						currentMessage.addText(msg);
					} else {
						String msg = decodeBCDWord(blocks[2]) + decodeBCDWord(blocks[3]);
						console.print("msg=..." + msg);

						if(currentMessage == null || currentMessage.getType() != MessageType.NUMERIC_10) {
							newMessage = currentMessage = new Message(time, null, MessageType.NUMERIC_10);
							currentMessage.addText("...");
						}

						currentMessage.addText(msg);
					}
				}
				if((blocks[1] & 0xF) == 1) console.print("Part of func");
				if((blocks[1] & 0xF) == 0) {
					console.print("Beep: " + addrStr);
					newMessage = currentMessage = new Message(time, addrStr, MessageType.BEEP);
				}


				// update the list of messages
				if(newMessage != null) messages.addLast(newMessage);    // add the new message
				if(currentMessage.isComplete()) currentMessage = null;  // a completed message is no longer current
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

		public Message(String time, String addr, MessageType type) {
			this.time = time;
			this.addr = addr;
			this.type = type;
			mesg = "";
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

}
