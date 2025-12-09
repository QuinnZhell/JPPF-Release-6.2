package org.jppf.application.euler;

import java.util.ArrayList;
import java.util.List;

import org.jppf.JPPFException;
import org.jppf.client.JPPFClient;
import org.jppf.client.JPPFConnectionPool;
import org.jppf.client.JPPFJob;
import org.jppf.node.protocol.Task;
import org.jppf.utils.Operator;

public class EulerRunner {

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
		//runner.executeBlockingEulerJob(jppfClient, 1000);
		
		// create and execute a non-blocking job
		//runner.executeNonBlockingEulerJob(jppfClient, 1000);
		
		// create and execute 3 jobs concurrently
		runner.executeMultipleConcurrentEulerJobs(jppfClient, 10, 1000);
		
		} catch(final Exception e) {
			e.printStackTrace();
		}
	}
  
	public static void print(String message) {
	  System.out.println(message);
	}
  
  
	public JPPFJob createEulerJob(final int lower, final int higher, final int range) throws JPPFException {
		print("Creating Euler Job [" + lower + " - " + higher + "]");
	  
		JPPFJob job = new JPPFJob();
		job.setName("Creating Euler Job [" + lower + " - " + higher + "]");
	  
		for(int i = lower; i < higher; i ++) {
			job.add("greatestCommonDivisor", EulerCalculation.class, i, range);
		}
	  
		return job;
	}
  
	public void executeBlockingEulerJob(final JPPFClient jppfClient, int range) throws Exception {
		final JPPFJob job = createEulerJob(2, range, range);
	    final List<Task<?>> results = jppfClient.submit(job);
	    
	    int totient = 1;
	    totient += collectEulerResults(job.getName(), results);
	    
	    System.out.println("Totient(" + range + "): " + totient);
	}
	
	public void executeNonBlockingEulerJob(final JPPFClient jppfClient, int range) throws Exception {
	    final JPPFJob job = createEulerJob(2, range, range);
	    jppfClient.submitAsync(job);
	    
	    final List<Task<?>> results = job.awaitResults();
	    
	    int totient = 1;
	    totient += collectEulerResults(job.getName(), results);
	    
	    System.out.println("Totient(" + range + "): " + totient);
	}
	
	public void executeMultipleConcurrentEulerJobs(final JPPFClient jppfClient, final int numberOfJobs, int range) throws Exception {
	    ensureNumberOfConnections(jppfClient, numberOfJobs);
	    final List<JPPFJob> jobList = new ArrayList<>(numberOfJobs);
	    int taskRange = range / numberOfJobs;
	    int higher = 2;
	    
	    for(int i = 0; i < numberOfJobs; i++) {
	    	int lower = higher;
	    	higher += taskRange;
	    	
	    	if(higher > range) {
	    		higher = range;
	    	}
	    	
	    	final JPPFJob job = createEulerJob(lower, higher, range);
	    	jppfClient.submitAsync(job);
	    	jobList.add(job);
	    }
	    
	    int totient = 1;
	    for (final JPPFJob job: jobList) {
	    	final List<Task<?>> results = job.awaitResults();
	    	totient += collectEulerResults(job.getName(), results);
	    }
	    
	    System.out.println("Totient(" + range + "): " + totient);
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
			if(res == 1) {
			totient++;
			}
		}
		
		System.out.printf("Results for job '%s' : %d \n", jobName, totient);
		return totient;
	}
}
