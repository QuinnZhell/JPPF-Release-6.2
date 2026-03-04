
package org.jppf.application.eulerfixed;
import org.jppf.node.protocol.JPPFRunnable;

public class EulerArrayTask {
	public static int greatestCommonDivisor(int a, int b) {
		if(a == 0) {
			return b;
		}
		return greatestCommonDivisor(b % a, a);
	}
	
	@JPPFRunnable
	public static int euler(int[] targets) {
		int eulerSum = 0;
		for(int i = 0; i < targets.length; i++) {
			int result = 1;
			for (int j = 2; j < targets[i]; j++) {
				if(greatestCommonDivisor(j, targets[i]) == 1) {
					result++;
				}
			}
			System.out.println("Euler("+targets[i]+"): " + result);
			eulerSum += result;
		}
		return eulerSum;
	}
}
