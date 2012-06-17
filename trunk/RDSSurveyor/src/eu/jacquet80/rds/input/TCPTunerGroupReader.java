/*
 RDS Surveyor -- RDS decoder, analyzer and monitor tool and library.
 For more information see
   http://www.jacquet80.eu/
   http://rds-surveyor.sourceforge.net/
 
 Copyright (c) 2009-2012 Christophe Jacquet

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


package eu.jacquet80.rds.input;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.LinkedList;

import eu.jacquet80.rds.input.group.FrequencyChangeEvent;
import eu.jacquet80.rds.input.group.GroupEvent;
import eu.jacquet80.rds.input.group.GroupReaderEvent;
import eu.jacquet80.rds.log.RealTime;

public class TCPTunerGroupReader extends TunerGroupReader {
	private String name = "";
	private final BufferedReader reader;
	private final PrintWriter writer;
	private final LinkedList<GroupReaderEvent> groups = new LinkedList<GroupReaderEvent>();
	private boolean newGroups = false;
	private int freq;
	
	public TCPTunerGroupReader(String hostname, int port) throws IOException {
		Socket socket = new Socket(hostname, port);
		reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
		writer = new PrintWriter(socket.getOutputStream(), true);  // true for autoflush
		
		// first, try to get the initial frequency
		writer.println("GET_FREQ");
	}
	
	@Override
	public GroupReaderEvent getGroup() throws IOException, EndOfStream {
		while(groups.size() == 0) {
			readUntil(null);
		}
		
		return groups.removeFirst();
	}

	@Override
	public boolean isStereo() {
		return false;
	}

	@Override
	public synchronized int setFrequency(int frequency) {
		writer.println("SET_FREQ " + frequency);
		return freq;
	}

	@Override
	public synchronized int getFrequency() {
		return freq;
	}

	@Override
	public int mute() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int unmute() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getSignalStrength() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void tune(boolean up) {
		writer.println(up ? "UP" : "DOWN");
	}

	@Override
	public boolean seek(boolean up) {
		writer.println("SEEK " + (up ? "UP" : "DOWN"));
		return true;
	}

	@Override
	public String getDeviceName() {
		return "TCP" + name;
	}

	@Override
	public synchronized boolean newGroups() {
		boolean ng = newGroups;
		newGroups = false;
		return ng;
	}
	
	private String readUntil(String start) throws IOException, EndOfStream {
		String line;
		
		do {
			line = reader.readLine();
			if(line == null) throw new EndOfStream();
			
			GroupReaderEvent event = HexFileGroupReader.parseHexLine(line, new RealTime());
			
			if(event instanceof GroupEvent) newGroups = true;
			else if(event instanceof FrequencyChangeEvent) {
				FrequencyChangeEvent fEvent = (FrequencyChangeEvent) event;
				synchronized(this) {
					freq = fEvent.frequency;
				}
			}
			
			if(event != null) groups.addLast(event);
		} while(start != null && !line.startsWith(start));
		return line;
		
	}

	@Override
	public boolean isSynchronized() {
		return true;
	}

}
