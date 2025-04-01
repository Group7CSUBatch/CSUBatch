package com.project.management;

import com.project.core.Job;
import com.project.core.JobStatus;
import com.project.logging.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Central manager for job state transitions in the system.
 * This class provides a single point of control for changing job statuses
 * and notifying interested components about these changes.
 */
public class JobStateManager {
    private final Logger logger;
    private final List<JobStateListener> listeners = new CopyOnWriteArrayList<>();
    private JobQueueManager jobQueueManager;

    /**
     * Constructs a JobStateManager with the specified logger.
     *
     * @param logger The logger to use for logging state transitions
     */
    public JobStateManager(Logger logger) {
        this.logger = logger;
    }
    
    /**
     * Sets the job queue manager for lock checking purposes.
     * 
     * @param jobQueueManager The job queue manager
     */
    public void setJobQueueManager(JobQueueManager jobQueueManager) {
        this.jobQueueManager = jobQueueManager;
    }
    
    /**
     * Gets the logger used by this job state manager.
     *
     * @return The logger
     */
    public Logger getLogger() {
        return logger;
    }

    /**
     * Updates the status of a job and notifies all registered listeners.
     *
     * @param job       The job to update
     * @param newStatus The new status for the job
     * @param source    A string identifying the source of this status change
     * @param message   An optional message explaining the status change
     * @return true if the state transition was valid and successful, false otherwise
     */
    public boolean updateJobStatus(Job job, JobStatus newStatus, String source, String message) {
        if (job == null) {
            return false;
        }

        JobStatus oldStatus = JobStatus.fromString(job.getStatus());
        
        // Validate state transition
        if (!isValidTransition(oldStatus, newStatus)) {
            if (logger != null) {
                logger.warning("Invalid job status transition attempted: " + oldStatus + " -> " + newStatus + 
                               " for job " + job.getName() + " by " + source);
            }
            return false;
        }

        // Update the job status
        job.setStatus(newStatus.getDisplayName());
        
        // Log the status change
        if (logger != null) {
            String logMessage = message != null ? message : 
                               "Job status changed from " + oldStatus + " to " + newStatus;
            logger.infoJob(job, logMessage + " (by " + source + ")");
        }
        
        // Notify all registered listeners
        JobStateEvent event = new JobStateEvent(job, oldStatus, newStatus, source, message);
        notifyListeners(event);
        
        // Check for lock issues when a job enters COMPLETED, INTERRUPTED or CANCELED state
        if (jobQueueManager != null && 
            (newStatus == JobStatus.COMPLETED || 
             newStatus == JobStatus.INTERRUPTED || 
             newStatus == JobStatus.CANCELED)) {
            // A short delay to allow any potential lock release operations to complete
            try {
                Thread.sleep(100); // Short delay
            } catch (InterruptedException e) {
                // Ignore
            }
            
            // Check if the job queue lock is still held after job completion
            jobQueueManager.checkLockStateAfterJobCompletion(job.getName(), newStatus.getDisplayName());
        }
        
        return true;
    }
    
    /**
     * Validates if a state transition is allowed.
     *
     * @param oldStatus The current status
     * @param newStatus The requested new status
     * @return true if the transition is valid, false otherwise
     */
    private boolean isValidTransition(JobStatus oldStatus, JobStatus newStatus) {
        // Define valid transitions
        switch (oldStatus) {
            case WAITING:
                // From WAITING, can only go to SELECTED or CANCELED
                return newStatus == JobStatus.SELECTED || newStatus == JobStatus.CANCELED;
                
            case SELECTED:
                // From SELECTED, can go to RUNNING, WAITING, or CANCELED
                return newStatus == JobStatus.RUNNING || 
                       newStatus == JobStatus.WAITING || 
                       newStatus == JobStatus.CANCELED;
                
            case RUNNING:
                // From RUNNING, can go to COMPLETED, INTERRUPTED, WAITING, or CANCELED
                return newStatus == JobStatus.COMPLETED || 
                       newStatus == JobStatus.INTERRUPTED || 
                       newStatus == JobStatus.WAITING || 
                       newStatus == JobStatus.CANCELED;
                
            case COMPLETED:
            case INTERRUPTED:
            case CANCELED:
                // Terminal states can't transition to other states
                return false;
                
            default:
                return false;
        }
    }
    
    /**
     * Adds a listener to be notified of job state changes.
     *
     * @param listener The listener to add
     */
    public void addListener(JobStateListener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
        }
    }
    
    /**
     * Removes a listener so it no longer receives job state change notifications.
     *
     * @param listener The listener to remove
     */
    public void removeListener(JobStateListener listener) {
        if (listener != null) {
            listeners.remove(listener);
        }
    }
    
    /**
     * Notifies all registered listeners about a job state change.
     *
     * @param event The event to notify about
     */
    private void notifyListeners(JobStateEvent event) {
        for (JobStateListener listener : listeners) {
            try {
                listener.onJobStateChanged(event);
            } catch (Exception e) {
                if (logger != null) {
                    logger.error("Error notifying listener: " + e.getMessage());
                }
            }
        }
    }
    
    /**
     * Interface for components that want to be notified of job state changes.
     */
    public interface JobStateListener {
        /**
         * Called when a job's state changes.
         *
         * @param event The event containing information about the state change
         */
        void onJobStateChanged(JobStateEvent event);
    }
    
    /**
     * Event class that contains information about a job state change.
     */
    public static class JobStateEvent {
        private final Job job;
        private final JobStatus oldStatus;
        private final JobStatus newStatus;
        private final String source;
        private final String message;
        
        /**
         * Constructs a new JobStateEvent.
         *
         * @param job       The job whose state changed
         * @param oldStatus The old status of the job
         * @param newStatus The new status of the job
         * @param source    The source of the state change
         * @param message   An optional message explaining the state change
         */
        public JobStateEvent(Job job, JobStatus oldStatus, JobStatus newStatus, String source, String message) {
            this.job = job;
            this.oldStatus = oldStatus;
            this.newStatus = newStatus;
            this.source = source;
            this.message = message;
        }
        
        public Job getJob() {
            return job;
        }
        
        public JobStatus getOldStatus() {
            return oldStatus;
        }
        
        public JobStatus getNewStatus() {
            return newStatus;
        }
        
        public String getSource() {
            return source;
        }
        
        public String getMessage() {
            return message;
        }
    }
} 