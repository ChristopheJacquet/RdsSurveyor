package eu.jacquet80.rds.core;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import eu.jacquet80.rds.input.group.GroupEvent;
import eu.jacquet80.rds.log.Log;
import eu.jacquet80.rds.log.SequentialTime;

class GroupLevelDecoderTest {
	TunedStation send(String ...groups) throws IOException {
        Log log = new Log();
        GroupLevelDecoder decoder = new GroupLevelDecoder(log);
	    for(String g : groups) {
	        String[] p = g.split(" ");
	        int[] blocks = new int[4];
	        for(int i=0; i<4; i++) blocks[i] = Integer.parseInt(p[i], 16);
	        decoder.processOneGroup(new GroupEvent(new SequentialTime(0), blocks, false));
	    }
	    return decoder.getTunedStation();
	}

	@Test
	void testPS() throws IOException {
		TunedStation ts = send("F202 0408 5C66 2043", 
		                       "F202 0409 5C62 554C", 
                               "F202 040A 1E5C 5455", 
		                       "F202 040F 1C5C 5245");
		assertEquals(ts.getPS().toString(), " CULTURE");
	}
	
	@Test
	void testFastPS() throws IOException {
	    // Real groups received on 2010-02-12 in France.
	    TunedStation ts = send("F999 F400 2052 2054",
	                           "F999 F401 2053 2020");
	    assertEquals(ts.getLPS().toString(), " R T S  ");
	}

	@Test
	void testLongPS() throws IOException {
	    // Totally synthetic test, it needs to be confirmed on the field.
        TunedStation ts = send("F999 F400 7065 6163",
                               "F999 F401 6520 D0BC",
                               "F999 F402 D0B8 D180",
                               "F999 F403 20E5 B9B3",
                               "F999 F404 E592 8C00");
        assertEquals(ts.getLPS().toString(), "peace мир 平和");
	}
}
