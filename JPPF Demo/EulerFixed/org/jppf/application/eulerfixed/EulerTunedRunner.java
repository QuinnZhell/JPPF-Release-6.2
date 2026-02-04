package org.jppf.application.eulerfixed;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;

import org.jppf.JPPFException;
import org.jppf.client.JPPFClient;
import org.jppf.client.JPPFJob;
import org.jppf.node.protocol.Task;

public class EulerTunedRunner {
	
	static long start;
	static long finish;
	long timeElapsed;
	long[] timeElapseCollection;
	final static int EULER_TARGET = 75000;
	final static int MAX_JOBS = 16;
	final static int ITERATION_COUNT = 1;
	
	public static void main(final String...args) {
		long timeElapsed = 0; //TODO: UN-INIT
		
		try (final JPPFClient jppfClient = new JPPFClient()) {
			final EulerTunedRunner runner = new EulerTunedRunner();
//			try (BufferedWriter writer = new BufferedWriter(new FileWriter("results.txt"))) {
//				
//				// Experiment
//				for(int jobs = 1; jobs <= MAX_JOBS; jobs++) {
//					writer.write("Job Count: " + jobs + "\n");
					for(int i = 0; i < ITERATION_COUNT; i++) {
						
						// run experiment
						createTunedEulerJobList(jppfClient, EULER_TARGET);
						
//						// write results
//						System.out.println("Iteration " + i + ": " + timeElapsed + "ms.");
//						writer.write("Iteration[" + i +"] Elapsed Time: " + timeElapsed + "ms\n");
					}
//					writer.write("\n"); 
//				}
//				writer.close();
//			}
//		} catch(final Exception e) {
//			e.printStackTrace();
		}
	}
	
	public static void createTunedEulerJobList(final JPPFClient jppfClient, int eulerTarget) {
		int power = 1;
		int range = (int) (eulerTarget / Math.pow(2, power));
		int lower = 0;
		int higher = lower + range;
		System.out.println("Euler Job " + power + ": [" + lower + " , " + higher + "], range: " + range);
		
		
		final List<JPPFJob> jobList = new ArrayList<>();
		while(power > 0) {
			JPPFJob newJob = createTunedEulerJob(jppfClient, lower, higher);
			jppfClient.submitAsync(newJob);
			
			range = (int) (eulerTarget / Math.pow(2, power++));
			lower = higher;
			higher = higher + range;
			System.out.println("Euler Job " + power + ": [" + lower + " , " + higher + "], range: " + range);
			
			if(higher > eulerTarget) {
				JPPFJob finalJob = createTunedEulerJob(jppfClient, lower, eulerTarget);
				jppfClient.submitAsync(finalJob);
				power = -1;
			}
		}
		
		int sumResult = 1;
	    for (final JPPFJob job: jobList) {
	      final List<Task<?>> results = job.awaitResults();
	      sumResult += processExecutionResults(results);
	    }
	    
	    System.out.println("SumEuler("+eulerTarget+"): " + sumResult);
	}
	
	public static JPPFJob createTunedEulerJob(final JPPFClient jppfClient, int lower, int higher){
		JPPFJob job = new JPPFJob();
		job.setName("EulerSum["+lower+","+higher+"]");
		
		for(int i = lower + 1; i <= higher; i++) {
			try {
				job.add(EulerTunedTask.class, i);
			} catch (JPPFException e) {
				e.printStackTrace();
			}
		}
		return job;
	}
	
	public static int processExecutionResults(final List<Task<?>> results) {
		int sumResult = 0;
		
		for (final Task<?> task: results) {
			sumResult += (Integer) task.getResult();
		}
		
		return sumResult;
	}
}
