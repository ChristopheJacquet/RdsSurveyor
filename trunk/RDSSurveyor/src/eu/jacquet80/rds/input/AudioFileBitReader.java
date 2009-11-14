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

import java.io.File;
import java.io.IOException;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

public class AudioFileBitReader implements BitReader {
	private int pos = 0; //, lastPos = 0;
	private final long frameLength;
	private final AudioInputStream ais;
	private final int frameSize;
	private int prevClock = 0;
	
	public AudioFileBitReader(File file) throws IOException {
		try {
			ais = AudioSystem.getAudioInputStream(file);
		} catch(UnsupportedAudioFileException e) {
			throw new IOException(e.toString());
		}
		
		AudioFormat format = ais.getFormat();
		format = new AudioFormat(format.getEncoding(), format.getSampleRate(), format.getSampleSizeInBits(), format.getChannels(), format.getFrameSize(), format.getFrameRate(), true);
		
		System.out.println("length = " + ais.getFrameLength() + " samples, format: " + format);
		
		frameLength = ais.getFrameLength();

		frameSize = format.getFrameSize();
		
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
		//if(pos >= frameLength) throw new IOException("End of audio file.");

		byte[] smpl = new byte[frameSize];

		int data, clock = -1;
		do {
			prevClock = clock;
			ais.read(smpl, 0, frameSize);
			data = (0xFF & (int)smpl[0]) + ((int)smpl[1]) * 256;
			clock = (0xFF & (int)smpl[2]) + ((int)smpl[3]) * 256;
			pos++;
		} while(! (prevClock >= 0 && clock < 0) );
		//System.out.print("(" + (pos - lastPos) + ")");
		//lastPos = pos;
		return data > 0;
	}

}
