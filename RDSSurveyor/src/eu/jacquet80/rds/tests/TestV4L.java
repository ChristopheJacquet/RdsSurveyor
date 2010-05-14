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
import eu.jacquet80.rds.input.TunerGroupReader;
import eu.jacquet80.rds.input.V4LTunerGroupReader;
import eu.jacquet80.rds.log.Log;

public class TestV4L {
	private final GroupLevelDecoder decoder;
	private int freq = 87500;
	private final TunerGroupReader radio;

	public TestV4L() throws FileNotFoundException {
		PrintStream out = new PrintStream("/tmp/rds.log");
		decoder = new GroupLevelDecoder(out);
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
						decoder.notifyFrequencyChange(0);
					}
				}
			};
		}.start();

	}

	public static void main(String[] args) throws IOException {
	

		TestV4L t = new TestV4L();
		
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
	
		
		
		int time = 0;
		
		Log log = new Log();

		for(;;) {
			
			int[] group = t.radio.getGroup();
			boolean[] ok = new boolean[4];
			int nbok = 0;

			if(group != null) {
				for(int j=0 ;j<group.length;j++) {
					//System.out.print(" " + String.format("%04X ", group[j]));
					ok[j] = group[j] >= 0;
					if(ok[j]) nbok++;
				}
			}
				//System.out.println();

			//System.out.println("InSync");
			synchronized(t) {
				if(group != null)
					t.decoder.processGroup(nbok, ok, group, time++, log);
			}
			//System.out.println("OutSync");
		}
		

	}

}
