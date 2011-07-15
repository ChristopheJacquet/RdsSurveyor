package eu.jacquet80.rds.input.group;

import eu.jacquet80.rds.log.RDSTime;

public abstract class GroupReaderEvent {
	private final RDSTime time;
	
	public abstract void accept(GroupReaderEventVisitor visitor);
	
	protected GroupReaderEvent(RDSTime time) {
		this.time = time;
	}
	
	public RDSTime getTime() {
		return time;
	}
}
