package eu.jacquet80.rds.input;

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
	
	private static GroupReader createReader(InputStream is) throws IOException {
		char[] guessBuffer = new char[GUESS_BUFFER_SIZE];
		BufferedReader br = new BufferedReader(new InputStreamReader(is, "ASCII"));
		br.mark(GUESS_BUFFER_SIZE+1);
		int guessCharCount = br.read(guessBuffer);
		
		if(guessCharCount < 1) {
			throw new IOException("File empty or almost empty.");
		}

		String guessString = new String(guessBuffer, 0, guessCharCount);
		
		if(guessString.startsWith("% RDS hexgroups") ||
				guessString.startsWith("<recorder=\"RDS Spy\"") ||
				HEXGROUP_PATTERN.matcher(guessString).matches()) {
			br.reset();
			return new HexFileGroupReader(br);
		} else {
			// binary file
			// do not call br.close()!
			br.reset();
			return new BitStreamSynchronizer(System.out, new BinaryFileBitReader(is));
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
