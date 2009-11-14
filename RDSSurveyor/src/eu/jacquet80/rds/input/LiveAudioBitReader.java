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

package eu.jacquet80.rds.input;

import java.io.IOException;
import java.util.concurrent.Semaphore;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.TargetDataLine;

public class LiveAudioBitReader implements BitReader {
	private final static int frameLength = 2000;
	private static byte[] 
	                    decodeFrame = new byte[2 * frameLength], 
	                    receiveFrame = new byte[2 * frameLength];
	private int pos = 2 * frameLength;
	private long globalPos = 0; //, lastPos = 0;
	//private final AudioInputStream ais;
	//private final int frameSize;
	//private byte prevClock = 0;
	private static final float SAMPLE_RATE = 8000f; //11025f; //11025f;
	private static final Semaphore semReady = new Semaphore(0);
	private TargetDataLine line;
	
	private final int LATEST_WINDOW_LEN = (int)(SAMPLE_RATE *.5f); // length .5s
	private final byte[] latestData = new byte[LATEST_WINDOW_LEN];
	private int latestDataSum = 0;
	private int latestPos = 0;
	
	private final byte[] latestClock = new byte[LATEST_WINDOW_LEN];
	private int latestClockSum = 0;
	
	
	// need to be here, because CLOCK MUST NOT BE RESET
	byte data, clock = -1;
	
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

		new Thread() {
			public void run() {
				for(;;) {
					line.read(receiveFrame, 0, 2 * frameLength);
					// swap frames
					byte[] temp = receiveFrame;
					receiveFrame = decodeFrame;
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

		do {
			prevClock = clock;
			
			// possibly wait for a frame to be ready
			if(pos >= 2 * frameLength) {
				//System.out.print("<W>");
				try {
					semReady.acquire();
				} catch (InterruptedException e) {
					System.err.println("InterruptedException.");
				}
				pos = 0;
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
			
			
			//if(latestPos == 0)
			//	System.out.print("\nClock: " + clockAvg + " l=" + clock + ", data: " + dataAvg + " l=" + data);

			
			//System.out.print("[" + clock + "/" + data + "|" + prevClock + "]");
			pos += 2;
			globalPos += 2;
		} while(! (prevClock >= clockAvg && clock < clockAvg) );   /// (prevClock >= 0) && (clock < 0));   ///
		//long delta = (globalPos - lastPos);
		//System.out.print("avg=" + clockAvg + ", " + dataAvg + "  " + (delta == 18 || delta == 20 ? "(" + delta + ")" : "{%" + delta + "%}"));
		//lastPos = globalPos;
		return data > dataAvg;   /// data > 0; ///
	}

}
