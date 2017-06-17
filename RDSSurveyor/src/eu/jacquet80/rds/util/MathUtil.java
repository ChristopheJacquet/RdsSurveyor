package eu.jacquet80.rds.util;

public final class MathUtil {
	/**
	 * @brief Returns the greatest common divisor of two positive integers.
	 * 
	 * Note that both arguments must be positive. The result is not specified if this method is
	 * called with negative arguments.
	 */
	public static int gcd(int a, int b) {
		if (b == 0) return a;
		return gcd(b, a%b);
	}
}
