package com.project.scheduler;

import com.project.core.Job;
import com.project.core.JobStatus;
import com.project.logging.Logger;
import com.project.management.JobQueueManager;
import com.project.management.JobStateManager;

/**
 * Dispatcher class responsible for executing jobs from the queue.
 * Implements the Runnable interface to operate in its own thread.
 * 
 * Simplified version: The dispatcher's only responsibility is to retrieve jobs
 * from the queue and execute them, with proper locking.
 */
public class Dispatcher implements Runnable {
    private static final long MILLISECONDS_PER_SECOND = 1000L;
    private final Scheduler scheduler; // Reference to scheduler for policy queries
    private Logger logger;
    private JobStateManager jobStateManager;
    private JobQueueManager jobQueueManager;
    private int cpuTimeSlice = Integer.MAX_VALUE; // Default to max value (no slicing)
    private volatile boolean running = true;

    /**
     * Constructs a new Dispatcher with the specified scheduler.
     *
     * @param scheduler The scheduler that manages the job queue
     * @throws IllegalArgumentException if scheduler is null
     */
    public Dispatcher(Scheduler scheduler) {
        if (scheduler == null) {
            throw new IllegalArgumentException("Scheduler cannot be null");
        }
        this.scheduler = scheduler;
    }

    /**
     * Sets the logger for this dispatcher.
     *
     * @param logger The logger to use
     */
    public void setLogger(Logger logger) {
        this.logger = logger;
    }
    
    /**
     * Sets the job state manager for this dispatcher.
     *
     * @param jobStateManager The job state manager to be used
     */
    public void setJobStateManager(JobStateManager jobStateManager) {
        this.jobStateManager = jobStateManager;
    }
    
    /**
     * Sets the job queue manager for this dispatcher.
     *
     * @param jobQueueManager The job queue manager to be used
     */
    public void setJobQueueManager(JobQueueManager jobQueueManager) {
        this.jobQueueManager = jobQueueManager;
    }

    /**
     * Sets the CPU time slice for job execution.
     *
     * @param timeSlice The time slice in seconds
     * @throws IllegalArgumentException if timeSlice is less than or equal to 0
     */
    public void setCpuTimeSlice(int timeSlice) {
        if (timeSlice <= 0) {
            throw new IllegalArgumentException("CPU time slice must be positive");
        }
        this.cpuTimeSlice = timeSlice;
    }

    /**
     * Stops the dispatcher thread safely.
     */
    public void stop() {
        running = false;
        
        // If the dispatcher is waiting for a job, interrupt it
        Thread.currentThread().interrupt();
        
        if (logger != null) {
            logger.info("Dispatcher stopped");
        }
    }

    @Override
    public void run() {
        running = true;
        
        if (logger != null) {
            logger.info("Dispatcher started");
        }
        
        while (running) {
            try {
                // Check if we have necessary components
                if (jobQueueManager == null) {
                    if (logger != null) {
                        logger.error("JobQueueManager not set, cannot dispatch jobs");
                    }
                    Thread.sleep(1000); // Sleep longer when not properly configured
                    continue;
                }
                
                // Check if there are jobs to process
                if (jobQueueManager.isQueueEmpty()) {
                    // No jobs to process, sleep for a while
                    Thread.sleep(100);
                    continue;
                }
                
                // Try to get a job from the queue
                // JobQueueManager.retrieveJob() handles locking internally
                Job job = null;
                try {
                    if (logger != null) {
                        logger.debug("Attempting to retrieve next job from queue");
                    }
                    
                    job = jobQueueManager.retrieveJob();
                    
                    if (logger != null && job != null) {
                        logger.debug("Successfully retrieved job: " + job.getName());
                    }
                } catch (InterruptedException e) {
                    if (!running) {
                        break; // Exit if we're shutting down
                    }
                    continue; // Otherwise try again
                } catch (Exception e) {
                    // Log any other errors and continue
                    if (logger != null) {
                        logger.error("Error retrieving job from queue: " + e.getMessage());
                    }
                    Thread.sleep(500);
                    continue;
                }
                
                // If no job is available, sleep and try again
                if (job == null) {
                    if (logger != null) {
                        logger.debug("No job available from queue, will retry");
                    }
                    Thread.sleep(100);
                    continue;
                }
                
                // Execute the job
                if (logger != null) {
                    logger.info("Executing job: " + job.getName());
                }
                executeJob(job);
                
                // Sleep a short time between jobs to prevent CPU overuse
                Thread.sleep(100);
                
            } catch (InterruptedException e) {
                if (!running) {
                    // Interrupted due to shutdown, exit gracefully
                    break;
                }
                // Otherwise log the interruption
                if (logger != null) {
                    logger.warning("Dispatcher interrupted: " + e.getMessage());
                }
            } catch (Exception e) {
                // Catch any other exceptions to keep the dispatcher running
                if (logger != null) {
                    logger.error("Dispatcher encountered an error: " + e.getMessage());
                }
                try {
                    Thread.sleep(1000); // Sleep longer when errors occur
                } catch (InterruptedException ie) {
                    if (!running) break;
                }
            }
        }
        
        if (logger != null) {
            logger.info("Dispatcher thread terminated");
        }
    }

