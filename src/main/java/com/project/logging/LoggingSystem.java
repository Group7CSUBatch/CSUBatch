package com.project.logging;

import com.project.core.Job;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * LoggingSystem provides centralized logging functionality for the CSUbatch system.
 * It handles writing log messages to files and console, as well as tracking
 * job execution statistics.
 */
public class LoggingSystem {
    // Constants for log file paths and formats
    private static final String LOG_DIRECTORY = "logs";
    private static final String LOG_FILE_PREFIX = "csubatch_";
    private static final String LOCK_LOG_FILE_PREFIX = "locks_";
    private static final String LOG_FILE_EXTENSION = ".log";
    private static final String DATE_FORMAT = "yyyy-MM-dd_HH-mm-ss";
    private static final String TIMESTAMP_FORMAT = "yyyy-MM-dd HH:mm:ss.SSS";
    private static final int MAX_LOG_FILE_SIZE = 5 * 1024 * 1024; // 5MB
    
    // File handling variables
    private final SimpleDateFormat dateFormat;
    private final SimpleDateFormat timestampFormat;
    private File currentLogFile;
    private File currentLockLogFile;
    private BufferedWriter logWriter;
    private BufferedWriter lockLogWriter;
    
    // Map to store statistics about each job
    private final Map<String, JobStats> jobStats = new HashMap<>();
    
    /**
     * Constructs a new LoggingSystem and creates a log file.
     */
    public LoggingSystem() {
        dateFormat = new SimpleDateFormat(DATE_FORMAT);
        timestampFormat = new SimpleDateFormat(TIMESTAMP_FORMAT);
        
        createLogFile();
        createLockLogFile();
    }
    
    /**
     * Logs a message with the specified level.
     *
     * @param level   The log level
     * @param message The message to log
     */
    public void log(LogLevel level, String message) {
        String timestamp = getTimestamp();
        String logMessage = String.format("[%s] [%s] %s", timestamp, level, message);
        
        // Check if this is a lock-related message
        boolean isLockMessage = message.contains("LOCK_STATUS") || 
                               message.contains("LOCK_ACQUIRED") || 
                               message.contains("LOCK_RELEASED");
        
        // Write to appropriate file
        if (isLockMessage) {
            writeToLockLogFile(logMessage);
        } else {
            writeToLogFile(logMessage);
        }
        
        // Also print to console for higher-priority messages or lock status messages
        if (level.getPriority() > LogLevel.INFO.getPriority() || isLockMessage) {
            System.out.println(logMessage);
        }
    }
    
    /**
     * Logs a transaction (general system operation).
     *
     * @param message The transaction message
     */
    public void logTransaction(String message) {
        log(LogLevel.INFO, "TRANSACTION: " + message);
    }
    
    /**
     * Logs details about a job.
     *
     * @param job          The job
     * @param waitTime     The time the job spent waiting
     * @param executionTime The time taken to execute the job
     * @param turnaroundTime The total time from submission to completion
     */
    public void logJobDetails(Job job, long waitTime, long executionTime, long turnaroundTime) {
        if (job == null) {
            return;
        }
        
        String jobInfo = String.format(
            "JOB_DETAILS: [%s] Wait: %dms, Execution: %dms, Turnaround: %dms, CPU: %d, Priority: %d",
            job.getName(),
            waitTime,
            executionTime,
            turnaroundTime,
            job.getCpuTime(),
            job.getPriority()
        );
        
        log(LogLevel.INFO, jobInfo);
        
        // Store stats for later analysis
        JobStats stats = new JobStats(job.getName(), job.getCpuTime(), job.getPriority());
        stats.setWaitTime(waitTime);
        stats.setExecutionTime(executionTime);
        stats.setTurnaroundTime(turnaroundTime);
        jobStats.put(job.getName(), stats);
    }
    
    /**
     * Updates the job queue view with the specified job.
     *
     * @param job The job that was updated
     */
    public void updateJobQueueView(Job job) {
        if (job == null) {
            return;
        }
        
        log(LogLevel.DEBUG, String.format(
            "QUEUE_UPDATE: Job [%s] is now in state [%s]",
            job.getName(),
            job.getStatus()
        ));
    }
    
    /**
     * Logs the queue status.
     *
     * @param message The queue status message
     */
    public void logQueueStatus(String message) {
        log(LogLevel.INFO, "QUEUE_STATUS: " + message);
    }
    
    /**
     * Logs a job-specific transaction.
     *
     * @param job     The job
     * @param message The transaction message
     */
    public void logJobTransaction(Job job, String message) {
        if (job == null) {
            return;
        }
        
        log(LogLevel.INFO, String.format(
            "JOB_TRANSACTION: [%s] %s",
            job.getName(),
            message
        ));
    }
    
    /**
     * Closes the log file.
     */
    public void close() {
        try {
            if (logWriter != null) {
                logWriter.flush();
                logWriter.close();
                logWriter = null;
            }
            
            if (lockLogWriter != null) {
                lockLogWriter.flush();
                lockLogWriter.close();
                lockLogWriter = null;
            }
        } catch (IOException e) {
            System.err.println("Error closing log files: " + e.getMessage());
        }
    }
    
    /**
     * Gets the current log file path.
     *
     * @return The current log file path
     */
    public String getCurrentLogFile() {
        return currentLogFile != null ? currentLogFile.getAbsolutePath() : "No log file";
    }
    
