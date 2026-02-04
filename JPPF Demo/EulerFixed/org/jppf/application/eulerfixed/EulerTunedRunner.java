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
	final static int EULER_TARGET = 20000;
	final static int MAX_JOBS = 16;
	final static int ITERATION_COUNT = 10;
	final static int MIN_TASK_RANGE = 1;
	
	enum EulerConfig {
		Tuned("EulerTuned") {
			@Override
			public long eulerSum(JPPFClient jppfClient, int eulerTarget) {
				return createTunedEulerJobList(jppfClient, EULER_TARGET);
			}
			
		},
		Naive("EulerNaive") {
			@Override
			public long eulerSum(JPPFClient jppfClient, int eulerTarget) {
				return createNaiveEulerJobList(jppfClient, EULER_TARGET);
			}
			
		};
		
		private String name;
		
		private EulerConfig(String name) {
			this.name = name;
		}
		
		public abstract long eulerSum(JPPFClient jppfClient, int eulerTarget);
		
		public String getName() {
			return name;
		}
	}
	
	public static void main(final String...args) {
		long timeElapsed;
		
		try (final JPPFClient jppfClient = new JPPFClient()) {
			
			for(EulerConfig euler : EulerConfig.values()) {
				try (BufferedWriter writer = new BufferedWriter(new FileWriter("Results/Nodes/Node1/"+ euler.getName() + "results.txt"))) {
					
						// Experiment
						for(int i = 0; i < ITERATION_COUNT; i++) {
							// Tuned Run
							timeElapsed = euler.eulerSum(jppfClient, EULER_TARGET);
							
							// write results
							System.out.println("Iteration " + i + ": " + timeElapsed + "ms.");
							writer.write("Iteration[" + i +"] Elapsed Time: " + timeElapsed + "ms\n");
						}
						writer.write("\n"); 
						writer.close();
					}
			}
		} catch(final Exception e) {
			e.printStackTrace();
		}
	}
	
	public static long createTunedEulerJobList(final JPPFClient jppfClient, int eulerTarget) {
		int power = 1;
		int range = ((int) (eulerTarget / Math.pow(2, power)));
		int lower = 0;
		int higher = lower + range;
		
		final List<JPPFJob> jobList = new ArrayList<>();
		start = System.nanoTime();
		while(range > MIN_TASK_RANGE) {
			System.out.println("Euler Job " + power + ": [" + lower + " , " + higher + "], range: " + range);
			JPPFJob newJob = createEulerJob(jppfClient, lower, higher);
			jppfClient.submitAsync(newJob);
			jobList.add(newJob);
			
			range = (int) (eulerTarget / Math.pow(2, ++power));
			lower = higher;
			higher = higher + range;
		}
		
		for(int i = lower; i < eulerTarget; i++) {
			System.out.println("Euler Job: [" + i + " , " + (i+1) + "], range: " + MIN_TASK_RANGE);
			JPPFJob newJob = createEulerJob(jppfClient, i, i+1);
			jppfClient.submitAsync(newJob);
			jobList.add(newJob);
		}
		
		int sumResult = 1;
	    for (final JPPFJob job: jobList) {
	      final List<Task<?>> results = job.awaitResults();
	      sumResult += processExecutionResults(results);
	    }
	    
	    finish = System.nanoTime();
	    System.out.println("SumEuler("+eulerTarget+"): " + sumResult);
	    
	    return (finish - start) / 1000000;
	}
	
	public static long createNaiveEulerJobList(final JPPFClient jppfClient, int eulerTarget) {
		try {
			ensureNumberOfConnections(jppfClient, MAX_JOBS);
		} catch (Exception e) {
			e.printStackTrace();
		}
		int range = eulerTarget / MAX_JOBS;
		int lower = 0;
		int higher = lower + range;
		
		final List<JPPFJob> jobList = new ArrayList<>(MAX_JOBS);
		start = System.nanoTime();
		while(higher < eulerTarget) {
			System.out.println("Euler Job: [" + lower + " , " + higher + "], range: " + range);
			JPPFJob newJob = createEulerJob(jppfClient, lower, higher);
			jppfClient.submitAsync(newJob);
			jobList.add(newJob);
			lower = higher;
			higher = higher + range;
		}
		
		if(higher >= eulerTarget) {
			System.out.println("Euler Job: [" + lower + " , " + eulerTarget + "], range: " + (eulerTarget - lower));
			JPPFJob newJob = createEulerJob(jppfClient, lower, eulerTarget);
			jppfClient.submitAsync(newJob);
			jobList.add(newJob);
		}
		
		int sumResult = 1;
	    for (final JPPFJob job: jobList) {
	      final List<Task<?>> results = job.awaitResults();
	      sumResult += processExecutionResults(results);
	    }
	    
	    finish = System.nanoTime();
	    System.out.println("SumEuler("+eulerTarget+"): " + sumResult);
	    return (finish - start) / 1000000;
	}
	
	public static JPPFJob createEulerJob(final JPPFClient jppfClient, int lower, int higher){
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
