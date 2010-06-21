/*
 RDS Surveyor -- RDS decoder, analyzer and monitor tool and library.
 For more information see
   http://www.jacquet80.eu/
   http://rds-surveyor.sourceforge.net/
 
 Copyright (c) 2009, 2010 Christophe Jacquet

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

package eu.jacquet80.rds;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.Date;

import eu.jacquet80.rds.core.GroupLevelDecoder;
import eu.jacquet80.rds.core.RDSDecoder;
import eu.jacquet80.rds.core.StreamLevelDecoder;
import eu.jacquet80.rds.core.StreamLevelDecoder.BitInversion;
import eu.jacquet80.rds.input.AudioFileBitReader;
import eu.jacquet80.rds.input.BinStringFileBitReader;
import eu.jacquet80.rds.input.BinaryFileBitReader;
import eu.jacquet80.rds.input.BitReader;
import eu.jacquet80.rds.input.GroupReader;
import eu.jacquet80.rds.input.HexFileGroupReader;
import eu.jacquet80.rds.input.LiveAudioBitReader;
import eu.jacquet80.rds.input.RDSReader;
import eu.jacquet80.rds.input.SyncBinaryFileBitReader;
import eu.jacquet80.rds.input.TeeBitReader;
import eu.jacquet80.rds.input.TeeGroupReader;
import eu.jacquet80.rds.input.TunerGroupReader;
import eu.jacquet80.rds.input.USBFMRadioGroupReader;
import eu.jacquet80.rds.input.V4LTunerGroupReader;
import eu.jacquet80.rds.log.EndOfStream;
import eu.jacquet80.rds.log.Log;
import eu.jacquet80.rds.ui.MainWindow;
import eu.jacquet80.rds.ui.Segmenter;
import eu.jacquet80.rds.ui.input.InputToolBar;

public class RDSSurveyor {
	/**
	 * The nullConsole just does nothing. It silently discards any message.
	 */
	private static PrintStream nullConsole = new PrintStream(new OutputStream() {
		@Override
		public void write(int b) throws IOException {
		}
	});

	private static String getParam(String param, String[] args, int pos) {
		if(pos >= args.length) {
			System.out.println("-" + param + " needs a filename.");
			System.exit(0);
		}
		return args[pos];
	}
	
	public static void main(String[] args) throws IOException {
		System.out.println("RDS Surveyor - (C) Christophe Jacquet, 2009-2010.");
		
		RDSReader reader = null, realReader = null;
		boolean showGui = true;
		boolean liveInput = false;    // true if input is "live", not playback
		boolean liveGroupInput = false;
		boolean scan = false;
		Segmenter segmenter = null;
		File outBinFile = null;
		File outGroupFile = null;
		PrintStream console = System.out;
		StreamLevelDecoder.BitInversion inversion = BitInversion.AUTO;
		
		for(int i=0; i<args.length; i++) {
			RDSReader newReader = null;
			if("-inaudio".equals(args[i])) {
				newReader = new LiveAudioBitReader();
				liveInput = true;
			} else if("-inbinfile".equals(args[i])) {
				newReader = new BinaryFileBitReader(new File(getParam("inbinfile", args, ++i)));
			} else if("-insyncbinfile".equals(args[i])) {
				newReader = new SyncBinaryFileBitReader(new File(getParam("insyncbinfile", args, ++i)));
			} else if("-inbinstrfile".equals(args[i])) {
				newReader = new BinStringFileBitReader(new File(getParam("inbinstrfile", args, ++i)));
			} else if("-ingrouphexfile".equals(args[i])) {
				newReader  = new HexFileGroupReader(new File(getParam("ingrouphexfile", args, ++i)));
			} else if("-inusbkey".equals(args[i])) {
				newReader = new USBFMRadioGroupReader();
				((USBFMRadioGroupReader)newReader).init();
				((USBFMRadioGroupReader)newReader).setFrequency(105500);
			} else if("-inv4l".equals(args[i])) {
				newReader = new V4LTunerGroupReader(getParam("inv4l", args, ++i));
				liveGroupInput = true;
			} else if("-invert".equals(args[i])) {
				inversion = BitInversion.INVERT;
			} else if("-noinvert".equals(args[i])) {
				inversion = BitInversion.NOINVERT;
			} else if("-inaudiofile".equals(args[i])) {
				newReader = new AudioFileBitReader(new File(getParam("inaudiofile", args, ++i)));
			} else if("-outbinfile".equals(args[i])) {
				outBinFile = new File(getParam("outbinfile", args, ++i));
			} else if("-outgrouphexfile".equals(args[i])) {
				outGroupFile = new File(getParam("outgrouphexfile", args, ++i));
			} else if("-nogui".equals(args[i])) {
				showGui = false;
			} else if("-noconsole".equals(args[i])) {
				console = nullConsole;
			} else if("-segment".equals(args[i])) {
				console = nullConsole;   // implies -noconsole
				showGui = false;         // implies -nogui
				segmenter = new Segmenter(System.out);
			} else if("-scan".equals(args[i])) {
				scan = true;
			} else {
				System.out.println("Unknown argument: " + args[i]);
				
				System.out.println("Arguments:");
				System.out.println("  -inaudio                 Use sound card audio as input");
				System.out.println("  -inbinfile <file>        Use the given binary file as input");
				System.out.println("  -inbinstrfile <file>     Use the given binary string file as input");
				System.out.println("  -inaudiofile <file>      Use the given audio file as input");
				System.out.println("  -ingrouphexfile <file>   Use the given group-level file as input");
				System.out.println("  -inv4l <device>          Reads from Video4Linux device, e.g. /dev/radio");
				System.out.println("  -invert / -noinvert      Force bit inversion (default: auto-detect");
				System.out.println("  -outbinfile <file>       Write bitstream to binary file (if applicable)");
				System.out.println("  -outgrouphexfile <file>  Write groups to file (in hexadecimal)");
				System.out.println("  -nogui                   Do not show the graphical user interface");
				System.out.println("  -noconsole               No console analysis");
				System.exit(1);
			}
			
			if(newReader != null) {
				if(reader == null) reader = newReader;
				else {
					System.out.println("Cannot have two different input sources.");
					System.exit(0);
				}
			}
		}
		
		if(reader == null) {
			System.out.println("A source must be provided.");
			System.exit(0);
		}
		
		// when input is "live", then always create an output file
		// if there is no output file, create one in the temp directory
		if(liveInput && outBinFile == null) {
			System.out.print("Using default output file. ");
			String tempDir = System.getProperty("java.io.tmpdir");
			outBinFile = new File(tempDir, "rdslog_" + new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date()) + ".rds");
		}
		
		if(liveGroupInput && outGroupFile == null) {
			System.out.print("Using default group output file. ");
			String tempDir = System.getProperty("java.io.tmpdir");
			outGroupFile = new File(tempDir, "rdslog_" + new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date()) + ".l2");			
		}
		
		realReader = reader;
		
		// use the output binary file if defined
		if(outBinFile != null && reader instanceof BitReader) {
			System.out.println("Binary output file is " + outBinFile.getAbsoluteFile());
			reader = new TeeBitReader((BitReader)reader, outBinFile);
		}
		
		// use the output hex file if defined
		if(outGroupFile != null && reader instanceof GroupReader) {
			System.out.println("Hex group output file is " + outGroupFile.getAbsoluteFile());
			reader = new TeeGroupReader((GroupReader)reader, outGroupFile);
		}
		
		Log log = new Log();
		final RDSDecoder streamLevelDecoder = 
			reader instanceof BitReader ? new StreamLevelDecoder(console) : new GroupLevelDecoder(console);

		// force inversion if necessary
		if(streamLevelDecoder instanceof StreamLevelDecoder && inversion != BitInversion.AUTO) {
			((StreamLevelDecoder)streamLevelDecoder).forceInversion(inversion);
		}
		
		if(scan) {
			if(realReader instanceof TunerGroupReader) {
				final TunerGroupReader tgr = (TunerGroupReader) realReader;
				new Thread() {
					public void run() {
						while(true) {
							int cnt = 0;
							boolean rdsReceived = false;
							do {
								try {
									sleep(2000);
									if(rdsReceived) sleep(4000);
								} catch (InterruptedException e) {}
								cnt++;
								rdsReceived = tgr.newGroups();
							} while(cnt < 4 && rdsReceived);
							System.out.print("*** Tuning... ");
							System.out.flush();
							tgr.seek(true);
							System.out.println("At " + tgr.getFrequency());
						}
					}
				}.start();
			} else {
				System.out.println("Scanning may be used only with a tuner (" + realReader.getClass() + ")");
				System.exit(1);
			}
		}

			
		if(showGui) {
			/*
			JFrame frame = new JFrame("RDS Surveyor");
			final JTextArea area = new JTextArea();
			area.setFont(new Font("Monospaced", Font.PLAIN, 14));
			frame.setLayout(new BorderLayout());
			frame.add(new JScrollPane(area), BorderLayout.CENTER);
			frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			frame.setPreferredSize(new Dimension(800, 400));
			frame.pack();
			frame.setVisible(true);
			*/
		
			/*
			JFrame fTL = new JFrame("Timeline");
			final TimeLine timeLine = new TimeLine(log);
			fTL.setLayout(new BorderLayout());
			fTL.add(new JScrollPane(timeLine), BorderLayout.CENTER);
			fTL.setPreferredSize(new Dimension(1000, 200));
			fTL.pack();
			fTL.setVisible(true);
			*/
			
			InputToolBar toolbar = InputToolBar.forReader(realReader);
			
			MainWindow mainWindow = new MainWindow(log, toolbar);
			mainWindow.setVisible(true);
			
			/*
			log.addGroupListener(new Runnable() {
				public void run() {
					area.setText(streamLevelDecoder.getTunedStation().toString().replace("\n", "\r\n"));
					//timeLine.update();
				}
			});
			*/
		}
		
		if(segmenter != null) {
			segmenter.registerAtLog(log);
		}

		streamLevelDecoder.processStream(reader, log);
		
		System.out.println("\nProcessing complete.");
		log.addMessage(new EndOfStream(0));
	}
}
