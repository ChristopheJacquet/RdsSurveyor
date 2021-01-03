/*
 RDS Surveyor -- RDS decoder, analyzer and monitor tool and library.
 For more information see
   http://www.jacquet80.eu/
   http://rds-surveyor.sourceforge.net/

 Copyright (c) 2009-2012 Christophe Jacquet

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

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.ClosedChannelException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import eu.jacquet80.rds.input.group.FrequencyChangeEvent;
import eu.jacquet80.rds.input.group.GroupEvent;
import eu.jacquet80.rds.input.group.GroupReaderEvent;
import eu.jacquet80.rds.log.RealTime;

/**
 * A {@code TunerGroupReader} which controls a GNS TMC dongle.
 * 
 * Due to the constraints of the hardware, and because little is known about the GNS protocol, this
 * reader is somewhat limited in its functionality:
 * 
 * There is no audio support (because the hardware presumably does not support audio).
 * 
 * No block error rate is reported because there is no known way to obtain it from the device.
 * Presumably error correction happens in the device and groups with faulty blocks are suppressed.
 * As a result, this reader will never report block errors, although the data rate may vary.
 * 
 * GNS devices are known to use one of two different command sets (which have some opcodes in
 * common but differ in others). Currently only the FM9 is supported, as we lack a way to reliably
 * identify the command set used by the device.
 * 
 * Communication with the device is through an {@link InputStream} (to which commands are sent) and
 * an {@link OutputStream} (which receives responses).
 * 
 * Some devices are pure TMC devices, which can be connected directly via Bluetooth or a USB-to-TTL
 * serial bridge and controlled through a serial communications library such as jSerialComm.
 * 
 * Other devices are combined GPS/TMC devices, which deliver GPS data via NMEA and insert responses
 * into the NMEA data stream. For these devices, it may be necessary to write wrappers which
 * extract TMC output from the data stream and provide it via an {@link OutputStream}, and accept
 * commands via an {@link InputStream}. This class has only rudimentary logic to discard invalid
 * data and accept only valid response sentences.
 */
public class GnsGroupReader extends TunerGroupReader implements Closeable {
	/** Unknown command set */
	private static final int CMD_SET_UNKNOWN = -1;

	/** FM9 command set */
	private static final int CMD_SET_FM9 = 0;

	/** 9830 command set */
	private static final int CMD_SET_9830 = 1;

	/** Command set names */
	private static final String[] CMD_SET_NAMES = {"TrafficBox FM9 family", "9830 (and related)"};

	/** Delimiter for commands */
	private static final int DELIM_COMMAND = 0xFF;

	/** Delimiter for responses */
	private static final int DELIM_RESPONSE = 0x3F;

	/** The display name for the device. */
	private static final String DEVICE_NAME = "GNS";

	/** Opcode to disable the device (same for all command sets). */
	private static final int OPCODE_DISABLE = 0x53;

	/** Opcode to enable the device (same for all command sets). */
	private static final int OPCODE_ENABLE = 0x56;

	/** Opcode to request an identification string (same for all command sets). */
	private static final int OPCODE_IDENTIFICATION = 0x43;

	/** Opcode to retrieve signal strength of current station */
	// TODO opcode for 9830 is not known
	private static final int[] OPCODE_RSSI = {0x6c, 0x6c};

	/** Opcode to seek forward to the next valid station. */
	private static final int[] OPCODE_SEEK_UP = {0x59, 0x79};

	/** Opcode to seek backward to the next valid station. */
	private static final int[] OPCODE_SEEK_DOWN = {0x58, 0x78};

	/** Opcode to report seek status. */
	private static final int[] OPCODE_SEEK_STATUS = {0x66, 0x66};

	/** Opcode to tune to a specified frequency. */
	private static final int[] OPCODE_TUNE = {0x73, 0x46};

	/** Whether this reader has been closed. */
	private boolean closed = false;

	/** The command set used by this device */
	private int cmdSet = CMD_SET_UNKNOWN;

	private GnsData gnsData = new GnsData();

	/** Stream which receives status and RDS data coming from the device */
	private InputStream in;

	/** Whether a flush operation is in progress */
	private boolean isFlushing = true;

	private boolean newGroups;

	/** Time at which we got the last RSSI */
	private long lastRssiTimestamp = 0;

	/** Opcodes sent to the device for which we are awaiting a response */
	private List<Integer> opcodes = Collections.synchronizedList(new LinkedList<Integer>());

	/** Stream used to send commands to the device */
	private OutputStream out;

	/** Response reader thread */
	private ResponseReader responseReader;


