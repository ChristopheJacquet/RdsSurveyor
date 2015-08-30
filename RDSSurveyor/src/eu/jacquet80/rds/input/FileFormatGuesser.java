package eu.jacquet80.rds.input;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.regex.Pattern;

import eu.jacquet80.rds.core.BitStreamSynchronizer;

public class FileFormatGuesser {
	private static final int GUESS_BUFFER_SIZE = 40;
	private static final Pattern HEXGROUP_PATTERN = 
			Pattern.compile("([0-9A-F]{4}|----)\\s+([0-9A-F]{4}|----)\\s+([0-9A-F]{4}|----)\\s+([0-9A-F]{4}|----)");
	private static final Pattern BINSTR_PATTERN =
			Pattern.compile("[01]{40}");
	
	private static GroupReader createReader(InputStream is) throws IOException {
		char[] guessBuffer = new char[GUESS_BUFFER_SIZE];
		BufferedInputStream bis = new BufferedInputStream(is);
		InputStreamReader isr = new InputStreamReader(bis, "ASCII");
		bis.mark(GUESS_BUFFER_SIZE+1);
		int guessCharCount = isr.read(guessBuffer);
		
		if(guessCharCount < 1) {
			throw new IOException("File empty or almost empty.");
		}

		String guessString = new String(guessBuffer, 0, guessCharCount);
		
		if(guessString.startsWith("% RDS hexgroups") ||
				guessString.startsWith("<recorder=\"RDS Spy\"") ||
				HEXGROUP_PATTERN.matcher(guessString).matches()) {
			// grouphexfile
			System.out.println("Detected a group-level file.");
			bis.reset();
			return new HexFileGroupReader(new BufferedReader(new InputStreamReader(bis)));
		} else if (BINSTR_PATTERN.matcher(guessString).matches()) {
			// binstrfile
			System.out.println("Detected a binary string file.");
			bis.reset();
			return new BitStreamSynchronizer(System.out, new BinStringFileBitReader(bis));
		} else if ((guessString.length() >= 2) && (guessString.codePointAt(0) == 0xfffd) && (guessString.codePointAt(1) == 0x6)) {
			// syncbinfile
			System.out.println("Detected a synchronized binary file.");
			bis.reset();
			return new BitStreamSynchronizer(System.out, new SyncBinaryFileBitReader(bis));
		} else {
			// binfile
			System.out.println("Detected a binary file.");
			bis.reset();
			return new BitStreamSynchronizer(System.out, new BinaryFileBitReader(bis));
		}
		
		//throw new IOException("Could not identify the file format");
	}
	
	public static GroupReader createReader(URL url) throws IOException {
		return createReader(url.openStream());
	}
	
	public static GroupReader createReader(File file) throws IOException {
		return createReader(new FileInputStream(file));
	}
}
