package eu.jacquet80.rds.input.group;

import eu.jacquet80.rds.log.RDSTime;

public class FrequencyChangeEvent extends GroupReaderEvent {
	public final int frequency;
	
	@Override
	public void accept(GroupReaderEventVisitor visitor) {
		visitor.visit(this);
	}

	public FrequencyChangeEvent(RDSTime time, int frequency) {
		super(time);
		this.frequency = frequency;
	}
}
