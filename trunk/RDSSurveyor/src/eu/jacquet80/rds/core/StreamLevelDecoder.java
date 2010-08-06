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
import java.io.EOFException;
import java.io.IOException;
import java.io.PrintStream;
import java.util.LinkedList;

import eu.jacquet80.rds.input.BitReader;
import eu.jacquet80.rds.input.GroupReader;
import eu.jacquet80.rds.input.group.GroupEvent;
import eu.jacquet80.rds.input.group.GroupReaderEvent;


public class StreamLevelDecoder implements GroupReader {
	private final static int SYNC_THRESHOLD = 2;  // need 2 blocks after initial block to confirm synchronization
	private final static int SYNC_CONFIRM_DURATION = 5;  // 3 blocks in 5 groups
	private final static int SYNC_LOSS_DURATION = 10;    // lose synchronization if 10 groups without a good syndrome

	private final PrintStream console;
	//private final Log log;
	private final BitReader reader;
	private BitInversion inversion = BitInversion.AUTO;
	
	private int block = 0;        // block contents
	private int blockCount = 0;   // block counter within group
	private int bitCount = 0;     // bit count within block
	private int[] group = {0, 0, 0, 0};   // group
	private boolean synced = false;
	private int nbOk = 0;
	private boolean[] blocksOk = {false, false, false, false};
	private int nbUnsync = 0;
	private int groupCount = 0;
	private int bitTime = 0;
	private boolean negativePolarity = false;
	private @SuppressWarnings("unchecked") LinkedList<Integer> nbSyncAtOffset[][][] = new LinkedList[26][4][2];
	
	
	public StreamLevelDecoder(PrintStream console, BitReader reader) {
		this.console = console;
		//this.log = log;
		this.reader = reader;
		
		eraseSyncArray(nbSyncAtOffset);
	}
	
	private final static void eraseSyncArray(LinkedList<Integer> nbSyncAtOffset[][][]) {
		for(int i=0; i<nbSyncAtOffset.length; i++)
			for(int j=0; j<nbSyncAtOffset[i].length; j++)
				for(int k=0; k<nbSyncAtOffset[i][j].length; k++)
					nbSyncAtOffset[i][j][k] = new LinkedList<Integer>();
	}
	
	@Override
	public GroupReaderEvent getGroup() throws IOException, EndOfStream {
		while(true) {
			// read bit and add it to stream
			boolean bit = false;
			try {
				bit = reader.getBit();
			} catch(EOFException e) {
				throw new EndOfStream();
			}
			block = (block << 1) & 0x3FFFFFF;
			if(bit) block |= 1;
			bitCount++;
			bitTime++;
						
			if(! synced) {
				int[] synd = { RDS.calcSyndrome(block), RDS.calcSyndrome(~block) };
				
				console.print(".");

				for(int i=0; i<4; i++) {
					for(int j=0; j<2; j++) {
						if(j==0 && inversion == BitInversion.INVERT ||
								j==1 && inversion == BitInversion.NOINVERT) continue;
						if(synd[j] == RDS.syndromes[i][0] || synd[j] == RDS.syndromes[i][1]) {
							int offset = bitTime % 26;
							int pseudoBlock = (bitTime / 26 + 4 - i) % 4;

							console.print("[" + (j==0 ? "+" : "-") + ((char)('A'+i)) + ":" + offset + "/" + pseudoBlock + "]");

							// add current time to the list of syndrome hits
							nbSyncAtOffset[offset][pseudoBlock][j].addLast(bitTime);

							// weed out out-of-time hits
							while(nbSyncAtOffset[offset][pseudoBlock][j].getFirst() < bitTime - SYNC_CONFIRM_DURATION * 104)
								nbSyncAtOffset[offset][pseudoBlock][j].removeFirst();

							// are we above threshold
							if(nbSyncAtOffset[offset][pseudoBlock][j].size() > SYNC_THRESHOLD) {
								synced = true;
								eraseSyncArray(nbSyncAtOffset);

								group[i] = (block >> 10) & 0xFFFF;
								blockCount = (i+1) % 4;
								bitCount = 0;
								nbOk = 1;
								for(int k=0; k<4; k++) blocksOk[k] = (k == i);
								negativePolarity = (j==1);
								
								if(negativePolarity) group[i] = ~ group[i];
								
								console.println("\nGot synchronization on block " + (char)('A' + i) + "! (" + (j==0 ? "positive" : "negative") + " polarity)");
								console.print("      ");
								for(int k=0; k<i; k++) console.print(".");
								console.print("S");
								if(blockCount == 0) console.println();
							}
							break;

						}
					}
				}
			} else {   // if synced
				if(bitCount == 26) {
					if(negativePolarity) block = ~block;    // invert block if polarity is negative
					group[blockCount] = (block>>10) & 0xFFFF;
					int synd = RDS.calcSyndrome(block);

					if(synd == RDS.syndromes[blockCount][0] || synd == RDS.syndromes[blockCount][1]) {
						nbOk++;
						blocksOk[blockCount] = true;
						if(synd == RDS.syndromes[blockCount][0]) console.print("G");   // type A offset word
						else console.print("g");   // type B offset word (for group C)
					} else {
						blocksOk[blockCount] = false;
						group[blockCount] = -1;
						console.print(".");
						/*
						This is a placeholder for error correction code
						
						int syndrome = (synd ^ syndromes[blockCount][0])>>6;///
						
						int nbErrors = 1;
						if(nbErrors <= 0) {
							block = RDS.correct(block, syndrome); ///
							group[blockCount] = (block>>10) & 0xFFFF; ///
							blocksOk[blockCount] = true;
							output.print("c");
						} else {
							blocksOk[blockCount] = false;
							output.print(".");
						}
						*/
					}
					
					//console.printf("-%07X>%04X+%03X ", block, group[blockCount], synd);
					
					bitCount = 0;
					blockCount++;
					
					// end of group?
					if(blockCount > 3) {
						console.print(" ");
						groupCount++;
						
						blockCount = 0;
						
						if(nbOk > 0) nbUnsync = 0; else nbUnsync++;
						
						// after a while without a correct block, decide we have lost synchronization
						if(nbUnsync > SYNC_LOSS_DURATION) {
							synced = false;
							//groupLevelDecoder.loseSync();
							//TODO: need a means to inform a group decoder of a sync loss?
							console.println(" Lost synchronization.");
						}
						
						
						
						//if(log != null) log.notifyGroup();

						//console.println();
						//console.printf("%04d: ", bitTime / 26);

						nbOk = 0;
						
						// return group data
						int[] theGroup = new int[4];
						System.arraycopy(group, 0, theGroup, 0, 4);
						return new GroupEvent(bitTime, theGroup, false);
						//groupLevelDecoder.processGroup(nbOk, blocksOk, group, bitTime);
					}
				}
			}
		}
	}
	
	public void forceInversion(BitInversion inversion) {
		this.inversion = inversion;
	}
	
	
	public static enum BitInversion {
		AUTO, INVERT, NOINVERT;
	}
}