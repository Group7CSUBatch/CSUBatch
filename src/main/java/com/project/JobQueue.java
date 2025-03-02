package com.project;

import java.util.LinkedList;
import java.util.Queue;
import java.util.stream.Stream;

/**
 * Represents a thread-safe queue of jobs for the CSUbatch scheduling system.
 * Provides methods for adding, retrieving, and managing jobs in the queue.
 */
public class JobQueue {
    private final Queue<Job> queue;
    private final Object lock = new Object();

    /**
     * Constructs a new JobQueue with an empty LinkedList.
     */
    public JobQueue() {
        this.queue = new LinkedList<>();
    }

    /**
     * Adds a job to the queue and notifies waiting threads.
     *
     * @param job The job to add to the queue
     * @throws IllegalArgumentException if the job is null
     */
    public void addJob(Job job) {
        synchronized (lock) {
            if (job == null) {
                throw new IllegalArgumentException("Job cannot be null");
            }
            queue.add(job);
            lock.notifyAll();
        }
    }

    /**
     * Retrieves and removes the head of the queue, waiting if necessary
     * until a job becomes available.
     *
     * @return The head job of the queue
     * @throws InterruptedException if the current thread is interrupted
     */
    public Job retrieveJob() throws InterruptedException {
        synchronized (lock) {
            while (queue.isEmpty()) {
                lock.wait();
            }
            return queue.poll();
        }
    }

    /**
     * Reschedules a job by adding it back to the queue.
     *
     * @param job The job to reschedule
     * @throws IllegalArgumentException if the job is null
     */
    public void rescheduleJob(Job job) {
        synchronized (lock) {
            if (job == null) {
                throw new IllegalArgumentException("Job cannot be null");
            }
            queue.add(job);
            lock.notifyAll();
        }
    }

    /**
     * Checks if the queue is empty.
     *
     * @return true if the queue is empty, false otherwise
     */
    public boolean isEmpty() {
        synchronized (lock) {
            return queue.isEmpty();
        }
    }

    /**
     * Returns a stream of jobs in the queue.
     *
     * @return A stream of jobs in the queue
     */
    public Stream<Job> stream() {
        synchronized (lock) {
            return queue.stream();
        }
    }

    /**
     * Returns the number of jobs in the queue.
     *
     * @return The number of jobs in the queue
     */
    public int size() {
        synchronized (lock) {
            return queue.size();
        }
    }
}
