package org.jppf.application.eulerfixed;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.jppf.JPPFException;
import org.jppf.client.JPPFClient;
import org.jppf.client.JPPFConnectionPool;
import org.jppf.client.JPPFJob;
import org.jppf.node.protocol.Task;
import org.jppf.utils.Operator;

import com.opencsv.CSVWriter;

public class EulerTaskParallel {
	
	static long start;
	static long finish;
	final static int EULER_TARGET = 10000;
	static int TASK_COUNT = 16;
	final static int ITERATION_COUNT = 10;
	static boolean SINGLE_TASK = false;
	
	enum EulerConfig {
		TunedSingleJobMultipleTask("TunedSingleJobMultipleTask", false) {
			@Override
			public long eulerSum(JPPFClient jppfClient, int eulerTarget) {
				return createTunedEulerJob(jppfClient, EULER_TARGET);
			}
			
		},
		TunedMultipleJobMultipleTask("TunedMultipleJobMultipleTask", false) {
			@Override
			public long eulerSum(JPPFClient jppfClient, int eulerTarget) {
				return createTunedJobList(jppfClient, EULER_TARGET);
			}
		},
		TunedMultipleJobSingleTask("TunedMultipleJobSingleTask", true) {
			@Override
			public long eulerSum(JPPFClient jppfClient, int eulerTarget) {
				return createTunedJobList(jppfClient, EULER_TARGET);
			}
		},
		NaiveSingleJobMultipleTask("NaiveSingleJobMultipleTask", false) {
			@Override
			public long eulerSum(JPPFClient jppfClient, int eulerTarget) {
				return createNaiveEulerJob(jppfClient, EULER_TARGET);
			}
			
		},
		NaiveMultipleJobMultipleTask("NaiveMultipleJobMultipleTask", false) {
			@Override
			public long eulerSum(JPPFClient jppfClient, int eulerTarget) {
				return createNaiveJobList(jppfClient, EULER_TARGET);
			}
			
		},
		NaiveMultipleJobSingleTask("NaiveMultipleJobSingleTask", true) {
			@Override
			public long eulerSum(JPPFClient jppfClient, int eulerTarget) {
				return createNaiveJobList(jppfClient, EULER_TARGET);
			}
			
		}
		;
		
		private String name;
		private boolean singleTask;
		
		private EulerConfig(String name, boolean singleTask) {
			this.name = name;
			this.singleTask = singleTask;
		}
		
		public abstract long eulerSum(JPPFClient jppfClient, int eulerTarget);
		
		public String getName() {
			return name;
		}
		
		public boolean isSingleTask() {
			return singleTask;
		}
	}
	
	public static void main(final String...args) {
		long timeElapsed;
		
		try (final JPPFClient jppfClient = new JPPFClient()) {
			
			for(EulerConfig euler : EulerConfig.values()) {
					File file = new File("Results/ThreadPool/SingleJob_MultipleTask/ThreadPool_16/"+ euler.getName() + "results.csv");
					SINGLE_TASK = euler.isSingleTask();
					
					try {
						// create FileWriter object with file as parameter
				        FileWriter outputfile = new FileWriter(file);

				        // create CSVWriter object filewriter object as parameter
				        CSVWriter writer = new CSVWriter(outputfile);
				        
				        String[] header = {"Task Count" ,"Iteration 1 Elapsed Time (ms)", "Iteration 2 Elapsed Time (ms)", "Iteration 3 Elapsed Time (ms)", "Iteration 4 Elapsed Time (ms)", "Iteration 5 Elapsed Time (ms)", "Iteration 6 Elapsed Time (ms)", "Iteration 7 Elapsed Time (ms)", "Iteration 8 Elapsed Time (ms)", "Iteration 9 Elapsed Time (ms)", "Iteration 10 Elapsed Time (ms)", "Average Elapsed Time (ms)"};
				        //String[] header = {"Task Count" ,"Iteration 1 Elapsed Time (ms)", "Iteration 2 Elapsed Time (ms)", "Iteration 3 Elapsed Time (ms)", "Iteration 4 Elapsed Time (ms)", "Iteration 5 Elapsed Time (ms)", "Average Elapsed Time (ms)"};
				        writer.writeNext(header);
				        //for(int task = 1; task <= 20; task++) {
				        //	TASK_COUNT = task;
				        
					        // Experiment
					        int elapsedTimeAverage = 0;
					        String[] resultData = new String[12];
					        resultData[0] = TASK_COUNT+"";
							for(int i = 0; i < ITERATION_COUNT; i++) {
								timeElapsed = euler.eulerSum(jppfClient, EULER_TARGET);
								elapsedTimeAverage += timeElapsed;
								
								// write results
								System.out.println("Iteration " + i + ": " + timeElapsed + "ms.");
								resultData[i+1] = timeElapsed+"";
							}
							resultData[11] = (elapsedTimeAverage / ITERATION_COUNT) + "";
							writer.writeNext(resultData);
						
				        //}
						
						writer.close();
					}
					catch (IOException e) {
				        e.printStackTrace();
				    }
			}
		} catch(final Exception e) {
			e.printStackTrace();
		}
	}
	
