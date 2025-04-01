package com.project.logging;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * LogFormatter provides utility methods for formatting log messages.
 * This helps ensure consistent formatting across the system.
 */
public class LogFormatter {
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
    
    /**
     * Formats a lock status message.
     * 
     * @param lockName The name of the lock
     * @param isLocked Whether the lock is currently held
     * @param owner The owner of the lock (may be null if not known)
     * @param waitingThreads The number of threads waiting for the lock (may be -1 if not known)
     * @return A formatted lock status message
     */
    public static String formatLockStatus(String lockName, boolean isLocked, String owner, int waitingThreads) {
        StringBuilder sb = new StringBuilder();
        sb.append("LOCK_STATUS [").append(DATE_FORMAT.format(new Date())).append("] ");
        sb.append("Lock: ").append(lockName).append(" | ");
        sb.append("Status: ").append(isLocked ? "HELD" : "FREE").append(" | ");
        
        if (owner != null && isLocked) {
            sb.append("Owner: ").append(owner).append(" | ");
        }
        
        if (waitingThreads >= 0) {
            sb.append("Waiting Threads: ").append(waitingThreads);
        }
        
        return sb.toString();
    }
    
    /**
     * Formats a lock acquisition message.
     * 
     * @param lockName The name of the lock
     * @param owner The thread or component acquiring the lock
     * @param purpose The purpose for acquiring the lock
     * @return A formatted lock acquisition message
     */
    public static String formatLockAcquired(String lockName, String owner, String purpose) {
        return String.format("LOCK_ACQUIRED [%s] Lock: %s | Owner: %s | Purpose: %s", 
                DATE_FORMAT.format(new Date()), lockName, owner, purpose);
    }
    
    /**
     * Formats a lock release message.
     * 
     * @param lockName The name of the lock
     * @param owner The thread or component releasing the lock
     * @param heldForMs Time the lock was held in milliseconds
     * @return A formatted lock release message
     */
    public static String formatLockReleased(String lockName, String owner, long heldForMs) {
        return String.format("LOCK_RELEASED [%s] Lock: %s | Owner: %s | Held for: %dms", 
                DATE_FORMAT.format(new Date()), lockName, owner, heldForMs);
    }
    
    /**
     * Formats thread information for debugging purposes.
     * 
     * @param threadName The name of the thread
     * @param threadId The ID of the thread
     * @param threadState The state of the thread
     * @param action The action the thread is performing
     * @return A formatted thread information message
     */
    public static String formatThreadInfo(String threadName, long threadId, Thread.State threadState, String action) {
        return String.format("THREAD_INFO [%s] Thread: %s | ID: %d | State: %s | Action: %s", 
                DATE_FORMAT.format(new Date()), threadName, threadId, threadState, action);
    }
    
    /**
     * Formats current thread information.
     * 
     * @param action The action the thread is performing
     * @return A formatted thread information message for the current thread
     */
    public static String formatCurrentThreadInfo(String action) {
        Thread currentThread = Thread.currentThread();
        return formatThreadInfo(
            currentThread.getName(),
            currentThread.getId(),
            currentThread.getState(),
            action
        );
    }
} 