    /**
     * Executes a job by simulating CPU time.
     * All state transitions are managed through the JobStateManager.
     *
     * @param job The job to execute
     */
    private void executeJob(Job job) {
        if (job == null) {
            return;
        }
        
        try {
            // Update job status to "Selected" first if needed
            if (job.getJobStatus() != JobStatus.SELECTED) {
                updateJobStatus(job, JobStatus.SELECTED, "Job selected for execution");
            }
            
            // Update job status to "Running" 
            updateJobStatus(job, JobStatus.RUNNING, "Executing job, CPU Time: " + job.getCpuTime() + " seconds");
            
            // Calculate time to execute based on time slice
            int timeToExecute = Math.min(job.getCpuTime(), cpuTimeSlice);
            
            if (logger != null) {
                logger.debug("Simulating execution of job " + job.getName() + 
                             " for " + timeToExecute + " seconds");
            }
            
            // Ensure the job is set as the running job in the JobQueue
            if (jobQueueManager != null) {
                jobQueueManager.getUnderlyingQueue().setRunningJob(job);
            }
            
            // Simulate CPU time
            Thread.sleep(timeToExecute * MILLISECONDS_PER_SECOND);
            
            // Check if job completed (used its full CPU time)
            boolean jobCompleted = timeToExecute >= job.getCpuTime();
            
            if (jobCompleted) {
                // Update job status to "Completed"
                if (logger != null) {
                    logger.debug("Job " + job.getName() + " completed execution");
                }
                updateJobStatus(job, JobStatus.COMPLETED, "Job execution completed");
                
                // Clear the running job reference since it's completed
                if (jobQueueManager != null) {
                    jobQueueManager.getUnderlyingQueue().clearRunningJob();
                }
            } else {
                // Job didn't complete (time slice expired), reschedule it
                if (logger != null) {
                    logger.debug("Job " + job.getName() + " time slice expired, rescheduling");
                }
                updateJobStatus(job, JobStatus.WAITING, "Job rescheduled after time slice");
                
                // Clear the running job reference since it's being rescheduled
                if (jobQueueManager != null) {
                    jobQueueManager.getUnderlyingQueue().clearRunningJob();
                }
                
                // Reschedule using the job queue manager
                if (jobQueueManager != null) {
                    jobQueueManager.rescheduleJob(job, "Dispatcher-TimeSlice");
                    
                    // Mark that sorting is needed
                    jobQueueManager.setNeedsSort(true);
                }
            }
        } catch (InterruptedException e) {
            // Update job status to "Interrupted"
            if (logger != null) {
                logger.debug("Job " + job.getName() + " was interrupted: " + e.getMessage());
            }
            updateJobStatus(job, JobStatus.INTERRUPTED, "Job execution was interrupted: " + e.getMessage());
            
            // Clear the running job reference since it's interrupted
            if (jobQueueManager != null) {
                jobQueueManager.getUnderlyingQueue().clearRunningJob();
            }
            
            // Reschedule the interrupted job if needed
            if (jobQueueManager != null) {
                jobQueueManager.rescheduleJob(job, "Dispatcher-Interrupted");
                
                // Mark that sorting is needed
                jobQueueManager.setNeedsSort(true);
            }
            
            Thread.currentThread().interrupt();
        }
    }
    
    /**
     * Updates job status using JobStateManager if available, or directly if not.
     * All status updates are centralized through this method.
     * 
     * @param job The job to update
     * @param status The new status
     * @param message The status change message
     */
    private void updateJobStatus(Job job, JobStatus status, String message) {
        if (job == null) return;
        
        if (logger != null) {
            logger.debug("Updating job " + job.getName() + " status to " + status);
        }
        
        if (jobStateManager != null) {
            jobStateManager.updateJobStatus(
                job, 
                status, 
                "Dispatcher", 
                message
            );
        } else {
            // Fall back to direct status update if job state manager is not available
            job.setStatus(status);
            if (logger != null) {
                logger.infoJob(job, message);
            }
        }
    }
}
