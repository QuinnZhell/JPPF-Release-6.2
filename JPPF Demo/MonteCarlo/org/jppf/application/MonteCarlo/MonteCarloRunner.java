package org.jppf.application.MonteCarlo;

import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import org.jppf.JPPFException;
import org.jppf.client.JPPFClient;
import org.jppf.client.JPPFConnectionPool;
import org.jppf.client.JPPFJob;
import org.jppf.utils.Operator;

import com.opencsv.CSVWriter;

public class MonteCarloRunner {
	private final static int ITERATION_COUNT = 100;
	private final static int NUMBER_OF_POINTS = 1000000;
	private final static int MAX_JOB_COUNT = 100;
	private final static int MAX_TASK_COUNT = 100;
	
	private final static String CURRENT_TEST = "Threads4";
	
	public static void main(final String...args) {
		try (final JPPFClient jppfClient = new JPPFClient()) {
			final MonteCarloRunner runner = new MonteCarloRunner();
			
			/**
			 * REPEAT FOR EACH JOB COUNT
			 */
//			File finalReport = new File("Results/VariableThreadPool/Manual/" + CURRENT_TEST + "Averages.csv");
//			FileWriter finalReportOutputFile = new FileWriter(finalReport);
//	        try (CSVWriter finalReportWriter = new CSVWriter(finalReportOutputFile)) {
//				finalReportWriter.writeNext(new String[]{"Job Count", "Average Result", "Average Job Runtime (ms)", "Average Total Runtime (ms)"});
	        
		        for(int jobCount = 100; jobCount <= MAX_JOB_COUNT; jobCount++) {
					try {
						//File file = new File("Results/Nodes1/"+ "JobCount[" + jobCount + "]" + "results.csv");
						File file = new File("Results/VariableThreadPool/Manual/"+ CURRENT_TEST + "Results.csv");
				        FileWriter outputfile = new FileWriter(file);
				        CSVWriter writer = new CSVWriter(outputfile);
				        
				        /**
				         * PREPARE HEADER
				         * | Iteration Count | Job[N] Result | Job[N] Runtime (ms) | Average Job Result | Average Job Runtime (ms) | Total Runtime (ms) |
				         * |        1        |     3.142     |          63         |       3.147..      |           61             |        84          | 
				         * |        ..       |      ..       |          ..         |         ..         |           ..             |        ..          |       
				         * |     Averages    |      3.141    |          59         |         3.141      |           62             |        81          | 
				         */
				        String[] header = createHeader(jobCount);
						writer.writeNext(header);
						
						/**
						 * REPEATED FOR ITERATION COUNT
						 */
						
						// Recording Averages Across Iterations
						double[] avgResult = new double[jobCount];
						long[] avgRuntime = new long[jobCount];
						
						// Averages Of Iterations
						long avgTotalRuntime = 0;
						double avgOfAvgResult = 0;
						long avgOfAvgRuntime = 0;
						
						for(int iterationCount = 1; iterationCount <= ITERATION_COUNT; iterationCount++) {
							// Prepare job list
							final List<TimeRecordedJob> jobList = new ArrayList<>(jobCount);
							for(int j = 0; j < jobCount; j++) {
								jobList.add(new TimeRecordedJob(jppfClient));
							}
							
							// Execute the problem
							long start = System.nanoTime();
							runner.estimatePi(jppfClient, jobCount, NUMBER_OF_POINTS, jobList);
							long finish = System.nanoTime();
							
							// Record Results
							List<String> results = new ArrayList<>(4 + (jobList.size() * 2));
							results.add(iterationCount+"");
							
							int jobIndex = 0;
							double currentResultAverage = 0;
							long currentRuntimeAverage = 0;
							for(TimeRecordedJob job : jobList) {
								double result = job.getResult();
								avgResult[jobIndex] += result;
								currentResultAverage += result;
								results.add(result + "");
								
								long runtime = job.getRunTime();
								avgRuntime[jobIndex] += runtime;
								currentRuntimeAverage += runtime;
								results.add(runtime + "");
								
								jobIndex++;
							}
							
							currentResultAverage = currentResultAverage / jobList.size();
							avgOfAvgResult += currentResultAverage;
							results.add(currentResultAverage + "");
							
							currentRuntimeAverage = currentRuntimeAverage / jobList.size();
							avgOfAvgRuntime += currentRuntimeAverage;
							results.add(currentRuntimeAverage + "");
							
							
							long totalRuntime = (finish - start) / 1000000;
							avgTotalRuntime += totalRuntime;
							results.add(totalRuntime+"");
							
							String[] resultArr = new String[results.size()];
							writer.writeNext(results.toArray(resultArr));
							
							System.out.println("Iteration (" + iterationCount + "/" + ITERATION_COUNT + ")");
						}
						
						// Record Averages
						List<String> resultsAveraged = new ArrayList<>(4 + (jobCount * 2));
						resultsAveraged.add("Averages");
						
						for(int i = 0; i < jobCount; i++) {
							avgResult[i] = (avgResult[i] / ITERATION_COUNT);
							resultsAveraged.add(avgResult[i]+"");
							
							avgRuntime[i] = (avgRuntime[i] / ITERATION_COUNT);
							resultsAveraged.add(avgRuntime[i]+"");
						}
						
						avgOfAvgResult = avgOfAvgResult / ITERATION_COUNT;
						resultsAveraged.add(avgOfAvgResult + "");
						
						avgOfAvgRuntime = avgOfAvgRuntime / ITERATION_COUNT;
						resultsAveraged.add(avgOfAvgRuntime + "");
						
						avgTotalRuntime = avgTotalRuntime / ITERATION_COUNT;
						resultsAveraged.add(avgTotalRuntime + "");
						
						String[] averagesArr = new String[resultsAveraged.size()];
						writer.writeNext(resultsAveraged.toArray(averagesArr));
						writer.close();
						
//						finalReportWriter.writeNext(new String[]{jobCount + "", avgOfAvgResult + "", avgOfAvgRuntime + "", avgTotalRuntime + ""});
					} catch(final Exception e) {
						e.printStackTrace();
					}
				}
//		        finalReportWriter.close();
//	        } catch(final Exception e) {
//				e.printStackTrace();
//			}
		} catch(final Exception e) {
			e.printStackTrace();
		}
	}
	
