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

package eu.jacquet80.rds.core;

import java.io.IOException;
import java.io.PrintStream;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.SimpleTimeZone;

import eu.jacquet80.rds.app.Application;
import eu.jacquet80.rds.app.InHouse;
import eu.jacquet80.rds.app.Paging;
import eu.jacquet80.rds.app.oda.AlertC;
import eu.jacquet80.rds.app.oda.ODA;
import eu.jacquet80.rds.input.GroupReader;
import eu.jacquet80.rds.input.GroupReader.EndOfStream;
import eu.jacquet80.rds.input.RDSReader;
import eu.jacquet80.rds.input.group.FrequencyChangeEvent;
import eu.jacquet80.rds.input.group.GroupEvent;
import eu.jacquet80.rds.input.group.GroupReaderEvent;
import eu.jacquet80.rds.input.group.GroupReaderEventVisitor;
import eu.jacquet80.rds.input.group.StationChangeEvent;
import eu.jacquet80.rds.log.ApplicationChanged;
import eu.jacquet80.rds.log.ClockTime;
import eu.jacquet80.rds.log.EONReturn;
import eu.jacquet80.rds.log.EONSwitch;
import eu.jacquet80.rds.log.Log;
import eu.jacquet80.rds.log.StationLost;
import eu.jacquet80.rds.log.StationTuned;

public class GroupLevelDecoder implements RDSDecoder {
	private int[] qualityHistory = new int[40];
	private int historyPtr = 0;
	private double time = 0.0;
	private TunedStation station = null;  // realStation is used in case station is a dummy one
	private boolean synced = true;
	private Log log;
	//private int badPIcount = 0;
	
	private final String[] RP_TNGD_VALUES = {
		"No RP",
		"RP groups 00-99",
		"RP groups 00-39",
		"RP groups 40-99",
		"RP groups 40-69",
		"RP groups 70-99",
		"RP groups 00-19",
		"RP groups 20-39",
	};

	private final PrintStream console;
	
	public GroupLevelDecoder(PrintStream console, Log log) {
		this.console = console;
		this.log = log;
	}
	
	public void loseSync() {
		synced = false;
	}
	
	private int processBasicTuningBits(int block1) {
		// Groups 0A, 0B, 15B : for TA, M/S and DI we need only block 1 (or block 3 for 15B)
		int ta = (block1>>4) & 1;
		int ms = (block1>>3) & 1;
		int addr = block1 & 3;
		
		console.print("TA=" + ta + ", " + (ms==1 ? "M/s" : "m/S") + ", ");
		
		boolean diInfo = ((block1>>2) & 1) == 1;		
		station.setDIbit(addr, diInfo, console);
		
		station.setTA(ta == 1);
		
		// we return addr because it is also used to address PS segment in 0A/0B
		return addr;
	}
	
