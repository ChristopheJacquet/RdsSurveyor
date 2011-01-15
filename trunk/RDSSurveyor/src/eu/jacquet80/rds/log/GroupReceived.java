package eu.jacquet80.rds.log;

public class GroupReceived extends LogMessage {

	public GroupReceived(int bitTime, boolean[] blocksOk, int[] blocks) {
		super(bitTime);
	}
	
	@Override
	public void accept(LogMessageVisitor visitor) {
		visitor.visit(this);
	}

}