	public static String[] createHeader(int jobCount) {
		List<String> header = new ArrayList<>(4 + (jobCount * 2));
        header.add("Iteration Count");
        for(int i = 1; i <= jobCount; i++) {
        	header.add("Job["+i+"] Result");
	        header.add("Job["+i+"] Runtime (ms)");
        }
        header.add("Average Result");
        header.add("Average Runtime (ms)");
        header.add("Total Runtime (ms)");
        String[] headerArr = new String[header.size()];
        return header.toArray(headerArr); 
	}
	
	public void estimatePi(final JPPFClient jppfClient, final int numberOfJobs, final int numberOfPoints, List<TimeRecordedJob> jobs) throws Exception {
		ensureNumberOfConnections(jppfClient, numberOfJobs);
		
		final CountDownLatch countDown = new CountDownLatch(jobs.size());
		for(final TimeRecordedJob job : jobs) {
			JPPFJob newJob = createJob(jppfClient, numberOfPoints);
			job.assignJob(newJob, numberOfPoints, countDown);
		}
		
		countDown.await();
	}
	
	// Create a job that estimated Pi from a number of point evaluations
	public static JPPFJob createJob(final JPPFClient jppfClient, int numberOfPoints){
		JPPFJob job = new JPPFJob();
		job.setName("EstimatePi(" + numberOfPoints+")");
		int pointsPerTask = numberOfPoints / MAX_TASK_COUNT;
		
		for(int i = 0; i < MAX_TASK_COUNT; i++) {
			try {
				job.add(CountPointsTask.class, pointsPerTask);
			} catch (JPPFException e) {
				e.printStackTrace();
			}
		}
		
		//job.getSLA().setSuspended(true);
		return job;
	}
	
	
	
	public void ensureNumberOfConnections(final JPPFClient jppfClient, final int numberOfConnections) throws Exception {
	    final JPPFConnectionPool pool = jppfClient.awaitActiveConnectionPool();
	    
	    if (pool.getConnections().size() != numberOfConnections) {
	      pool.setSize(numberOfConnections);
	    }
	    
	    pool.awaitActiveConnections(Operator.AT_LEAST, numberOfConnections);
	  }
}
