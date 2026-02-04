package org.jppf.application.eulerfixed;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;

import org.jppf.JPPFException;
import org.jppf.client.JPPFClient;
import org.jppf.client.JPPFConnectionPool;
import org.jppf.client.JPPFJob;
import org.jppf.node.protocol.Task;
import org.jppf.utils.Operator;

public class EulerTunedRunner {
	
	static long start;
	static long finish;
	long timeElapsed;
	long[] timeElapseCollection;
	final static int EULER_TARGET = 25000;
	final static int MAX_JOBS = 16;
	final static int ITERATION_COUNT = 1;
	final static int MIN_TASK_RANGE = 1;
	
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
		
		final List<JPPFJob> jobList = new ArrayList<>();
		while(range > MIN_TASK_RANGE) {
			System.out.println("Euler Job " + power + ": [" + lower + " , " + higher + "], range: " + range);
			JPPFJob newJob = createTunedEulerJob(jppfClient, lower, higher);
			jppfClient.submitAsync(newJob);
			jobList.add(newJob);
			
			range = (int) (eulerTarget / Math.pow(2, ++power));
			lower = higher;
			higher = higher + range;
		}
		
		for(int i = lower; i < eulerTarget; i++) {
			System.out.println("Euler Job: [" + i + " , " + (i+1) + "], range: " + MIN_TASK_RANGE);
			JPPFJob newJob = createTunedEulerJob(jppfClient, i, i+1);
			jppfClient.submitAsync(newJob);
			jobList.add(newJob);
		}
		
		
		try {
			System.out.println("Job Count: " + jobList.size());
			ensureNumberOfConnections(jppfClient, jobList.size());
		} catch (Exception e) {
			e.printStackTrace();
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
	
	  public static void ensureNumberOfConnections(final JPPFClient jppfClient, final int numberOfConnections) throws Exception {
		    // wait until the client has at least one connection pool with at least one avaialable connection
		    final JPPFConnectionPool pool = jppfClient.awaitActiveConnectionPool();

		    // if the pool doesn't have the expected number of connections, change its size
		    if (pool.getConnections().size() != numberOfConnections) {
		      // set the pool size to the desired number of connections
		      pool.setSize(numberOfConnections);
		    }

		    // wait until all desired connections are available (ACTIVE status)
		    pool.awaitActiveConnections(Operator.AT_LEAST, numberOfConnections);
		  }
	
	public static int processExecutionResults(final List<Task<?>> results) {
		int sumResult = 0;
		
		for (final Task<?> task: results) {
			sumResult += (Integer) task.getResult();
		}
		
		return sumResult;
	}
}
