package eu.jacquet80.rds.app.oda.tmc;


public class TMCEvent {
	public int code;
	public String text;
	public String textQ;
	public EventNature nature; // N (blank, F, S)
	public int quantifierType; // Q
	public EventDurationType durationType; // T duration type (D=Dynamic, L=Longer lasting)
	public boolean bidirectional; // D directionality (1=unidirectional, 2=bidirectional)
	public EventUrgency urgency; // U (blank, U, X)
	public int updateClass;  // C
	// R phrasal code (NOT TO BE IMPLEMENTED HERE)
	
	public static enum EventDurationType {
		DYNAMIC, LONGER_LASTING;
		
		static EventDurationType forCode(String s) {
			if ("L".equals(s)) {
				return LONGER_LASTING;
			} else {
				return DYNAMIC;
			}
		}
		
		/**
		 * @brief Returns the inverted duration type.
		 */
		public EventDurationType invert() {
			switch(this) {
			case LONGER_LASTING: return DYNAMIC;
			case DYNAMIC: return LONGER_LASTING;
			default: return this;
			}
		}

		@Override
		public String toString() {
			switch(this) {
			case LONGER_LASTING: return "Longer Lasting";
			case DYNAMIC: return "Dynamic";
			default: return "ERR";
			}
		}
	}
	
	public static enum EventNature {
		INFO, FORECAST, SILENT;
		
		static EventNature forCode(String s) {
			if("F".equals(s)) {
				return FORECAST;
			} else if("S".equals(s)) {
				return SILENT;
			} else {
				return INFO;
			}
		}

		@Override
		public String toString() {
			switch(this) {
			case FORECAST: return "Forecast";
			case SILENT: return "Silent";
			case INFO: return "Info";
			default: return "ERR";
			}
		}
	}
	
	public static enum EventUrgency {
		NORMAL, URGENT, XURGENT; // xurgent = extremely urgent
		
		static EventUrgency forCode(String s) {
			if("U".equals(s)) {
				return URGENT;
			} else if("X".equals(s)) {
				return XURGENT;
			} else {
				return NORMAL;
			}
		}
		
		public static final EventUrgency max(EventUrgency a, EventUrgency b) {
			if (a == XURGENT) {
				if ((b == XURGENT) || (b == URGENT) || (b == NORMAL))
					return a;
				else
					return null;
			} else if (a == URGENT) {
				if (b == XURGENT)
					return b;
				else if ((b == URGENT) || (b == NORMAL))
					return a;
				else
					return null;
			} else if (a == NORMAL) {
				if ((b == XURGENT) || (b == URGENT) || (b == NORMAL))
					return b;
				else
					return null;
			} else
				return null;
		}
		
		public final EventUrgency prev() {
			switch(this) {
			case NORMAL: return XURGENT;
			case URGENT: return NORMAL;
			case XURGENT: return URGENT;
			default: return null;
			}
		}

		public final EventUrgency next() {
			switch(this) {
			case NORMAL: return URGENT;
			case URGENT: return XURGENT;
			case XURGENT: return NORMAL;
			default: return null;
			}
		}

		@Override
		public String toString() {
			switch(this) {
			case NORMAL: return "Normal";
			case URGENT: return "Urgent";
			case XURGENT: return "XUrgent";
			default: return "ERR";
			}
		}
	
	}
	
	TMCEvent(String line) {
		String[] comp = TMC.colonPattern.split(line);
		this.code = Integer.parseInt(comp[0]);
		this.textQ = comp[1];
		this.text = comp[2];
		if(this.text.length() == 0) this.text = this.textQ;
		this.nature = EventNature.forCode(comp[5]);
		
		if("".equals(comp[6])) {
			this.quantifierType = -1;
		} else {
			this.quantifierType = Integer.parseInt(comp[6]);
		}
		
		this.durationType = EventDurationType.forCode(comp[7]);
		this.bidirectional = "2".equals(comp[8]);
		this.urgency = EventUrgency.forCode(comp[9]);
		this.updateClass = Integer.parseInt(comp[10]);
	}

	public String formatQuantifier(int q) {
		// q == 0 is the highest value, i.e. 2^5 (types 0-5) or 2^8 (types 6-12)
		if(q == 0) {
			q = this.quantifierType <= 5 ? 32 : 256;
		}
		
		switch(this.quantifierType) {
		case 0:
			if(q <= 28) return Integer.toString(q);
			else return Integer.toString((q-29)*2 + 30);
			
		case 1:
			if(q <= 4) return Integer.toString(q);
			else if(q <= 14) return Integer.toString((q-4)*10);
			else return Integer.toString((q-12)*50);
			
		case 2:
			return "less than " + (q*10) + " m";
			
		case 3:
			return (q-1)*5 + " %";
			
		case 4:
			return "of up to " + (5*q) + " km/h";
			
		case 5:
			if(q <= 10) return "of up to " + (5*q) + " minutes";
			else if(q <=22) return "of up to " + (q-10) + " hours";
			else return "of up to " + (q-20)*6 + " hours";
			
		case 6:
			return (q-51) + " °C";
			
		case 7:
			return String.format("%02d:%02d",(q-1)/6, ((q-1)%6)*10);
			
		case 8:
		case 9:
			double val = q <= 100 ? q/10. : .5 * (q-80);
			if(this.quantifierType == 8) return val + " tonnes";
			else return val + " m";
			
		case 10:
			return "of up to " + q + " mm";
			
		case 11:
			return (87.5 + q/10.) + " MHz";
			
		case 12:
			return "TODO kHz";
			
		default:
			return "ILLEGAL";	
			
			
		}
	}
	
	public boolean isCancellation() {
		return "message cancelled".equals(this.text);
	}
}
