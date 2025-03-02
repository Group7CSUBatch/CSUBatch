package com.project;

import java.util.Comparator;
import java.util.PriorityQueue;

/**
 * Scheduler class responsible for managing job scheduling policies and job queue.
 * Implements the Runnable interface to operate in its own thread.
 */
public class Scheduler implements Runnable {
    private final JobQueue jobQueue;
    private SchedulingPolicy currentPolicy;
    private LoggingSystem loggingSystem;

    /**
     * Enum representing different scheduling policies.
     */
    public enum SchedulingPolicy {
        /**
         * First Come First Served - jobs are executed in the order they arrive.
         */
        FCFS,

        /**
         * Shortest Job First - jobs with the shortest CPU time are executed first.
         */
        SJF,

        /**
         * Priority-based scheduling - jobs with higher priority (lower value) are executed first.
         */
        PRIORITY
    }

    /**
     * Constructs a new Scheduler with the specified job queue.
     * Default scheduling policy is set to FCFS.
     *
     * @param jobQueue The job queue to be managed by this scheduler
     * @throws IllegalArgumentException if jobQueue is null
     */
    public Scheduler(JobQueue jobQueue) {
        if (jobQueue == null) {
            throw new IllegalArgumentException("JobQueue cannot be null");
        }
        this.jobQueue = jobQueue;
        this.currentPolicy = SchedulingPolicy.FCFS;
    }

    /**
     * Sets the scheduling policy and reschedules jobs if necessary.
     *
     * @param policy The scheduling policy to set
     * @throws IllegalArgumentException if policy is null
     */
    public void setPolicy(SchedulingPolicy policy) {
        if (policy == null) {
            throw new IllegalArgumentException("Scheduling policy cannot be null");
        }
        this.currentPolicy = policy;
        System.out.println("Scheduling policy is switched to " + policy + ".");
        rescheduleJobs();
        System.out.println("All waiting jobs have been rescheduled.");
    }

    /**
     * Reschedules jobs based on the current scheduling policy.
     * Jobs are reordered in the queue according to the policy.
     */
    private void rescheduleJobs() {
        synchronized (jobQueue) {
            // Create a temporary priority queue based on the current policy
            PriorityQueue<Job> tempQueue;
            switch (currentPolicy) {
                case SJF:
                    tempQueue = new PriorityQueue<>(Comparator.comparingInt(Job::getCpuTime));
                    break;
                case PRIORITY:
                    tempQueue = new PriorityQueue<>(Comparator.comparingInt(Job::getPriority));
                    break;
                default:
                    return; // FCFS does not require rescheduling
            }
            // Add all jobs to the temporary queue
            while (!jobQueue.isEmpty()) {
                try {
                    tempQueue.add(jobQueue.retrieveJob());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt(); // Restore the interrupted status
                    System.err.println("Thread was interrupted during job retrieval: " + e.getMessage());
                    return; // Exit the method if interrupted
                }
            }
            // Add them back to the job queue in the new order
            while (!tempQueue.isEmpty()) {
                jobQueue.addJob(tempQueue.poll());
            }
        }
    }

    /**
     * Sets the logging system for this scheduler.
     *
     * @param loggingSystem The logging system to use
     */
    public void setLoggingSystem(LoggingSystem loggingSystem) {
        this.loggingSystem = loggingSystem;
    }

    /**
     * Gets the next job to be executed based on the current scheduling policy.
     *
     * @return The next job to be executed
     * @throws InterruptedException if the thread is interrupted while waiting
     */
    public Job getNextJob() throws InterruptedException {
        Job job;
        switch (currentPolicy) {
            case FCFS:
                job = jobQueue.retrieveJob();
                break;
            case SJF:
                job = getShortestJob();
                break;
            case PRIORITY:
                job = getHighestPriorityJob();
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + currentPolicy);
        }

        if (job != null && loggingSystem != null) {
            job.setStatus("Selected");
            loggingSystem.updateJobQueueView(job);
            loggingSystem.logTransaction("Job selected for execution: " + job.getName());
            loggingSystem.logJobTransaction("Job selected for execution: " + job.getName());
        }

        return job;
    }

    /**
     * Gets the job with the shortest CPU time.
     *
     * @return The job with the shortest CPU time, or null if no jobs are available
     */
    private Job getShortestJob() {
        synchronized (jobQueue) {
            return jobQueue.stream().min(Comparator.comparingInt(Job::getCpuTime)).orElse(null);
        }
    }

    /**
     * Gets the job with the highest priority (lowest priority value).
     *
     * @return The job with the highest priority, or null if no jobs are available
     */
    private Job getHighestPriorityJob() {
        synchronized (jobQueue) {
            return jobQueue.stream().min(Comparator.comparingInt(Job::getPriority)).orElse(null);
        }
    }

    /**
     * Gets the job queue managed by this scheduler.
     *
     * @return The job queue
     */
    public JobQueue getJobQueue() {
        return jobQueue;
    }

    /**
     * Gets the current scheduling policy.
     *
     * @return The current scheduling policy
     */
    public SchedulingPolicy getCurrentPolicy() {
        return currentPolicy;
    }

    /**
     * The run method implementation for the Runnable interface.
     * This is a placeholder as the actual job execution is handled by the Dispatcher.
     * In a more complex implementation, this method could be used for monitoring
     * and managing the scheduling process.
     */
    @Override
    public void run() {
        System.out.println("Scheduler thread started.");
        // This is a placeholder. The actual job execution is handled by the Dispatcher.
        // In a more complex implementation, this method could be used for monitoring
        // and managing the scheduling process.
    }
}
