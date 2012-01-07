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

import java.io.File;
import java.io.IOException;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

public class AudioFileBitReader extends BitReader {
	//private int pos = 0; //, lastPos = 0;
	//private final long frameLength;
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
		
		//frameLength = ais.getFrameLength();

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
			//pos++;
		} while(! (prevClock >= 0 && clock < 0) );
		//System.out.print("(" + (pos - lastPos) + ")");
		//lastPos = pos;
		return data > 0;
	}

}
