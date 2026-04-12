
package org.jppf.application.MonteCarlo;

import java.io.Serializable;
import java.util.Random;

import org.jppf.node.protocol.JPPFRunnable;

@SuppressWarnings("serial")
public class CountPointsTask implements Serializable {

	@JPPFRunnable
	public static int countPoints(int n) {
		Random random = new Random();
		int countInside = 0;
		for(int i = 0; i < n; i++) {
			double x = (random.nextDouble() * 2D) - 1;
			double y = (random.nextDouble() * 2D) - 1;
			//System.out.println("(" + x + "," + y + ")");
			if(x * x + y * y <= 1) {
				countInside++;
			}
		}
		
		//System.out.println(countInside);
		return countInside;
	}
}
