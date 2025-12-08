
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
}
