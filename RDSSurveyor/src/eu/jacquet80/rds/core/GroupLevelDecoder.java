package eu.jacquet80.rds.core;

import java.io.PrintStream;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.SimpleTimeZone;

import eu.jacquet80.rds.log.ClockTime;
import eu.jacquet80.rds.log.EONReturn;
import eu.jacquet80.rds.log.EONSwitch;
import eu.jacquet80.rds.log.Log;
import eu.jacquet80.rds.log.StationLost;
import eu.jacquet80.rds.log.StationTuned;
import eu.jacquet80.rds.oda.ODA;

public class GroupLevelDecoder {
	private int[] qualityHistory = new int[40];
	private int historyPtr = 0;
	private double time = 0.0;
	private TunedStation station = new TunedStation();
	private boolean synced = true;
	
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

	private final PrintStream output;
	
	public GroupLevelDecoder(PrintStream output) {
		this.output = output;
	}
	
	public void loseSync() {
		synced = false;
	}
	
	private char decodeBCD(int bcd, int pos) {
		int val =  ((bcd>>(pos*4)) & 0xF);
		if(val < 10) return (char)('0' + val);
		else if(val == 10) return ' ';
		else return '!';
	}
	
	private String decodeBCDWord(int word) {
		String res = "";
		for(int i=3; i>=0; i--) res += decodeBCD(word, i);
		return res;
	}
	
	private int processBasicTuningBits(int block1) {
		// Groups 0A, 0B, 15B : for TA, M/S and DI we need only block 1 (or block 3 for 15B)
		int ta = (block1>>4) & 1;
		int ms = (block1>>3) & 1;
		int addr = block1 & 3;
		
		station.setDIbit(addr, (block1>>2) & 1);
		
		output.print("TA=" + ta + ", M/S=" + ms + ", ");
		
		// we return addr because it is also used to address PS segment in 0A/0B
		return addr;
	}
	
