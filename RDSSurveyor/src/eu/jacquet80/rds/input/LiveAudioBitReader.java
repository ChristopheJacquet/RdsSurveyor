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

package eu.jacquet80.rds.input;

import java.io.IOException;
import java.util.concurrent.Semaphore;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.TargetDataLine;

import eu.jacquet80.rds.core.RDS;
import eu.jacquet80.rds.util.NumberRingBuffer;

public class LiveAudioBitReader extends BitReader {
	private final static int frameLength = 2000;
	private static byte[] 
	                    decodeFrame = new byte[2 * frameLength], 
	                    receiveFrame = new byte[2 * frameLength];
	private int pos = 2 * frameLength;
	//private long globalPos = 0; //, lastPos = 0;
	//private final AudioInputStream ais;
	//private final int frameSize;
	//private byte prevClock = 0;
	private static final float SAMPLE_RATE = 8000f; //11025f; //11025f;
	private static final Semaphore semReady = new Semaphore(0);
	
	private static final int LOWEST_THEORICAL_BIT_DURATION = (int)Math.floor(SAMPLE_RATE / RDS.RDS_BITRATE);
	
	private TargetDataLine line;
	
	private final int LATEST_WINDOW_LEN = (int)(SAMPLE_RATE *.5f); // length .5s
	private final byte[] latestData = new byte[LATEST_WINDOW_LEN];
	private int latestDataSum = 0;
	private int latestPos = 0;
	
	private final byte[] latestClock = new byte[LATEST_WINDOW_LEN];
	private int latestClockSum = 0;
	
	//private int latestBitPos = 0, latestBitPosOffset = 0;
	
	
	// need to be here, because CLOCK MUST NOT BE RESET
	byte data, clock = -1;
	
	boolean processingComplete = true;
	
	private final int BITLENGTHS_WINDOW_LEN = 3000;
	private final NumberRingBuffer bitLengths = new NumberRingBuffer(BITLENGTHS_WINDOW_LEN);
	
	public LiveAudioBitReader() throws IOException {
		/*
		try {
			ais = AudioSystem.getAudioInputStream(new File(""));
		} catch(UnsupportedAudioFileException e) {
			throw new IOException(e.toString());
		}
		*/
		
		AudioFormat	audioFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, SAMPLE_RATE, 8, 2, 2, SAMPLE_RATE, false);

		DataLine.Info info = new DataLine.Info(TargetDataLine.class, audioFormat, 40000);

		line = null;


		try
		{
			line = (TargetDataLine) AudioSystem.getLine(info);
			line.open(audioFormat);
		}
		catch (LineUnavailableException e)
		{
			System.out.println("unable to get a recording line");
			e.printStackTrace();
			System.exit(1);
		}
		
		//frameSize = audioFormat.getFrameSize();
		
		line.start();

		new Thread("LiveAudioBitReader") {
			public void run() {
				for(;;) {
					line.read(receiveFrame, 0, 2 * frameLength);
					// swap frames
					byte[] temp = receiveFrame;
					receiveFrame = decodeFrame;
					
					synchronized(this) {
						if(!processingComplete) {
							System.err.println("Warning, decoding not done in real-time, you will get errors.");
						}
					}
					
					decodeFrame = temp;
					
					// signal
					semReady.release();
					//System.out.print("(*)");
				}
			}
		}.start();
		
		
		/*
		//AudioFormat format = ais.getFormat();
		format = new AudioFormat(format.getEncoding(), format.getSampleRate(), format.getSampleSizeInBits(), format.getChannels(), format.getFrameSize(), format.getFrameRate(), true);
		
		System.out.println("length = " + ais.getFrameLength() + " samples, format: " + format);
		
		frameLength = ais.getFrameLength();

		frameSize = format.getFrameSize();
		*/
		
		
		/*
		byte[] smpl = new byte[frameSize];

		for(; pos < frameLength; pos++) {
			ais.read(smpl, 0, frameSize);
			int data = (0xFF & (int)smpl[0]) + ((int)smpl[1]) * 256;
			int clock = (0xFF & (int)smpl[2]) + ((int)smpl[3]) * 256;
			System.out.print(clock + "|" + data + "/ ");
		}
		*/
	}
	
	
	public boolean getBit() throws IOException {
		//byte[] smpl = new byte[frameSize];
		float clockAvg, dataAvg;
		byte prevClock;
		int bitDuration = 0;

		do {
			prevClock = clock;
			
			// possibly wait for a frame to be ready
			if(pos >= 2 * frameLength) {
				//System.out.print("<W>");
				
				synchronized(this) { processingComplete = true; }
				
				try {
					semReady.acquire();
				} catch (InterruptedException e) {
					System.err.println("InterruptedException.");
				}
				pos = 0;
				//latestBitPosOffset = (int)(2 * SAMPLE_RATE);
				
				synchronized(this) { processingComplete = false; }
			}			
			
			data = decodeFrame[pos]; //(0xFF & (int)smpl[0]) + ((int)smpl[1]) * 256;
			clock = decodeFrame[pos+1]; //(0xFF & (int)smpl[2]) + ((int)smpl[3]) * 256;
			
			
			latestDataSum = latestDataSum - latestData[latestPos] + data;
			latestData[latestPos] = data;
			latestClockSum = latestClockSum - latestClock[latestPos] + clock;
			latestClock[latestPos] = clock;
			latestPos = (latestPos + 1) % LATEST_WINDOW_LEN;
			
			clockAvg = ((float)latestClockSum) / LATEST_WINDOW_LEN;
			dataAvg = ((float)latestDataSum) / LATEST_WINDOW_LEN;
						
			pos += 2;
			//globalPos += 2;
			bitDuration++;
		} while(! (prevClock <= clockAvg && clock > clockAvg && bitDuration >= LOWEST_THEORICAL_BIT_DURATION) );
		
		bitLengths.addValue(bitDuration);
		
		if(bitDuration < 6 || bitDuration > 7) System.out.println("!! WARNING: bit duration was " + bitDuration + ", theoretical " + SAMPLE_RATE / RDS.RDS_BITRATE);
		bitDuration = 0;

		return data > dataAvg;   /// data > 0; ///
	}


	
	public double getClockFrequency() {
		if(bitLengths.countValuesAdded() > 0) {
			return SAMPLE_RATE / bitLengths.getAverageValue();
		} else {
			return -1;
		}
	}
}