	/* TODO FIXME
	 * This method should become private in the future. Everything should go through a
	 * StreamReader, even if we decode a bitstream in the first place.
	 */
	public void processGroup(int nbOk, boolean[] blocksOk, int[] blocks, int bitTime) {
		//console.print(" (" + (station == null ? null : station.getStationName() ) + ") ");
		Application newApp = null;
		
		TunedStation workingStation = station;
		time += 104 / 1187.5;
		
		qualityHistory[historyPtr] = nbOk;
		historyPtr = (historyPtr + 1) % qualityHistory.length;
		int sum = 0;
		for(int i=0; i < qualityHistory.length; i++) sum += qualityHistory[i];
		//float quality = sum / (4f * qualityHistory.length);
		
		int pi = 0;
		if(blocksOk[0]) {
			pi = blocks[0];
			console.printf("PI=%04X, ", pi);
			if(station.getPI() == 0) {
				// new station
				station.setPI(pi);
			}/*
			TODO: should be improved
			the 'StationTuned' message should be emitted when the 
			StationChangeEvent is received in the first place. 
			
			if(station.getPI() == pi) {
				// same PI as before => same station
				station.pingPI(bitTime);
				badPIcount = 0;
				synced = true;
			} else {
				// different PI => new station or PI error
				badPIcount++;
				if(badPIcount > 3 ) {
					// several different PIs in a row => new station
					log.addMessage(new StationLost(station.getTimeOfLastPI(), station));
					station = new TunedStation(bitTime);
					log.addMessage(new StationTuned(bitTime, station));
					station.setPI(pi);
				} else {
					// not enough different PIs => guess it's just an error
					// use a dummy TunedStation to silently ignore this group's info
					console.print("Ign, ");  // for ignore
					workingStation = new TunedStation(bitTime); // dummy station
				}
				
			}*/
		} else console.print("         ");
		
		if(!synced) return;   // after a sync loss, we wait for a PI before processing further data
		
		int type = -1, version = -1;
		
		if(blocksOk[1]) {
			type = ((blocks[1]>>12) & 0xF);
			version = ((blocks[1]>>11) & 1);
			
			workingStation.addGroupToStats(type, version, nbOk);
			
			int tp = (blocks[1]>>10) & 1;
			workingStation.setTP(tp == 1);
			
			int pty = (blocks[1]>>5) & 0x1F;
			workingStation.setPTY(pty);
			
			console.print("Group (" + (nbOk == 4 ? "full" : "part") + ") type " + type + (char)('A' + version) + ", TP=" + tp + ", PTY=" + pty + ", ");
		} else workingStation.addUnknownGroupToStats(nbOk);
		
		// Groups 0A & 0B
		if(type == 0) {
			int addr = processBasicTuningBits(blocks[1]);
			
			// Groups 0A & 0B: to extract PS segment we need blocks 1 and 3
			if(blocksOk[3]) {
				char ch1 = RDS.toChar( (blocks[3]>>8) & 0xFF);
				char ch2 = RDS.toChar(blocks[3] & 0xFF);
				console.print("PS pos=" + addr + ": \"" + toASCII(ch1) + toASCII(ch2) + "\" ");
				workingStation.getPS().setChars(addr, ch1, ch2);
			}
			
			// Groups 0A: to extract AFs we need blocks 1 and 2
			if(version == 0 && blocksOk[2]) {
				//console.printf("Raw AF: %d %d", (blocks[2]>>8) & 0xFF, blocks[2] & 0xFF);
				console.print(workingStation.addAFPair((blocks[2]>>8) & 0xFF, blocks[2] & 0xFF));
			}
		}

		// Group 1A: to extract RP info we need only block 1
		if(type == 1 && version == 0) {
			int tngd = (blocks[1]>>2) & 7;   // transmitter network group designator
			int bsi = (blocks[1]) & 3;       // battery saving interval sync and id
			console.print("RP Config: [" + RP_TNGD_VALUES[tngd]);
			if(tngd > 0) {   // print the rest only if there IS RP
				Application app = workingStation.getApplicationForGroup(7, 0);
				if(app == null) {
					newApp = app = new Paging(workingStation, console, RP_TNGD_VALUES[tngd]);
					
					workingStation.setApplicationForGroup(7, 0, app);
				} else if(!(app instanceof Paging)) {
					console.print("Error: this group indicates the presence of paging, while group 7A is used for '" + app.getName() + "'!");
				}

				console.print(", " + ((Paging)app).syncInfo((bsi >> 1) & 1, bsi & 1));
			}
			console.print("], ");
		}
		
		// Groups 1A & 1B: to extract PIN we need blocks 1 and 3
		if(type == 1 && blocksOk[3]) {
			int pin = blocks[3];
			int day = (pin>>11) & 0x1F;
			int hour = (pin>>6) & 0x1F;
			int min = pin & 0x3F;
			console.printf("PIN=%04X [D=%d, H=%02d:%02d] ", pin, day, hour, min);
		}
		
		// Group 1A: to extract slow labeling codes, we need blocks 1 and 3
		if(type == 1 && version == 0 && blocksOk[2]) {
			int variant = (blocks[2] >> 12) & 0x7;
			int la = (blocks[2] >> 15) & 0x1;
			console.print("LA=" + la + " v=" + variant + " ");
			switch(variant) {
			case 0:
				int opc = (blocks[2] >> 8) & 0xF;
				int ecc = blocks[2] & 0xFF;
				console.printf("OPC=%01X ECC=%02X ", opc, ecc);
				workingStation.setECC(ecc);
				if(pi != 0) console.print("[" + RDS.getISOCountryCode((pi>>12) & 0xF, ecc) + "] ");
				break;
				
			case 1:
				int tmcid = blocks[2] & 0xFFF;
				console.printf("TMC (old way) ID=0x%03X / (dec)%d", tmcid, tmcid);
				
				// connect 8A groups with the TMC application
				Application app = workingStation.getApplicationForGroup(8, 0);
				if(app == null) {
					Application appTMC = new AlertC();
					workingStation.setApplicationForGroup(8, 0, appTMC);
					appTMC.setStation(workingStation);
					appTMC.setConsole(console);
				} else if(!(app instanceof AlertC)) {
					console.print("Error: this group indicates the presence of TMC, while group 8A is used for '" + app.getName() + "'!");
				}
				break;			
				
			case 3:
				int langID = blocks[2] & 0xFF;
				workingStation.setLanguage(langID);
				console.printf("Language: %02X [%s]", langID, 
						langID < RDS.languages.length ? RDS.languages[langID][1] : "");
				break;
			
			case 6:
				console.printf("Broadcaster data: %03X", blocks[2] & 0xFFF);
				break;
				
			case 7:
				console.printf("EWS identification: %03X", blocks[2] & 0xFFF);
				break;
				
			default:
				console.printf("Unhandled data: %03X", blocks[2] & 0xFFF);
			}
		}
		
		// Groups 2A and 2B: to extract RT characters we need blocks 1 and (2 or 3)
		if(type == 2 && (blocksOk[2] || blocksOk[3])) {
			int addr = blocks[1] & 0xF;
			int ab = (blocks[1]>>4) & 1;
			char ch1 = '?', ch2 = '?', ch3 = '?', ch4 = '?';
			
			if(blocksOk[2] && version == 0) {
				ch1 = RDS.toChar( (blocks[2]>>8) & 0xFF);
				ch2 = RDS.toChar(blocks[2] & 0xFF);
				
				workingStation.getRT().setChars(version == 0 ? addr*2 : addr, ch1, ch2);
				workingStation.getRT().setFlag(ab);
			}
			
			if(blocksOk[3]) {
				ch3 = RDS.toChar( (blocks[3]>>8) & 0xFF);
				ch4 = RDS.toChar(blocks[3] & 0xFF);
				workingStation.getRT().setChars(version == 0 ? addr*2+1 : addr, ch3, ch4);
				workingStation.getRT().setFlag(ab);
			}
			
			console.print("RT A/B=" + (ab == 0 ? 'A' : 'B') + " pos=" + addr + ": \"");
			if(version == 0) console.print(toASCII(ch1) + "" + toASCII(ch2));
			console.print(toASCII(ch3) + "" + toASCII(ch4));
			console.print('\"');
		}
		
		// Groups 3A: to extract AID we need blocks 1 and 3
		if(type == 3 && version == 0 && blocksOk[3]) {
			int aid = blocks[3];
			int odaG = (blocks[1]>>1) & 0xF;
			int odaV = blocks[1] & 1;
			
			if(aid == 0) console.print("NO AID: ");
			else console.printf("AID #%04X ", aid);
			
			// return the ODA
			Application app = workingStation.getApplicationForGroup(odaG, odaV);
			if(app != null) {
				if(!(app instanceof ODA)) {
					console.printf("Currently group assigned to '%s' (non-ODA); it should not be assigned to AID %04X", app.getName(), aid);
				} else if(((ODA)app).getAID() != aid) { 
					console.printf("Current AID for group (%04X) does not match new AID (%04X)", ((ODA)app).getAID(), aid);
					app = null;
				}
			} else {
				app = ODA.forAID(aid);
				
				if(app != null) {
					newApp = app;
					workingStation.setApplicationForGroup(odaG, odaV, app);
					app.setStation(workingStation);
					app.setConsole(console);
				} else {
					console.print("Unknown AID!");
				}
			}
			
			if(app != null) {
				console.print("(" + app.getName() + "): ");
			}
			else console.print(" ");
			
			if(odaG == 0 && odaV == 0) console.print("only in group 3A   ");
			else if(odaG == 0xF && odaV == 1) console.print("temporary data fault at encoder   ");
			else console.print("group " + odaG + (char)('A' + odaV) + "   ");
			
			// if data ok, pass it to the ODA handler
			if(app != null && blocksOk[2]) {
				console.printf("ODA data=%04X", blocks[2]);
				
				console.println();
				console.print("\t" + app.getName()  + " --> ");
				app.receiveGroup(type, version, blocks, blocksOk, bitTime);
			}
		}
		
		// Groups 4A: to extract time we need blocks 1, 2 and 3
		if(type == 4 && version == 0 && blocksOk[2] && blocksOk[3]) {
			int mjd = ((blocks[1] & 0x3)<<15) | ((blocks[2] & 0xFFFE)>>1);
			
			int hour = ((blocks[2] & 1)<<4) | ((blocks[3] & 0xF000)>>12);
			int minute = ((blocks[3]>>6) & 0x3F);
			int sign = (blocks[3] & 0x20) == 0 ? 1 : -1;
			int offset = blocks[3] & 0x1F;
			
			int yp = (int)((mjd - 15078.2)/365.25);
			int mp = (int)( ( mjd - 14956.1 - (int)(yp * 365.25) ) / 30.6001 );
			int day = mjd - 14956 - (int)( yp * 365.25 ) - (int)( mp * 30.6001 );
			int k = (mp == 14 || mp == 15) ? 1 : 0;
			int year = 1900 + yp + k;
			int month = mp - 1 - k * 12;
			
			Calendar cal = new GregorianCalendar(year, month-1, day, hour, minute);
			cal.add(Calendar.MINUTE, sign * offset * 30);
			cal.setTimeZone(new SimpleTimeZone(sign * offset * 30 * 60 * 1000, ""));
			Date date = cal.getTime();
			
			console.printf("CT %02d:%02d%c%dmin %04d-%02d-%02d", hour, minute, sign>0 ? '+' : '-', offset*30, year, month, day);
			workingStation.setDate(date, bitTime);
			log.addMessage(new ClockTime(bitTime, date));
			
			// is there paging ?
			Application app = workingStation.getApplicationForGroup(7, 0);
			if(app != null && app instanceof Paging) {
				// then the 4A group act as 1A - start of interval
				console.print(", [RT: " + ((Paging)app).fullMinute() + "]");
			}
			
		}
		
		// Groups 5A-9A, 11A-13A: TDC, we need blocks 1, 2 and 3
		// but don't handle 7A groups here if using RP
		if(((type >= 5 && type <= 9) || (type >= 11 && type <= 13)) && version == 0) {
			int a = (blocks[1] & 0x1F);

			switch(type) {
			case 5: console.print("TDC/ODA "); break;
			case 6: console.print("IH/ODA "); break;
			case 7: console.print("RP/ODA "); break;
			case 8: console.print("TMC/ODA "); break;
			case 9: console.print("EWS/ODA "); break;
			case 11: console.print("ODA "); break;
			case 12: console.print("ODA "); break;
			case 13: console.print("ERP "); break;
			}
			
			if(blocksOk[2] && blocksOk[3])
				console.printf("%02X/%04X-%04X", a, blocks[2], blocks[3]);
			
			Application app = workingStation.getApplicationForGroup(type, version);
			
			if(app == null) {
				if(type == 6) {
					newApp = new InHouse(console);
					workingStation.setApplicationForGroup(6, 0, newApp);
				}
			}
			
			if(app != null) {
				console.println();
				console.print("\t" + app.getName() +  " --> ");
				app.receiveGroup(type, version, blocks, blocksOk, bitTime);
			}
		}
		
		
		
		// Groups 10A: PTYN, we need blocks 1, 2 and 3
		if(type == 10 && version == 0 && blocksOk[1]) {
			int ab = (blocks[1] >> 4) & 1;
			int pos = blocks[1] & 1;
			
			console.print("PTYN, flag=" + (char)('A' + ab) + ", pos=" + pos + ": \"");
			
			if(blocksOk[2]) {
				char c1 = RDS.toChar((blocks[2]>>8) & 0xFF);
				char c2 = RDS.toChar(blocks[2] & 0xFF);
				workingStation.getPTYN().setChars(pos*2, c1, c2);
				console.print(Character.toString(toASCII(c1)) + Character.toString(toASCII(c2)));
			} else console.print("??");
			
			if(blocksOk[3]) {
				char c1 = RDS.toChar((blocks[3]>>8) & 0xFF);
				char c2 = RDS.toChar(blocks[3] & 0xFF);
				workingStation.getPTYN().setChars(pos*2+1, c1, c2);
				console.print(Character.toString(toASCII(c1)) + Character.toString(toASCII(c2)));
			} else console.print("??");
			
			console.print("\"");
		}
		
		// Groups 14: to extract variant we need only block 1
		if(type == 14) {
			Station on = null;
			console.print("EON, ");

			// in both version if we have block 3 we have ON PI
			if(blocksOk[3]) {
				int onPI = blocks[3];
				console.printf("ON.PI=%04X%s, ", onPI, onPI == workingStation.getPI() ? " (self)" : "");
				
				if(onPI != workingStation.getPI()) {
					on = workingStation.getON(onPI);
					if(on == null) {
						on = new OtherNetwork(onPI);
						workingStation.addON(on);
					}
				} else { 
					// ON.PI may be equal to TN.PI in case of variant 12: it
					// is used to transmit linkage information for the
					// transmitting network. In this case we must surely not
					// create a new OtherNetwork instance.
					on = workingStation;
				}
			}
			
			int ontp = (blocks[1]>>4) & 1;
			if(on != null) on.setTP(ontp == 1);
			console.print("ON.TP=" + ontp + ", ");
			
			if(version == 0) { // info about ON only in 14A groups
				int variant = blocks[1] & 0xF; 
				console.print("v=" + variant + ", ");
			
				// to extract ON info we need block 2
				if(blocksOk[2]) {
					if(variant >= 0 && variant <= 3) {  // ON PS
						char ch1 = RDS.toChar( (blocks[2]>>8) & 0xFF);
						char ch2 = RDS.toChar( blocks[2] & 0xFF);
						console.print("ON.PS pos=" + variant + ": \"" + toASCII(ch1) + toASCII(ch2) + "\", ");
						
						if(on != null) on.getPS().setChars(variant, ch1, ch2);
					}
					
					if(variant == 4) { // frequencies
						if(on != null) {
							console.print("ON.AF: " + on.addAFPair((blocks[2]>>8)&0xFF, blocks[2]&0xFF) + " ");
						}
					}
					
					if(variant >= 5 && variant <= 8) {
						if(on != null) {
							console.print("ON.AF: " + on.addMappedFreq((blocks[2]>>8) & 0xFF, blocks[2] & 0xFF));
						}
					}
					
					if(variant == 12) {
						console.printf("Linkage information: %04X ", blocks[2]);
					}
					
					if(variant == 13) {
						int onpty = (blocks[2]>>11) & 0x1F;
						int onta = (blocks[2]) & 1;
						console.printf("ON.PTY=%d, ON.TA=%d ", onpty, onta);
						if(on != null) {
							on.setPTY(onpty);
							on.setTA(onta == 1);
						}
					}
					
					if(variant == 14) {
						int onpin = blocks[2];
						console.printf("ON.PIN=%04X ", onpin);
					}
				}
			} else { // 14B groups
				int onta = (blocks[1]>>3) & 1;
				console.print("ON.TA=" + onta + ", " + (onta==1 ? "switch now to ON" : "switch back from ON"));
				if(onta == 1) log.addMessage(new EONSwitch(bitTime, on));
				else log.addMessage(new EONReturn(bitTime, on));
			}
			
		}
		
		// Type 15A
		if(type == 15 && version == 0) {
			int addr = blocks[1] & 1;
			console.print("DEPRECATED RBDS-only fast PS, pos=" + addr + ": \"");
			for(int i=2; i<=3; i++) {
				if(blocksOk[i]) {
					char ch1 = RDS.toChar( (blocks[i]>>8) & 0xFF);
					char ch2 = RDS.toChar(blocks[i] & 0xFF);
					console.print(Character.toString(toASCII(ch1)) + Character.toString(toASCII(ch2)));
				} else console.print("??");
			}
			console.print("\", TA=" + ((blocks[1]>>4) & 1));
		}
		
		// For 15B we need only group 1, and possibly group 3
		if(type == 15 && version == 1) {
			processBasicTuningBits(blocks[1]);
			if(blocksOk[3]) processBasicTuningBits(blocks[3]);
		}
		

		// post log message for app creation only if the group is not being ignored
		if(newApp != null && station == workingStation)
			log.addMessage(new ApplicationChanged(bitTime, null, newApp));
	}
	
	
	public TunedStation getTunedStation() {
		return station;
	}
	
