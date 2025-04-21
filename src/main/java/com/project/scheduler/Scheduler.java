package com.project.scheduler;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.TimeUnit;

import com.project.core.Job;
import com.project.core.JobStatus;
import com.project.logging.Logger;
import com.project.management.JobStateManager;
import com.project.management.JobQueueManager;
import com.project.management.SystemController;
/**
 * Scheduler for the CSUbatch system.
 * Responsible for sorting the job queue based on the selected scheduling policy.
 */
public class Scheduler implements Runnable {
    /**
     * Enum representing the available scheduling policies.
     */
    public enum Policy {
        /** First Come First Served scheduling policy. */
        FCFS,
        /** Shortest Job First scheduling policy. */
        SJF,
        /** Priority-based scheduling policy. */
        PRIORITY
    }
    
    // Thread that runs the scheduler
    private Thread schedulerThread;
    
    // Flag to control the scheduler thread
    private volatile boolean running = false;
    
    // The current scheduling policy
    private Policy policy = Policy.FCFS;
    
    // Job queue manager for accessing the job queue
    private JobQueueManager jobQueueManager;
    
    // Job state manager for updating job status
    private JobStateManager jobStateManager;
    
    // Logger for the scheduler
    private Logger logger;
    
    /**
     * Comparator for FCFS (First-Come-First-Served) scheduling.
     * Jobs are sorted by arrival time.
     */
    private static class FCFSComparator implements Comparator<Job> {
        @Override
        public int compare(Job job1, Job job2) {
            return Long.compare(job1.getArrivalTime(), job2.getArrivalTime());
        }
    }
    
    /**
     * Comparator for SJF (Shortest Job First) scheduling.
     * Jobs are sorted by their estimated CPU time.
     */
    private static class SJFComparator implements Comparator<Job> {
        @Override
        public int compare(Job job1, Job job2) {
            return Integer.compare(job1.getCpuTime(), job2.getCpuTime());
        }
    }
    
    /**
     * Comparator for Priority-based scheduling.
     * Jobs are sorted by their priority (higher priority value = higher priority).
     */
    private static class PriorityComparator implements Comparator<Job> {
        @Override
        public int compare(Job job1, Job job2) {
            // Note: Higher priority value means higher priority
            return Integer.compare(job2.getPriority(), job1.getPriority());
        }
    }
    
    /**
     * Creates a new scheduler.
     * 
     * @param jobQueueManager The job queue manager to use
     * @param jobStateManager The job state manager to use
     * @param logger The logger to use
     */
    public Scheduler(JobQueueManager jobQueueManager, JobStateManager jobStateManager, Logger logger) {
        this.jobQueueManager = jobQueueManager;
        this.jobStateManager = jobStateManager;
        this.logger = logger;
    }
    
    /**
     * Starts the scheduler thread.
     */
    public void start() {
        if (schedulerThread == null || !schedulerThread.isAlive()) {
            running = true;
            schedulerThread = new Thread(this, "Scheduler");
            schedulerThread.start();
            if (logger != null) {
                logger.info("Scheduler started with policy: " + policy.toString());
            }
        } else {
            if (logger != null) {
                logger.warning("Attempt to start scheduler that is already running");
            }
        }
    }
    
    /**
     * Sets the scheduling policy and marks the job queue for sorting.
     * 
     * @param policy The new policy to use
     */
    public void setPolicy(Policy policy) {
        if (policy == null) {
            if (logger != null) {
                logger.warning("Attempted to set null policy, using default: FCFS");
            }
            policy = Policy.FCFS;
        }
        
        if (this.policy != policy) {
            this.policy = policy;
            
            if (jobQueueManager != null) {
                logger.info("Setting needsSort to true");
                jobQueueManager.setNeedsSort(true);
                // sort the job queue
                sortJobQueue();
            }
            
            if (logger != null) {
                logger.info("Scheduling policy changed to: " + policy.toString());
            }
        }
    }
    
    /**
     * Gets the current scheduling policy.
     * 
     * @return The current policy
     */
    public Policy getPolicy() {
        return policy;
    }
    
