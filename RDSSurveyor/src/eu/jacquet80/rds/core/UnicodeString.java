package eu.jacquet80.rds.core;

import java.nio.charset.Charset;

public class UnicodeString {
	private final byte[] bytes;
	private final boolean[] presence;
	private final int length;
	private Charset charset;
	
	public UnicodeString(int length) {
		this.length = length;
		this.bytes = new byte[length];
		this.presence = new boolean[length];
	}
	
	public UnicodeString(int length, Charset charset) {
		this(length);
		this.setCharset(charset);
	}
	
	public void set(int pos, byte b) {
	    if(b == 0) return;
		this.bytes[pos] = b;
		this.presence[pos] = true;
	}
	
	public void set(int pos, byte b1, byte b2) {
		this.set(pos, b1);
		this.set(pos+1, b2);
	}
	
	public void setCharset(Charset charset) {
		this.charset = charset;
	}

	public String toString() {
		if(this.charset == null) return "<unknown charset>";
		else return new String(bytes, 0, lengthSet(), this.charset);
	}

	/**
	 * Return the number of consecutive bytes that are set from the first one.
	 * 
	 * @return number of consecutive bytes
	 */
	private int lengthSet() {
		for(int i=0; i<this.length; i++) {
			if(!this.presence[i]) return i;
		}
		return this.length;
	}
}
