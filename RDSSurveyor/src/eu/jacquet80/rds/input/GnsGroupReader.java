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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

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
 * RSSI is not reported because, again, there is no known way to obtain it from the device. As a
 * substitute, RSSI is reported to be at a maximum when RDS is detected, zero otherwise.
 */
public class GnsGroupReader extends TunerGroupReader {
	/** FM9 command set */
	private static final int CMD_SET_FM9 = 0;

	/** 9830 command set */
	private static final int CMD_SET_9830 = 1;

	/** Delimiter for commands */
	private static final int DELIM_COMMAND = 0xFF;

	/** Delimiter for responses */
	private static final int DELIM_RESPONSE = 0x3F;

	/** Opcode to disable the device. */
	private static final int[] OPCODE_DISABLE = {0x53, 0x53};

	/** Opcode to enable the device. */
	private static final int[] OPCODE_ENABLE = {0x56, 0x56};

	/** Opcode to request an identification string. */
	private static final int[] OPCODE_IDENTIFICATION = {0x43, 0x43};

	/** Opcode to seek to the next valid station. */
	private static final int[] OPCODE_SEEK = {0x59, 0x79};

	/** Opcode to report seek status. */
	private static final int[] OPCODE_SEEK_STATUS = {0x66, 0x66};

	/** Opcode to tune to a specified frequency. */
	private static final int[] OPCODE_TUNE = {0x73, 0x46};

	/** The command set used by this device */
	private int cmdSet = -1;

	private String deviceName = "GNS (not initialized)";

	/** Frequency in kHz */
	private int frequency;

	private boolean frequencyChanged = true;

	private boolean groupReady;

	/** Stream which receives status and RDS data coming from the device */
	private InputStream in;

	private boolean newGroups;

	/** Last opcode sent to the device */
	private int opcode = -1;

	/** Stream used to send commands to the device */
	private OutputStream out;

	private boolean rdsSynchronized;

	/** Received Signal Strength Indicator, 0..65535 */
	private int rssi;

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
		Long response;
		
		byte[] buffer = new byte[1024];

		this.in = in;
		this.out = out;

		cmdSet = CMD_SET_FM9; // TODO detect command set

