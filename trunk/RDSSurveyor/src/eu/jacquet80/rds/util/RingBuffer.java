package eu.jacquet80.rds.util;

import java.lang.reflect.Array;

public class RingBuffer<T> {
	protected final int length;
	protected final T[] values;
	private int index = 0;
	
	@SuppressWarnings("unchecked")
	public RingBuffer(Class<T> typeClass, int length) {
		this.length = length;
		values = (T[]) Array.newInstance(typeClass, length);
		//values = (T[]) new Object[length];
	}
	
	/**
	 * Adds a new value to the ring buffer.
	 * 
	 * @param value the value to add
	 * @return {@code true} if the buffer wraps around, {@code false} otherwise
	 */
	public boolean addValue(T value) {
		values[index] = value;
		index = (index + 1) % length;
		return index == 0;
	}
	
	public T[] getValues() {
		return values;
	}
}