    /**
     * Sorts the job queue according to the current scheduling policy.
     * This method is optimized to only sort when necessary based on the needsSort flag.
     */
    public void sortJobQueue() {
        if (jobQueueManager == null) {
            if (logger != null) {
                logger.warning("Cannot sort job queue - no job queue manager available");
            }
            return;
        }
        
        // Only sort if needed
        if (!jobQueueManager.isNeedingSort()) {
            if (logger != null) {
                logger.debug("Skipping sort as job queue doesn't need sorting");
            }
            return;
        }
        
        if (logger != null) {
            logger.debug("Sorting job queue with policy: " + policy.toString());
        }
        
        // Get all jobs from the queue and sort them
        List<Job> jobs = null;
        
        try {
            jobs = jobQueueManager.getAllJobs("Scheduler-Sort");
            
            if (jobs == null || jobs.isEmpty()) {
                if (logger != null) {
                    logger.debug("Job queue is empty, nothing to sort");
                }
                // Even though empty, we've "sorted" so clear the flag
                jobQueueManager.setNeedsSort(false);
                return;
            }
            
            // Sort based on policy
            switch (policy) {
                case FCFS:
                    // First Come First Served - sort by arrival time
                    Collections.sort(jobs, new FCFSComparator());
                    break;
                case SJF:
                    // Shortest Job First - sort by estimated runtime
                    Collections.sort(jobs, new SJFComparator());
                    break;
                case PRIORITY:
                    // Priority - sort by priority level
                    Collections.sort(jobs, new PriorityComparator());
                    break;
                default:
                    if (logger != null) {
                        logger.warning("Unknown scheduling policy: " + policy + ", defaulting to FCFS");
                    }
                    Collections.sort(jobs, new FCFSComparator());
            }
            
            // Clear the queue and add the sorted jobs back
            jobQueueManager.clearQueue("Scheduler-Sort");
            
            // Re-add all jobs in sorted order
            for (Job job : jobs) {
                SystemController.getInstance().addJob(job, "Scheduler-Sort");
            }
            
            // Clear the needs-sort flag
            jobQueueManager.setNeedsSort(false);
            
            if (logger != null) {
                logger.debug("Job queue sorted successfully with policy: " + policy.toString());
            }
        } catch (Exception e) {
            if (logger != null) {
                logger.error("Error sorting job queue: " + e.getMessage());
            }
        }
    }
    
    /**
     * Stops the scheduler thread.
     */
    public void stop() {
        running = false;
        
        // Interrupt the thread if it's sleeping
        if (schedulerThread != null && schedulerThread.isAlive()) {
            schedulerThread.interrupt();
            
            try {
                // Wait for the thread to terminate
                schedulerThread.join(3000);
                
                if (schedulerThread.isAlive()) {
                    if (logger != null) {
                        logger.warning("Scheduler thread did not terminate gracefully within timeout");
                    }
                } else {
                    if (logger != null) {
                        logger.info("Scheduler stopped successfully");
                    }
                }
            } catch (InterruptedException e) {
                if (logger != null) {
                    logger.error("Interrupted while waiting for scheduler thread to stop: " + e.getMessage());
                }
                Thread.currentThread().interrupt();
            }
        } else {
            if (logger != null) {
                logger.info("Scheduler thread was not running");
            }
        }
    }
    
    /**
     * Main run method for the scheduler thread.
     * Periodically checks if the job queue needs sorting and sorts it if necessary.
     */
    @Override
    public void run() {
        if (logger != null) {
            logger.info("Scheduler thread started with policy: " + policy.toString());
        }
        
        while (running) {
            try {
                // Sort the job queue if needed
                sortJobQueue();
                
                // Sleep for a short time before checking again
                TimeUnit.MILLISECONDS.sleep(500);
            } catch (InterruptedException e) {
                if (running) {
                    if (logger != null) {
                        logger.warning("Scheduler thread interrupted: " + e.getMessage());
                    }
                } else {
                    if (logger != null) {
                        logger.info("Scheduler thread interrupted for shutdown");
                    }
                    break;
                }
            } catch (Exception e) {
                if (logger != null) {
                    logger.error("Error in scheduler thread: " + e.getMessage());
                }
            }
        }
        
        if (logger != null) {
            logger.info("Scheduler thread exiting");
        }
    }

    public String getPolicyName() {
        switch (policy) {
            case FCFS:
                return "FCFS";
            case SJF:
                return "SJF";
            case PRIORITY:
                return "PRIORITY";
            default:
                return "UNKNOWN";
        }
    }
}
