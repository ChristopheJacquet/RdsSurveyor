package eu.jacquet80.rds.input.group;

public class StationChangeEvent extends GroupReaderEvent {
	@Override
	public void accept(GroupReaderEventVisitor visitor) {
		visitor.visit(this);
	}
}
