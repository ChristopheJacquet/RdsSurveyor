package eu.jacquet80.rds.app;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import eu.jacquet80.rds.core.RDS;

public class InHouse extends Application {
	private final List<Message> messages = new ArrayList<Message>();
	private final PrintStream console;
	
	public InHouse(PrintStream console) {
		this.console = console;
	}
	
	@Override
	public String getName() {
		return "IH";
	}

	@Override
	public void receiveGroup(int type, int version, int[] blocks,
			boolean[] blocksOk, int bitTime) {
		if(blocksOk[1] && blocksOk[2] && blocksOk[3]) {
			Message m = new Message(blocks[1] & 0x1F, blocks[2], blocks[3]);
			synchronized(this) {
				messages.add(m);
			}
			console.print(m.getDump());
		}
	}
	
	public synchronized int getMessageCount() {
		return messages.size();
	}
	
	public synchronized String getMessage(int index) {
		return messages.get(index).getDump();
	}

	public static class Message {
		private final int w1, w2, w3;
		private final String contents;
		
		public Message(int w1, int w2, int w3) {
			this.w1 = w1;
			this.w2 = w2;
			this.w3 = w3;
			
			contents = 
				Character.toString(character((w2 >> 8) & 0xFF)) +
				Character.toString(character(w2 & 0xFF)) +
				Character.toString(character((w3 >> 8) & 0xFF)) +
				Character.toString(character(w3 & 0xFF));
		}
		
		private char character(int v) {
			if(v >= 32 && v<=255) return RDS.toChar(v); else return '.';
		}
		
		public String getDump() {
			return String.format("%02X/%04X-%04X", w1, w2, w3) + " (" + contents + ")";
		}
	}
}