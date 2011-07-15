package eu.jacquet80.rds.log;



public class GroupReceived extends LogMessage {
	private final int[] blocks;
	private final String analysis;
	private final int nbOk;
	
	public GroupReceived(RDSTime time, int[] blocks, int nbOk, String analysis) {
		super(time);
		
		this.blocks = blocks;
		this.nbOk = nbOk;
		this.analysis = analysis;
	}
	
	@Override
	public void accept(LogMessageVisitor visitor) {
		visitor.visit(this);
	}
	
	public int[] getBlocks() {
		return blocks;
	}
	
	public int getNbOk() {
		return nbOk;
	}
	
	public String getAnalysis() {
		return analysis;
	}

	public String toString(boolean includeTime) {
		StringBuilder sb = new StringBuilder();
		
		if(includeTime) {
			sb.append(getTime()).append(": ");
		}
		
		sb.append("[");
		
		for(int i=0; i<4; i++) {
			if(blocks[i] >= 0) sb.append(String.format("%04X", blocks[i]));
			else sb.append("----");
			if(i < 3) sb.append(' ');
		}
		sb.append("] ").append(getAnalysis());
		
		return sb.toString();
	}
	
	public String toString() {
		return toString(true);
	}
	
	public int getGroupType() {
		if(blocks[1] == -1) return -1;
		else return (blocks[1] >> 11) & 0x1F;
	}
}