	// single job, multiple task
	public static long createNaiveEulerJob(final JPPFClient jppfClient, int eulerRemaining) {
		JPPFJob job = new JPPFJob();
		job.setName("EulerSum["+eulerRemaining+"]");
		
		int itemsPerTask = eulerRemaining / TASK_COUNT;
		System.out.println("Items Expected Per Task: " + itemsPerTask);
		
		for(int i = 0; i < TASK_COUNT; i++) {
			final List<Integer> targets = new ArrayList<Integer>();
			
			for(int j = 0; j < itemsPerTask; j++) {
				targets.add(eulerRemaining);
				//System.out.println("Added " + eulerRemaining);
				eulerRemaining--;
			}
			
			if(i == TASK_COUNT - 1) {
				//System.out.println("Remaining to add");
				while(eulerRemaining > 0) {
					targets.add(eulerRemaining);
					eulerRemaining--;
				}
			}
			
			int[] target_array = targets.stream().mapToInt(t->t).toArray();
			
			//System.out.println("Item Count: " + target_array.length);
			
			try {
				job.add(EulerArrayTask.class, target_array);
			} catch (JPPFException e) {
				e.printStackTrace();
			}
		}
		
		System.out.println("Task Count: " + job.getTaskCount());
		
		start = System.nanoTime();
		jppfClient.submitAsync(job);
		final List<Task<?>> results = job.awaitResults();
		//System.out.println("Got results");
		finish = System.nanoTime();
		
		int eulerSumFinal = processExecutionResults(results);
		System.out.println("SumEuler("+EULER_TARGET+"): " + eulerSumFinal);
		
		return (finish - start) / 1000000;
	}
	
