package eu.jacquet80.rds.log;

public class EndOfStream extends LogMessage {

	public EndOfStream(int bittime) {
		super(bittime);
	}
	
	@Override
	public void accept(LogMessageVisitor visitor) {
		visitor.visit(this);
	}

}
