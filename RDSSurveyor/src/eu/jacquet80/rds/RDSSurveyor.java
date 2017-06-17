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

import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.prefs.Preferences;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import eu.jacquet80.rds.app.oda.TDC;
import eu.jacquet80.rds.app.oda.tmc.TMC;
import eu.jacquet80.rds.core.BitStreamSynchronizer;
import eu.jacquet80.rds.core.BitStreamSynchronizer.BitInversion;
import eu.jacquet80.rds.core.DecoderShell;
import eu.jacquet80.rds.core.TunedStation;
import eu.jacquet80.rds.img.Image;
import eu.jacquet80.rds.input.AudioBitReader;
import eu.jacquet80.rds.input.AudioFileBitReader;
import eu.jacquet80.rds.input.BinStringFileBitReader;
import eu.jacquet80.rds.input.BinaryFileBitReader;
import eu.jacquet80.rds.input.BitReader;
import eu.jacquet80.rds.input.FileFormatGuesser;
import eu.jacquet80.rds.input.GroupReader;
import eu.jacquet80.rds.input.HexFileGroupReader;
import eu.jacquet80.rds.input.LiveAudioBitReader;
import eu.jacquet80.rds.input.NativeTunerGroupReader;
import eu.jacquet80.rds.input.SdrGroupReader;
import eu.jacquet80.rds.input.SyncBinaryFileBitReader;
import eu.jacquet80.rds.input.TCPTunerGroupReader;
import eu.jacquet80.rds.input.TeeBitReader;
import eu.jacquet80.rds.input.TunerGroupReader;
import eu.jacquet80.rds.input.USBFMRadioGroupReader;
import eu.jacquet80.rds.input.UnavailableInputMethod;
import eu.jacquet80.rds.input.V4LTunerGroupReader;
import eu.jacquet80.rds.ui.InputSelectionDialog;
import eu.jacquet80.rds.ui.MainWindow;
import eu.jacquet80.rds.ui.Overviewer;
import eu.jacquet80.rds.ui.Segmenter;

public class RDSSurveyor {
	public final static Preferences preferences = Preferences.userRoot().node("/eu/jacquet80/rdssurveyor");
	
	public final static String PREF_REALTIME = "playback_realtime";
	public final static String PREF_RBDS = "core_rbds";
	public final static String PREF_LAST_DIR = "directory_last";
	public final static String PREF_TUNER_FREQ = "tuner_frequency";
	
	private final static String[] candidateTempDirs = {
		"log", "Log",
		"logs", "Logs",
	};
	
	public final static String tempDir;
	
	static {
		String d = System.getProperty("java.io.tmpdir");
		for(String c : candidateTempDirs) {
			if(new File(c).isDirectory()) {
				d = c;
				break;
			}
		}
		tempDir = d;
	}
	
	static final private Pattern GROUP_AID = Pattern.compile("(\\d{1,2})([AB]):([0-9A-F]{4})");
	