		try {
			System.out.println("\nSending disable command and flushing buffer");
			/* Disable to stop any data delivery from a previous instance */
			sendCommand(OPCODE_DISABLE[0], 0x78, 0x78);
			/* Discard any leftover data in the buffer */
			while (in.read(buffer) > 0) { }

			System.out.println("\nSending enable command");
			sendCommand(OPCODE_ENABLE[0], 0x78, 0x78);
			do {
			response = processResponse();
			} while (!((response == null) || hasOpcode(response, OPCODE_ENABLE[0])));
			if (response == null)
				throw new UnavailableInputMethod("No response to enable command");
			else
				System.out.printf("Response: %016X\n", response);

			System.out.println("\nRequesting identification");
			sendCommand(OPCODE_IDENTIFICATION[0], 0x00, 0x00);
			response = processResponse();
			if (response == null)
				throw new UnavailableInputMethod("No response to identification command");
			else
				System.out.printf("Response: %016X\n", response);
		} catch (IOException e) {
			throw new UnavailableInputMethod("I/O exception");
		}
		System.out.print("\n");
		setFrequency(87500);
	}

	@Override
	public String getDeviceName() {
		return deviceName;
	}

	@Override
	public int getFrequency() {
		return frequency;
	}

	@Override
	public synchronized GroupReaderEvent getGroup() throws IOException {
		Long response;
		
		if (frequencyChanged) {
			// if frequency has just been changed, must report an event
			frequencyChanged = false;
			return new FrequencyChangeEvent(new RealTime(), frequency);
		}

		response = processResponse();

		if ((response == null) || !groupReady) return null;

		int[] res = new int[4];
		for (int i = 0; i < 4; i++) {
			res[i] = (int) ((response >> (48 - i * 16)) & 0xFFFF);
		}

		newGroups = true;
		return new GroupEvent(new RealTime(), res, false);
	}

	@Override
	public int getSignalStrength() {
		return rssi;
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
		return rdsSynchronized;
	}

	@Override
	public int mute() {
		return 0;
	}

	@Override
	public boolean newGroups() {
		boolean ng = newGroups;
		newGroups = false;
		return ng;
	}

	@Override
	public boolean seek(boolean up) {
		Long response;
		try {
			synchronized(this) {
				sendCommand(OPCODE_SEEK[cmdSet], getChannelFromFrequency(frequency), up ? 0x01 : 0x00);
				rdsSynchronized = false;
				do {
					response = processResponse();
				} while (!hasOpcode(response, OPCODE_SEEK[cmdSet]));
			}
			do {
				response = processResponse();
			} while (!rdsSynchronized);
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}

	@Override
	public synchronized int setFrequency(int frequency) {
		Long response;
		try {
			sendCommand(OPCODE_TUNE[cmdSet], getChannelFromFrequency(frequency), 0x05);
			do {
				response = processResponse();
			} while (!hasOpcode(response, OPCODE_TUNE[cmdSet]));
		} catch (IOException e) {
			e.printStackTrace();
			return 0;
		}
		if (response == null)
			return 0;
		return frequency;
	}

	@Override
	public void tune(boolean up) {
		int freq = frequency + (up ? 100 : -100);

		if(freq > 108000) freq = 87500;
		if(freq < 87500) freq = 108000;

		frequency = setFrequency(freq);
	}

	@Override
	public int unmute() {
		return 0;
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
	 * @brief Whether a response is a status response to a particular opcode
	 * 
	 * @param response The response, as obtained from {@link #processResponse()}
	 * @param opcode The opcode
	 * 
	 * @return True if the response starts with two null bytes followed by the opcode, false if not
	 * (including if the response is null)
	 */
	private static boolean hasOpcode(Long response, int opcode) {
		if (response == null)
			return false;
		int first3 = (int) (response >> 40);
		return (((first3 & 0xFF) == first3) && (first3 == opcode));
	}
	

	/**
	 * @brief Reads and processes a response from the device.
	 * 
	 * @return The payload of the response (without enclosing delimiters) as a big-endian 64-bit
	 * integer, or null if no valid response was read
	 * 
	 * @throws IOException 
	 */
	private Long processResponse() throws IOException {
		long res;
		byte[] byteData = new byte[10];
		int[] data = new int[10];
		int start = 0;
		int len = -1;

		while ((start < 10) && ((len = in.read(byteData, start, byteData.length - start)) > 0)) {
			System.out.printf("Read %d bytes\n", len);
			start += len;
		}

		if (start == 0) {
			return null;
		} else if (start < 10) {
			System.err.printf("Bad response length (%d)\n", start);
			return null;
		}

		if (((byteData[0] & 0xFF) != DELIM_RESPONSE) || ((byteData[9] & 0xFF) != DELIM_RESPONSE)) {
			System.err.printf("Malformed response (does not start and end with 0x%02X):", DELIM_RESPONSE);
			for (int i = 0; i < len; i++)
				System.err.printf(" %02X", byteData[i]);
			System.err.print("\n");
			return null;
		}

		/* we now have a complete response, convert it */

		res = 0;
		for (int i = 0; i < 10; i++) {
			data[i] = byteData[i] & 0xFF;
			if ((i >= 1) && (i <= 8))
				res |= ((long) data[i]) << (64 - (i * 8));
		}

		/* set groupReady to false until we know better */
		groupReady = false;

		if (opcode == OPCODE_IDENTIFICATION[cmdSet]) {
			/* we requested an identification */
			deviceName = new String(byteData, 1, 8);
			opcode = -1;
			System.out.printf("Identification: %s\n", deviceName);
			return res;
		} else if ((data[1] == 0) && (data[2] == 0)) {
			/* likely a status response (or an illegal PI code of 0000) */
			if (((data[3] == opcode) && (data[3] == OPCODE_TUNE[cmdSet]))
					|| (data[3] == OPCODE_SEEK_STATUS[cmdSet])) {
				/* frequency changed by either a seek or a tune operation */
				frequency = getFrequencyFromChannel(data[4]);
				rdsSynchronized = ((data[5] == 0x01) && (data[6] == 0x55));
				// TODO figure out if we can get true RSSI
				rssi = (rdsSynchronized ? 65535 : 0);
				frequencyChanged = true;
				opcode = -1;
				System.out.printf("Tuned to %.1f (0x%02X), RDS: %b\n", frequency / 1000.0f, data[4], rdsSynchronized);
				return res;
			} else if (data[3] == opcode) {
				/* this is the response to a previously issued command */
				/* OPCODE_IDENTIFICATION is not repeated in the response */
				/* OPCODE_SEEK_STATUS is not a valid command opcode */
				/* OPCODE_TUNE is already handled above */
				if (opcode == OPCODE_DISABLE[cmdSet]) {
					/* nothing to do */
					System.out.println("Disable response received");
				} else if (opcode == OPCODE_ENABLE[cmdSet]) {
					/* nothing to do yet, as we don't know what the response means */
					System.out.println("Enable response received");
				} else if (opcode == OPCODE_SEEK[cmdSet]) {
					if ("ok".equals(new String(data, 5, 2)))
						System.out.println("Starting seek operation");
					else
						System.err.println("Seek command failed");
				}
				opcode = -1;
				return res;
			} else if (res == 0x45727200) {
				/* Error */
				System.err.println("Error");
				opcode = -1;
				return res;
			}
		}
		/* if we get here, treat the response as RDS */
		groupReady = true;
		return res;
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
		out.write(new byte[] {(byte) DELIM_COMMAND, (byte) opcode, (byte) aParam, (byte) bParam, (byte) opcode});
		this.opcode = opcode;
	}
}