	/**
	 * @brief Instantiates a new {@code GnsGroupReader}.
	 * 
	 * The reader is transport agnostic, allowing it to work with different transport types on
	 * different platforms, as long as they provide an {@link InputStream} and an
	 * {@link OutputStream} to send and receive data.
	 * 
	 * @param in Stream which receives status and RDS data coming from the device
	 * @param out Stream used to send commands to the device
	 * 
	 * @throws UnavailableInputMethod if the device cannot be initialized
	 */
	public GnsGroupReader(InputStream in, OutputStream out) throws UnavailableInputMethod {
		this.in = in;
		this.out = out;

		try {
			System.out.println("Sending disable command and flushing buffer");
			/* Disable to stop any data delivery from a previous instance */
			sendCommand(OPCODE_DISABLE, 0x78, 0x78);

			responseReader = new ResponseReader();
			responseReader.start();

			System.out.println("Sending enable command");
			sendCommand(OPCODE_ENABLE, 0x78, 0x78);

			System.out.println("Probing to determine command set");
			/* 
			 * The tune command is different between the two known command sets, and the opcode of
			 * one device family is invalid on the other (this goes for the 9830 command on the
			 * FM9, the reverse has not been tested yet). By trying both flavors and waiting for
			 * a response to one of them, we can identify the command set used by the device.
			 */
			for (int i = 0; i < OPCODE_TUNE.length; i++) {
				/* TODO somehow the first one does not always go through */
				sendCommand(OPCODE_TUNE[i], 0x00, 0x05);
				sendCommand(OPCODE_TUNE[i], 0x00, 0x05);
			}
		} catch (IOException e) {
			throw new UnavailableInputMethod("I/O exception");
		}

		while (cmdSet == CMD_SET_UNKNOWN) {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
				return;
			}
		}

