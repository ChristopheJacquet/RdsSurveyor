package eu.jacquet80.rds.core;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.Semaphore;

import eu.jacquet80.rds.RDSSurveyor;
import eu.jacquet80.rds.input.GroupReader;
import eu.jacquet80.rds.input.StationChangeDetector;
import eu.jacquet80.rds.input.TeeGroupReader;
import eu.jacquet80.rds.input.group.GroupReaderEvent;
import eu.jacquet80.rds.log.DefaultLogMessageVisitor;
import eu.jacquet80.rds.log.EndOfStream;
import eu.jacquet80.rds.log.GroupReceived;
import eu.jacquet80.rds.log.Log;
import eu.jacquet80.rds.log.LogMessageVisitor;
import eu.jacquet80.rds.log.StationLost;

public class DecoderShell {
	private final Log log = new Log();
	
	private final Thread worker;
	
	// concurrent accesses to reader must be synchronized on DecoderShell's monitor
	private GroupReader reader;
	private final GroupLevelDecoder groupDecoder = new GroupLevelDecoder(log);
	
	private final Semaphore groupReady = new Semaphore(0);
	private boolean quitAfterProcess = false;
	
	public final static DecoderShell instance = new DecoderShell();
	
	private PrintStream console = RDSSurveyor.nullConsole;
	
	private DecoderShell() {
		this.log.addNewMessageListener(consolePrinter);
		
		worker = new Thread() {
			{
				setName("RDS-Worker");
			}
			
			public void run() {
				try {
					while(true) {
						groupReady.acquireUninterruptibly();
						
						GroupReaderEvent evt;
						boolean goOn;

						try {
							GroupReader r;
							synchronized(DecoderShell.this) {
								r = reader;
							}
							evt = r.getGroup();
							goOn = true;
							groupDecoder.processOneGroup(evt);
						} catch(eu.jacquet80.rds.input.GroupReader.EndOfStream eos) {
							TunedStation lastStation = groupDecoder.getTunedStation();
							if(lastStation != null) {
								log.addMessage(new StationLost(null, lastStation, true));
							}
							log.addMessage(new eu.jacquet80.rds.log.EndOfStream(null));
							goOn = false;
							if(quitAfterProcess) return;
						}

						if(goOn) groupReady.release();
					}
				} catch (IOException e) {
					System.err.println("In RDS worker thread: " + e);
					e.printStackTrace(System.err);
				}
			};
		};

		this.worker.start();
	}
	
	public void setConsole(final PrintStream console) {
		this.console = console == null ? RDSSurveyor.nullConsole : console;
	}
	
	private final LogMessageVisitor consolePrinter = new DefaultLogMessageVisitor() {
		@Override
		public void visit(EndOfStream endOfStream) {
			//console.println("\nProcessing complete.");
		}
		
		@Override
		public void visit(GroupReceived groupReceived) {
			console.println(groupReceived.toString(true));
		}
	};
	
	public Log getLog() {
		return this.log;
	}
	
	public synchronized void process(final GroupReader aReader, boolean outFile) {
		// implicitly, this is the end of the previous stream
		// (important to have this for UI parts that may react to stream changes)
		log.addMessage(new EndOfStream(null));

		this.reader = aReader;
		
		// output file?
		if(outFile) {
			System.out.print("Using default group output file. ");
			File outGroupFile = new File(RDSSurveyor.tempDir, "rdslog_" + new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date()) + ".rds");			

			System.out.println("Hex group output file is " + outGroupFile.getAbsoluteFile());
			try {
				this.reader = new TeeGroupReader(this.reader, outGroupFile);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		
		// add a station change detector
		this.reader = new StationChangeDetector(this.reader);

		this.groupReady.release();
	}
	
	public void processAndQuit(final GroupReader reader, boolean outFile) {
		this.quitAfterProcess = true;
		process(reader, outFile);
	}
	
	public GroupLevelDecoder getGroupReader() {
		return groupDecoder;
	}
}
