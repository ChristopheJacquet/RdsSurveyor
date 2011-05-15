package eu.jacquet80.rds.log;

import java.text.Format;

public class GroupReceived extends LogMessage {
	private final int[] blocks;
	private String analysis;
	
	public GroupReceived(int bitTime, int[] blocks, String analysis) {
		super(bitTime);
		
		this.blocks = blocks;
		this.analysis = analysis;
	}
	
	@Override
	public void accept(LogMessageVisitor visitor) {
		visitor.visit(this);
	}
	
	public int[] getBlocks() {
		return blocks;
	}
	
	public String getAnalysis() {
		return analysis;
	}

	public String toString() {
		StringBuilder sb = new StringBuilder("[");
		
		for(int i=0; i<4; i++) {
			if(blocks[i] >= 0) sb.append(String.format("%04X", blocks[i]));
			else sb.append("----");
			if(i < 3) sb.append(' ');
		}
		sb.append("] ").append(getAnalysis());
		
		return sb.toString();
	}
	
	public int getGroupType() {
		if(blocks[1] == -1) return -1;
		else return (blocks[1] >> 11) & 0x1F;
	}
}
