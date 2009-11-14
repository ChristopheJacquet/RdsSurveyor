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
import java.util.LinkedList;

import eu.jacquet80.rds.input.BitReader;
import eu.jacquet80.rds.log.Log;


public class StreamLevelDecoder {
	//private final static int STATE_NOT_SYNC = 0, STATE_SYNC = 100;
	private final static int SYNC_THRESHOLD = 2;  // need 2 blocks after initial block to confirm synchronization
	private final static int SYNC_CONFIRM_DURATION = 5;  // 3 blocks in 5 groups
	private final static int SYNC_LOSS_DURATION = 10;    // lose synchronization if 10 groups without a good syndrome
	

	private final static int syndromes[][] = {{0xF600, 0xF600}, {0xF500, 0xF500}, {0x9700, 0xF300}, {0x9600, 0x9600}};

	
	private final static void eraseSyncArray(LinkedList<Integer> nbSyncAtOffset[][]) {
		for(int i=0; i<nbSyncAtOffset.length; i++)
			for(int j=0; j<nbSyncAtOffset[i].length; j++)
				nbSyncAtOffset[i][j] = new LinkedList<Integer>();
	}
	
	private final GroupLevelDecoder groupLevelDecoder = new GroupLevelDecoder();
		
	public void processStream(BitReader reader, Log log) throws IOException { 
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
		
		eraseSyncArray(nbSyncAtOffset);
		
		while(true) {
			// read bit and add it to stream
			boolean bit = false;
			try {
				bit = reader.getBit();
			} catch(EOFException e) {
				return;
			}
			block = (block << 1) & 0x3FFFFFF;
			if(bit) block |= 1;
			bitCount++;
			bitTime++;
			
			///
			/*
			System.out.print(".");
			int syndr = calcSyndrome(block);
			for(int i=0; i<4; i++)
				if(syndr == syndromes[i]) System.out.println(i+1);
			if(true) continue;
			*/
			///
			
			if(! synced) {
				///System.out.printf("%07X " , block);
				///if(bitCount >1000 ) System.exit(0);
				
				int synd = RDS.calcSyndrome(block);
				
				///System.out.printf("(%04X)", synd); 
				System.out.print(".");

				for(int i=0; i<4; i++) {
					if(synd == syndromes[i][0] || synd == syndromes[i][1]) {
						System.out.print("[" + (bitTime%26) + "/" + ((bitTime/26+4-i)%4) + "]");
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
						
						
						///syncState = 1;
						group[i] = (block>>10) & 0xFFFF;
						blockCount = (i+1) % 4;
						bitCount = 0;
						nbOk = 1;
						for(int j=0; j<4; j++) blocksOk[j] = false;
						blocksOk[i] = true;
						System.out.println("\nGot synchronization on block " + (char)('A' + i) + "!");
						System.out.print("      ");
						for(int j=0; j<i; j++) System.out.print(".");
						System.out.print("S");
						if(blockCount == 0) System.out.println();
						}
						break;
						
					}
				}
			} else {   // if synced
				if(bitCount == 26) {
					group[blockCount] = (block>>10) & 0xFFFF;
					int synd = RDS.calcSyndrome(block);
					///System.out.printf("(%04X)", synd);
					if(synd == syndromes[blockCount][0] || synd == syndromes[blockCount][1]) {
						nbOk++;
						blocksOk[blockCount] = true;
						if(synd == syndromes[blockCount][0]) System.out.print("G");   // type A offset word
						else System.out.print("g");   // type B offset word (for group C)
					} else {
						//System.out.println("<" + synd + ", " + syndromes[blockCount] + ">");
						//int blockSansOffset = (block & 0x3FFFC00) | (((block & 0x3FF) + 0x3FF - RDS.offsetWords[blockCount]) & 0x3FF);
					///	int blockSansOffset = block ^ RDS.offsetWords[blockCount];
					///	int syndrome = RDS.calcSyndrome(blockSansOffset)>>6; //((synd>>6) - (syndromes[blockCount]>>6) + 0x3FF) % 0x400;
						int syndrome = (synd ^ syndromes[blockCount][0])>>6;///
						
						//System.out.printf("   Block: %07X, BlockSO: %07X, Synd: %03X\n", block, blockSansOffset, syndrome);
						
						int nbErreurs = 1; //RDS.nbErrors(syndrome);
						if(nbErreurs <= 0) {
						///	blockSansOffset = RDS.correct(blockSansOffset, syndrome);
							block = RDS.correct(block, syndrome); ///
							//System.out.print("<corr: " + RDS.calcSyndrome(blockSansOffset) + "/" + syndromes[blockCount] + ">");
							group[blockCount] = (block>>10) & 0xFFFF; ///
							blocksOk[blockCount] = true;
							System.out.print("c");
						} else {
							blocksOk[blockCount] = false;
							System.out.print(".");
						}
						/// SI ERRCOR System.out.print(nbErreurs<10 ? nbErreurs : "+");
					}
					// SI ERRCOR System.out.print(" ");
					
					bitCount = 0;
					blockCount++;
					
					// end of group?
					if(blockCount > 3) {
						System.out.print(" ");
						groupCount++;
						
						blockCount = 0;
						
						if(nbOk > 0) nbUnsync = 0; else nbUnsync++;
						
						/*if(!synced) {
							if(syncState > SYNC_THRESHOLD) {
								syncState = STATE_SYNC;
								System.out.print(" Confirmed synchronization. ");
							}
						}*/
						
						// after a while without a correct block, decide we have lost synchronization
						if(nbUnsync > SYNC_LOSS_DURATION) {
							synced = false;
							groupLevelDecoder.loseSync();
							System.out.println(" Lost synchronization.");
						}
						
						
						// process group data
						if(log != null)
							groupLevelDecoder.processGroup(nbOk, blocksOk, group, bitTime, log);
							//currentStation = new TunedStation();
							//timeLine.update();
						//}

						//if(groupCount % 10 == 0) System.out.println(data);
						

						log.notifyGroup();

						System.out.println();
						System.out.printf("%04d: ", bitTime / 26);
						///if(bitTime/26 >= 64000) ((DumpFileBitReader)reader).setLogOn();
						///if(bitTime/26 > 64600) { ((DumpFileBitReader)reader).setLogOff();}

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
