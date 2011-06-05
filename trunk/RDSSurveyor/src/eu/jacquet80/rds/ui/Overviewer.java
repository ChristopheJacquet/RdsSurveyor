package eu.jacquet80.rds.ui;

import java.io.PrintStream;
import java.util.Arrays;

import eu.jacquet80.rds.core.DecoderShell;
import eu.jacquet80.rds.core.GroupLevelDecoder;
import eu.jacquet80.rds.core.TunedStation;
import eu.jacquet80.rds.input.TunerGroupReader;

public class Overviewer extends Thread {
	private final TunerGroupReader tgr;
	private PrintStream console;
	
	
	public Overviewer(TunerGroupReader tgr, PrintStream console) {
		this.tgr = tgr;
		this.console = console;
	}
	
	public void run() {
		GroupLevelDecoder groupDecoder = DecoderShell.instance.getGroupReader();
		
		console.println("Measuring signal strength");
		MeasuredSignal[] strength = new MeasuredSignal[206];
		for(int i=0; i<strength.length; i++) {
			int freq = 87500 + i * 100;
			tgr.setFrequency(freq);
			int s = tgr.getSignalStrength();
			strength[i] = new MeasuredSignal(freq, s);
			
			if(freq % 1000 == 0) {
				console.printf("%3.1f  ", freq / 1000f);
			}
			
			//console.printf("%3.1f   %d\n", freq / 1000f, s);
		}
		console.println();
		console.println();
		
		Arrays.sort(strength);
		
		for(int i=strength.length-1; i>=0; i--) {
			MeasuredSignal s = strength[i];
			tgr.setFrequency(s.frequency);
			console.printf("%3.1f\t %d\t ", s.frequency / 1000f, s.rssi);
			groupDecoder.reset();
			
			try {
				sleep(5000);
			} catch (InterruptedException e) {}
			
			TunedStation station = groupDecoder.getTunedStation();
			if(station != null) {
				console.printf("%04X   %8s  %s\n", station.getPI(), station.getStationName(), station.getCompactGroupStats());
			} else {
				console.printf("--\n");
			}
		}
	}
}


class MeasuredSignal implements Comparable<MeasuredSignal> {
	public final int frequency;
	public final int rssi;
	
	public MeasuredSignal(int frequency, int rssi) {
		this.frequency = frequency;
		this.rssi = rssi;
	}

	@Override
	public int compareTo(MeasuredSignal o) {
		return rssi - o.rssi;
	}
}