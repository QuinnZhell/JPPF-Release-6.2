package org.jppf.application.euler;

import java.util.List;

import org.jppf.client.JPPFClient;
import org.jppf.client.JPPFJob;
import org.jppf.node.protocol.Task;

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
      runner.calculateEulerTotient(jppfClient);

      // create and execute a non-blocking job
      //runner.executeNonBlockingJob(jppfClient);

      // create and execute 3 jobs concurrently
      //runner.executeMultipleConcurrentJobs(jppfClient, 3);

    } catch(final Exception e) {
      e.printStackTrace();
    }
  }
  
  public static void print(String message) {
	  System.out.println(message);
  }

  /**
   * Execute a job in blocking mode. The application will be blocked until the job execution is complete.
   * @param jppfClient the {@link JPPFClient} instance which submits the job for execution.
   * @throws Exception if an error occurs while executing the job.
   */
  public void calculateEulerTotient(final JPPFClient jppfClient) throws Exception {
	print("calculate euler totient started");
    // Create a job
	JPPFJob job = new JPPFJob();
	job.setName("Euler Totient");
	
	for(int i = 2; i < 11; i++) {
		job.add("greatestCommonDivisor", EulerCalculation.class, 1, 2);
	}
    job.getSLA().setSuspended(true);

    print("submitting jobs");
    jppfClient.submitAsync(job);
    final List<Task<?>> results = job.awaitResults();

    // process the results
    processExecutionResults(job.getName(), results);
  }

  /**
   * Process the execution results of each submitted task.
   * @param jobName the name of the job whose results are processed. 
   * @param results the tasks results after execution on the grid.
   */
  public synchronized void processExecutionResults(final String jobName, final List<Task<?>> results) {
    System.out.printf("Results for job '%s' :\n", jobName);
    
    int totient = 1;
    // process the results
//    for (final Task<?> task: results) {
//    	Integer res = task.getResult();
//    	if(res == 1) {
//    		totient++;
//    	}
//    	System.out.println(task.getResult().getClass());
//    }
    
    System.out.print(totient);
  }
}
