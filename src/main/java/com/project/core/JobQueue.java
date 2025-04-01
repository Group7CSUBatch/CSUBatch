package com.project.core;

import java.util.LinkedList;
import java.util.Queue;
import java.util.stream.Stream;

/**
 * JobQueue class manages the queue of jobs to be executed.
 * It uses a FIFO queue structure and provides thread-safe operations.
 */
public class JobQueue {
    private final Queue<Job> queue = new LinkedList<>();
    
    // Add a variable to track the currently running job
    private Job runningJob = null;

    /**
     * Adds a job to the queue.
     *
     * @param job The job to add
     */
    public synchronized void addJob(Job job) {
        queue.offer(job);
        notify(); // Notify that a job is available
    }

    /**
     * Retrieves and removes the next job from the queue.
     * If the queue is empty, waits until a job becomes available.
     * Also updates the runningJob reference to keep track of the job after removal.
     *
     * @return The next job
     * @throws InterruptedException If the waiting thread is interrupted
     */
    public synchronized Job retrieveJob() throws InterruptedException {
        while (queue.isEmpty()) {
            wait(); // Wait until a job is available
        }
        Job job = queue.poll();
        runningJob = job; // Update running job reference
        return job;
    }

    /**
     * Peeks at the next job in the queue without removing it.
     *
     * @return The next job, or null if the queue is empty
     */
    public synchronized Job peekJob() {
        return queue.peek();
    }

    /**
     * Checks if the queue is empty.
     *
     * @return true if the queue is empty, false otherwise
     */
    public synchronized boolean isEmpty() {
        return queue.isEmpty();
    }

    /**
     * Returns the number of jobs in the queue.
     *
     * @return The number of jobs
     */
    public synchronized int size() {
        return queue.size();
    }

    /**
     * Clears all jobs from the queue.
     */
    public synchronized void clear() {
        queue.clear();
        runningJob = null; // Clear the running job reference when queue is cleared
    }

    /**
     * Retrieves and removes the next job from the queue without waiting.
     * Unlike retrieveJob(), this method does not block if the queue is empty.
     * Also updates the runningJob reference to keep track of the job after removal.
     *
     * @return The next job, or null if the queue is empty
     */
    public synchronized Job pollJob() {
        if (queue.isEmpty()) {
            return null;
        }
        Job job = queue.poll();
        runningJob = job; // Update running job reference
        return job;
    }

    /**
     * Adds a job back to the queue (for rescheduling).
     * 
     * @param job The job to reschedule
     */
    public synchronized void rescheduleJob(Job job) {
        addJob(job);
    }
    
    /**
     * Returns a stream of all jobs in the queue.
     * This is primarily used for operations like getting the shortest job
     * or highest priority job without removing them from the queue.
     * 
     * Note: This method should be used with caution and within a synchronized block
     * to prevent concurrent modification issues.
     * 
     * @return A stream of all jobs
     */
    public synchronized Stream<Job> stream() {
        return queue.stream();
    }
    
    /**
     * Gets the currently running job.
     * 
     * @return The currently running job, or null if no job is running
     */
    public synchronized Job getRunningJob() {
        return runningJob;
    }
    
    /**
     * Sets the currently running job.
     * 
     * @param job The job to set as currently running
     */
    public synchronized void setRunningJob(Job job) {
        this.runningJob = job;
    }
    
    /**
     * Clears the currently running job reference.
     * This should be called when a job is completed or canceled.
     */
    public synchronized void clearRunningJob() {
        this.runningJob = null;
    }
}
