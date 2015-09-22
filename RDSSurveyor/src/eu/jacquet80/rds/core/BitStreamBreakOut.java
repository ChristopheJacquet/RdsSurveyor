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

package eu.jacquet80.rds.core;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import eu.jacquet80.rds.input.BinStringFileBitReader;
import eu.jacquet80.rds.input.BitReader;

public class BitStreamBreakOut {
	public static void main(String[] args) throws IOException {
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
					if(synd == RDS.syndromes[i][j]) {
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
