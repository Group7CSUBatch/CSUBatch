package com.project.management;

import com.project.core.Job;
import com.project.core.JobQueue;
import com.project.core.JobStatus;
import com.project.logging.Logger;
import com.project.scheduler.Scheduler;
import com.project.scheduler.Dispatcher;

import java.util.List;

/**
 * SystemController class responsible for managing the scheduling system state
 * and controlling job scheduling and execution.
 * Implemented as a singleton to ensure only one instance exists.
 */
public class SystemController {
    // Singleton instance
    private static volatile SystemController instance;
    
    // Private reference to JobQueue, only accessible via JobQueueManager
    private final JobQueue jobQueue;
    private Scheduler scheduler;
    private Dispatcher dispatcher;
    private final JobStateManager jobStateManager;
    private final JobQueueManager jobQueueManager;
    private final PerformanceMetrics performanceMetrics;
    private boolean systemRunning;
    private boolean systemPaused;

    /**
     * Private constructor to prevent direct instantiation.
     *
     * @param jobQueue   The job queue
     * @param scheduler  The scheduler (can be null for deferred initialization)
     * @param dispatcher The dispatcher (can be null for deferred initialization)
     * @param logger     The logger to use for the job state manager
     * @throws IllegalArgumentException if jobQueue is null
     */
    private SystemController(JobQueue jobQueue, Scheduler scheduler, Dispatcher dispatcher, Logger logger) {
        if (jobQueue == null) {
            throw new IllegalArgumentException("JobQueue cannot be null");
        }

        this.jobQueue = jobQueue;
        this.scheduler = scheduler;
        this.dispatcher = dispatcher;
        this.jobStateManager = new JobStateManager(logger);
        this.jobQueueManager = new JobQueueManager(jobQueue, jobStateManager, logger);
        this.performanceMetrics = new PerformanceMetrics();
        
        // Connect managers to each other
        jobStateManager.setJobQueueManager(jobQueueManager);
    }

    /**
     * Gets the singleton instance of SystemController.
     * Creates it if it doesn't exist using double-checked locking.
     *
     * @param jobQueue   The job queue (only used for first initialization)
     * @param scheduler  The scheduler (only used for first initialization)
     * @param dispatcher The dispatcher (only used for first initialization) 
     * @param logger     The logger (only used for first initialization)
     * @return The singleton SystemController instance
     */
    public static SystemController getInstance(JobQueue jobQueue, Scheduler scheduler, 
                                            Dispatcher dispatcher, Logger logger) {
        if (instance == null) {
            synchronized (SystemController.class) {
                if (instance == null) {
                    instance = new SystemController(jobQueue, scheduler, dispatcher, logger);
                }
            }
        }
        return instance;
    }

    // getexisting instance no parameters
    public static SystemController getInstance() {
        return instance;
    }

