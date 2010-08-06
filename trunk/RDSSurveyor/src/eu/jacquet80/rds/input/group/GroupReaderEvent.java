package eu.jacquet80.rds.input.group;

public abstract class GroupReaderEvent {
	public abstract void accept(GroupReaderEventVisitor visitor);
}
