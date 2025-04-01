package com.project.management;

import com.project.core.Job;
import com.project.core.JobQueue;
import com.project.core.JobStatus;
import com.project.logging.Logger;
import com.project.logging.LogFormatter;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * JobQueueManager provides a centralized point of access for all job queue operations.
 * It ensures thread safety, provides validation, and abstracts the underlying queue implementation.
 */
public class JobQueueManager {
    private final JobQueue jobQueue;
    private final JobStateManager jobStateManager;
    private final Logger logger;
    private final Lock queueLock = new ReentrantLock();
    
    // Thread local storage for lock timing
    private final ThreadLocal<Long> lockAcquisitionTime = new ThreadLocal<>();
    // Track active locks for debugging
    private final AtomicReference<String> currentLockOwner = new AtomicReference<>(null);
    private final AtomicReference<String> currentLockPurpose = new AtomicReference<>(null);
    private volatile long lockAcquiredTimestamp = 0;
    
    // Lock monitor for better diagnostics
    private LockMonitor lockMonitor;
    
    // Flag indicating if the queue needs to be sorted
    private volatile boolean needsSort = false;
    
    /**
     * Constructs a new JobQueueManager.
     *
     * @param jobQueue The job queue to manage
     * @param jobStateManager The job state manager to use for status updates
     * @param logger The logger to use
     */
    public JobQueueManager(JobQueue jobQueue, JobStateManager jobStateManager, Logger logger) {
        this.jobQueue = jobQueue;
        this.jobStateManager = jobStateManager;
        this.logger = logger;
        
        // Start lock monitoring with exception handling
        try {
            startMonitoring();
        } catch (Exception e) {
            // Log the error but don't fail construction
            System.err.println("Warning: Failed to start lock monitoring during initialization: " + e.getMessage());
            if (logger != null) {
                logger.error("Failed to start lock monitoring during initialization: " + e.getMessage());
            }
        }
    }
    
    /**
     * Stops the lock monitoring thread.
     */
    public void stopMonitoring() {
        if (lockMonitor != null) {
            lockMonitor.stop();
            if (logger != null) {
                logger.info("Lock monitor stopped");
            }
        }
    }
    
