package org.jppf.application.euler;

import java.util.ArrayList;
import java.util.List;

import org.jppf.JPPFException;
import org.jppf.application.eulerfixed.EulerRunner;
import org.jppf.client.JPPFClient;
import org.jppf.client.JPPFConnectionPool;
import org.jppf.client.JPPFJob;
import org.jppf.node.protocol.Task;
import org.jppf.utils.Operator;

public class EulerRunner {
	
	long start;
	long finish;
	long timeElapsed;
	long[] timeElapseCollection;

	/**
	* The entry point for this application runner to be run from a Java command line.
	* @param args by default, we do not use the command line arguments,
	* however nothing prevents us from using them if need be.
	*/
	public static void main(final String...args) {

		// create the JPPFClient. This constructor call causes JPPF to read the configuration file
		// and connect with one or multiple JPPF drivers.
		print("starting client");
		try (final JPPFClient jppfClient = new JPPFClient()) {
		print("client started, starting runner");
		// create a runner instance.
		final EulerRunner runner = new EulerRunner();
		print("runner started");
		// create and execute a blocking job
		//runner.executeBlockingEulerJob(jppfClient, 10000);
		
		// create and execute a non-blocking job
		//runner.executeNonBlockingEulerJob(jppfClient, 100);
		
		// create and execute 3 jobs concurrently
		//runner.executeMultipleConcurrentEulerJobs(jppfClient, 16, 1000);
		
		long[] timeElapsedCollection = new long[5];
		for(int jobs = 16; jobs <= 16; jobs++) {
			System.out.println("Job Count: " + jobs);
			
			for(int i = 0; i < 5; i++) {
				timeElapsedCollection[i] = runner.executeEulerJobParallelism(jppfClient, jobs, 10000);
			}
			
			for(int i = 0; i < timeElapsedCollection.length; i++) {
				System.out.println("Iteration " + i + ": " + timeElapsedCollection[i] + "ms.");
			}
		}
		
		
		
		} catch(final Exception e) {
			e.printStackTrace();
		}
	}
	
	public int[] getDivisionData(final int min, final int max, final int divisor) {
		int range = (max - min) / divisor;
		int[] divisions = new int[divisor + 1];
		divisions[0] = min;
		
		int counter = min;
		for(int i = 1; i < divisions.length; i++) {
			counter += range;
			divisions[i] = counter;
		}
		divisions[divisor] = max;
		
		return divisions;
	}
	
	public JPPFJob createEulerJob(final int min, final int max) throws JPPFException {
		JPPFJob job = new JPPFJob();
		
		job.setName("Euler Sum: " + min + " -> " + max);
		for(int i = min; i < max; i++) {
			job.add("euler", EulerCalculation.class, i);
		}
	  
		return job;
	}
	
	public long executeEulerJobParallelism(final JPPFClient jppfClient, final int numberOfJobs, int range) throws Exception {
	    start = System.nanoTime();
		ensureNumberOfConnections(jppfClient, 1);
	    final List<JPPFJob> jobList = new ArrayList<>(numberOfJobs);
	    
	    int[] divisions = getDivisionData(2, range, numberOfJobs);
	    for(int i = 1; i < divisions.length; i++) {
	    	final JPPFJob job = createEulerJob(divisions[i-1], divisions[i]);
	    	jppfClient.submitAsync(job);
	    	jobList.add(job);
	    }
	    int totient = 1;
	    for (final JPPFJob job: jobList) {
	    	final List<Task<?>> results = job.awaitResults();
	    	totient += collectEulerResults(job.getName(), results);
	    }
	    
	    finish = System.nanoTime();
	    return (finish - start) / 1000000;
	}
	
	public static void print(String message) {
	  System.out.println(message);
	}
  
	public void ensureNumberOfConnections(final JPPFClient jppfClient, final int numberOfConnections) throws Exception {
	    final JPPFConnectionPool pool = jppfClient.awaitActiveConnectionPool();
	    if (pool.getConnections().size() != numberOfConnections) {
	      pool.setSize(numberOfConnections);
	    }
	    pool.awaitActiveConnections(Operator.AT_LEAST, numberOfConnections);
	  }

	public synchronized int collectEulerResults(final String jobName, final List<Task<?>> results) {
		int totient = 0;
		for (final Task<?> task: results) {
			Integer res = (Integer) task.getResult();
			totient += res;
		}
		return totient;
	}
}
