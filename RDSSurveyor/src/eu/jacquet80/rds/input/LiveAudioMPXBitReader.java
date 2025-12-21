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
		// Try to open audio line with different configurations
		AudioFormat audioFormat = null;
		int channels = 1; // Track which configuration worked

		// Try different sample rates and channel configurations
		int[] sampleRatesToTry = {sampleRate, 192000, 96000, 48000};
		int[] channelsToTry = {1, 2}; // mono first, then stereo

		LineUnavailableException lastException = null;

		for (int sr : sampleRatesToTry) {
			for (int ch : channelsToTry) {
				try {
					audioFormat = new AudioFormat(
						AudioFormat.Encoding.PCM_SIGNED,
						sr,
						16,      // 16-bit samples
						ch,      // channels
						ch * 2,  // frame size (2 bytes per sample * channels)
						sr,
						false    // little-endian
					);

					DataLine.Info info = new DataLine.Info(TargetDataLine.class, audioFormat);
					line = (TargetDataLine) AudioSystem.getLine(info);
					line.open(audioFormat);

					// Success!
					this.sampleRate = sr;
					channels = ch;
					System.out.println("Opened audio line: " + sr + " Hz, " +
					                   (ch == 1 ? "mono" : "stereo") + ", 16-bit");
					break;
				} catch (LineUnavailableException e) {
					lastException = e;
					if (line != null) {
						line.close();
						line = null;
					}
					// Try next configuration
				}
			}
			if (line != null) break; // Found a working configuration
		}

		if (line == null) {
			System.err.println("Unable to get a recording line for MPX audio input");
			if (lastException != null) {
				lastException.printStackTrace();
			}
			throw new IOException("Cannot open audio line with any supported format. " +
			                      "Tried sample rates: 192kHz, 96kHz, 48kHz in mono and stereo.");
		}

		final int finalChannels = channels;

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
					byte[] monoBuffer = new byte[4096]; // For converting stereo to mono
					try {
						while (true) {
							int bytesRead = line.read(buffer, 0, buffer.length);
							if (bytesRead > 0) {
								if (finalChannels == 2) {
									// Convert stereo to mono by taking only the left channel
									int monoBytes = 0;
									for (int i = 0; i < bytesRead; i += 4) {
										// Copy left channel (first 2 bytes of each frame)
										monoBuffer[monoBytes++] = buffer[i];
										monoBuffer[monoBytes++] = buffer[i + 1];
										// Skip right channel (bytes i+2 and i+3)
									}
									outputStream.write(monoBuffer, 0, monoBytes);
								} else {
									// Already mono, write directly
									outputStream.write(buffer, 0, bytesRead);
								}
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