	public void processGroup(int nbOk, boolean[] blocksOk, int[] blocks, int bitTime, Log log) {
		time += 104 / 1187.5;
		
		qualityHistory[historyPtr] = nbOk;
		historyPtr = (historyPtr + 1) % qualityHistory.length;
		int sum = 0;
		for(int i=0; i < qualityHistory.length; i++) sum += qualityHistory[i];
		//float quality = sum / (4f * qualityHistory.length);
		
		if(blocksOk[0]) {
			int pi = blocks[0];
			output.printf("PI=%04X, ", pi);
			if(station.getPI() == pi) {
				station.pingPI(bitTime);
				synced = true;
			}
			int oldPI = station.getPI();
			if(! station.setPI(pi) ) {
				log.addMessage(new StationLost(station.getTimeOfLastPI(), station));
				station = new TunedStation();
				return;
			}
			if(station.getPI() != oldPI) log.addMessage(new StationTuned(bitTime, station));
		} else output.print("         ");
		
		if(!synced) return;   // after a sync loss, we wait for a PI before processing further data
		
		int type = -1, version = -1;
		
		if(blocksOk[1]) {
			type = ((blocks[1]>>12) & 0xF);
			version = ((blocks[1]>>11) & 1);
			
			station.addGroupToStats(type, version);
			
			int tp = (blocks[1]>>10) & 1;
			int pty = (blocks[1]>>5) & 0x1F;
			
			output.print("Group (" + (nbOk == 4 ? "full" : "part") + ") type " + type + (char)('A' + version) + ", TP=" + tp + ", PTY=" + pty + ", ");
			station.setPTY(pty);
		} else station.addUnknownGroupToStats();
		
		// Groups 0A & 0B
		if(type == 0) {
			int addr = processBasicTuningBits(blocks[1]);
			
			// Groups 0A & 0B: to extract PS segment we need blocks 1 and 3
			if(blocksOk[3]) {
				char ch1 = (char) ( (blocks[3]>>8) & 0xFF);
				char ch2 = (char) (blocks[3] & 0xFF);
				output.print("PS pos=" + addr + ": \"" + ch1 + ch2 + "\" ");
				station.setPSChars(addr, ch1, ch2);
			}
			
			// Groups 0A: to extract AFs we need blocks 1 and 2
			if(version == 0 && blocksOk[2]) {
				output.print(station.addAFPair((blocks[2]>>8) & 0xFF, blocks[2] & 0xFF));
			}
		}

		// Group 1A: to extract RP info we need only block 1
		if(type == 1 && version == 0) {
			int tngd = (blocks[1]>>2) & 7;   // transmitter network group designator
			int bsi = (blocks[1]) & 3;       // battery saving interval sync and id
			output.print("RP Config: [" + RP_TNGD_VALUES[tngd]);
			if(tngd > 0) {   // print the rest only if there IS RP
				station.setUsesRP(true);
				if((bsi & 2) > 0) output.print(", Start of Interval");
				else output.print(", Interval number bit=" + (bsi & 1));
			}
			output.print("], ");
		}
		
		// Groups 1A & 1B: to extract PIN we need blocks 1 and 3
		if(type == 1 && blocksOk[3]) {
			int pin = blocks[3];
			int day = (pin>>11) & 0x1F;
			int hour = (pin>>6) & 0x1F;
			int min = pin & 0x3F;
			output.print("PIN=" + pin + " [D=" + day + " H=" + hour + ":" + min + "] ");
		}
		
		// Group 1A: to extract slow labeling codes, we need blocks 1 and 3
		if(type == 1 && version == 0 && blocksOk[2]) {
			int variant = (blocks[2] >> 12) & 0x7;
			int la = (blocks[2] >> 15) & 0x1;
			output.print("LA=" + la + " v=" + variant + " ");
			switch(variant) {
			case 0:
				int opc = (blocks[2] >> 8) & 0xF;
				int ecc = blocks[2] & 0xFF;
				output.printf("OPC=%01X ECC=%02X ", opc, ecc);
			}
		}
		
		// Groups 2A: to extract 2 RT characters we need blocks 1 and (2 or 3)
		if(type == 2 && version == 0 && (blocksOk[2] || blocksOk[3])) {
			int addr = blocks[1] & 0xF;
			int ab = (blocks[1]>>4) & 1;
			char ch1 = '?', ch2 = '?', ch3 = '?', ch4 = '?';
			
			if(blocksOk[2]) {
				ch1 = (char) ( (blocks[2]>>8) & 0xFF);
				ch2 = (char) (blocks[2] & 0xFF);
				station.setRTChars(ab, addr*2, ch1, ch2);
			}
			
			if(blocksOk[3]) {
				ch3 = (char) ( (blocks[3]>>8) & 0xFF);
				ch4 = (char) (blocks[3] & 0xFF);
				station.setRTChars(ab, addr*2+1, ch3, ch4);
			}
			
			output.print("RT A/B=" + (ab == 0 ? 'A' : 'B') + " pos=" + addr + ": \"" + ch1 + ch2 + ch3 + ch4 + "\"");
		}
		
		// Groups 3A: to extract AID we need blocks 1 and 3
		if(type == 3 && version == 0 && blocksOk[3]) {
			int aid = blocks[3];
			int odaG = (blocks[1]>>1) & 0xF;
			int odaV = blocks[1] & 1;
			
			if(aid == 0) output.print("NO AID: ");
			else output.printf("AID #%04X ", aid);
			
			ODA oda = ODA.forAID(aid);
			if(oda != null) {
				station.setODAforGroup(odaG, odaV, oda);
				oda.setStation(station);
				output.print("(" + oda.getName() + "): ");
			}
			else output.print(" ");
			
			if(odaG == 0 && odaV == 0) output.print("only in group 3A   ");
			else if(odaG == 0xF && odaV == 1) output.print("temporary data fault at encoder   ");
			else output.print("group " + odaG + (char)('A' + odaV) + "   ");
			
			// if data ok, pass it to the ODA handler
			if(oda != null && blocksOk[2]) {
				output.printf("ODA data=%04X", blocks[2]);
				
				output.println();
				output.print("\t--> ");
				oda.receiveGroup(type, version, blocks, blocksOk);
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
			
			output.printf("CT %02d:%02d%c%dmin %04d-%02d-%02d", hour, minute, sign>0 ? '+' : '-', offset*30, year, month, day);
			station.setDate(date);
			log.addMessage(new ClockTime(bitTime, date));
			
		}
		
		// Groups 5A-9A, 11A-13A: TDC, we need blocks 1, 2 and 3
		// but don't handle 7A groups here if using RP
		if(((type >= 5 && type <= 9) || (type >= 11 && type <= 13)) && version == 0 && !(station.isUsingRP() && type==7)) {
			int a = (blocks[1] & 0x1F);

			switch(type) {
			case 5: output.print("TDC/ODA "); break;
			case 6: output.print("IH/ODA "); break;
			case 7: output.print("RP/ODA "); break;
			case 8: output.print("TMC/ODA "); break;
			case 9: output.print("EWS/ODA "); break;
			case 11: output.print("ODA "); break;
			case 12: output.print("ODA "); break;
			case 13: output.print("ERP "); break;
			}
			
			if(blocksOk[2] && blocksOk[3])
				output.printf("%02X/%04X-%04X", a, blocks[2], blocks[3]);
			
			ODA oda = station.getODAforGroup(type, version);
			if(oda != null) {
				output.println();
				output.print("\t" + oda.getName() +  " --> ");
				oda.receiveGroup(type, version, blocks, blocksOk);
			}
		}
		
		// Groups 7A used for RP
		if(type == 7 && version == 0 && station.isUsingRP() && blocksOk[2] && blocksOk[3]) {
			String addrStr = "addr=" + 
				decodeBCD(blocks[2], 3) + decodeBCD(blocks[2], 2) + "/" +
				decodeBCD(blocks[2], 1) + "" + decodeBCD(blocks[2], 0) + "" + decodeBCD(blocks[3], 3) + "" + decodeBCD(blocks[3], 2);

			output.print("RP: flag=" + (char)('A' + ((blocks[1]>>5) & 1)) + ", ");
			
			if((blocks[1] & 0x8) == 8) {
				int idx = blocks[1] & 0x7;
				if(idx == 0)
					output.print("Alpha message: " + addrStr);
				else {
					output.print("Alpha message: msg[" + idx + "]=\"");
					for(int i=2; i<=3; i++) {
						output.print((char)((blocks[i]>>8) & 0xFF));
						output.print((char)(blocks[i] & 0xFF));
					}
					output.print("\"");
				}
				
			}
			if((blocks[1] & 0xC) == 4) output.print("18/15-digit numeric msg: " + addrStr);
			if((blocks[1] & 0xE) == 2) {
				output.print("10-digit msg " + (1+(blocks[1] & 1)) + "/2: ");
				if((blocks[1] & 1) == 0) output.print(addrStr +
						(", msg=" + decodeBCD(blocks[3], 1)) + (decodeBCD(blocks[3], 0) + "..."));
				else output.print("msg=..." + decodeBCDWord(blocks[2]) + decodeBCDWord(blocks[3]));
			}
			if((blocks[1] & 0xF) == 1) output.print("Part of func");
			if((blocks[1] & 0xF) == 0) output.print("Beep: " + addrStr);
		}
		
		// Groups 14A: to extract variant we need only block 1
		if(type == 14) {
			OtherNetwork on = null;
			output.print("EON, ");

			// in both version if we have block 3 we have ON PI
			if(blocksOk[3]) {
				int onPI = blocks[3];
				output.printf("ON.PI=%04X, ", onPI);
				
				on = station.getON(onPI);
				if(on == null) {
					on = new OtherNetwork(onPI);
					station.addON(on);
				}
			}
			
			int ontp = (blocks[1]>>4) & 1;
			output.print("ON.TP=" + ontp + ", ");
			
			if(version == 0) { // info about ON only in 14A groups
				int variant = blocks[1] & 0xF; 
				output.print("v=" + variant + ", ");
			
				// to extract ON info we need block 2
				if(blocksOk[2]) {
					if(variant >= 0 && variant <= 3) {  // ON PS
						char ch1 = (char) ( (blocks[2]>>8) & 0xFF);
						char ch2 = (char) ( blocks[2] & 0xFF);
						output.print("ON.PS pos=" + variant + ": \"" + ch1 + ch2 + "\", ");
						
						if(on != null) on.setChars(on.ps, variant, ch1, ch2);
					}
					
					if(variant == 4) { // frequencies
						if(on != null) {
							output.print("ON.AF: " + on.addAFPair((blocks[2]>>8)&0xFF, blocks[2]&0xFF) + " ");
						}
					}
					
					if(variant >= 5 && variant <= 8) {
						if(on != null) {
							output.print("ON.AF: " + on.addMappedFreq((blocks[2]>>8) & 0xFF, blocks[2] & 0xFF));
						}
					}
					
					if(variant == 12) {
						output.printf("Linkage information: %04X ", blocks[2]);
					}
					
					if(variant == 13) {
						int onpty = (blocks[2]>>11) & 0x1F;
						int onta = (blocks[2]) & 1;
						output.printf("ON.PTY=%d, ON.TA=%d ", onpty, onta);
					}
					
					if(variant == 14) {
						int onpin = blocks[2];
						output.printf("ON.PIN=%04X ", onpin);
					}
				}
			} else { // 14B groups
				int onta = (blocks[1]>>3) & 1;
				output.print("ON.TA=" + onta + ", " + (onta==1 ? "switch now to ON" : "switch back from ON"));
				if(onta == 1) log.addMessage(new EONSwitch(bitTime, on));
				else log.addMessage(new EONReturn(bitTime, on));
			}
			
		}
		
		// For 15B we need only group 1, and possibly group 3
		if(type == 15 && version == 1) {
			processBasicTuningBits(blocks[1]);
			if(blocksOk[3]) processBasicTuningBits(blocks[3]);
		}
		
		return; // true;
		
	}
	
	
	public TunedStation getTunedStation() {
		return station;
	}
}
