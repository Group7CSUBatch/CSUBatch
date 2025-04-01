package com.project.management;

import com.project.logging.Logger;
import com.project.logging.LogFormatter;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.lang.management.MonitorInfo;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A monitor for detecting and reporting lock-related issues like deadlocks or
 * locks held for too long in the system.
 */
public class LockMonitor {
    private final Logger logger;
    private ScheduledExecutorService scheduler;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final ConcurrentHashMap<String, LockInfo> activeLocks = new ConcurrentHashMap<>();
    private final long lockWarningThresholdMs;
    
    /**
     * Represents information about an active lock in the system.
     */
    public static class LockInfo {
        private final String lockName;
        private final String ownerThread;
        private final String purpose;
        private final long acquiredTimeMs;
        
        public LockInfo(String lockName, String ownerThread, String purpose) {
            this.lockName = lockName;
            this.ownerThread = ownerThread;
            this.purpose = purpose;
            this.acquiredTimeMs = System.currentTimeMillis();
        }
        
        public String getLockName() {
            return lockName;
        }
        
        public String getOwnerThread() {
            return ownerThread;
        }
        
        public String getPurpose() {
            return purpose;
        }
        
        public long getAcquiredTimeMs() {
            return acquiredTimeMs;
        }
        
        public long getHeldForMs() {
            return System.currentTimeMillis() - acquiredTimeMs;
        }
    }
    
    /**
     * Constructs a new LockMonitor.
     * 
     * @param logger The logger to use for reporting issues
     * @param lockWarningThresholdMs The threshold in ms after which a warning is logged
     *                             about a lock being held for too long
     */
    public LockMonitor(Logger logger, long lockWarningThresholdMs) {
        this.logger = logger;
        this.lockWarningThresholdMs = lockWarningThresholdMs;
        
        // Create a fresh scheduler
        initializeScheduler();
        
        if (logger != null) {
            logger.debug("Lock monitor created with warning threshold: " + 
                       lockWarningThresholdMs + " ms");
        }
    }
    
