package eu.jacquet80.rds.input;

import java.io.IOException;
import java.util.LinkedList;

import eu.jacquet80.rds.input.group.FrequencyChangeEvent;
import eu.jacquet80.rds.input.group.GroupEvent;
import eu.jacquet80.rds.input.group.GroupReaderEvent;
import eu.jacquet80.rds.input.group.GroupReaderEventVisitor;
import eu.jacquet80.rds.input.group.StationChangeEvent;

public class StationChangeDetector implements GroupReader {
	private final GroupReader reader;
	
	private final LinkedList<GroupEvent> queuedGroups = new LinkedList<GroupEvent>();
	private boolean expectingPI = true;
	private int currentPI = -1;
	private GroupReaderEvent result;
	
	private final GroupReaderEventVisitor visitor = new GroupReaderEventVisitor() {
		@Override
		public void visit(StationChangeEvent stationChangeEvent) {
			result = stationChangeEvent;
			// pass through station change events
			
			// if there are still queued groups because PI was not known,
			// then they should be ignored for good.
			queuedGroups.clear();
			
			expectingPI = true;
		}

		@Override
		public void visit(FrequencyChangeEvent frequencyChangeEvent) {
			result = frequencyChangeEvent;
			// pass through frequency change events
			
			// if there are still queued groups because PI was not known,
			// then they should be ignored for good.
			queuedGroups.clear();
			
			expectingPI = true;
		}

		@Override
		public void visit(GroupEvent groupEvent) {
			int[] blocks = groupEvent.blocks;
			
			///System.out.println(". " + blocks[0]);
			
			if(expectingPI) {
				if(blocks[0] != -1) {	// PI was found in the current group
					expectingPI = false;	// don't expect PI anymore
					if(blocks[0] != currentPI) {	// if PI has changed
						// 1) memorize new current PI
						currentPI = blocks[0];
						// 2) flush out any queued groups
						queuedGroups.clear();
						// 3) enqueue current group
						queuedGroups.addLast(groupEvent);
						// 4) send station changed event
						result = new StationChangeEvent();
					} else {	// if PI has not changed
						// 1) enqueue current group
						queuedGroups.addLast(groupEvent);
						// 2) return void group, so that the queued groups be unqueued next
						result = null;
					}
				} else {	// PI still not found => enqueue group
					queuedGroups.addLast(groupEvent);
					result = null;
				}
			} else {
				if(blocks[0] == -1) {
					expectingPI = true;
					queuedGroups.addLast(groupEvent);
					result = null;
				} else {
					if(currentPI == blocks[0])
						result = groupEvent;	// "normal" behavior, when not expecting PI
					else {
						currentPI = blocks[0];
						queuedGroups.clear();
						queuedGroups.addLast(groupEvent);
						result = new StationChangeEvent();
					}
				}
			}
		}

	};

	
	public StationChangeDetector(GroupReader reader) {
		this.reader = reader;
	}
	
	
	public GroupReaderEvent getGroupOrNull() throws IOException, EndOfStream {
		if(!expectingPI && !queuedGroups.isEmpty()) {
			// There are queued groups awaiting to be sent out
			return queuedGroups.removeFirst();
		}
		
		// Otherwise get group event from reader
		final GroupReaderEvent event = reader.getGroup();
		///System.out.println("g: " + event);
		
		if(event == null) return null;		// propagate null events
		
		result = null;
		
		event.accept(visitor);
		
		return result;
	}


	@Override
	public GroupReaderEvent getGroup() throws IOException, EndOfStream {
		while(true) {
			GroupReaderEvent event = getGroupOrNull();
			//System.out.println(event);
			if(event != null) return event;
		}
	}


}
