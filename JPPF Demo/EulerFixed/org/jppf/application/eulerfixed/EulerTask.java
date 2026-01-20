
package org.jppf.application.eulerfixed;

import org.jppf.node.protocol.AbstractTask;

public class EulerTask extends AbstractTask<Integer> {
	
	private final int min;
	private final int max;
	private int result;
	
	public EulerTask(final int min, final int max) {
		this.min = min;
		this.max = max;
		result = 0;
	}
	
	@Override
	public void run() {
		System.out.println("Running: min: " + min + ", max: " + max);
		for(int i = min; i < max; i++) {
			result += euler(i);
			System.out.println("Result at " + i + ": " + result);
		}
		
		setResult(result);
	}
	
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
				//System.out.println("GCD FOUND: " + result);
			}
		}
		return result;
	}
}
