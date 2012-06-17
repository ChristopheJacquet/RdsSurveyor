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
		String[] r = new String[4];
		
		for(int i=0; i<4; i++) {
			if(blocks[i] >= 0) r[i] = String.format("%04X", blocks[i]);
			else r[i] = "----";
		}
		
		return "(Group: " + r[0] + " " + r[1] + " " + r[2] + " " + r[3] + ")";
	}
}
