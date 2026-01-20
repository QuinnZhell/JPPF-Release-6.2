
package org.jppf.application.euler;

import java.io.Serializable;

@SuppressWarnings("serial")
public class EulerCalculation implements Serializable {
	public static int greatestCommonDivisor(int a, int b) {
		if(a == 0) {
			return b;
		}
		return greatestCommonDivisor(b % a, a);
	}
	
	public static int euler(int n) {
		int result = 1;
		for (int i = 2; i < n; i++) {
			if(greatestCommonDivisor(i, n) == 1) {
				result++;
				System.out.println("GCD FOUND: " + result);
			}
		}
		return result;
	}
}