    /**
     * Starts the system.
     */
    public void startSystem() {
        systemRunning = true;
        systemPaused = false;
        
        // Ensure any existing lock monitoring is stopped and restarted
        if (jobQueueManager != null) {
            // Try multiple times with exponential backoff in case of issues
            boolean success = false;
            int retryCount = 0;
            int maxRetries = 3;
            long retryDelayMs = 100;
            
            while (!success && retryCount < maxRetries) {
                try {
                    // Make sure monitoring is properly stopped before restarting
                    jobQueueManager.stopMonitoring();
                    
                    // Brief pause to ensure clean restart
                    try {
                        Thread.sleep(retryDelayMs);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    
                    // This will start monitoring again
                    jobQueueManager.startMonitoring();
                    success = true;
                    
                    if (retryCount > 0 && jobStateManager != null && jobStateManager.getLogger() != null) {
                        jobStateManager.getLogger().info("Successfully started lock monitoring after " + 
                                                      (retryCount + 1) + " attempts");
                    }
                } catch (Exception e) {
                    retryCount++;
                    retryDelayMs *= 2; // Exponential backoff
                    
                    String errorMsg = "Error starting lock monitoring (attempt " + retryCount + 
                                     " of " + maxRetries + "): " + e.getMessage();
                    
                    // Only log on the last failure or with high severity
                    if (retryCount == maxRetries) {
                        System.err.println("Warning: " + errorMsg);
                        if (jobStateManager != null && jobStateManager.getLogger() != null) {
                            jobStateManager.getLogger().error(errorMsg);
                        }
                    } else {
                        // Log intermediate retry attempts at info level
                        if (jobStateManager != null && jobStateManager.getLogger() != null) {
                            jobStateManager.getLogger().info("Retrying: " + errorMsg);
                        }
                    }
                }
            }
        }
    }

    /**
     * Stops the system.
     */
    public void stopSystem() {
        systemRunning = false;
        systemPaused = false;
        
        // Stop lock monitoring when system stops
        if (jobQueueManager != null) {
            try {
                jobQueueManager.stopMonitoring();
            } catch (Exception e) {
                // Log the error but continue with system shutdown
                System.err.println("Warning: Error stopping lock monitoring during system shutdown: " + e.getMessage());
                if (jobStateManager != null && jobStateManager.getLogger() != null) {
                    jobStateManager.getLogger().error("Lock monitoring error during system shutdown: " + e.getMessage());
                }
            }
        }
    }

    /**
     * Pauses the system.
     */
    public void pauseSystem() {
        systemPaused = true;
    }

    /**
     * Resumes the system.
     */
    public void resumeSystem() {
        systemPaused = false;
    }

    /**
     * Sets the scheduling policy.
     *
     * @param policy The scheduling policy to set
     * @throws IllegalArgumentException if policy is null
     */
    public void setSchedulingPolicy(Scheduler.Policy policy) {
        if (policy == null) {
            throw new IllegalArgumentException("Scheduling policy cannot be null");
        }
        scheduler.setPolicy(policy);
    }

    /**
     * Checks if the system is running.
     *
     * @return true if the system is running, false otherwise
     */
    public boolean isSystemRunning() {
        return systemRunning;
    }

    /**
     * Checks if the system is paused.
     *
     * @return true if the system is paused, false otherwise
     */
    public boolean isSystemPaused() {
        return systemPaused;
    }

    /**
     * Gets the scheduler.
     *
     * @return The scheduler
     */
    public Scheduler getScheduler() {
        return scheduler;
    }

    /**
     * Sets the scheduler.
     *
     * @param scheduler The scheduler to set
     * @throws IllegalArgumentException if scheduler is null
     */
    public void setScheduler(Scheduler scheduler) {
        if (scheduler == null) {
            throw new IllegalArgumentException("Scheduler cannot be null");
        }
        // Using direct field assignment since this is not recommended to be changed after initialization
        this.scheduler = scheduler;
    }

    /**
     * Gets the dispatcher.
     *
     * @return The dispatcher
     */
    public Dispatcher getDispatcher() {
        return dispatcher;
    }
    
    /**
     * Sets the dispatcher.
     *
     * @param dispatcher The dispatcher to set
     * @throws IllegalArgumentException if dispatcher is null
     */
    public void setDispatcher(Dispatcher dispatcher) {
        if (dispatcher == null) {
            throw new IllegalArgumentException("Dispatcher cannot be null");
        }
        // Using direct field assignment since this is not recommended to be changed after initialization
        this.dispatcher = dispatcher;
    }
    
    /**
     * Gets the job state manager.
     * 
     * @return The job state manager
     */
    public JobStateManager getJobStateManager() {
        return jobStateManager;
    }
    
    /**
     * Gets the job queue manager, which provides centralized access to the job queue.
     * 
     * @return The job queue manager
     */
    public JobQueueManager getJobQueueManager() {
        return jobQueueManager;
    }
    
    /**
     * Adds a listener to the job state manager.
     * 
     * @param listener The listener to add
     */
    public void addJobStateListener(JobStateManager.JobStateListener listener) {
        jobStateManager.addListener(listener);
    }
    
    /**
     * Removes a listener from the job state manager.
     * 
     * @param listener The listener to remove
     */
    public void removeJobStateListener(JobStateManager.JobStateListener listener) {
        jobStateManager.removeListener(listener);
    }
    
    /**
     * Gets the performance metrics, which provides system performance monitoring.
     * 
     * @return The performance metrics manager
     */
    public PerformanceMetrics getPerformanceMetrics() {
        return performanceMetrics;
    }
    
    /**
     * Adds a job to the queue through the job queue manager.
     * Also records the job submission in performance metrics.
     * 
     * @param job    The job to add
     * @param source The component adding the job
     * @return true if the job was added successfully, false otherwise
     */
    public boolean addJob(Job job, String source) {
        boolean added = jobQueueManager.addJob(job, source);
        if (added && source.equals("ConsoleInterface")) {
            performanceMetrics.recordJobSubmission(job.getName(), job.getCpuTime(), 
                                                 job.getPriority(), job.getArrivalTime());
        }
        return added;
    }
    
    /**
     * Removes a job from the queue through the job queue m anager.
     * 
     * @param job    The job to remove
     * @param source The component removing the job
     * @return true if the job was removed successfully, false otherwise
     */
    public boolean removeJob(Job job, String source) {
        return jobQueueManager.removeJob(job, source);
    }
    
    /**
     * Gets a list of all jobs in the queue through the job queue manager.
     * 
     * @return A list of all jobs
     */
    public List<Job> listJobs() {
        return jobQueueManager.listJobs();
    }
    
    /**
     * Updates the status of a job through the job state manager.
     * Also records job state transitions in performance metrics.
     * 
     * @param job       The job to update
     * @param newStatus The new status
     * @param source    The component updating the status
     * @param message   The status update message
     * @return true if the status was updated successfully, false otherwise
     */
    public boolean updateJobStatus(Job job, JobStatus newStatus, String source, String message) {
        boolean updated = jobStateManager.updateJobStatus(job, newStatus, source, message);
        
        if (updated) {
            if (newStatus == JobStatus.RUNNING) {
                performanceMetrics.recordJobStart(job.getName(), System.currentTimeMillis());
            } else if (newStatus == JobStatus.COMPLETED) {
                performanceMetrics.recordJobCompletion(job.getName(), System.currentTimeMillis());
            }
        }
        
        return updated;
    }
}
