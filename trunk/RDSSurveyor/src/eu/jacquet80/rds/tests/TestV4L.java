/*
 RDS Surveyor -- RDS decoder, analyzer and monitor tool and library.
 For more information see
   http://www.jacquet80.eu/
   http://rds-surveyor.sourceforge.net/

 Copyright 2010 Christophe Jacquet
 Copyright 2010 Dominique Matz

 This file is part of RDS Surveyor.

 RDS Surveyor is free software: you can redistribute it and/or modify
 it under the terms of the GNU Lesser Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 RDS Surveyor is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Lesser Public License for more details.

 You should have received a copy of the GNU Lesser Public License
 along with RDS Surveyor.  If not, see <http://www.gnu.org/licenses/>.

*/

package eu.jacquet80.rds.tests;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;

import eu.jacquet80.rds.core.GroupLevelDecoder;
import eu.jacquet80.rds.input.GroupReader.EndOfStream;
import eu.jacquet80.rds.input.TunerGroupReader;
import eu.jacquet80.rds.input.V4LTunerGroupReader;
import eu.jacquet80.rds.input.group.FrequencyChangeEvent;
import eu.jacquet80.rds.input.group.GroupEvent;
import eu.jacquet80.rds.input.group.GroupReaderEvent;
import eu.jacquet80.rds.input.group.GroupReaderEventVisitor;
import eu.jacquet80.rds.input.group.StationChangeEvent;
import eu.jacquet80.rds.log.Log;
import eu.jacquet80.rds.log.RealTime;

public class TestV4L {
	private final GroupLevelDecoder decoder;
	private int freq = 87500;
	private final TunerGroupReader radio;

	public TestV4L() throws FileNotFoundException {
		final Log log = new Log();
		PrintStream out = new PrintStream("/tmp/rds.log");
		decoder = new GroupLevelDecoder(log);
		radio = new V4LTunerGroupReader("/dev/radio0");

		new Thread() {
			public void run() {
				while(true) {
					try {
						sleep(5000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					//System.out.println("**");
					synchronized(TestV4L.this) {
						//System.out.println("%%");
						System.out.println(radio.getFrequency() + " <" + radio.getSignalStrength() + ">: " + String.format("%04X ", decoder.getTunedStation().getPI()) + decoder.getTunedStation().getStationName());
						freq += 50;
						if(freq > 108000) freq = 87500;
						radio.setFrequency(freq);
						decoder.notifyFrequencyChange(new RealTime());
					}
				}
			};
		}.start();

	}

	public static void main(String[] args) throws IOException, EndOfStream {
	

		final TestV4L t = new TestV4L();
		
		/*
		System.setProperty(
				"java.library.path", System.getProperty("user.dir"));
				//System.getProperty("java.library.path"));
				//System.getProperty(System.getProperty("user.dir")));
		*/
		
		System.setProperty("java.library.path", ".");
		
		
		System.out.println(System.getProperty("java.library.path"));
		
		System.out.println(System.getProperty("user.dir"));

		System.out.println("unmute" + t.radio.unmute());
		
		System.out.println("isStereo" + t.radio.isStereo());
		
		System.out.println("setFrequency" + t.radio.setFrequency(t.freq));
		System.out.println("getFrequency" + t.radio.getFrequency());
		System.out.println("getSignalStrength" + t.radio.getSignalStrength());
	
		
		for(;;) {
			
			final GroupReaderEvent event = t.radio.getGroup();
			
			event.accept(new GroupReaderEventVisitor() {
				int time = 0;
				
				@Override
				public void visit(StationChangeEvent stationChangeEvent) {
					System.out.println("New station tuned");
				}
				
				@Override
				public void visit(GroupEvent groupEvent) {
					boolean[] ok = new boolean[4];
					int nbok = 0;

					if(groupEvent.blocks != null) {
						for(int j=0 ;j<groupEvent.blocks.length;j++) {
							//System.out.print(" " + String.format("%04X ", group[j]));
							ok[j] = groupEvent.blocks[j] >= 0;
							if(ok[j]) nbok++;
						}
					}
						//System.out.println();

					//System.out.println("InSync");
					synchronized(t) {
						if(groupEvent.blocks != null) {}
							/*
							t.decoder.processGroup(nbok, ok, groupEvent.blocks, time++);
							TODO FIXME
							*/
					}
				}

				@Override
				public void visit(FrequencyChangeEvent frequencyChangeEvent) {
					System.out.println("Tuned to frequency " + frequencyChangeEvent.frequency);
				}
			});
		}
		

	}

}
