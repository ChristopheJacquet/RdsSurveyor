package eu.jacquet80.rds.input;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

public class HexFileGroupReader implements GroupReader {
	private final BufferedReader br;
	
	public HexFileGroupReader(File file) throws FileNotFoundException {
		br = new BufferedReader(new FileReader(file));
	}
	
	
	public int[] getGroup() throws IOException {
		String line = br.readLine();
		if(line == null) throw new IOException("End of file");
		
		String[] components = line.split("\\s");
		if(components.length < 4) throw new IOException("Not enough blocks on line \"" + line + "\"");
		int[] res = new int[4];
		
		for(int i=0; i<4; i++) {
			res[i] = Integer.parseInt(components[components.length-4+i], 16);
		}
		
		return res;
	}

}