	public void notifyFrequencyChange(int time) {
		station = new TunedStation(time);
	}
	
	public void processStream(RDSReader rdsReader) throws IOException {
		GroupReader reader = (GroupReader)rdsReader;
		//final boolean[] allOk = {true, true, true, true};
		//final boolean[] noneOk = {false, false, false, false};
		GroupReaderEvent evt;
		
		for(;;) {
			try {
				///System.out.print(reader + "> ");
				evt = reader.getGroup();
				///System.out.println(evt);
			} catch(EndOfStream eos) {
				///System.out.println("EOS " + eos);
				return;
			}
			
			evt.accept(new GroupReaderEventVisitor() {
				int bitTime = 0;
				
				@Override
				public void visit(StationChangeEvent stationChangeEvent) {
					if(station != null)
						log.addMessage(new StationLost(station.getTimeOfLastPI(), station));
					station = new TunedStation(bitTime);
					log.addMessage(new StationTuned(bitTime, station));
					console.println("% New station tuned");
				}
				
				@Override
				public void visit(GroupEvent groupEvent) {
					// defensive programming: station should not be null...
					// but a (defective) input driver may forget to send the
					// StationChangeEvent...
					if(station == null) {
						station = new TunedStation(bitTime);
						log.addMessage(new StationTuned(bitTime, station));
					}
					// end defensive programming section
					
					int[] blocks = groupEvent.blocks;
					
					console.printf("%04d: [", bitTime / 26);
					//boolean[] blocksOk = allOk;
					boolean[] blocksOk = new boolean[4];
					int nbOk = 0;
					for(int i=0; i<4; i++) {
						blocksOk[i] = (blocks[i] >= 0);
						if(blocksOk[i]) nbOk++;
						if(blocksOk[i]) console.printf("%04X ", blocks[i]);
						else console.print("---- ");
					}
					console.print("] ");
					
					processGroup(nbOk, blocksOk, blocks, bitTime);
					console.println();
					bitTime += 104;  // 104 bits per group
					if(log != null) log.notifyGroup();
				}

				@Override
				public void visit(FrequencyChangeEvent frequencyChangeEvent) {
					console.println("% Frequency changed: " + frequencyChangeEvent.frequency);
				}
			});
			

			
			/*
			 * TODO FIXME: do we really need this?
			// detect isolated groups with wrong PI
			// (only if block 0 was received correctly...)
			if(blocks[0] != -1) {
				if(nextBlocks[0] != -1 && lastPI == nextBlocks[0] && lastPI != blocks[0]) {
					blocksOk = noneOk;
				} else lastPI = blocks[0];
			}
			
			// detect isolated groups with wrong PTY/TP
			// (only if block 1 was received correctly...)
			if(blocks[1] != -1) {
				if(nextBlocks[1] != -1 && lastPTY == ((nextBlocks[1]>>5) & 0x3F) && lastPTY != ((blocks[1]>>5) & 0x3F)) {
					blocksOk = noneOk;
				} else lastPTY = ((blocks[1]>>5) & 0x3F);
			}
			
			// detect spurious B-type groups
			// need to have blocks 0, 1 & 2 to do this
			if(blocks[0] != -1 && blocks[1] != -1 && blocks[2] != -1) { 
				if(((blocks[1]>>11) & 1) == 1) {
					if(blocks[0] != blocks[2]) blocksOk = noneOk;
				}
			}
			*/
		}
	}
	
	private static String toASCII(String s) {
		StringBuffer res = new StringBuffer(s.length());
		for(int i=0; i<s.length(); i++) {
			char current = s.charAt(i);
			res.append(current >= 32 && current < 128 ? current : '.');
		}
		return res.toString();
	}
	
	private static char toASCII(char c) {
		return c >= 32 && c < 128 ? c : '.';
	}
}
