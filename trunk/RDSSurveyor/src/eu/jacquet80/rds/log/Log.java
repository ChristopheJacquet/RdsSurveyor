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

package eu.jacquet80.rds.log;

import java.util.ArrayList;
import java.util.LinkedList;

public class Log {
	private ArrayList<LogMessage> messages = new ArrayList<LogMessage>();
	//private LinkedList<Runnable> groupListeners = new LinkedList<Runnable>();
	private LinkedList<LogMessageVisitor> newMessageListeners = new LinkedList<LogMessageVisitor>();
	
	public synchronized void addMessage(LogMessage message) {
		messages.add(message);
		
		for(LogMessageVisitor v : newMessageListeners) message.accept(v);
	}
	
	public synchronized int getLastTime() {
		return messages.get(messages.size() - 1).getBitTime();
	}
	
	// the list of messages can only grow, so it is not really a critical
	// section issue if some message appears between the moment the count is
	// gotten and the moment the items are iterated. Newer messages simply are
	// not scanned this time.
	public synchronized int messageCount() {
		return messages.size();
	}
	
	public synchronized LogMessage getMessage(int i) {
		return messages.get(i);
	}
	
	/*public void addGroupListener(Runnable r) {
		groupListeners.add(r);
	}*/
	
	public void addNewMessageListener(LogMessageVisitor v) {
		newMessageListeners.add(v);
	}
	
	public String toString() {
		StringBuffer res = null;
		synchronized(this) {
			for(LogMessage m : messages) {
				if(res == null) res = new StringBuffer("Log\t");
				else res.append("\n\t");
				res.append(m);
			}
		}
		return (res == null) ? "Empty Log" : res.toString();
	}
	
	public synchronized boolean empty() {
		return messages.size() == 0;
	}
	
	public void notifyGroup() {
		//for(Runnable r : groupListeners) r.run();
	}
}
