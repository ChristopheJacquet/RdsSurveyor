/*
 RDS Surveyor -- RDS decoder, analyzer and monitor tool and library.
 For more information see
   http://www.jacquet80.eu/
   http://rds-surveyor.sourceforge.net/
 
 Copyright (c) 2009 Christophe Jacquet

 Permission is hereby granted, free of charge, to any person
 obtaining a copy of this software and associated documentation
 files (the "Software"), to deal in the Software without
 restriction, including without limitation the rights to use,
 copy, modify, merge, publish, distribute, sublicense, and/or sell
 copies of the Software, and to permit persons to whom the
 Software is furnished to do so, subject to the following
 conditions:

 The above copyright notice and this permission notice shall be
 included in all copies or substantial portions of the Software.

 THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 OTHER DEALINGS IN THE SOFTWARE.
*/

package eu.jacquet80.rds.core;
import java.io.EOFException;
import java.io.IOException;
import java.io.PrintStream;
import java.util.LinkedList;

import eu.jacquet80.rds.input.BitReader;
import eu.jacquet80.rds.input.RDSReader;
import eu.jacquet80.rds.log.Log;
import eu.jacquet80.rds.log.StationLost;


public class StreamLevelDecoder implements RDSDecoder {
	private final static int SYNC_THRESHOLD = 2;  // need 2 blocks after initial block to confirm synchronization
	private final static int SYNC_CONFIRM_DURATION = 5;  // 3 blocks in 5 groups
	private final static int SYNC_LOSS_DURATION = 10;    // lose synchronization if 10 groups without a good syndrome

	final static int syndromes[][] = {{0xF600, 0xF600}, {0xF500, 0xF500}, {0x9700, 0xF300}, {0x9600, 0x9600}};

	private final PrintStream console;
	private final GroupLevelDecoder groupLevelDecoder;
	
	public StreamLevelDecoder(PrintStream console) {
		this.console = console;
		groupLevelDecoder = new GroupLevelDecoder(console);
	}
	
	private final static void eraseSyncArray(LinkedList<Integer> nbSyncAtOffset[][]) {
		for(int i=0; i<nbSyncAtOffset.length; i++)
			for(int j=0; j<nbSyncAtOffset[i].length; j++)
				nbSyncAtOffset[i][j] = new LinkedList<Integer>();
	}
		
	public void processStream(RDSReader rdsReader, Log log) throws IOException { 
		int block = 0;        // block contents
		int blockCount = 0;   // block counter within group
		int bitCount = 0;     // bit count within block
		int[] group = {0, 0, 0, 0};   // group
		boolean synced = false;
		int nbOk = 0;
		boolean[] blocksOk = {false, false, false, false};
		int nbUnsync = 0;
		//TunedStation currentStation = new TunedStation();
		int groupCount = 0;
		int bitTime = 0;
		@SuppressWarnings("unchecked") LinkedList<Integer> nbSyncAtOffset[][] = new LinkedList[26][4];
		
		BitReader reader = (BitReader) rdsReader;
		
		eraseSyncArray(nbSyncAtOffset);
		
		while(true) {
			// read bit and add it to stream
			boolean bit = false;
			try {
				bit = reader.getBit();
			} catch(EOFException e) {
				// when the end of stream is reached, must add log information about the last tuned station
				log.addMessage(new StationLost(groupLevelDecoder.getTunedStation().getTimeOfLastPI(), groupLevelDecoder.getTunedStation()));
				return;
			}
			block = (block << 1) & 0x3FFFFFF;
			if(bit) block |= 1;
			bitCount++;
			bitTime++;
						
			if(! synced) {
				int synd = RDS.calcSyndrome(block);
				
				console.print(".");

				for(int i=0; i<4; i++) {
					if(synd == syndromes[i][0] || synd == syndromes[i][1]) {
						console.print("[" + ((char)('A'+i)) + ":" + (bitTime%26) + "/" + ((bitTime/26+4-i)%4) + "]");
						int offset = bitTime % 26;
						int pseudoBlock = (bitTime / 26 + 4 - i) % 4;
						
						// add current time to the list of syndrome hits
						nbSyncAtOffset[offset][pseudoBlock].addLast(bitTime);
						
						// weed out out-of-time hits
						while(nbSyncAtOffset[offset][pseudoBlock].getFirst() < bitTime - SYNC_CONFIRM_DURATION * 104)
							nbSyncAtOffset[offset][pseudoBlock].removeFirst();
						
						// are we above threshold
						if(nbSyncAtOffset[offset][pseudoBlock].size() > SYNC_THRESHOLD) {
							synced = true;
							eraseSyncArray(nbSyncAtOffset);
						
							group[i] = (block>>10) & 0xFFFF;
							blockCount = (i+1) % 4;
							bitCount = 0;
							nbOk = 1;
							for(int j=0; j<4; j++) blocksOk[j] = false;
							blocksOk[i] = true;
							console.println("\nGot synchronization on block " + (char)('A' + i) + "!");
							console.print("      ");
							for(int j=0; j<i; j++) console.print(".");
							console.print("S");
							if(blockCount == 0) console.println();
						}
						break;
						
					}
				}
			} else {   // if synced
				if(bitCount == 26) {
					group[blockCount] = (block>>10) & 0xFFFF;
					int synd = RDS.calcSyndrome(block);

					if(synd == syndromes[blockCount][0] || synd == syndromes[blockCount][1]) {
						nbOk++;
						blocksOk[blockCount] = true;
						if(synd == syndromes[blockCount][0]) console.print("G");   // type A offset word
						else console.print("g");   // type B offset word (for group C)
					} else {
						blocksOk[blockCount] = false;
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
							groupLevelDecoder.loseSync();
							console.println(" Lost synchronization.");
						}
						
						
						// process group data
						groupLevelDecoder.processGroup(nbOk, blocksOk, group, bitTime, log);
						
						if(log != null) log.notifyGroup();

						console.println();
						console.printf("%04d: ", bitTime / 26);

						nbOk = 0;
					}
				}
			}
		}
	}
	
	public TunedStation getTunedStation() {
		return groupLevelDecoder.getTunedStation();
	}
}