	// multiple job, multiple task
	public static long createNaiveJobList(final JPPFClient jppfClient, int eulerRemaining) {
		try {
			ensureNumberOfConnections(jppfClient, TASK_COUNT);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		final List<JPPFJob> jobList = new ArrayList<>(TASK_COUNT);
		int itemsPerTask = eulerRemaining / TASK_COUNT;
		
		for(int i = 0; i < TASK_COUNT; i++) {
			final List<Integer> targets = new ArrayList<Integer>();
			
			for(int j = 0; j < itemsPerTask; j++) {
				targets.add(eulerRemaining);
				//System.out.println("Added " + eulerRemaining);
				eulerRemaining--;
			}
			
			if(i == TASK_COUNT - 1) {
				//System.out.println("Remaining to add");
				while(eulerRemaining > 0) {
					targets.add(eulerRemaining);
					eulerRemaining--;
				}
			}
			
			int[] target_array = targets.stream().mapToInt(t->t).toArray();
			
			//System.out.println("Item Count: " + target_array.length);
			
			try {
				JPPFJob job = new JPPFJob();
				
				if(SINGLE_TASK) {
					job.add(EulerArrayTask.class, target_array);
				} else {
					for(int task : target_array) {
						job.add(EulerArrayTask.class, new int[] {task});
					}
				}
				
				jobList.add(job);
			} catch (JPPFException e) {
				e.printStackTrace();
			}
		}
		
		start = System.nanoTime();
		for (final JPPFJob job : jobList) {
			jppfClient.submitAsync(job);
		}
		
		int sumResult = 0;
		for (final JPPFJob job: jobList) {
		      final List<Task<?>> results = job.awaitResults();
		      sumResult += processExecutionResults(results);
		}
		finish = System.nanoTime();
		System.out.println("SumEuler("+EULER_TARGET+"): " + sumResult);
		
		return (finish - start) / 1000000;
	}
	
	// single job, multiple task
	public static long createTunedEulerJob(final JPPFClient jppfClient, int eulerRemaining) {
		JPPFJob job = new JPPFJob();
		job.setName("EulerSum["+eulerRemaining+"]");
		
		int itemsPerTask = eulerRemaining / TASK_COUNT;
		int lower_target = 1;
		
		for(int i = 0; i < TASK_COUNT; i++) {
			final List<Integer> targets = new ArrayList<Integer>();
			
			targets.add(eulerRemaining);
			eulerRemaining--;
			
			for(int j = 0; j < itemsPerTask - 1; j++) {
				targets.add(lower_target);
				lower_target++;
			}
			
			//System.out.print("}");
			//System.out.println("");
			
			if(i == TASK_COUNT - 1) {
				System.out.println("lower: " + lower_target);
				System.out.println("higher: " + eulerRemaining);
				while(lower_target <= eulerRemaining) {
					targets.add(lower_target);
					lower_target++;
				}
			}
			
			int[] target_array = targets.stream().mapToInt(t->t).toArray();
			
			try {
				job.add(EulerArrayTask.class, target_array);
			} catch (JPPFException e) {
				e.printStackTrace();
			}
		}
		
		System.out.println("Task Count: " + job.getTaskCount());
		
		start = System.nanoTime();
		final List<Task<?>> results = jppfClient.submit(job);
		finish = System.nanoTime();
		
		int eulerSumFinal = processExecutionResults(results);
		System.out.println("SumEuler("+EULER_TARGET+"): " + eulerSumFinal);
		
		return (finish - start) / 1000000;
	}
	
	// multiple job, multiple task
	public static long createTunedJobList(final JPPFClient jppfClient, int eulerRemaining) {
		try {
			ensureNumberOfConnections(jppfClient, TASK_COUNT);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		final List<JPPFJob> jobList = new ArrayList<>(TASK_COUNT);
		int itemsPerTask = eulerRemaining / TASK_COUNT;
		int lower_target = 1;
		
		for(int i = 0; i < TASK_COUNT; i++) {
			final List<Integer> targets = new ArrayList<Integer>();
			
			targets.add(eulerRemaining);
			eulerRemaining--;
			
			for(int j = 0; j < itemsPerTask - 1; j++) {
				targets.add(lower_target);
				lower_target++;
			}
			
			//System.out.print("}");
			//System.out.println("");
			
			if(i == TASK_COUNT - 1) {
				System.out.println("lower: " + lower_target);
				System.out.println("higher: " + eulerRemaining);
				while(lower_target <= eulerRemaining) {
					targets.add(lower_target);
					lower_target++;
				}
			}
			
			int[] target_array = targets.stream().mapToInt(t->t).toArray();
			
			try {
				JPPFJob job = new JPPFJob();
				
				if(SINGLE_TASK) {
					job.add(EulerArrayTask.class, target_array);
				} else {
					for(int task : target_array) {
						job.add(EulerArrayTask.class, new int[] {task});
					}
				}
				
				jobList.add(job);
			} catch (JPPFException e) {
				e.printStackTrace();
			}
		}
		
		start = System.nanoTime();
		for (final JPPFJob job : jobList) {
			jppfClient.submitAsync(job);
		}
		
		int sumResult = 0;
		for (final JPPFJob job: jobList) {
		      final List<Task<?>> results = job.awaitResults();
		      sumResult += processExecutionResults(results);
		}
		finish = System.nanoTime();
		
		System.out.println("SumEuler("+EULER_TARGET+"): " + sumResult);
		
		return (finish - start) / 1000000;
	}
	
	public static int processExecutionResults(final List<Task<?>> results) {
		//System.out.println("Collecting results");
		int sumResult = 0;
		for (final Task<?> task: results) {
			sumResult += (Integer) task.getResult();
			//System.out.println(counter + " : " + (Integer) task.getResult());
		}
		
		return sumResult;
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
}
