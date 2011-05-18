package eu.jacquet80.rds.core;

import java.io.IOException;
import java.io.PrintStream;

import eu.jacquet80.rds.input.GroupReader;
import eu.jacquet80.rds.input.StationChangeDetector;
import eu.jacquet80.rds.log.DefaultLogMessageVisitor;
import eu.jacquet80.rds.log.EndOfStream;
import eu.jacquet80.rds.log.GroupReceived;
import eu.jacquet80.rds.log.Log;

public class DecoderShell {
	private final Log log;
	private final GroupReader reader;
	
	public DecoderShell(final GroupReader reader, final PrintStream console) {
		log = new Log();
		
		// add a station change detector
		this.reader = new StationChangeDetector(reader);
		
		log.addNewMessageListener(new DefaultLogMessageVisitor() {
			@Override
			public void visit(EndOfStream endOfStream) {
				console.println("\nProcessing complete.");
			}
			
			@Override
			public void visit(GroupReceived groupReceived) {
				console.printf("%04d: ", groupReceived.getBitTime() / 26);
				console.println(groupReceived);
			}
		});
	}
	
	public Log getLog() {
		return log;
	}
	
	public void process() throws IOException {
		final GroupLevelDecoder groupDecoder = new GroupLevelDecoder(log);

		groupDecoder.processStream(reader);
	}
}
