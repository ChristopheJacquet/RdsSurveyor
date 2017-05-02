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
import java.io.IOException;
import java.util.concurrent.ArrayBlockingQueue;

import biz.source_code.dsp.filter.FilterCharacteristicsType;
import biz.source_code.dsp.filter.FilterPassType;
import biz.source_code.dsp.filter.IirFilter;
import biz.source_code.dsp.filter.IirFilterCoefficients;
import biz.source_code.dsp.filter.IirFilterDesignFisher;

public class AudioBitReader extends BitReader {
	private static final boolean DEBUG = false; // set to true to enable debug output
	
	/** RDS carrier frequency */
	private static final double FC_0 = 57000.0;
	
	/** Input buffer length, in samples */
	private static final int IBUFLEN = 4096;
	
	/** Output buffer length for decoded data bits */
	private static final int OBUFLEN = 128;
	
	/** The input stream */
	private final DataInputStream in;
	
	/** Sample rate, or frames per second */
	private final int sampleRate;
	
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
		new Thread() {
			public void run() {
				// Array of audio samples retrieved, IBUFLEN samples, 16 bits (2 bytes) per sample
				short sample[]        = new short[IBUFLEN];
				double fsc = FC_0;

				double subcarr_phi    = 0;
				double subcarr_bb[]   = new double[] {0, 0};
				double clock_offset   = 0;
				double clock_phi      = 0;
				double lo_clock       = 0;
				double prevclock      = 0;
				double prev_bb        = 0;
				double d_phi_sc       = 0;
				double d_cphi         = 0;
				double acc            = 0;
				int bytesread; // note that this is really the number of samples (16-bit), not bytes

				int numsamples = 0;
				
				IirFilterCoefficients lp2400Coeffs = IirFilterDesignFisher.design(FilterPassType.lowpass,
						FilterCharacteristicsType.butterworth, 5, 0, 2000.0 / sampleRate, 2000.0 / sampleRate);
				
				IirFilterCoefficients lpPllCoeffs = IirFilterDesignFisher.design(FilterPassType.lowpass,
						FilterCharacteristicsType.butterworth, 1, 0, 2200.0 / sampleRate, 2200.0 / sampleRate);

				IirFilter lp2400iFilter = new IirFilter(lp2400Coeffs);
				IirFilter lp2400qFilter = new IirFilter(lp2400Coeffs);
				IirFilter lpPllFilter = new IirFilter(lpPllCoeffs);
				
				// for debugging only
				short outbuf;
				Process pU;
				Process pIQ;
				Process pRaw;
				String[] cmdU = {"sox", "-c", "5", "-r", Integer.toString(sampleRate), "-t", ".s16", "-", "dbg-out.wav"};
				String[] cmdIQ = {"sox", "-c", "2", "-r", Integer.toString(sampleRate), "-t", ".s16", "-", "dbg-out-iq.wav"};
				String[] cmdRaw = {"sox", "-c", "1", "-r", Integer.toString(sampleRate), "-t", ".s16", "-", "dbg-out-raw.wav"};
				DataOutputStream outU = null;
				DataOutputStream outIQ = null;
				DataOutputStream outRaw = null;
				
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
					
					/* From Oona Räisänen's original code, not implemented here for the moment */
					/*
					FILE *STATS;
					STATS = fopen("stats.csv", "w");
					fprintf(STATS, "t,fp,d_phi_sc,clock_offset,qua\n");
					*/
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

					int i;
					for (i = 0; i < bytesread; i++) {
						if (DEBUG) {
							if (outRaw != null)
								try {
									outRaw.writeShort(Short.reverseBytes(sample[i]));
								} catch (IOException e) {
									e.printStackTrace();
								}
						}

						/* Subcarrier downmix & phase recovery */

						subcarr_phi    += 2 * Math.PI * fsc * (1.0/sampleRate);
						subcarr_bb[0]  = lp2400iFilter.step(sample[i] / 32768.0 * Math.cos(subcarr_phi));
						subcarr_bb[1]  = lp2400qFilter.step(sample[i] / 32768.0 * Math.sin(subcarr_phi));

						double pll_beta = 50;

						d_phi_sc = lpPllFilter.step(subcarr_bb[1] * subcarr_bb[0]);
						subcarr_phi -= pll_beta * d_phi_sc;
						fsc         -= 0.5 * pll_beta * d_phi_sc;

						/* 1187.5 Hz clock */

						clock_phi = subcarr_phi / 48.0 + clock_offset;
						lo_clock  = ((clock_phi % (2 * Math.PI)) < Math.PI ? 1 : -1);

						if (DEBUG) {
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
						}

						/* Clock phase recovery */

						if (sign(prev_bb) != sign(subcarr_bb[0])) {
							d_cphi = clock_phi % Math.PI;
							if (d_cphi >= (Math.PI / 2)) d_cphi -= Math.PI;
							clock_offset -= 0.005 * d_cphi;
						}

						if (DEBUG) {
							/* dbg-out.wav channel 3: acc */
							outbuf = (short) (acc * 800);
							if (outU != null)
								try {
									outU.writeShort(Short.reverseBytes(outbuf));
								} catch (IOException e) {
									e.printStackTrace();
								}
							sbit = 0;
						}

						/* Decimate band-limited signal */
						if (numsamples % 8 == 0) {

							/* biphase symbol integrate & dump */
							acc += subcarr_bb[0] * lo_clock;

							if (sign(lo_clock) != sign(prevclock)) {
								biphase(acc);
								acc = 0;
							}

							prevclock = lo_clock;
						}
						
						numsamples++;

						if (DEBUG) {
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
							
							/* From Oona Räisänen's original code, not implemented here for the moment */
							/*
							t += 1.0/FS;
							if (numsamples % 125 == 0)
								fprintf(STATS,"%f,%f,%f,%f,%f\n",
										t,fsc,d_phi_sc,clock_offset,qua);
							*/
						}

						prev_bb = subcarr_bb[0];
					}
				}
			}
		}.start();
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
}
