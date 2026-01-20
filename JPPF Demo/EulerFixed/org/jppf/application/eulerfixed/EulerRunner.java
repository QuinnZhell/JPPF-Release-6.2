package org.jppf.application.eulerfixed;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.jppf.JPPFException;
import org.jppf.client.JPPFClient;
import org.jppf.client.JPPFJob;
import org.jppf.node.protocol.Task;

public class EulerRunner {
	
	static long start;
	static long finish;
	long timeElapsed;
	long[] timeElapseCollection;
	final static int EULER_TARGET = 75000;
	final static int MAX_JOBS = 1;
	final static int TASK_COUNT = 16;
	final static int ITERATION_COUNT = 5;

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
		
		try (BufferedWriter writer = new BufferedWriter(new FileWriter("results.txt"))) {
			long[] timeElapsedCollection = new long[ITERATION_COUNT];
			for(int jobs = 1; jobs <= TASK_COUNT; jobs++) {
				System.out.println("Job Count: " + jobs);
				writer.write("Job Count: " + jobs + "\n");
				
				for(int i = 0; i < ITERATION_COUNT; i++) {
					//timeElapsedCollection[i] = runner.createEulerJobs(EULER_TARGET, jobs);
					timeElapsedCollection[i] = runner.createEulerThreads(EULER_TARGET, jobs);
				}
				
				for(int i = 0; i < timeElapsedCollection.length; i++) {
					System.out.println("Iteration " + i + ": " + timeElapsedCollection[i] + "ms.");
					writer.write("Iteration[" + i +"] Elapsed Time: " + timeElapsedCollection[i] + "ms\n");
				}
				writer.write("\n");
			}
			
			writer.close();
		}
		
		
		} catch(final Exception e) {
			e.printStackTrace();
		}
	}
	

	/**
	 * Creates our job objects for calculating sum of euler totient
	 * @param eulerTarget, inclusive maximum for sum of totients
	 * @throws Exception
	 */
	public long createEulerJobs(int eulerTarget, int numJobs) throws Exception {
		final ExecutorService executor = Executors.newFixedThreadPool(numJobs);
		int totientResult = 1;
		int[] divisions = getDivisionData(2, eulerTarget, numJobs);
		
		start = System.nanoTime();
		try (final JPPFClient jppfClient = new JPPFClient()) {
			final List<Future<JPPFJob>> futures = new ArrayList<>(numJobs);
			for (int i=0; i<numJobs - 1; i++) {
				final JPPFJob job = createJob("euler job: " + i, TASK_COUNT ,divisions[i], divisions[i+1]);
				futures.add(executor.submit(new EulerCallable(jppfClient, job)));
			}
			
			// final job
			futures.add(executor.submit(new EulerCallable(jppfClient, createJob("euler final job", TASK_COUNT ,divisions[numJobs - 1], eulerTarget + 1))));
			
			for (final Future<JPPFJob> future: futures) {
				try {
					final JPPFJob job = future.get();
					// process the job results
					totientResult += processResults(job);
				} catch (final Exception e) {
					e.printStackTrace();
				}
			}
		} finally {
			finish = System.nanoTime();
			executor.shutdown();
		}
		
		print("Result: " + totientResult);
		return (finish - start) / 1000000;
	}
	
	public long createEulerThreads(int eulerTarget, int numTasks) {
		final ExecutorService executor = Executors.newFixedThreadPool(numTasks);
		int totientResult = 1;
		int[] divisions = getDivisionData(2, eulerTarget, numTasks);
		
		start = System.nanoTime();
		try (final JPPFClient jppfClient = new JPPFClient()) {
			final List<Future<JPPFJob>> futures = new ArrayList<>(1);
			futures.add(executor.submit(new EulerCallable(jppfClient, createThreadedJob("tasks", numTasks, divisions, eulerTarget))));
			
			for (final Future<JPPFJob> future: futures) {
				try {
					final JPPFJob job = future.get();
					// process the job results
					totientResult += processResults(job);
				} catch (final Exception e) {
					e.printStackTrace();
				}
			}
		} finally {
			finish = System.nanoTime();
			executor.shutdown();
		}
		
		print("Result: " + totientResult);
		return (finish - start) / 1000000;
	}
	
	/**
	 * Creates a callable unit for the euler jobs
	 */
	public static class EulerCallable implements Callable<JPPFJob> {
		  
		private final JPPFClient jppfClient;
		private final JPPFJob job;
		
		public EulerCallable(final JPPFClient jppfClient, final JPPFJob job) {
			this.jppfClient = jppfClient;
			this.job = job;
		}
		
		@Override
		public JPPFJob call() throws Exception {
			jppfClient.submit(job);
			return job;
		}
	}
	
	/**
	 * Create a singular euler calculation job
	 * @param jobName, identifier
	 * @param nbTasks, how many divisions for this job to handle
	 * @param min, lowest euler target in range
	 * @param max, largest euler target in range
	 * @return job object with the sum of totient for the given min/max range
	 */
	public static JPPFJob createJob(final String jobName, final int nbTasks, int min, int max) {
		final JPPFJob job = new JPPFJob();
		
		print("Creating " + jobName + ", min: " + min + ", max: " + max);
		
		job.setName(jobName);
		for (int i=0; i<nbTasks; i++) {
			final EulerTask task = new EulerTask(min, max);
			
			try {
				job.add(task).setId(jobName + " - " + "task " + i);
			} catch (final Exception e) {
				e.printStackTrace();
			}
		}
		
		return job;
	}
	
	/**
	 * Create a singular euler calculation job
	 * @param jobName, identifier
	 * @param nbTasks, how many divisions for this job to handle
	 * @param min, lowest euler target in range
	 * @param max, largest euler target in range
	 * @return job object with the sum of totient for the given min/max range
	 */
	public static JPPFJob createThreadedJob(final String jobName, final int nbTasks, int[] divisions, int eulerTarget) {
		final JPPFJob job = new JPPFJob();
		job.setName(jobName);
		for (int i=0; i<nbTasks; i++) {
			final EulerTask task = new EulerTask(divisions[i], divisions[i+1]);
			
			try {
				job.add(task).setId(jobName + " - " + "task " + i);
			} catch (final Exception e) {
				e.printStackTrace();
			}
		}
		
		final EulerTask task = new EulerTask(divisions[nbTasks - 1], eulerTarget);
		try {
			job.add(task).setId(jobName + " - " + "task final");
		} catch (JPPFException e) {
			e.printStackTrace();
		}
		
		return job;
	}
	
	/**
	 * Collate results from all euler jobs and their tasks
	 * @param job
	 */
	public static int processResults(final JPPFJob job) {
		int totient = 0;
		final List<Task<?>> results = job.getAllResults();
		for (final Task<?> task: results) {
			Integer res = (Integer) task.getResult();
			totient += res;
		}
		return totient;
	}
	
	public static void print(String message) {
		System.out.println(message);
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
}
