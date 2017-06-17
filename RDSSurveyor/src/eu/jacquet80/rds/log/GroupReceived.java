package eu.jacquet80.rds.log;



/**
 * Reported when a RDS group has been received from the tuned station.
 */
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
	
	/**
	 * @brief Returns the blocks in the group.
	 */
	public int[] getBlocks() {
		return blocks;
	}
	
	/**
	 * @brief Returns the number of correctly received blocks in the group.
	 */
	public int getNbOk() {
		return nbOk;
	}
	
	/**
	 * @brief Returns a bit field indicating which blocks were received correctly.
	 * 
	 * If a block was discarded due to transmission errors, its corresponding bit is set to 1,
	 * otherwise it is 0. Block 0 is indicated by the least-significant bit.
	 */
	public int getOKMask() {
		return (blocks[0] != -1 ? 1 : 0) |
				(blocks[1] != -1 ? 2 : 0) |
				(blocks[2] != -1 ? 4 : 0) |
				(blocks[3] != -1 ? 8 : 0);
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
