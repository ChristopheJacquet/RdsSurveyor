package eu.jacquet80.rds.util;

import java.lang.reflect.Array;
import java.util.Arrays;

public class RingBuffer<T> {
	protected final int length;
	protected final T[] values;
	private final T defaultValue;
	private int index = 0;
	private int valuesAdded = 0;
	
	@SuppressWarnings("unchecked")
	public RingBuffer(Class<T> typeClass, int length, T defaultValue) {
		this.length = length;
		this.defaultValue = defaultValue;
		values = (T[]) Array.newInstance(typeClass, length);
		//values = (T[]) new Object[length];
		clear();
	}
	
	public synchronized void clear() {
		Arrays.fill(values, defaultValue);
		index = 0;
	}
	
	/**
	 * Adds a new value to the ring buffer.
	 * 
	 * @param value the value to add
	 * @return {@code true} if the buffer wraps around, {@code false} otherwise
	 */
	public synchronized boolean addValue(T value) {
		values[index] = value;
		index = (index + 1) % length;
		valuesAdded++;
		return index == 0;
	}
	
	/**
	 * Returns a past value.
	 * 
	 * @param relIdx the relative index of the past value. 1 is for the
	 * latest, value, 2 for the one before the latest, etc.
	 * 
	 * @return the value
	 */
	public synchronized T getValue(int relIdx) {
		return values[(length + index - relIdx) % length];
	}
	
	/**
	 * This method is not synchronized, therefore one must explicitly
	 * synchronize onto the RingBuffer object when perfoming bulk access
	 * to the array.
	 * @return
	 */
	public T[] getValues() {
		return values;
	}
	
	public synchronized int countValuesAdded() {
		int result = valuesAdded;
		valuesAdded = 0;
		return result;
	}
}
