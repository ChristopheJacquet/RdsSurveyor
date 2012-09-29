package eu.jacquet80.rds.app.oda;

import java.util.Vector;

public class CatalunyaRadioTDC extends TDC {
	private final Vector<String> messages = new Vector<String>();  // concurrent list
	private final StringBuffer currentMessage = new StringBuffer();

	@Override
	protected String getTDCAppName() {
		return "CATRADIO";
	}

	@Override
	protected String processTDCData(int channel, int[] contents) {
		int iStart, iStop;
		if(contents[0] == 1) {
			// start of message
			currentMessage.setLength(0);
			iStart = 1;
			iStop = 3;
		} else if(contents[3] == 4) {
			// end of message

			iStart = 0;
			iStop = 0;
			
		} else {
			iStart = 0;
			iStop = 3;
		}
		
		for(int i=iStart; i<=iStop; i++) {
			currentMessage.append(character(contents[i]));
		}

		if(contents[3] == 4) {
			if(currentMessage.length() > 0) {
				messages.add(currentMessage.toString());
				fireChangeListeners();
			}
		}
		
		return "";
	}

	public Vector<String> getMessageList() {  // concurrent list
		return messages;
	}
}
