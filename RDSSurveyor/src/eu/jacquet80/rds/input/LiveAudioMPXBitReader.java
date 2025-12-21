/*
 RDS Surveyor -- RDS decoder, analyzer and monitor tool and library.
 For more information see
   http://www.jacquet80.eu/
   http://rds-surveyor.sourceforge.net/

 Copyright (c) 2009, 2010 Christophe Jacquet
 Copyright (c) 2025 - Live MPX audio input support

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

import java.io.DataInputStream;
import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.TargetDataLine;

/**
 * Live audio input for raw FM MPX (multiplex) audio.
 * This class handles real-time audio from the sound card and uses
 * AudioBitReader's demodulation to extract RDS data from the 57 kHz subcarrier.
 *
 * Supports audio from sources like StereoTool, MicroMPX, or any FM MPX generator.
 */
public class LiveAudioMPXBitReader extends BitReader {
	// Default to 192 kHz for high-quality MPX demodulation
	// Can be changed to 128k, 96k, or other rates depending on sound card capabilities
	private static final int DEFAULT_SAMPLE_RATE = 192000;

	private final AudioBitReader demodulator;
	private TargetDataLine line;
	private final int sampleRate;

	/**
	 * Creates a new live MPX audio input with the default sample rate (192 kHz).
	 * @throws IOException if the audio line cannot be opened
	 */
	public LiveAudioMPXBitReader() throws IOException {
		this(DEFAULT_SAMPLE_RATE);
	}

	/**
	 * Creates a new live MPX audio input with a specified sample rate.
	 * @param sampleRate the sample rate in Hz (recommended: 192000, 128000, or 96000)
	 * @throws IOException if the audio line cannot be opened
	 */
	public LiveAudioMPXBitReader(int sampleRate) throws IOException {
		this.sampleRate = sampleRate;

		// Set up audio format: mono, 16-bit PCM, little-endian
		AudioFormat audioFormat = new AudioFormat(
			AudioFormat.Encoding.PCM_SIGNED,
			sampleRate,
			16,      // 16-bit samples
			1,       // mono
			2,       // frame size (2 bytes per sample)
			sampleRate,
			false    // little-endian
		);

		DataLine.Info info = new DataLine.Info(TargetDataLine.class, audioFormat);

		try {
			line = (TargetDataLine) AudioSystem.getLine(info);
			line.open(audioFormat);
		} catch (LineUnavailableException e) {
			System.err.println("Unable to get a recording line for MPX audio input");
			e.printStackTrace();
			throw new IOException("Cannot open audio line: " + e.getMessage());
		}

		// Create a piped stream to feed audio data to the AudioBitReader demodulator
		try {
			PipedOutputStream pos = new PipedOutputStream();
			PipedInputStream pis = new PipedInputStream(pos);
			DataInputStream dis = new DataInputStream(pis);

			// Create the demodulator that will extract RDS from the MPX audio
			demodulator = new AudioBitReader(dis, sampleRate);

			// Start the audio capture thread
			final PipedOutputStream outputStream = pos;
			new Thread("LiveAudioMPXBitReader-Capture") {
				public void run() {
					line.start();
					byte[] buffer = new byte[4096];
					try {
						while (true) {
							int bytesRead = line.read(buffer, 0, buffer.length);
							if (bytesRead > 0) {
								outputStream.write(buffer, 0, bytesRead);
								outputStream.flush();
							}
						}
					} catch (IOException e) {
						System.err.println("Error reading from audio line: " + e.getMessage());
						e.printStackTrace();
					}
				}
			}.start();

		} catch (IOException e) {
			if (line != null) {
				line.close();
			}
			throw e;
		}
	}

	@Override
	public boolean getBit() throws IOException {
		// Delegate to the AudioBitReader demodulator
		return demodulator.getBit();
	}

	/**
	 * Closes the audio line and stops capture.
	 */
	public void close() {
		if (line != null) {
			line.stop();
			line.close();
		}
	}

	/**
	 * Gets the sample rate being used for audio capture.
	 * @return the sample rate in Hz
	 */
	public int getSampleRate() {
		return sampleRate;
	}
}