		setFrequency(87500);
	}

	/**
	 * @brief Closes this GroupReader.
	 * 
	 * The input and output streams are left open and must be closed by the caller as needed.
	 */
	@Override
	public void close() {
		try {
			sendCommand(OPCODE_DISABLE, 0x78, 0x78);
		} catch (IOException e) {
			e.printStackTrace();
		}
		this.closed = true;
	}

	@Override
	public String getDeviceName() {
		return DEVICE_NAME;
	}

	@Override
	public int getFrequency() {
		synchronized(gnsData) {
			return gnsData.frequency;
		}
	}

	@Override
	public GroupReaderEvent getGroup() throws IOException, EndOfStream {
		if (closed)
			throw new EndOfStream();

		if (System.currentTimeMillis() - lastRssiTimestamp > 1000) {
			/* query RSSI periodically (npo more than once per second) */
			try {
				sendCommand(OPCODE_RSSI[cmdSet], 0, 0);
			} catch (IOException e) {
				e.printStackTrace();
			}
			lastRssiTimestamp = System.currentTimeMillis();
		}

		synchronized(gnsData) {
			if (gnsData.frequencyChanged) {
				// if frequency has just been changed, must report an event
				gnsData.frequencyChanged = false;
				return new FrequencyChangeEvent(new RealTime(), gnsData.frequency);
			}

			if (!gnsData.groupReady) return null;

			gnsData.groupReady = false;

			newGroups = true;
			return new GroupEvent(new RealTime(), gnsData.blocks, false);
		}
	}

	@Override
	public int getSignalStrength() {
		if (closed)
			return 0;

		synchronized(gnsData) {
			return gnsData.rssi;
		}
	}

	@Override
	public boolean isAudioCapable() {
		return false;
	}

	@Override
	public boolean isPlayingAudio() {
		return false;
	}

	@Override
	public boolean isStereo() {
		return false;
	}

	@Override
	public boolean isSynchronized() {
		if (closed)
			return false;
		synchronized(gnsData) {
			return gnsData.rdsSynchronized;
		}
	}

	@Override
	public int mute() {
		return 0;
	}

	@Override
	public boolean newGroups() {
		if (closed)
			return false;

		boolean ng = newGroups;
		newGroups = false;
		return ng;
	}

	@Override
	public boolean seek(boolean up) {
		if (closed)
			return false;

		int frequency;
		synchronized(gnsData) {
			frequency = gnsData.frequency;
		}
		try {
			sendCommand(up ? OPCODE_SEEK_UP[cmdSet] : OPCODE_SEEK_DOWN[cmdSet],
					getChannelFromFrequency(frequency), 0x00);
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}

	@Override
	public int setFrequency(int frequency) {
		if (closed)
			return -1;

		try {
			sendCommand(OPCODE_TUNE[cmdSet], getChannelFromFrequency(frequency), 0x05);
		} catch (IOException e) {
			e.printStackTrace();
			return 0;
		}
		return frequency;
	}

	@Override
	public void tune(boolean up) {
		if (closed)
			return;

		int freq;
		synchronized(gnsData) {
			freq = gnsData.frequency + (up ? 100 : -100);
		}

		if(freq > 108000) freq = 87500;
		if(freq < 87500) freq = 108000;

		setFrequency(freq);
	}

	@Override
	public int unmute() {
		return 0;
	}


	/**
	 * @brief Whether an array of bytes represents a printable string.
	 * 
	 * A printable string is one which contains no characters with values lower than 0x20.
	 * 
	 * @param data The array of bytes to examine.
	 * 
	 * @return True if the array holds a printable string, false if not
	 */
	private static boolean isPrintable(int[] data) {
		boolean res = true;
		for (int i = 0; i < data.length; i++)
			res &= (data[i] >= 0x20);
		return res;
	}


	/**
	 * @brief Converts a frequency to a channel.
	 * 
	 * No validation of the input frequency is currently performed.
	 * 
	 * @param frequency The frequency in kHz (e.g. 87600 for 87.6 MHz)
	 * 
	 * @return The channel number for the frequency
	 */
	private static int getChannelFromFrequency(int frequency) {
		return (frequency - 87500) / 100;
	}


	/**
	 * @brief Converts a channel to a frequency.
	 * 
	 * No validation of the input frequency is currently performed.
	 * 
	 * @param channel The channel number to convert
	 * 
	 * @return The frequency for the channel, in kHz(e.g. 87600 for 87.6 MHz)
	 */
	private static int getFrequencyFromChannel(int channel) {
		return channel * 100 + 87500;
	}


	/**
	 * @brief Sends a command to the device.
	 * 
	 * Calls to this method must be followed by a call to {@link #processResponse()} and synced to
	 * the class instance to ensure they are processed in an atomic manner.
	 * 
	 * @param opcode The opcode for the command.
	 * @param aParam The first parameter byte
	 * @param bParam The second parameter byte
	 * 
	 * @throws IOException
	 */
	private void sendCommand(int opcode, int aParam, int bParam) throws IOException {
		if (closed)
			throw new ClosedChannelException();
		out.write(new byte[] {(byte) DELIM_COMMAND, (byte) opcode, (byte) aParam, (byte) bParam, (byte) opcode});
		if (opcode != OPCODE_DISABLE)
			opcodes.add(opcode);
	}


	/**
	 * Data read from the tuner.
	 * 
	 * Since instances of this class are shared between threads, access must be synchronized to the
	 * instance.
	 */
	private static class GnsData {
		private int[] blocks = {-1, -1, -1, -1};

		/** Frequency in kHz */
		private int frequency;

		private boolean frequencyChanged = true;

		private boolean groupReady;

		private boolean rdsSynchronized;

		/** Received Signal Strength Indicator, 0..65535 */
		private int rssi;
	}


	/**
	 * A separate thread which reads the responses received from the device and stores them in the
	 * appropriate data structures.
	 */
	private class ResponseReader extends Thread {
		@Override
		public void run() {
			while (!closed) {
				long res;

				/* Last response received from device */
				byte[] responseData = new byte[10];

				/* Next response byte to be read */
				int responseStart = 0;

				/* Response converted to integers */
				int[] intData = new int[10];

				int len = -1;

				if (responseStart == 10) {
					responseStart = 0;
					responseData = new byte[10];
				}

				while ((responseStart < 10) && !closed) {
					try {
						while ((responseStart < 10) && ((len = in.read(responseData, responseStart, responseData.length - responseStart)) > 0)) {
							responseStart += len;
						}
					} catch (IOException e) {
						e.printStackTrace();
						close();
						return;
					}

					if (responseStart < 10) {
						/* wait and repeat until we have 10 bytes */
						try {
							sleep(100);
						} catch (InterruptedException e) {
							return;
						}
					} else if (((responseData[0] & 0xFF) != DELIM_RESPONSE) || ((responseData[9] & 0xFF) != DELIM_RESPONSE)) {
						/* malformed response; look for two consecutive delimiters */
						System.err.printf("Malformed response (does not start and end with 0x%02X):", DELIM_RESPONSE);
						for (int i = 0; i < 10; i++)
							System.err.printf(" %02X", responseData[i]);
						System.err.print("\n");
						int discard = 1;
						while ((discard < 9) && ((responseData[discard] != DELIM_RESPONSE) || (responseData[discard + 1] == DELIM_RESPONSE)))
							discard++;
						/* 
						 * if we're discarding all but the last character, check if it's a
						 * delimiter, else discard it too 
						 */
						if ((discard == 9) && (responseData[discard] != DELIM_RESPONSE))
							discard++;
						for (int i = 0; (i + discard) < 10; i++)
							responseData[i] = responseData[i + discard];
						responseStart -= discard;
						System.err.printf("Discarded %d bytes\n", discard);
					}
				}

				if (closed) {
					return;
				}

				/* we now have a complete response, convert it */
				res = 0;
				for (int i = 0; i < 10; i++) {
					intData[i] = responseData[i] & 0xFF;
					if ((i >= 1) && (i <= 8))
						res |= ((long) intData[i]) << (64 - (i * 8));
				}

				if ((intData[1] == 0) && (intData[2] == 0)) {
					/* likely a status response (or an illegal PI code of 0000) */
					if (opcodes.contains(intData[3]) && (intData[3] == OPCODE_ENABLE)) {
						/* enable response, just print it and end flush mode */
						// TODO can we use the response to identify the command set?
						isFlushing = false;
						System.out.printf("Enable response: GNS V%02d %02d/%02d %02X%X (%016X)\n",
								intData[6], intData[5], intData[4], intData[7], intData[8], res);
						continue;
					} else if ((intData[3] == OPCODE_DISABLE) && ((res & 0xffffffffffl) == 0)) {
						/* response to disable command, end flush mode */
						isFlushing = false;
						System.out.println("Disable response received");
						continue;
					} else if (isFlushing) {
						/* still in flushing mode, continue */
						continue;
					} else if (cmdSet == CMD_SET_UNKNOWN) {
						/* command set probing returned a result */
						for (int i = 0; (i < OPCODE_TUNE.length) && (cmdSet == CMD_SET_UNKNOWN); i++) {
							opcodes.remove(Integer.valueOf(intData[3]));
							if (intData[3] == OPCODE_TUNE[i]) {
								System.out.printf("Command set is %s\n", CMD_SET_NAMES[i]);
								cmdSet = i;
							}
						}
						continue;
					} else if ((opcodes.contains(intData[3]) && (intData[3] == OPCODE_TUNE[cmdSet]))
							|| (intData[3] == OPCODE_SEEK_STATUS[cmdSet])) {
						/* frequency changed by either a seek or a tune operation */
						synchronized(gnsData) {
							gnsData.frequency = getFrequencyFromChannel(intData[4]);
							gnsData.rdsSynchronized = ((intData[5] == 0x01) && (intData[6] == 0x55));
							gnsData.frequencyChanged = true;
							System.out.printf("Tuned to %.1f (0x%02X), RDS: %b\n", gnsData.frequency / 1000.0f, intData[4], gnsData.rdsSynchronized);
						}
						opcodes.remove(Integer.valueOf(intData[3]));
						continue;
					} else if (opcodes.contains(intData[3])) {
						/* this is the response to a previously issued command */
						/* OPCODE_DISABLE is already handled above */
						/* OPCODE_ENABLE is already handled above */
						/* OPCODE_IDENTIFICATION is not repeated in the response */
						/* OPCODE_SEEK_STATUS is not a valid command opcode */
						/* OPCODE_TUNE is already handled above */
						if ((intData[3] == OPCODE_SEEK_UP[cmdSet])
								|| (intData[3] == OPCODE_SEEK_DOWN[cmdSet])) {
							if ("ok".equals(new String(intData, 5, 2)))
								System.out.println("Starting seek operation");
							else
								System.err.println("Seek command failed");
						} else if ((intData[3] == OPCODE_RSSI[cmdSet])) {
							/* RSSI has changed (frequency is reported here as well, so check it) */
							synchronized(gnsData) {
								int frequency = getFrequencyFromChannel(intData[7]);
								if (frequency != gnsData.frequency) {
									gnsData.frequency = frequency;
									gnsData.frequencyChanged = true;
								}
								/*
								 * TODO not sure if conversion is correct
								 * (maximum is 0xFF in theory, 0x0F is maximum observed so far)
								 */
								gnsData.rssi = intData[6] * 0x1111;
							}
						}
						opcodes.remove(Integer.valueOf(intData[3]));
						continue;
					} else if (res == 0x4572720000l) {
						/* Error */
						System.err.println("Error");
						try {
							opcodes.remove(0);
						} catch (IndexOutOfBoundsException e) {
							// NOP
						}
						continue;
					}
				} else if (isFlushing) {
					/* still in flushing mode, continue */
					continue;
				} else if (opcodes.contains(OPCODE_IDENTIFICATION) && isPrintable(intData)) {
					/* we requested an identification */
					System.out.printf("Identification: %s\n", new String(responseData, 1, 8));
					opcodes.remove(Integer.valueOf(OPCODE_IDENTIFICATION));
					continue;
				}

				/* if we get here, treat the response as RDS */
				System.out.println("RDS group received");
				int[] newBlocks = new int[4];
				for (int i = 0; i < 4; i++)
					newBlocks[i] = (intData[2 * i + 1] << 8) | intData[2 * i + 2];
				synchronized(gnsData) {
					gnsData.rdsSynchronized = true;
					gnsData.blocks = newBlocks;
					gnsData.groupReady = true;
				}
			}
		}
	}
}