package com.project;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * LoggingSystem class responsible for logging system events and job transactions.
 * Provides methods for logging queue status and transactions with timestamps.
 * Creates a new log file with a timestamp every time the application is initiated.
 */
public class LoggingSystem {
    private static final String LOG_DIRECTORY = "logs";
    private static final String LOG_FILE_PREFIX = "csubatch_";
    private static final String JOB_LOG_FILE_PREFIX = "jobs_";
    private static final String JOB_QUEUE_VIEW_FILE = "jobqueueview.log";
    private static final String LOG_FILE_EXTENSION = ".log";
    private static final long MAX_LOG_SIZE = 1024 * 1024; // 1MB
    private static final int MAX_SEQUENCE_NUMBER = 999; // Maximum sequence number (3 digits)

    private final String currentLogFile;
    private final String currentJobLogFile;
    private final String jobQueueViewFile;

    /**
     * Constructs a new LoggingSystem and creates a new log file with a timestamp.
     */
    public LoggingSystem() {
        // Create logs directory if it doesn't exist
        File logsDir = new File(LOG_DIRECTORY);
        if (!logsDir.exists()) {
            boolean created = logsDir.mkdir();
            if (!created) {
                System.err.println("Failed to create logs directory: " + LOG_DIRECTORY);
            }
        }

        // Generate a unique log filename with month name, date, time, and sequence number
        String logFileName = generateLogFileName(LOG_FILE_PREFIX);
        currentLogFile = LOG_DIRECTORY + File.separator + logFileName;

        // Generate a unique job log filename with the same sequence number
        String jobLogFileName = generateLogFileName(JOB_LOG_FILE_PREFIX);
        currentJobLogFile = LOG_DIRECTORY + File.separator + jobLogFileName;

        // Set the job queue view file path
        jobQueueViewFile = LOG_DIRECTORY + File.separator + JOB_QUEUE_VIEW_FILE;

        // Create the new log files with initialization messages
        createNewLogFile(currentLogFile, "System");
        createNewJobLogFile();
        createJobQueueViewFile();

        System.out.println("Log files created: " + logFileName + " and " + jobLogFileName);
        System.out.println("Job queue view file created: " + JOB_QUEUE_VIEW_FILE);
    }

    /**
     * Generates a log file name with the format: prefix_monthname_date_time_sequence.log
     * Automatically determines the next sequence number by examining existing log files.
     *
     * @param prefix The prefix to use for the log file name
     * @return The generated log file name
     */
    private String generateLogFileName(String prefix) {
        // Format: prefix_monthname_date_time_sequence.log
        // Example: csubatch_february_28_223553_001.log or jobs_february_28_223553_001.log
        SimpleDateFormat monthFormat = new SimpleDateFormat("MMMM", Locale.ENGLISH);
        SimpleDateFormat dayFormat = new SimpleDateFormat("dd");
        SimpleDateFormat timeFormat = new SimpleDateFormat("HHmmss");

        Date now = new Date();
        String monthName = monthFormat.format(now).toLowerCase();
        String day = dayFormat.format(now);
        String time = timeFormat.format(now);

        // Find the next available sequence number by examining existing log files
        int nextSequence = findNextSequenceNumber(monthName, day, prefix);
        String sequenceStr = String.format("%03d", nextSequence);

        return prefix + monthName + "_" + day + "_" + time + "_" + sequenceStr + LOG_FILE_EXTENSION;
    }

    /**
     * Finds the next sequence number by examining existing log files for the current day.
     * This method handles edge cases and ensures a valid sequence number is always returned.
     *
     * @param monthName The current month name
     * @param day The current day
     * @param prefix The prefix of the log file to check
     * @return The next sequence number to use
     */
    private int findNextSequenceNumber(String monthName, String day, String prefix) {
        File logsDir = new File(LOG_DIRECTORY);
        File[] logFiles = logsDir.listFiles();

        if (logFiles == null || logFiles.length == 0) {
            return 1; // Start with 1 if no files exist
        }

        // Pattern to match log files for the current day and extract sequence numbers
        // Example: prefix_february_28_223553_001.log
        String patternStr = prefix + monthName + "_" + day + "_\\d+_(\\d{3})" + LOG_FILE_EXTENSION;
        Pattern pattern = Pattern.compile(patternStr);

        int maxSequence = 0;
        int filesForToday = 0;

        for (File file : logFiles) {
            String fileName = file.getName();
            Matcher matcher = pattern.matcher(fileName);

            if (matcher.matches()) {
                filesForToday++;
                try {
                    int sequence = Integer.parseInt(matcher.group(1));
                    if (sequence > maxSequence) {
                        maxSequence = sequence;
                    }
                } catch (NumberFormatException e) {
                    // Ignore files with invalid sequence numbers
                    System.err.println("Invalid sequence number in file: " + fileName);
                }
            }
        }

        // If we've reached the maximum sequence number, start over
        if (maxSequence >= MAX_SEQUENCE_NUMBER) {
            System.out.println("Warning: Maximum sequence number reached. Starting over from 1.");
            return 1;
        }

        // If no files found for today, start with 1
        if (filesForToday == 0) {
            return 1;
        }

        return maxSequence + 1; // Next sequence number
    }

