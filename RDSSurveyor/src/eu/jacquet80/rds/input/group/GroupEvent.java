package eu.jacquet80.rds.input.group;

import eu.jacquet80.rds.log.RDSTime;



public class GroupEvent extends GroupReaderEvent {
	public final int[] blocks;
	public final boolean ignored;
	
	@Override
	public void accept(GroupReaderEventVisitor visitor) {
		visitor.visit(this);
	}

	public GroupEvent(RDSTime time, int[] blocks, boolean ignored) {
		super(time);
		this.blocks = blocks;
		this.ignored = ignored;
	}
	
	@Override
	public String toString() {
		return String.format("(Group: %04X %04X %04X %04X)", blocks[0], blocks[1], blocks[2], blocks[3]);
	}
}