    /**
     * Starts the lock monitoring.
     */
    public void startMonitoring() {
        if (logger != null) {
            try {
                // Stop any existing monitor first to ensure clean state
                if (this.lockMonitor != null) {
                    this.lockMonitor.stop();
                    // Brief delay to ensure complete shutdown
                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
                
                // Create a new lock monitor with a 5-second warning threshold
                this.lockMonitor = new LockMonitor(logger, 5000);
                
                // Start the monitor with a 10-second check interval
                this.lockMonitor.start(10);
                logger.info("Lock monitoring started");
            } catch (Exception e) {
                logger.error("Failed to start lock monitoring: " + e.getMessage());
            }
        }
    }
    
    /**
     * Logs lock acquisition with the purpose.
     * 
     * @param purpose The purpose for acquiring the lock
     */
    private void logLockAcquisition(String purpose) {
        if (logger != null) {
            String threadName = Thread.currentThread().getName();
            lockAcquisitionTime.set(System.currentTimeMillis());
            
            // Track for debugging
            currentLockOwner.set(threadName);
            currentLockPurpose.set(purpose);
            lockAcquiredTimestamp = System.currentTimeMillis();
            
            // Use the lock monitor if available
            if (lockMonitor != null) {
                lockMonitor.registerLockAcquisition("JobQueueLock", threadName, purpose);
            } else {
                String message = LogFormatter.formatLockAcquired("JobQueueLock", threadName, purpose);
                logger.debug(message);
            }
        }
    }
    
    /**
     * Logs lock release with timing information.
     */
    private void logLockRelease() {
        if (logger != null) {
            String threadName = Thread.currentThread().getName();
            Long acquisitionTime = lockAcquisitionTime.get();
            if (acquisitionTime != null) {
                long heldForMs = System.currentTimeMillis() - acquisitionTime;
                
                // Use the lock monitor if available
                if (lockMonitor != null) {
                    lockMonitor.registerLockRelease("JobQueueLock", threadName);
                } else {
                    String message = LogFormatter.formatLockReleased("JobQueueLock", threadName, heldForMs);
                    logger.debug(message);
                }
                
                lockAcquisitionTime.remove(); // Clean up
                
                // Clear tracking variables
                currentLockOwner.set(null);
                currentLockPurpose.set(null);
            }
        }
    }
    
    /**
     * Checks if the lock is currently held and logs detailed information.
     * This method is meant to be called after job state changes to help debug lock issues.
     * 
     * @param jobName The name of the job that changed state
     * @param newStatus The new status of the job
     */
    public void checkLockStateAfterJobCompletion(String jobName, String newStatus) {
        boolean isLockHeld = !queueLock.tryLock();
        
        if (isLockHeld) {
            // Lock is held by another thread, log detailed information
            String owner = currentLockOwner.get();
            String purpose = currentLockPurpose.get();
            long heldFor = System.currentTimeMillis() - lockAcquiredTimestamp;
            
            String message = String.format(
                "WARNING: Lock still held after job '%s' changed to %s state! " +
                "Current owner: %s, Purpose: %s, Held for: %dms",
                jobName, newStatus, 
                (owner != null ? owner : "Unknown"),
                (purpose != null ? purpose : "Unknown"),
                heldFor
            );
            if (logger != null) {
                logger.warning(message);
                
                // Dump all thread stacks to help with debugging
                dumpThreadStacks();
            }
        } else {
            // We acquired the lock for this check, release it immediately
            if (logger != null) {
                logger.info(String.format(
                    "Lock check after job '%s' changed to %s state: Lock is FREE",
                    jobName, newStatus
                ));
            }
            queueLock.unlock();
        }
    }
    
    /**
     * Dumps all thread stacks to the log to help debug lock issues.
     */
    private void dumpThreadStacks() {
        if (logger == null) return;
        
        logger.warning("=== DUMPING ALL THREAD STACKS FOR LOCK DEBUGGING ===");
        Map<Thread, StackTraceElement[]> stackTraces = Thread.getAllStackTraces();
        
        for (Map.Entry<Thread, StackTraceElement[]> entry : stackTraces.entrySet()) {
            Thread thread = entry.getKey();
            StackTraceElement[] stack = entry.getValue();
            
            // Only log threads that might be relevant to job queue operations
            if (thread.getName().contains("JobQueue") || 
                thread.getName().contains("Scheduler") || 
                thread.getName().contains("Dispatcher")) {
                
                logger.warning(String.format(
                    "Thread %s (id: %d, state: %s):",
                    thread.getName(), thread.getId(), thread.getState()
                ));
                
                for (StackTraceElement element : stack) {
                    logger.warning("    at " + element);
                }
                
                logger.warning("-------------------");
            }
        }
        logger.warning("=== END OF THREAD DUMP ===");
    }
    
    /**
     * Checks if the queue needs to be sorted.
     * 
     * @return true if the queue needs sorting, false otherwise
     */
    public boolean isNeedingSort() {
        return needsSort;
    }
    
    /**
     * Sets or clears the flag indicating the queue needs to be sorted.
     * 
     * @param needsSort true if the queue needs sorting, false otherwise
     */
    public void setNeedsSort(boolean needsSort) {
        this.needsSort = needsSort;
    }
    
    /**
     * Adds a job to the queue and sets the needs-sort flag.
     *
     * @param job    The job to add
     * @param source The component name that is adding the job
     * @return true if the job was added successfully, false otherwise
     */
    public boolean addJob(Job job, String source) {
        if (job == null) {
            if (logger != null) {
                logger.warning("Attempted to add null job to queue from " + source);
            }
            return false;
        }
        
        try {
            queueLock.lock();
            logLockAcquisition("Add job: " + job.getName() + " from " + source);
            
            jobQueue.addJob(job);
            
            // Set the needs-sort flag whenever a job is added (except from Scheduler-Sort)
            if (!"Scheduler-Sort".equals(source)) {
                needsSort = true;
            }
            
            // Log the addition
            if (logger != null) {
                logger.infoJob(job, "Job added to queue by " + source);
            }
            
            // Update job status if needed and state manager is available
            if (jobStateManager != null && job.getJobStatus() != JobStatus.WAITING) {
                SystemController.getInstance().updateJobStatus(
                    job,
                    JobStatus.WAITING,
                    "JobQueueManager",
                    "Job added to queue by " + source
                );
            }
            
            return true;
        } finally {
            logLockRelease();
            queueLock.unlock();
        }
    }
    
    /**
     * Retrieves and removes the next job from the queue using a non-blocking approach.
     * This implementation optimizes locking to minimize contention.
     *
     * @return The next job
     * @throws InterruptedException If interrupted while waiting
     */
    public Job retrieveJob() throws InterruptedException {
        if (jobQueue == null) {
            return null;
        }

        // First try with a shorter lock to just check if queue is empty
        boolean isEmpty;
        try {
            queueLock.lock();
            logLockAcquisition("Quick check if queue is empty");
            isEmpty = jobQueue.isEmpty();
        } finally {
            logLockRelease();
            queueLock.unlock();
        }
        
        // If empty, poll with minimal locking until a job becomes available
        if (isEmpty) {
            while (true) {
                // Sleep briefly to avoid busy-waiting
                Thread.sleep(100); 
                
                // Quick check with lock
                Job job = null;
                try {
                    queueLock.lock();
                    logLockAcquisition("Poll for job (quick check)");
                    if (!jobQueue.isEmpty()) {
                        job = jobQueue.pollJob();  // Using non-blocking poll
                    }
                } finally {
                    logLockRelease();
                    queueLock.unlock();
                }
                
                if (job != null) {
                    return job;
                }
                
                // Check if thread was interrupted
                if (Thread.currentThread().isInterrupted()) {
                    throw new InterruptedException("Interrupted while waiting for job");
                }
            }
        } else {
            // Queue is likely not empty, try to get a job with lock
            try {
                queueLock.lock();
                logLockAcquisition("Retrieve job (known not empty)");
                Job job = jobQueue.pollJob();  // Using non-blocking poll
                
                // If job is null despite queue appearing non-empty earlier,
                // we had a race condition - restart the retrieval process
                if (job == null) {
                    if (logger != null) {
                        logger.debug("Queue falsely appeared non-empty, restarting job retrieval");
                    }
                }
                
                return job;
            } finally {
                logLockRelease();
                queueLock.unlock();
            }
        }
    }
    
    /**
     * Reschedules a job by adding it back to the queue.
     *
     * @param job    The job to reschedule
     * @param source The component name that is rescheduling the job
     * @return true if the job was rescheduled successfully, false otherwise
     */
    public boolean rescheduleJob(Job job, String source) {
        if (job == null) {
            if (logger != null) {
                logger.warning("Attempted to reschedule null job from " + source);
            }
            return false;
        }
        
        try {
            queueLock.lock();
            logLockAcquisition("Reschedule job: " + job.getName() + " from " + source);
            
            jobQueue.rescheduleJob(job);
            
            // Log the rescheduling
            if (logger != null) {
                logger.infoJob(job, "Job rescheduled by " + source);
            }
            
            return true;
        } finally {
            logLockRelease();
            queueLock.unlock();
        }
    }
    
    /**
     * Gets a job by its name.
     *
     * @param jobName The name of the job to find
     * @return An Optional containing the job if found, or empty if not found
     */
    public Optional<Job> getJobByName(String jobName) {
        if (jobName == null || jobName.isEmpty()) {
            return Optional.empty();
        }
        
        try {
            queueLock.lock();
            logLockAcquisition("Get job by name: " + jobName);
            
            return jobQueue.stream()
                .filter(job -> jobName.equals(job.getName()))
                .findFirst();
        } finally {
            logLockRelease();
            queueLock.unlock();
        }
    }
    
    /**
     * Removes a job from the queue.
     *
     * @param job    The job to remove
     * @param source The component name that is removing the job
     * @return true if the job was removed successfully, false otherwise
     */
    public boolean removeJob(Job job, String source) {
        if (job == null) {
            if (logger != null) {
                logger.warning("Attempted to remove null job from " + source);
            }
            return false;
        }
        
        try {
            queueLock.lock();
            logLockAcquisition("Remove job: " + job.getName() + " from " + source);
            
            // We need to first retrieve all jobs except the one to remove
            List<Job> remainingJobs = jobQueue.stream()
                .filter(j -> !j.equals(job))
                .collect(Collectors.toList());
            
            // If the sizes are the same, the job wasn't in the queue
            int originalSize = jobQueue.size();
            if (originalSize == remainingJobs.size()) {
                return false;
            }
            
            // Clear the queue and add back all jobs except the removed one
            jobQueue.clear();
            
            remainingJobs.forEach(jobQueue::addJob);
            
            // Log the removal
            if (logger != null) {
                logger.infoJob(job, "Job removed from queue by " + source);
            }
            
            // Update job status if needed and state manager is available
            if (jobStateManager != null) {
                SystemController.getInstance().updateJobStatus(
                    job,
                    JobStatus.CANCELED,
                    "JobQueueManager",
                    "Job removed from queue by " + source
                );
            }
            
            return true;
        } finally {
            logLockRelease();
            queueLock.unlock();
        }
    }
    
    /**
     * Removes a job from the queue by its name.
     *
     * @param jobName The name of the job to remove
     * @param source  The component name that is removing the job
     * @return true if the job was removed successfully, false otherwise
     */
    public boolean removeJobByName(String jobName, String source) {
        Optional<Job> jobOpt = getJobByName(jobName);
        return jobOpt.isPresent() && removeJob(jobOpt.get(), source);
    }
    
    /**
     * Gets the shortest job in the queue based on CPU time.
     *
     * @return The shortest job, or null if the queue is empty
     */
    public Job getShortestJob() {
        try {
            queueLock.lock();
            logLockAcquisition("Get shortest job");
            
            return jobQueue.stream()
                .min((j1, j2) -> Integer.compare(j1.getCpuTime(), j2.getCpuTime()))
                .orElse(null);
        } finally {
            logLockRelease();
            queueLock.unlock();
        }
    }
    
    /**
     * Gets the highest priority job in the queue.
     *
     * @return The highest priority job, or null if the queue is empty
     */
    public Job getHighestPriorityJob() {
        try {
            queueLock.lock();
            logLockAcquisition("Get highest priority job");
            
            return jobQueue.stream()
                .min((j1, j2) -> Integer.compare(j1.getPriority(), j2.getPriority()))
                .orElse(null);
        } finally {
            logLockRelease();
            queueLock.unlock();
        }
    }
    
    /**
     * Gets a stream of all jobs in the queue.
     *
     * @return A stream of all jobs
     */
    public Stream<Job> streamJobs() {
        logger.info("Stream jobs called");
        try {
            queueLock.lock();
            logLockAcquisition("Stream all jobs");
            
            // Create a copy of the jobs to avoid concurrent modification issues
            return jobQueue.stream().collect(Collectors.toList()).stream();
        } finally {
            logLockRelease();
            queueLock.unlock();
        }
    }
    
    /**
     * Gets a list of all jobs in the queue.
     *
     * @return A list of all jobs
     */
    public List<Job> listJobs() {
        return streamJobs().collect(Collectors.toList());
    }
    
    /**
     * Gets all jobs in the queue for sorting.
     * This method is primarily used by the Scheduler for sorting operations.
     *
     * @param source The component requesting the jobs
     * @return A list of all jobs in the queue
     */
    public List<Job> getAllJobs(String source) {
        try {
            queueLock.lock();
            logLockAcquisition("Get all jobs for " + source);
            
            // Return a copy of the jobs to prevent external modifications
            List<Job> jobs = jobQueue.stream().collect(Collectors.toList());
            
            if (logger != null) {
                logger.debug("Retrieved " + jobs.size() + " jobs for " + source);
            }
            
            return jobs;
        } finally {
            logLockRelease();
            queueLock.unlock();
        }
    }
    
    /**
     * Clears the queue, removing all jobs.
     * After clearing, the queue will need to be sorted again if jobs are added.
     *
     * @param source The component name that is clearing the queue
     */
    public void clearQueue(String source) {
        try {
            queueLock.lock();
            logLockAcquisition("Clear queue from " + source);
            
            jobQueue.clear();
            
            // Set needs-sort flag after clearing (if not from Scheduler-Sort)
            if (!"Scheduler-Sort".equals(source)) {
                needsSort = true;
            }
            
            if (logger != null) {
                logger.info("Job queue cleared by " + source);
            }
        } finally {
            logLockRelease();
            queueLock.unlock();
        }
    }
    
    /**
     * Gets the number of jobs in the queue.
     *
     * @return The number of jobs
     */
    public int getQueueSize() {
        try {
            queueLock.lock();
            logLockAcquisition("Get queue size");
            
            return jobQueue.size();
        } finally {
            logLockRelease();
            queueLock.unlock();
        }
    }
    
    /**
     * Checks if the queue is empty.
     *
     * @return true if the queue is empty, false otherwise
     */
    public boolean isQueueEmpty() {
        try {
            queueLock.lock();
            logLockAcquisition("Check if queue is empty");
            
            return jobQueue.isEmpty();
        } finally {
            logLockRelease();
            queueLock.unlock();
        }
    }
    
    /**
     * Gets the underlying job queue.
     * This should only be used for special cases where direct access is necessary.
     *
     * @return The underlying job queue
     */
    public JobQueue getUnderlyingQueue() {
        return jobQueue;
    }
    
    /**
     * Gets a job by ID (name) and performs an operation on it with proper locking.
     * 
     * @param jobId The ID/name of the job to retrieve
     * @param source The identifier of the caller for logging
     * @param operation The operation to perform on the job if found
     * @param <R> The return type of the operation
     * @return The result of the operation, or null if job not found
     */
    public <R> R getJobById(String jobId, String source, JobOperation<R> operation) {
        if (jobId == null || source == null || operation == null) {
            if (logger != null) {
                logger.warning("Invalid parameters provided to getJobById: " + 
                              "jobId=" + jobId + ", source=" + source);
            }
            return null;
        }
        
        boolean lockAcquired = false;
        try {
            // Acquire lock with timeout
            lockAcquired = queueLock.tryLock(30, TimeUnit.SECONDS);
            if (!lockAcquired) {
                if (logger != null) {
                    logger.warning("Failed to acquire lock for getJobById after 30 seconds. " +
                                 "Source: " + source + ", JobId: " + jobId);
                }
                return null;
            }
            
            // Log lock acquisition
            logLockAcquisition("GetJobById_" + jobId);
            
            // Find the job
            Optional<Job> jobOpt = jobQueue.stream()
                .filter(job -> job.getName().equals(jobId))
                .findFirst();
            
            if (!jobOpt.isPresent()) {
                if (logger != null) {
                    logger.info("Job not found: " + jobId + " (requested by " + source + ")");
                }
                return null;
            }
            
            // Perform the operation on the job
            return operation.apply(jobOpt.get());
        } catch (InterruptedException e) {
            if (logger != null) {
                logger.warning("Thread interrupted while waiting for lock: " + e.getMessage());
            }
            Thread.currentThread().interrupt();
            return null;
        } catch (Exception e) {
            if (logger != null) {
                logger.error("Error in getJobById: " + e.getMessage());
            }
            return null;
        } finally {
            if (lockAcquired) {
                logLockRelease();
                queueLock.unlock();
            }
        }
    }
    
    /**
     * Functional interface for operations on jobs.
     */
    @FunctionalInterface
    public interface JobOperation<R> {
        /**
         * Applies an operation to a job.
         * 
         * @param job The job to operate on
         * @return The result of the operation
         */
        R apply(Job job);
    }
} 