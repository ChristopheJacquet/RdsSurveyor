/*
 RDS Surveyor -- RDS decoder, analyzer and monitor tool and library.
 For more information see
   http://www.jacquet80.eu/
   http://rds-surveyor.sourceforge.net/
 
 Copyright (c) 2015 Michael von Glasow
 Portions Copyright (c) Oona Räisänen OH2EIQ (windyoona@gmail.com)

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
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PrintStream;
import java.util.concurrent.ArrayBlockingQueue;

import eu.jacquet80.rds.util.MathUtil;
import biz.source_code.dsp.filter.FilterCharacteristicsType;
import biz.source_code.dsp.filter.FilterPassType;
import biz.source_code.dsp.filter.IirFilter;
import biz.source_code.dsp.filter.IirFilterCoefficients;
import biz.source_code.dsp.filter.IirFilterDesignFisher;

public class AudioBitReader extends BitReader {
	private static final boolean DEBUG = false; // set to true to enable debug output
	
	/** RDS carrier frequency */
	private static final double FC_0 = 57000.0;
	
	/** 
	 * Tolerance of RDS subcarrier frequency.
	 * As per the specs, tolerance is +/- 6 Hz. We use twice the value to allow for some tolerance
	 * in the processing chain.
	 */
	private static final double FC_TOLERANCE = 12.0;
	
	/** Input buffer length, in samples */
	private static final int IBUFLEN = 4096;
	
	/** Output buffer length for decoded data bits */
	private static final int OBUFLEN = 128;
	
	/** The input stream */
	private final DataInputStream in;
	
	/** A stream from which other applications can retrieve audio data */
	private PipedInputStream audioMirrorSource;
	
	/** Stream to which audio input is mirrored as it is processed */
	private DataOutputStream audioMirrorSink = null;
	
	/** Whether audio is being mirrored */
	private boolean isPlaying = false;
	
	/** The sample rate for audio output (default 48k) */
	private int outSampleRate = 48000;
	
	/* Audio downsampling ratio */
	int inRatio;
	int outRatio;
	
	/* Counter for audio samples received and written */
	int inCount = 0;
	int outCount = 0;
	
	/** Input sample rate, or frames per second */
	private final int sampleRate;
	
	/** Decimation factor, determined based on the sample rate */
	private final int decimate;
	
	/** A queue for the bits decoded from the audio stream. */
	private final ArrayBlockingQueue<Boolean> bits = new ArrayBlockingQueue<Boolean>(OBUFLEN);

	/** Demodulated sample from RDS data stream (NRZ-M encoded) */
	private int dbit = 0;
	
	// Used by biphase()
	private double prev_acc = 0;
	private int counter = 0;
	private int reading_frame = 0;
	private int tot_errs[] = new int[] {0, 0};

	/* Decoded data bit from RDS stream, for debugging only */
	private int sbit;

	/**
	 * Creates a new AudioBitReader and starts decoding RDS date from it.
	 * 
	 * @param srate
	 */
	public AudioBitReader(DataInputStream stream, int srate) {
		this.in = stream;
		this.sampleRate = srate;
		this.decimate = this.sampleRate / 7125;
		this.audioMirrorSource = new PipedInputStream();
		try {
			this.audioMirrorSink = new DataOutputStream(new PipedOutputStream(audioMirrorSource));
		} catch (Exception e) {
			e.printStackTrace();
		}
		new Thread() {
			public void run() {
				/* Array of audio samples retrieved, IBUFLEN samples, 16 bits (2 bytes) per sample */
				short sample[]        = new short[IBUFLEN];
				
				/* Subcarrier frequency */
				double fsc = FC_0;

				/* Subcarrier phase */
				double subcarr_phi    = 0;
				
				double subcarr_bb[]   = new double[] {0, 0};
				
				/* Clock phase offset */
				double clock_offset   = 0;
				
				/* Clock phase */
				double clock_phi      = 0;
				
				double lo_clock       = 0;
				double prevclock      = 0;
				double prev_bb        = 0;
				
				/* Subcarrier phase error */
				double d_phi_sc       = 0;

				/* Clock phase error */
				double d_cphi         = 0;
				
				double acc            = 0;
				
				double pll_beta       = 50;

				/* Number of samples (NOT bytes) read */
				int bytesread;

				int numsamples = 0;
				int i;
				
				calculateResampleRatio();
			
				IirFilterCoefficients lp2400Coeffs = IirFilterDesignFisher.design(FilterPassType.lowpass,
						FilterCharacteristicsType.butterworth, 5, 0, 2000.0 / sampleRate, 2000.0 / sampleRate);
				
				IirFilterCoefficients lpPllCoeffs = IirFilterDesignFisher.design(FilterPassType.lowpass,
						FilterCharacteristicsType.butterworth, 1, 0, 2200.0 / sampleRate, 2200.0 / sampleRate);

				IirFilter lp2400iFilter = new IirFilter(lp2400Coeffs);
				IirFilter lp2400qFilter = new IirFilter(lp2400Coeffs);
				IirFilter lpPllFilter = new IirFilter(lpPllCoeffs);
				
				// for debugging only
				double t = 0;
				short outbuf;
				Process pU;
				Process pIQ;
				Process pRaw;
				String tempPath = "/tmp";
				String pathSep ="/";
				try {
					tempPath = System.getProperty("java.io.tmpdir", tempPath);
				} catch (Exception e) {
					// NOP
				}
				try {
					pathSep = System.getProperty("file.separator", pathSep);
				} catch (Exception e) {
					// NOP
				}
				String[] cmdU = {"sox", "-c", "5", "-r", Integer.toString(sampleRate), "-t", ".s16", "-", tempPath + pathSep + "dbg-out.wav"};
				String[] cmdIQ = {"sox", "-c", "2", "-r", Integer.toString(sampleRate), "-t", ".s16", "-", tempPath + pathSep + "dbg-out-iq.wav"};
				String[] cmdRaw = {"sox", "-c", "1", "-r", Integer.toString(sampleRate), "-t", ".s16", "-", tempPath + pathSep + "dbg-out-raw.wav"};
				DataOutputStream outU = null;
				DataOutputStream outIQ = null;
				DataOutputStream outRaw = null;
				PrintStream stats = null;
				
				if (DEBUG) {
					sbit = 0;
					dbit = 0;

					try {
						pU = new ProcessBuilder()
							.command(cmdU)
							.redirectErrorStream(true)
							.start();
						outU = new DataOutputStream(pU.getOutputStream());
					} catch (IOException e) {
						e.printStackTrace();
					}
					
					try {
						pIQ = new ProcessBuilder()
							.command(cmdIQ)
							.redirectErrorStream(true)
							.start();
						outIQ = new DataOutputStream(pIQ.getOutputStream());
					} catch (IOException e) {
						e.printStackTrace();
					}
					
					try {
						pRaw = new ProcessBuilder()
							.command(cmdRaw)
							.redirectErrorStream(true)
							.start();
						outRaw = new DataOutputStream(pRaw.getOutputStream());
					} catch (IOException e) {
						e.printStackTrace();
					}
					
					try {
						stats = new PrintStream(new File(tempPath, "stats.csv"));
						stats.print("t,fsc,d_phi_sc,subcarr_bb_re,subcarr_bb_im,clock_offset\n");
					} catch (FileNotFoundException e) {
						e.printStackTrace();
						stats = null;
					}
				}

				while (true) {
					bytesread = 0;
					while (bytesread < IBUFLEN) {
						try {
							sample[bytesread] = Short.reverseBytes(in.readShort());
							bytesread++;
						} catch (EOFException e) {
							break;
						} catch (IOException e) {
							System.err.println("IOException.");
							break;
						}
					}
					
					if (bytesread < 1) break;

					/* Reset audio counters periodically to prevent overflow */
					inCount %= inRatio;
					outCount %= outRatio;
					
					for (i = 0; i < bytesread; i++) {
						if (isPlaying && (audioMirrorSink != null))
							try {
								/* resample */
								inCount++;
								/* 
								 * if the downsampling ratio has not been exceeded yet
								 * (outCount * inRatio <= outRatio * inCount
								 * is just an integer-friendly and div-by-zero-proof representation of 
								 * outCount/inCount <= outRatio/inRatio)
								 */
								if (outCount * inRatio <= outRatio * inCount) {
									audioMirrorSink.writeShort(Short.reverseBytes(sample[i]));
									outCount++;
								}
							} catch (IOException e) {
								e.printStackTrace();
							}

						/* Subcarrier downmix & phase recovery */

						subcarr_phi    += 2 * Math.PI * fsc / (double) sampleRate;
						subcarr_bb[0]  = lp2400iFilter.step(sample[i] / 32768.0 * Math.cos(subcarr_phi));
						subcarr_bb[1]  = lp2400qFilter.step(sample[i] / 32768.0 * Math.sin(subcarr_phi));

						d_phi_sc = lpPllFilter.step(subcarr_bb[1] * subcarr_bb[0]);
						subcarr_phi -= pll_beta * d_phi_sc;
						fsc         -= 0.5 * pll_beta * d_phi_sc;
						
						/* Decimate band-limited signal */
						if (numsamples % decimate == 0) {
							/* Reset subcarrier frequency if it is outside tolerance range */
							if ((fsc > FC_0 + FC_TOLERANCE) || (fsc < FC_0 - FC_TOLERANCE)) {
								fsc = FC_0;
							}

							/* 1187.5 Hz clock */

							clock_phi = subcarr_phi / 48.0 + clock_offset;
							lo_clock  = ((clock_phi % (2 * Math.PI)) < Math.PI ? 1 : -1);

							/* Clock phase recovery */

							if (sign(prev_bb) != sign(subcarr_bb[0])) {
								d_cphi = clock_phi % Math.PI;
								if (d_cphi >= (Math.PI / 2)) d_cphi -= Math.PI;
								clock_offset -= 0.005 * d_cphi;
							}

							/* biphase symbol integrate & dump */
							acc += subcarr_bb[0] * lo_clock;

							if (sign(lo_clock) != sign(prevclock)) {
								biphase(acc);
								acc = 0;
							}

							prevclock = lo_clock;
							prev_bb = subcarr_bb[0];

							if (DEBUG) {
								if (outRaw != null)
									try {
										outRaw.writeShort(Short.reverseBytes(sample[i]));
									} catch (IOException e) {
										e.printStackTrace();
									}
								/* dbg-out.wav channel 1: d_phi_sc */
								outbuf = (short) (d_phi_sc * 6000);
								if (outU != null)
									try {
										outU.writeShort(Short.reverseBytes(outbuf));
									} catch (IOException e) {
										e.printStackTrace();
									}

								/* dbg-out.wav channel 2: 1187.5 Hz clock */
								outbuf = (short) (lo_clock * 16000);
								if (outU != null)
									try {
										outU.writeShort(Short.reverseBytes(outbuf));
									} catch (IOException e) {
										e.printStackTrace();
									}

								/* dbg-out-iq.wav channel 1 */
								outbuf = (short) (subcarr_bb[0] * 32000);
								if (outIQ != null)
									try {
										outIQ.writeShort(Short.reverseBytes(outbuf));
									} catch (IOException e) {
										e.printStackTrace();
									}

								/* dbg-out-iq.wav channel 2 */
								outbuf = (short) (subcarr_bb[1] * 32000);
								if (outIQ != null)
									try {
										outIQ.writeShort(Short.reverseBytes(outbuf));
									} catch (IOException e) {
										e.printStackTrace();
									}
								/* dbg-out.wav channel 3: acc */
								outbuf = (short) (acc * 800);
								if (outU != null)
									try {
										outU.writeShort(Short.reverseBytes(outbuf));
									} catch (IOException e) {
										e.printStackTrace();
									}
								/* dbg-out.wav channel 4: dbit (demodulated RDS stream) */
								outbuf = (short) (dbit * 16000);
								if (outU != null)
									try {
										outU.writeShort(Short.reverseBytes(outbuf));
									} catch (IOException e) {
										e.printStackTrace();
									}

								/* dbg-out.wav channel 5: sbit (decoded RDS data stream) */
								outbuf = (short) (sbit * 16000);
								if (outU != null)
									try {
										outU.writeShort(Short.reverseBytes(outbuf));
									} catch (IOException e) {
										e.printStackTrace();
									}

								t += 1.0/sampleRate;
								if ((stats != null) && (numsamples % (decimate * 16) == 0))
									// qua (quality) is not implemented so far
									stats.printf("%f,%f,%f,%f,%f,%f\n", t, fsc, d_phi_sc, subcarr_bb[0], subcarr_bb[1], clock_offset);
							}
						}
						
						numsamples++;
					}
				}
			}
		}.start();
	}
	
	/**
	 * @brief Returns a stream on which audio output is mirrored as it is processed.
	 * 
	 * To start receiving data from the stream, consumers must call {@link #startPlaying()}.
	 * Consumers who are no longer interested in audio data must call {@link #stopPlaying()}.
	 */
	public InputStream getAudioMirrorStream() {
		return audioMirrorSource;
	}

	/**
	 * @brief Returns the sample rate for audio output.
	 * @return the outSampleRate
	 */
	public int getAudioSampleRate() {
		return outSampleRate;
	}

	@Override
	public boolean getBit() throws IOException {
		boolean ret;
		while (true) {
			try {
				ret = bits.take();
				return ret;
			} catch (InterruptedException e) {
				System.err.println("InterruptedException.");
			}
		}
	}
	
	/**
	 * @brief Sets the sample rate for audio output.
	 * 
	 * Changes take effect immediately, and audio will be briefly interrupted.
	 *  
	 * @param outSampleRate the outSampleRate to set
	 */
	public synchronized void setAudioSampleRate(int outSampleRate) {
		boolean wasPlaying = isPlaying;
		stopPlaying();
		this.outSampleRate = outSampleRate;
		calculateResampleRatio();
		if (wasPlaying)
			startPlaying();
	}

	/**
	 * @brief Starts mirroring audio samples to the audio stream.
	 * 
	 * Consumers must call this method to receive audio data on the audio mirror stream.
	 * 
	 * The audio mirror stream can be obtained by calling {@link #getAudioMirrorStream()}.
	 */
	public synchronized void startPlaying() {
		inCount = 0;
		outCount = 0;
		isPlaying = true;
	}
	
	/**
	 * @brief Stops mirroring audio samples to the audio stream.
	 * 
	 * When consumers are no longer interested in audio data from the audio mirror stream, they
	 * must call this method.
	 * 
	 * The audio mirror stream can be obtained by calling {@link #getAudioMirrorStream()}.
	 */
	public synchronized void stopPlaying() {
		isPlaying = false;
	}
	
	/**
	 * Stores a value in the queue {@code bits}.
	 * 
	 * @param b The new bit received. If it is different from the last bit that was received, 1 is
	 * stored, else 0 is stored.
	 */
	private void storeValue(int b) {
		if (DEBUG) {
			sbit = (((b ^ dbit) != 0) ? 1 : -1);
		}
		try {
			bits.put((b ^ dbit) != 0);
		} catch (InterruptedException e) {
			System.err.println("InterruptedException.");
		}
		dbit = b;
	}

	private int sign(double a) {
		return (a >= 0 ? 1 : 0);
	}

	private void biphase(double acc) {
		if (sign(acc) != sign(prev_acc)) {
			tot_errs[counter % 2] ++;
		}

		if (counter % 2 == reading_frame) {
			storeValue(sign(acc + prev_acc));
		}
		if (counter == 0) {
			if (tot_errs[1 - reading_frame] < tot_errs[reading_frame]) {
				reading_frame = 1 - reading_frame;
			}
			tot_errs[0] = 0;
			tot_errs[1] = 0;
		}

		prev_acc = acc;
		counter = (counter + 1) % 800;
	}

	private void calculateResampleRatio() {
		/* Set up audio downsampling */
		int gcd = MathUtil.gcd(sampleRate, outSampleRate);
		inRatio = sampleRate / gcd;
		outRatio = outSampleRate / gcd;
		
		System.out.println(String.format("AudioBitReader: audio downsampling ratio set to %d:%d", inRatio, outRatio));
	}
}
