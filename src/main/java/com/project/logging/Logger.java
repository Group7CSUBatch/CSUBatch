package com.project.logging;

import com.project.core.Job;

/**
 * Logger class provides a wrapper around the LoggingSystem for component-specific logging.
 * Each component should have its own Logger instance to identify the source of log messages.
 */
public class Logger {
    private final String componentName;
    private final LoggingSystem loggingSystem;
    private LogLevel minLogLevel = LogLevel.INFO;

    /**
     * Constructs a new Logger for a specific component.
     *
     * @param componentName The name of the component that owns this logger
     * @param loggingSystem The logging system to use
     */
    public Logger(String componentName, LoggingSystem loggingSystem) {
        this.componentName = componentName;
        this.loggingSystem = loggingSystem;
    }

    /**
     * Gets the logging system used by this logger.
     *
     * @return The logging system
     */
    public LoggingSystem getLoggingSystem() {
        return loggingSystem;
    }

    /**
     * Sets the minimum log level for this logger.
     * Messages with a level lower than this will not be logged.
     *
     * @param level The minimum log level
     */
    public void setMinLogLevel(LogLevel level) {
        this.minLogLevel = level != null ? level : LogLevel.INFO;
    }

    /**
     * Gets the minimum log level for this logger.
     *
     * @return The minimum log level
     */
    public LogLevel getMinLogLevel() {
        return minLogLevel;
    }

    /**
     * Logs a message at INFO level.
     *
     * @param message The message to log
     */
    public void info(String message) {
        log(LogLevel.INFO, message);
    }

    /**
     * Logs a message at WARNING level.
     *
     * @param message The message to log
     */
    public void warning(String message) {
        log(LogLevel.WARNING, message);
    }

    /**
     * Logs a message at ERROR level.
     *
     * @param message The message to log
     */
    public void error(String message) {
        log(LogLevel.ERROR, message);
    }

    /**
     * Logs a message at DEBUG level.
     *
     * @param message The message to log
     */
    public void debug(String message) {
        log(LogLevel.DEBUG, message);
    }

    /**
     * Logs a message related to a specific job at INFO level.
     *
     * @param job     The job related to the message
     * @param message The message to log
     */
    public void infoJob(Job job, String message) {
        logJob(LogLevel.INFO, job, message);
    }

    /**
     * Logs a message related to a specific job at WARNING level.
     *
     * @param job     The job related to the message
     * @param message The message to log
     */
    public void warningJob(Job job, String message) {
        logJob(LogLevel.WARNING, job, message);
    }

    /**
     * Logs a message related to a specific job at ERROR level.
     *
     * @param job     The job related to the message
     * @param message The message to log
     */
    public void errorJob(Job job, String message) {
        logJob(LogLevel.ERROR, job, message);
    }

    /**
     * Logs a message related to a specific job at DEBUG level.
     *
     * @param job     The job related to the message
     * @param message The message to log
     */
    public void debugJob(Job job, String message) {
        logJob(LogLevel.DEBUG, job, message);
    }

    /**
     * Logs a message at the specified level if it meets the minimum log level requirement.
     *
     * @param level   The log level
     * @param message The message to log
     */
    private void log(LogLevel level, String message) {
        if (shouldLog(level) && loggingSystem != null) {
            loggingSystem.log(level, formatMessage(message));
        }
    }

    /**
     * Logs a message related to a specific job at the specified level.
     *
     * @param level   The log level
     * @param job     The job related to the message
     * @param message The message to log
     */
    private void logJob(LogLevel level, Job job, String message) {
        if (shouldLog(level) && loggingSystem != null && job != null) {
            String jobInfo = formatJobInfo(job);
            loggingSystem.log(level, formatMessage(jobInfo + " - " + message));
        }
    }

    /**
     * Formats information about a job into a string.
     *
     * @param job The job
     * @return A formatted string with job information
     */
    private String formatJobInfo(Job job) {
        return String.format("Job[%s, CPU:%d, Pri:%d, Status:%s]",
                job.getName(),
                job.getCpuTime(),
                job.getPriority(),
                job.getStatus());
    }

    /**
     * Formats a log message to include the component name.
     *
     * @param message The original message
     * @return The formatted message
     */
    private String formatMessage(String message) {
        return String.format("[%s] %s", componentName, message);
    }

    /**
     * Checks if a message with the given level should be logged.
     *
     * @param level The log level to check
     * @return true if the message should be logged, false otherwise
     */
    private boolean shouldLog(LogLevel level) {
        return level.getPriority() >= minLogLevel.getPriority();
    }
} 