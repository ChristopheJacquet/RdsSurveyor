package eu.jacquet80.rds.input.group;



public class GroupEvent extends GroupReaderEvent {
	public final int bitTime;
	public final int[] blocks;
	public final boolean ignored;
	
	@Override
	public void accept(GroupReaderEventVisitor visitor) {
		visitor.visit(this);
	}

	public GroupEvent(int bitTime, int[] blocks, boolean ignored) {
		this.bitTime = bitTime;
		this.blocks = blocks;
		this.ignored = ignored;
	}
	
	@Override
	public String toString() {
		return String.format("(Group: %04X %04X %04X %04X)", blocks[0], blocks[1], blocks[2], blocks[3]);
	}
}
