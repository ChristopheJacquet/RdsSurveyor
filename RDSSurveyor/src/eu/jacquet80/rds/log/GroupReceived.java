package eu.jacquet80.rds.log;

public class GroupReceived extends LogMessage {
	private final int[] blocks;
	private String decoding;
	
	public GroupReceived(int bitTime, int[] blocks, String decoding) {
		super(bitTime);
		
		this.blocks = blocks;
		this.decoding = decoding;
	}
	
	@Override
	public void accept(LogMessageVisitor visitor) {
		visitor.visit(this);
	}
	
	public int[] getBlocks() {
		return blocks;
	}
	
	public String getDecoding() {
		return decoding;
	}

}
