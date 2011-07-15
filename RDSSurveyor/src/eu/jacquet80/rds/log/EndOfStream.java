package eu.jacquet80.rds.log;


public class EndOfStream extends LogMessage {

	public EndOfStream(RDSTime time) {
		super(time);
	}
	
	@Override
	public void accept(LogMessageVisitor visitor) {
		visitor.visit(this);
	}

}
