package eu.jacquet80.rds.input.group;

public class FrequencyChangeEvent extends GroupReaderEvent {
	public final int frequency;
	
	@Override
	public void accept(GroupReaderEventVisitor visitor) {
		visitor.visit(this);
	}

	public FrequencyChangeEvent(int frequency) {
		this.frequency = frequency;
	}
}
