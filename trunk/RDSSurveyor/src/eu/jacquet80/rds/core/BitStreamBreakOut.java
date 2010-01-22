package eu.jacquet80.rds.core;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import eu.jacquet80.rds.input.BinStringFileBitReader;
import eu.jacquet80.rds.input.BitReader;

public class BitStreamBreakOut {
	public static void main(String[] args) throws FileNotFoundException {
		BitReader reader = new BinStringFileBitReader(new File(args[0]));
		int block = 0;
		StringBuffer strBlock = new StringBuffer();
		
		while(true) {
			boolean bit = false;
			try {
				bit = reader.getBit();
			} catch(IOException e) {
				return;
			}
			
			block = (block << 1) & 0x3FFFFFF;
			if(bit) block |= 1;
			strBlock.append(bit ? '1' : '0');
			int synd = RDS.calcSyndrome(block);
			
			for(int i=0; i<4; i++) {
				for(int j=0; j<2; j++) {
					if(synd == StreamLevelDecoder.syndromes[i][j]) {
						if(strBlock.length() > 26) {
							System.out.print(strBlock.substring(0, strBlock.length() - 26 - 1));
							System.out.print(strBlock.substring(strBlock.length() - 26));
						} else {
							System.out.print(strBlock);
						}
						strBlock.setLength(0);
						
						System.out.print(" " + (char)('A' + i));
						System.out.println((j==1) ? "'" : "");
						break;
					}
				}
			}
		}
	}
}
