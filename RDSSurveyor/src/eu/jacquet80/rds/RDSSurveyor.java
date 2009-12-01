package eu.jacquet80.rds;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import eu.jacquet80.rds.core.StreamLevelDecoder;
import eu.jacquet80.rds.input.AudioFileBitReader;
import eu.jacquet80.rds.input.BinStringFileBitReader;
import eu.jacquet80.rds.input.BinaryFileBitReader;
import eu.jacquet80.rds.input.BitReader;
import eu.jacquet80.rds.input.LiveAudioBitReader;
import eu.jacquet80.rds.input.TeeBitReader;
import eu.jacquet80.rds.log.Log;
import eu.jacquet80.rds.ui.Segmenter;
import eu.jacquet80.rds.ui.TimeLine;

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
		System.out.println("RDS Surveyor - (C) Christophe Jacquet, 2009.");
		
		BitReader reader = null;
		boolean showGui = true;
		boolean liveInput = false;    // true if input is "live", not playback
		Segmenter segmenter = null;
		File outFile = null;
		PrintStream console = System.out;
		
		for(int i=0; i<args.length; i++) {
			BitReader newReader = null;
			if("-inaudio".equals(args[i])) {
				newReader = new LiveAudioBitReader();
				liveInput = true;
			} else if("-inbinfile".equals(args[i])) {
				newReader = new BinaryFileBitReader(new File(getParam("inbinfile", args, ++i)));
			} else if("-inbinstrfile".equals(args[i])) {
				newReader = new BinStringFileBitReader(new File(getParam("inbinstrfile", args, ++i)));
			} else if("-inaudiofile".equals(args[i])) {
				newReader = new AudioFileBitReader(new File(getParam("inaudiofile", args, ++i)));
			} else if("-outbinfile".equals(args[i])) {
				outFile = new File(getParam("outbinfile", args, ++i));
			} else if("-nogui".equals(args[i])) {
				showGui = false;
			} else if("-noconsole".equals(args[i])) {
				console = nullConsole;
			} else if("-segment".equals(args[i])) {
				console = nullConsole;   // implies -noconsole
				showGui = false;         // implies -nogui
				segmenter = new Segmenter(System.out);
			} else {
				System.out.println("Arguments:");
				System.out.println("  -inaudio                 Use sound card audio as input");
				System.out.println("  -inbinfile <file>        Use the given binary file as input");
				System.out.println("  -inbinstrfile <file>     Use the given binary string file as input");
				System.out.println("  -inaudiofile <file>      Use the given audio file as input");
				System.out.println("  -outbinfile <file>       Write output bitstream to binary file");
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
		if(liveInput && outFile == null) {
			System.out.print("Using default output file. ");
			String tempDir = System.getProperty("java.io.tmpdir");
			outFile = new File(tempDir, "rdslog_" + new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date()) + ".rds");
		}
		
		// use the output file if defined
		if(outFile != null) {
			System.out.println("Binary output file is " + outFile.getAbsoluteFile());
			reader = new TeeBitReader(reader, outFile);
		}
		

		Log log = new Log();
		final StreamLevelDecoder streamLevelDecoder = new StreamLevelDecoder(console);
		
		if(showGui) {
			JFrame frame = new JFrame("RDS Surveyor");
			final JTextArea area = new JTextArea();
			area.setFont(new Font("Monospaced", Font.PLAIN, 14));
			frame.setLayout(new BorderLayout());
			frame.add(new JScrollPane(area), BorderLayout.CENTER);
			frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			frame.setPreferredSize(new Dimension(800, 400));
			frame.pack();
			frame.setVisible(true);
		
			JFrame fTL = new JFrame("Timeline");
			final TimeLine timeLine = new TimeLine(log);
			fTL.setLayout(new BorderLayout());
			fTL.add(new JScrollPane(timeLine), BorderLayout.CENTER);
			fTL.setPreferredSize(new Dimension(1000, 200));
			fTL.pack();
			fTL.setVisible(true);
			
			log.addGroupListener(new Runnable() {
				public void run() {
					area.setText(streamLevelDecoder.getTunedStation().toString().replace("\n", "\r\n"));
					timeLine.update();
				}
			});
		}
		
		if(segmenter != null) {
			segmenter.registerAtLog(log);
		}

		streamLevelDecoder.processStream(reader, log);
		
		System.out.println("\nProcessing complete.");
	}
}
