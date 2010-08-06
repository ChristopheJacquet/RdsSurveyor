package eu.jacquet80.rds.input.group;

public interface GroupReaderEventVisitor {
	public void visit(GroupEvent groupEvent);
	public void visit(StationChangeEvent stationChangeEvent);
	public void visit(FrequencyChangeEvent frequencyChangeEvent);
}
