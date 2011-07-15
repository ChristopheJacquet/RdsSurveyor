package eu.jacquet80.rds.input;

import java.io.IOException;

import eu.jacquet80.rds.input.group.GroupEvent;
import eu.jacquet80.rds.input.group.GroupReaderEvent;
import eu.jacquet80.rds.log.RealTime;


public class USBFMRadioGroupReader implements GroupReader {
	public native byte init();
	public native int getFrequency();
	public native void setFrequency(int frequency);	
	public native void seek(boolean seekUp);
	public native void tune(boolean tuneUp);
	public native int getSignal();
	private native short[] getRDSRegisters();

	
	static {
		//System.out.println("Path: " + System.getProperty("java.library.path"));
		System.loadLibrary("USBRadio");
	}
	
	public static void main(String[] args) {
		
		
		USBFMRadioGroupReader radio = new USBFMRadioGroupReader();
		
		System.out.println("init: " + radio.init());
		System.out.println("freq: " + radio.getFrequency());
		radio.setFrequency(89900);
		System.out.println("freq: " + radio.getFrequency());

		
		while(true) {
			short[] res = radio.getRDSRegisters();
			if(res == null) continue;
			if((res[10] & 0x8000) == 0) continue;  // RSSI.RDS_Receive
			for(short s : res) {
				System.out.print(Integer.toHexString((s>>12) & 0xF));
				System.out.print(Integer.toHexString((s>>8) & 0xF));
				System.out.print(Integer.toHexString((s>>4) & 0xF));
				System.out.print(Integer.toHexString(s & 0xF) + " ");
			}
			System.out.println();
		}
	}
	@Override
	public GroupReaderEvent getGroup() throws IOException {
		while(true) {
			short[] res = getRDSRegisters();
			if(res == null) continue;
			if((res[10] & 0x8000) == 0) continue;  // RSSI.RDS_Receive

			return new GroupEvent(new RealTime(), new int[] { 
					0xFFFF & ((int)res[12]), 
					0xFFFF & ((int)res[13]),
					0xFFFF & ((int)res[14]),
					0xFFFF & ((int)res[15]) }, false);
		}
	}
}