	/**
	 * The nullConsole just does nothing. It silently discards any message.
	 */
	public static PrintStream nullConsole = new PrintStream(new OutputStream() {
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
	
	public static void main(String[] args) throws IOException, UnavailableInputMethod {
		System.out.println("RDS Surveyor - (C) Christophe Jacquet and contributors, 2009-2014.");
		
		GroupReader reader = null;
		boolean showGui = true;
		boolean liveInput = false;    // true if input is "live", not playback
		boolean liveGroupInput = false;
		boolean scan = false;
		boolean overview = false;
		Segmenter segmenter = null;
		File outBinFile = null;
		File outGroupFile = null;
		PrintStream console = System.out;
		BitStreamSynchronizer.BitInversion inversion = BitInversion.AUTO;
		BitStreamSynchronizer bitStreamSynchronizer = null;
		String inLtPath = null;
		String dbUrl = "jdbc:hsqldb:mem:.";
		
		// RDS Surveyor is non-localized for the time being
		Locale.setDefault(Locale.US);
		
		// Application name for MacOS X
		try {
			System.setProperty("com.apple.mrj.application.apple.menu.about.name", "RDS Surveyor" );
			System.setProperty("apple.laf.useScreenMenuBar", "true");
		} catch(Exception e) {}
		
		
		// Application icon for MacOS X
		try {
			Class<?> c = Class.forName("com.apple.eawt.Application");
			Method m = c.getMethod("getApplication");
			Object applicationInstance = m.invoke(null);
			m = applicationInstance.getClass().getMethod("setDockIconImage", java.awt.Image.class);
			m.invoke(applicationInstance, Image.ICON);
		} catch(Exception e) {}
		
		
		if(args.length != 0) {
			// if arguments are provided, RDS Surveyor was launched from the
			// command line, so analyze those arguments

			for(int i=0; i<args.length; i++) {
				if("-inaudio".equals(args[i])) {
					BitReader binReader = new LiveAudioBitReader();
					// TODO Ugly hack
					{
						outBinFile = new File(tempDir, "rdslog_" + new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date()) + ".rds");
						
						System.out.println("Binary output file is " + outBinFile.getAbsoluteFile());
						binReader = new TeeBitReader(binReader, outBinFile);
					}
					
					bitStreamSynchronizer = new BitStreamSynchronizer(console, binReader);
					reader = bitStreamSynchronizer;
					liveInput = true;
				} else if("-inaudiopipe".equals(args[i])) {
					reader = new BitStreamSynchronizer(console, new AudioBitReader(new DataInputStream(System.in), Integer.parseInt(getParam("inaudiopipe", args, ++i))));
					liveInput = true;
				} else if("-inbinfile".equals(args[i])) {
					reader = new BitStreamSynchronizer(console, new BinaryFileBitReader(new File(getParam("inbinfile", args, ++i))));
				} else if("-insyncbinfile".equals(args[i])) {
					reader = new BitStreamSynchronizer(console, new SyncBinaryFileBitReader(new File(getParam("insyncbinfile", args, ++i))));
				} else if("-inbinstrfile".equals(args[i])) {
					reader = new BitStreamSynchronizer(console, new BinStringFileBitReader(new File(getParam("inbinstrfile", args, ++i))));
				} else if("-ingrouphexfile".equals(args[i])) {
					reader  = new HexFileGroupReader(new File(getParam("ingrouphexfile", args, ++i)));
				} else if("-infile".equals(args[i])) {
					reader = FileFormatGuesser.createReader(new File(getParam("infile", args, ++i)));
				} else if("-intcp".equals(args[i])) {
					reader = new TCPTunerGroupReader(getParam("intcp", args, ++i), 8750);
				} else if("-inusbkey".equals(args[i])) {
					reader = new USBFMRadioGroupReader();
					((USBFMRadioGroupReader)reader).init();
				} else if("-inv4l".equals(args[i])) {
					reader = new V4LTunerGroupReader(getParam("inv4l", args, ++i));
					liveGroupInput = true;
				} else if("-intuner".equals(args[i])) {
					reader = new NativeTunerGroupReader(getParam("intuner", args, ++i));
					liveGroupInput = true;
				} else if("-insdr".equals(args[i])) {
					reader = new SdrGroupReader(console, getParam("insdr", args, ++i));
					liveGroupInput = true;
				} else if("-invert".equals(args[i])) {
					inversion = BitInversion.INVERT;
				} else if("-noinvert".equals(args[i])) {
					inversion = BitInversion.NOINVERT;
				} else if("-inaudiofile".equals(args[i])) {
					reader = new BitStreamSynchronizer(console, new AudioFileBitReader(new File(getParam("inaudiofile", args, ++i))));
				} else if("-outbinfile".equals(args[i])) {
					outBinFile = new File(getParam("outbinfile", args, ++i));
				} else if("-outgrouphexfile".equals(args[i])) {
					outGroupFile = new File(getParam("outgrouphexfile", args, ++i));
				} else if("-nogui".equals(args[i])) {
					showGui = false;
				} else if("-noconsole".equals(args[i])) {
					console = null;
				} else if("-segment".equals(args[i])) {
					console = null;   // implies -noconsole
					showGui = false;         // implies -nogui
					segmenter = new Segmenter(System.out);
				} else if("-scan".equals(args[i])) {
					scan = true;
				} else if("-overview".equals(args[i])) {
					overview = true;
					showGui = false;
				} else if("-rds".equals(args[i])) {
					preferences.putBoolean(PREF_RBDS, false);
				} else if("-rbds".equals(args[i])) {
					preferences.putBoolean(PREF_RBDS, true);
				} else if("-tdc".equals(args[i])) {
					String tdcApp = getParam("tdc", args, ++i);
					if(TDC.setPreferredTDCApp(tdcApp)) {
						System.out.println("Using TDC decoder: " + tdcApp);
					} else {
						System.out.println("Unknown TDC decoder: " + tdcApp);
						System.exit(1);
					}
				} else if("-force".equals(args[i])) {
					String arg = getParam("force", args, ++i);
					Matcher m = GROUP_AID.matcher(arg);
					if(m.matches()) {
						int num = Integer.parseInt(m.group(1));
						char v = m.group(2).charAt(0);
						int aid = Integer.parseInt(m.group(3), 16);
						
						TunedStation.addForcedODA((num<<1) + (v=='A'?0:1), aid);
						System.out.printf("Forcing ODA: %d%c -> %04X\n", num, v, aid);
					} else {
						System.out.println("Malformed -force option");
						System.exit(1);
					}
				} else if("-lt".equals(args[i])) {
					inLtPath = getParam("lt", args, ++i);
				} else if("-ltdb".equals(args[i])) {
					dbUrl = String.format("jdbc:hsqldb:file:%s", getParam("ltdb", args, ++i));
				} else {
					System.out.println("Unknown argument: " + args[i]);
					
					System.out.println("Arguments:");
					System.out.println("  -inaudio                 Use sound card audio as input");
					System.out.println("  -inaudiopipe             Use raw audio from stdin as input");
					System.out.println("  -inbinfile <file>        Use the given binary file as input");
					System.out.println("  -insyncbinfile <file>    Use the given synchronized binary file as input");
					System.out.println("  -inbinstrfile <file>     Use the given binary string file as input");
					System.out.println("  -inaudiofile <file>      Use the given audio file as input");
					System.out.println("  -ingrouphexfile <file>   Use the given group-level file as input");
					System.out.println("  -infile <file>           Use the given file as input (autodetect format)");
					System.out.println("  -inv4l <device>          Reads from Video4Linux device, e.g. /dev/radio");
					System.out.println("  -intuner <driver>        Reads from a native tuner, specify driver (.so, .dll, .dylib)");
					System.out.println("  -insdr <driver>          Reads from an SDR, specify driver (.so, .dll, .dylib)");
					System.out.println("  -invert / -noinvert      Force bit inversion (default: auto-detect");
					System.out.println("  -outbinfile <file>       Write bitstream to binary file (if applicable)");
					System.out.println("  -outgrouphexfile <file>  Write groups to file (in hexadecimal)");
					System.out.println("  -nogui                   Do not show the graphical user interface");
					System.out.println("  -noconsole               No console analysis");
					System.out.println("  -rds                     Force standard RDS mode (and save as a preference)");
					System.out.println("  -rbds                    Force American RBDS mode (and save as a preference)");
					System.out.println("  -tdc <decoder>           Use a given TDC decoder (available decoder: CATRADIO)");
					System.out.println("  -force <group>:<aid>     Force to use a given ODA for the given group");
					System.out.println("  -lt <path>               Read TMC location tables found at the given path (or subdirs)");
					System.out.println("  -ltdb <path>             Use TMC location database at the given path");
					System.exit(1);
				}
			}
		}

		if ((reader == null) && (inLtPath == null)) {
			if(showGui) {
				console = null;
				InputSelectionDialog dialog = new InputSelectionDialog();
				reader = dialog.makeChoice();
				liveGroupInput = dialog.live;
			}
			if (reader == null) {
				System.out.println("A source or a set of TMC location tables must be provided. Aborting.");
				System.exit(0);
			}
		}
		
		TMC.setDbUrl(dbUrl);
		
		// Build db if needed
		if (inLtPath != null) {
			System.out.println("Processing TMC location tables...");
			TMC.readLocationTables(new File(inLtPath));
			System.out.println("Done processing TMC location tables.");
			if (reader == null)
				System.exit(0);
		}
		
		// Create a decoder "shell"
		final PrintStream fConsole = console == null ? nullConsole : console;
		DecoderShell.instance.setConsole(console);
		
		// Create the input toolbar before wrapping the reader into a station change detector
		// and possibly a group logger (tee)
		if(showGui) {
			MainWindow mainWindow = new MainWindow();
			mainWindow.setReader(DecoderShell.instance.getLog(), reader);
			mainWindow.setVisible(true);
		}
		
		// when input is "live", always create an output file
		// if there is no output file, create one in the temp directory
		if(liveInput && outBinFile == null) {
			System.out.print("Using default output file. ");
			outBinFile = new File(tempDir, "rdslog_" + new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date()) + ".binrds");
		}
		// TODO: guess above code is dead
		
		
		
		// force inversion if necessary
		if(bitStreamSynchronizer != null && inversion != BitInversion.AUTO) {
			bitStreamSynchronizer.forceInversion(inversion);
		}
		

			
		if(segmenter != null) {
			segmenter.registerAtLog(DecoderShell.instance.getLog());
		}

		if(showGui) {
			DecoderShell.instance.process(reader, liveGroupInput);
		} else {
			DecoderShell.instance.processAndQuit(reader, liveGroupInput);
		}
		
		
		// Now, set up special modes
		if(scan) {
			if(reader instanceof TunerGroupReader) {
				final TunerGroupReader tgr = (TunerGroupReader) reader;
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
							fConsole.print("*** Tuning... ");
							fConsole.flush();
							tgr.seek(true);
							fConsole.println("At " + tgr.getFrequency());
						}
					}
				}.start();
			} else {
				console.println("Scanning may be used only with a tuner (" + reader.getClass() + ")");
				System.exit(1);
			}
		} else if(overview) {
			if(reader instanceof TunerGroupReader) {
				final TunerGroupReader tgr = (TunerGroupReader) reader;
				DecoderShell.instance.setConsole(nullConsole);
				new Overviewer(tgr, fConsole).start();
			} else {
				console.println("Overview may be used only with a tuner (" + reader.getClass() + ")");
				System.exit(1);
			}
		}

	}
}