    /**
     * Creates a new log file with an initialization message.
     * Includes information about the sequence number in the log file.
     *
     * @param logFilePath The path to the log file to create
     * @param logType The type of log file being created (e.g., "System", "Job")
     */
    private void createNewLogFile(String logFilePath, String logType) {
        try (FileWriter writer = new FileWriter(logFilePath)) {
            writer.write("Log file created at " + getCurrentTimestamp() + "\n");
            writer.write("CSUbatch " + logType + " Log Started\n");

            // Extract sequence number from the file name
            Pattern pattern = Pattern.compile(".*_(\\d{3})" + Pattern.quote(LOG_FILE_EXTENSION) + "$");
            Matcher matcher = pattern.matcher(logFilePath);
            String sequenceInfo = "Unknown sequence";

            if (matcher.matches()) {
                sequenceInfo = "Sequence number: " + matcher.group(1);
            }

            writer.write("Log file information: " + sequenceInfo + "\n");
        } catch (IOException e) {
            System.err.println("Error creating new log file: " + e.getMessage());
        }
    }

    /**
     * Creates a new job log file with an initialization message.
     * This log file will contain detailed information about jobs.
     */
    private void createNewJobLogFile() {
        try (FileWriter writer = new FileWriter(currentJobLogFile)) {
            writer.write("Job log file created at " + getCurrentTimestamp() + "\n");
            writer.write("CSUbatch Job Tracking System started\n");

            // Extract sequence number from the file name
            Pattern pattern = Pattern.compile(".*_(\\d{3})" + Pattern.quote(LOG_FILE_EXTENSION) + "$");
            Matcher matcher = pattern.matcher(currentJobLogFile);
            String sequenceInfo = "Unknown sequence";

            if (matcher.matches()) {
                sequenceInfo = "Sequence number: " + matcher.group(1);
            }

            writer.write("Log file information: " + sequenceInfo + "\n");
            writer.write("----------------------------------------\n");
            writer.write("JOB ID | NAME | CPU TIME | PRIORITY | STATUS | ARRIVAL TIME | "
                    + "COMPLETION TIME | WAITING TIME | TURNAROUND TIME\n");
            writer.write("----------------------------------------\n");
        } catch (IOException e) {
            System.err.println("Error creating new job log file: " + e.getMessage());
        }
    }