    /**
     * Initializes or reinitializes the scheduler.
     */
    private void initializeScheduler() {
        // Create a new scheduler
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "LockMonitor");
            t.setDaemon(true);
            return t;
        });
    }
    
    /**
     * Starts the lock monitor.
     * 
     * @param checkIntervalSeconds The interval in seconds at which to check for lock issues
     */
    public void start(int checkIntervalSeconds) {
        if (running.compareAndSet(false, true)) {
            // Check if scheduler is terminated and recreate if necessary
            if (scheduler == null || scheduler.isShutdown() || scheduler.isTerminated()) {
                initializeScheduler();
                
                if (logger != null) {
                    logger.info("Lock monitor scheduler recreated - previous one was terminated or null");
                }
            }
            
            try {
                scheduler.scheduleAtFixedRate(this::checkForLockIssues, 
                                             checkIntervalSeconds, 
                                             checkIntervalSeconds, 
                                             TimeUnit.SECONDS);
                
                if (logger != null) {
                    logger.info("Lock monitor started with check interval of " + 
                               checkIntervalSeconds + " seconds");
                }
            } catch (Exception e) {
                running.set(false);
                if (logger != null) {
                    logger.error("Failed to start lock monitor: " + e.getMessage());
                }
                throw e; // Rethrow to allow higher level handling
            }
        }
    }
    
    /**
     * Stops the lock monitor.
     */
    public void stop() {
        if (running.compareAndSet(true, false)) {
            if (scheduler != null) {
                try {
                    scheduler.shutdown();
                    boolean terminated = scheduler.awaitTermination(5, TimeUnit.SECONDS);
                    
                    if (!terminated) {
                        scheduler.shutdownNow();
                    }
                    
                    if (logger != null) {
                        logger.info("Lock monitor stopped" + (!terminated ? " (forced)" : ""));
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    // Make sure to force shutdown even if interrupted
                    if (scheduler != null && !scheduler.isTerminated()) {
                        scheduler.shutdownNow();
                    }
                    
                    if (logger != null) {
                        logger.warning("Lock monitor stop was interrupted - forcing shutdown");
                    }
                } catch (Exception e) {
                    // Last resort effort to shut down
                    if (scheduler != null && !scheduler.isTerminated()) {
                        scheduler.shutdownNow();
                    }
                    
                    if (logger != null) {
                        logger.error("Error stopping lock monitor: " + e.getMessage());
                    }
                }
            }
        }
    }
    
    /**
     * Registers a lock acquisition.
     * 
     * @param lockName The name of the lock
     * @param ownerThread The thread that acquired the lock
     * @param purpose The purpose for which the lock was acquired
     */
    public void registerLockAcquisition(String lockName, String ownerThread, String purpose) {
        LockInfo lockInfo = new LockInfo(lockName, ownerThread, purpose);
        activeLocks.put(getKey(lockName, ownerThread), lockInfo);
        
        if (logger != null) {
            String message = LogFormatter.formatLockAcquired(lockName, ownerThread, purpose);
            logger.debug(message);
        }
    }
    
    /**
     * Registers a lock release.
     * 
     * @param lockName The name of the lock
     * @param ownerThread The thread that released the lock
     */
    public void registerLockRelease(String lockName, String ownerThread) {
        LockInfo removed = activeLocks.remove(getKey(lockName, ownerThread));
        
        if (logger != null && removed != null) {
            String message = LogFormatter.formatLockReleased(
                lockName, ownerThread, removed.getHeldForMs());
            logger.debug(message);
        }
    }
    
    /**
     * Creates a key for the activeLocks map.
     */
    private String getKey(String lockName, String ownerThread) {
        return lockName + "_" + ownerThread;
    }
    
    /**
     * Checks for locks that have been held for too long.
     */
    private void checkForLocksHeldTooLong() {
        long now = System.currentTimeMillis();
        
        for (Map.Entry<String, LockInfo> entry : activeLocks.entrySet()) {
            LockInfo lockInfo = entry.getValue();
            long heldTime = now - lockInfo.getAcquiredTimeMs();
            
            if (heldTime > lockWarningThresholdMs) {
                if (logger != null) {
                    logger.warning(String.format(
                        "Lock '%s' held by thread '%s' for too long (%d ms) " +
                        "for purpose: %s", 
                        lockInfo.getLockName(),
                        lockInfo.getOwnerThread(),
                        heldTime,
                        lockInfo.getPurpose()
                    ));
                }
            }
        }
    }
    
    /**
     * Checks for deadlocks in the JVM.
     */
    private void checkForDeadlocks() {
        ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();
        long[] deadlockedThreads = threadBean.findDeadlockedThreads();
        
        if (deadlockedThreads != null && deadlockedThreads.length > 0) {
            ThreadInfo[] threadInfos = threadBean.getThreadInfo(deadlockedThreads, true, true);
            
            if (logger != null) {
                logger.error("DEADLOCK DETECTED! Deadlocked threads:");
                for (ThreadInfo threadInfo : threadInfos) {
                    logger.error("Thread " + threadInfo.getThreadName() + " " +
                                 threadInfo.getThreadId() + " " +
                                 threadInfo.getThreadState());
                    
                    // Log the stack trace
                    for (StackTraceElement element : threadInfo.getStackTrace()) {
                        logger.error("    at " + element);
                    }
                    
                    // Log locks held by this thread
                    logger.error("    Locks held: " + threadInfo.getLockedMonitors().length);
                    for (MonitorInfo monitor : threadInfo.getLockedMonitors()) {
                        logger.error("      - locked " + monitor);
                    }
                    
                    // Log locks that this thread is waiting for
                    if (threadInfo.getLockInfo() != null) {
                        logger.error("    Waiting for lock: " + threadInfo.getLockInfo());
                        logger.error("    Lock owner: " + threadInfo.getLockOwnerName() +
                                     " (id: " + threadInfo.getLockOwnerId() + ")");
                    }
                    
                    logger.error("--------------------------");
                }
            }
        }
    }
    
    /**
     * Checks for all possible lock issues.
     */
    private void checkForLockIssues() {
        try {
            checkForLocksHeldTooLong();
            checkForDeadlocks();
        } catch (Exception e) {
            if (logger != null) {
                logger.error("Error in lock monitor: " + e.getMessage());
            }
        }
    }
} 