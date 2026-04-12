package org.jppf.application.MonteCarlo;

import java.util.List;
import java.util.concurrent.CountDownLatch;

import org.jppf.client.JPPFClient;
import org.jppf.client.JPPFJob;
import org.jppf.client.event.JobEvent;
import org.jppf.client.event.JobListenerAdapter;
import org.jppf.node.protocol.Task;

public class TimeRecordedJob {
	long start;
	long finish;
	double result;
	JPPFJob job;
	JPPFClient client;
	
	TimeRecordedJob(JPPFClient client) {
		this.client = client;
	}
	
	public void assignJob(JPPFJob job, int numberOfPoints, CountDownLatch countDown) {
		this.job = job;
		
		this.job.addJobListener(new JobListenerAdapter() {
			@Override
	          public synchronized void jobEnded(final JobEvent event) {
				processResults(event.getJob().getAllResults(), numberOfPoints);
				countDown.countDown();
			}
		});
		
		client.submitAsync(job);
		start = System.nanoTime();
	}
	
	public long getStartTime() {
		return start;
	}
	
	public long getFinishTime() {
		return finish;
	}
	
	public long getRunTime() {
		return (this.finish - this.start) / 1000000;
	}
	
	public double getResult() {
		return result;
	}
	
	public JPPFJob getJob() {
		return job;
	}
	
	public void processResults(final List<Task<?>> results, int numberOfPoints) {
		double countInside = 0;
		
		for(final Task<?> task : results) {
			
			countInside += (Integer) task.getResult();
		}
		
		this.result = (4 * countInside) / numberOfPoints;
		this.finish = System.nanoTime();
	}
}
