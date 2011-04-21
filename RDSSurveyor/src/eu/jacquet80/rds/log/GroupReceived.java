package eu.jacquet80.rds.log;

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

}