    /**
     * Gets the current lock log file path.
     *
     * @return The current lock log file path
     */
    public String getCurrentLockLogFile() {
        return currentLockLogFile != null ? currentLockLogFile.getAbsolutePath() : "No lock log file";
    }
    
    /**
     * Gets the current timestamp.
     *
     * @return The current timestamp as a string
     */
    private String getTimestamp() {
        return timestampFormat.format(new Date());
    }
    
    /**
     * Creates a new log file.
     */
    private void createLogFile() {
        try {
            // Create logs directory if it doesn't exist
            File logDir = new File(LOG_DIRECTORY);
            if (!logDir.exists()) {
                if (!logDir.mkdirs()) {
                    System.err.println("Failed to create log directory: " + logDir.getAbsolutePath());
                    return;
                }
            }
            
            // Create a new log file with timestamp in the name
            String timestamp = dateFormat.format(new Date());
            String logFileName = LOG_FILE_PREFIX + timestamp + LOG_FILE_EXTENSION;
            currentLogFile = new File(logDir, logFileName);
            
            // Close existing writer if any
            if (logWriter != null) {
                logWriter.close();
            }
            
            // Create a new writer
            logWriter = new BufferedWriter(new FileWriter(currentLogFile));
            
            // Log the creation of the new log file
            String message = "Created new log file: " + currentLogFile.getAbsolutePath();
            writeToLogFile(String.format("[%s] [%s] %s", getTimestamp(), LogLevel.INFO, message));
            System.out.println(message);
        } catch (IOException e) {
            System.err.println("Error creating log file: " + e.getMessage());
        }
    }
    
    /**
     * Writes a message to the log file.
     *
     * @param message The message to write
     */
    private synchronized void writeToLogFile(String message) {
        try {
            if (logWriter == null) {
                createLogFile();
            }
            
            // Check if we need to rotate the log file
            if (currentLogFile != null && currentLogFile.length() > MAX_LOG_FILE_SIZE) {
                System.out.println("Log file size exceeded " + (MAX_LOG_FILE_SIZE / 1024 / 1024) + "MB, rotating...");
                createLogFile(); // This creates a new log file
            }
            
            if (logWriter != null) {
                logWriter.write(message);
                logWriter.newLine();
                logWriter.flush();
            }
        } catch (IOException e) {
            System.err.println("Error writing to log file: " + e.getMessage());
        }
    }
    
    /**
     * Creates a new lock log file.
     */
    private void createLockLogFile() {
        try {
            // Create logs directory if it doesn't exist
            File logDir = new File(LOG_DIRECTORY);
            if (!logDir.exists()) {
                if (!logDir.mkdirs()) {
                    System.err.println("Failed to create log directory: " + logDir.getAbsolutePath());
                    return;
                }
            }
            
            // Create a new lock log file with timestamp in the name
            String timestamp = dateFormat.format(new Date());
            String lockLogFileName = LOCK_LOG_FILE_PREFIX + timestamp + LOG_FILE_EXTENSION;
            currentLockLogFile = new File(logDir, lockLogFileName);
            
            // Close existing writer if any
            if (lockLogWriter != null) {
                lockLogWriter.close();
            }
            
            // Create a new writer
            lockLogWriter = new BufferedWriter(new FileWriter(currentLockLogFile));
            
            // Log the creation of the new lock log file
            String message = "Created new lock log file: " + currentLockLogFile.getAbsolutePath();
            String formattedMessage = String.format("[%s] [%s] %s", getTimestamp(), LogLevel.INFO, message);
            lockLogWriter.write(formattedMessage);
            lockLogWriter.newLine();
            lockLogWriter.flush();
            System.out.println(message);
        } catch (IOException e) {
            System.err.println("Error creating lock log file: " + e.getMessage());
        }
    }
    
    /**
     * Writes a message to the lock log file.
     *
     * @param message The message to write
     */
    private synchronized void writeToLockLogFile(String message) {
        try {
            if (lockLogWriter == null) {
                createLockLogFile();
            }
            
            // Check if we need to rotate the log file
            if (currentLockLogFile != null && currentLockLogFile.length() > MAX_LOG_FILE_SIZE) {
                System.out.println("Lock log file size exceeded " + (MAX_LOG_FILE_SIZE / 1024 / 1024) + "MB, rotating...");
                createLockLogFile(); // This creates a new log file
            }
            
            if (lockLogWriter != null) {
                lockLogWriter.write(message);
                lockLogWriter.newLine();
                lockLogWriter.flush();
            }
        } catch (IOException e) {
            System.err.println("Error writing to lock log file: " + e.getMessage());
        }
    }
    
    /**
     * Class to store statistics about a job.
     */
    private static class JobStats {
        private final String jobName;
        private final int cpuTime;
        private final int priority;
        private long waitTime;
        private long executionTime;
        private long turnaroundTime;
        
        public JobStats(String jobName, int cpuTime, int priority) {
            this.jobName = jobName;
            this.cpuTime = cpuTime;
            this.priority = priority;
        }
        
        public void setWaitTime(long waitTime) {
            this.waitTime = waitTime;
        }
        
        public void setExecutionTime(long executionTime) {
            this.executionTime = executionTime;
        }
        
        public void setTurnaroundTime(long turnaroundTime) {
            this.turnaroundTime = turnaroundTime;
        }
    }
}
