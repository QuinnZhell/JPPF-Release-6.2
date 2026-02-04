
package org.jppf.application.eulerfixed;
import org.jppf.node.protocol.JPPFRunnable;

public class EulerTunedTask {
	public static int greatestCommonDivisor(int a, int b) {
		if(a == 0) {
			return b;
		}
		return greatestCommonDivisor(b % a, a);
	}
	
	@JPPFRunnable
	public static int euler(int n) {
		int result = 1;
		for (int i = 2; i < n; i++) {
			if(greatestCommonDivisor(i, n) == 1) {
				result++;
			}
		}
		System.out.println("Euler("+n+"): " + result);
		return result;
	}
}
