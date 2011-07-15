package eu.jacquet80.rds.input.group;

import eu.jacquet80.rds.log.RDSTime;

public class StationChangeEvent extends GroupReaderEvent {
	@Override
	public void accept(GroupReaderEventVisitor visitor) {
		visitor.visit(this);
	}
	
	public StationChangeEvent(RDSTime time) {
		super(time);
	}
}