    /**
     * Creates a new job queue view file that will be overwritten each time the application starts.
     * This file will contain a list of all jobs that enter the queue with their metadata.
     */
    private void createJobQueueViewFile() {
        try {
            File file = new File(jobQueueViewFile);
            // Ensure parent directory exists
            File parent = file.getParentFile();
            if (parent != null && !parent.exists()) {
                boolean created = parent.mkdirs();
                if (!created) {
                    System.err.println("Failed to create parent directory for job queue view file: "
                            + parent.getPath());
                }
            }

            // Create the file with overwrite mode
            try (FileWriter writer = new FileWriter(file, false)) {
                writer.write("JOB QUEUE VIEW LOG - Created at " + getCurrentTimestamp() + "\n");
                writer.write("This file contains a list of all jobs that enter the queue with their metadata.\n");
                writer.write("This file is overwritten each time the application is initiated.\n");
                writer.write("----------------------------------------\n");
                writer.write("JOB ID | NAME | CPU TIME | PRIORITY | STATUS | ARRIVAL TIME\n");
                writer.write("----------------------------------------\n");
                writer.flush(); // Ensure content is written to disk
            }

            System.out.println("Job queue view file created at: " + file.getAbsolutePath());
        } catch (IOException e) {
            System.err.println("Error creating job queue view file: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Logs the current status of the job queue.
     *
     * @param message The message describing the queue status
     */
    public synchronized void logQueueStatus(String message) {
        try (FileWriter writer = new FileWriter(currentLogFile, true)) {
            writer.write(getCurrentTimestamp() + " [QUEUE] " + message + "\n");
        } catch (IOException e) {
            System.err.println("Error writing to log file: " + e.getMessage());
        }
    }

    /**
     * Logs a job transaction or system event.
     *
     * @param message The message describing the transaction or event
     */
    public synchronized void logTransaction(String message) {
        try (FileWriter writer = new FileWriter(currentLogFile, true)) {
            writer.write(getCurrentTimestamp() + " [TRANSACTION] " + message + "\n");
        } catch (IOException e) {
            System.err.println("Error writing to log file: " + e.getMessage());
        }
    }

    /**
     * Logs detailed information about a job to the job log file.
     *
     * @param job The job to log
     * @param completionTime The completion time of the job (0 if not completed)
     * @param waitingTime The waiting time of the job (0 if not applicable)
     * @param turnaroundTime The turnaround time of the job (0 if not applicable)
     */
    public synchronized void logJobDetails(Job job, long completionTime, long waitingTime, long turnaroundTime) {
        try (FileWriter writer = new FileWriter(currentJobLogFile, true)) {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String arrivalTimeStr = dateFormat.format(new Date(job.getArrivalTime()));
            String completionTimeStr = completionTime > 0
                    ? dateFormat.format(new Date(completionTime))
                    : "N/A";

            // Format: JOB_ID | NAME | CPU_TIME | PRIORITY | STATUS | ARRIVAL_TIME | COMPLETION_TIME | WAITING_TIME
            writer.write(String.format("%s | %s | %d | %d | %s | %s | %s | %d | %d\n",
                    job.getName(),
                    job.getName(),
                    job.getCpuTime(),
                    job.getPriority(),
                    job.getStatus(),
                    arrivalTimeStr,
                    completionTimeStr,
                    waitingTime,
                    turnaroundTime));
        } catch (IOException e) {
            System.err.println("Error writing to job log file: " + e.getMessage());
        }
    }

    /**
     * Logs a job queue update to the job log file.
     *
     * @param message The message describing the queue update
     */
    public synchronized void logJobQueueUpdate(String message) {
        try (FileWriter writer = new FileWriter(currentJobLogFile, true)) {
            writer.write(getCurrentTimestamp() + " [QUEUE_UPDATE] " + message + "\n");
        } catch (IOException e) {
            System.err.println("Error writing to job log file: " + e.getMessage());
        }
    }

    /**
     * Logs a job transaction to the job log file.
     *
     * @param message The message describing the job transaction
     */
    public synchronized void logJobTransaction(String message) {
        try (FileWriter writer = new FileWriter(currentJobLogFile, true)) {
            writer.write(getCurrentTimestamp() + " [JOB_TRANSACTION] " + message + "\n");
        } catch (IOException e) {
            System.err.println("Error writing to job log file: " + e.getMessage());
        }
    }

    /**
     * Updates the job queue view file with information about a job.
     * This method appends job information to the job queue view file.
     *
     * @param job The job to log
     */
    public synchronized void updateJobQueueView(Job job) {
        try {
            File file = new File(jobQueueViewFile);
            if (!file.exists()) {
                System.out.println("Job queue view file does not exist, creating it now...");
                createJobQueueViewFile();
            }

            try (FileWriter writer = new FileWriter(file, true)) {
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                String arrivalTimeStr = dateFormat.format(new Date(job.getArrivalTime()));

                // Format: JOB_ID | NAME | CPU_TIME | PRIORITY | STATUS | ARRIVAL_TIME
                String entry = String.format("%s | %s | %d | %d | %s | %s\n",
                        job.getName(),
                        job.getName(),
                        job.getCpuTime(),
                        job.getPriority(),
                        job.getStatus(),
                        arrivalTimeStr);

                writer.write(entry);
                writer.flush(); // Ensure content is written to disk

                // System.out.println("Updated job queue view file with job: " + job.getName()
                //         + ", Status: " + job.getStatus());
            }
        } catch (IOException e) {
            System.err.println("Error writing to job queue view file: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Gets the current timestamp in a formatted string.
     *
     * @return The formatted timestamp
     */
    private String getCurrentTimestamp() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
        return dateFormat.format(new Date());
    }

    /**
     * Gets the path of the current log file.
     *
     * @return The path of the current log file
     */
    public String getCurrentLogFile() {
        return currentLogFile;
    }

    /**
     * Gets the path of the current job log file.
     *
     * @return The path of the current job log file
     */
    public String getCurrentJobLogFile() {
        return currentJobLogFile;
    }

    /**
     * Gets the path of the job queue view file.
     *
     * @return The path of the job queue view file
     */
    public String getJobQueueViewFile() {
        return jobQueueViewFile;
    }
}
