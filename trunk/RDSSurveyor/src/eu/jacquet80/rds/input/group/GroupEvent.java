package eu.jacquet80.rds.input.group;


public class GroupEvent extends GroupReaderEvent {
	public final int[] blocks;
	public final boolean ignored;
	
	@Override
	public void accept(GroupReaderEventVisitor visitor) {
		visitor.visit(this);
	}

	public GroupEvent(int[] blocks, boolean ignored) {
		this.blocks = blocks;
		this.ignored = ignored;
	}
}
