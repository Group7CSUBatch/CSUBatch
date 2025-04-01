package com.project.core;

/**
 * Enum representing the possible states of a Job in the system.
 * This provides a standardized way to represent job statuses across components.
 */
public enum JobStatus {
    /** Job is waiting to be processed */
    WAITING("Waiting"),
    
    /** Job has been selected by the scheduler */
    SELECTED("Selected"),
    
    /** Job is currently running */
    RUNNING("Running"),
    
    /** Job execution has been completed */
    COMPLETED("Completed"),
    
    /** Job execution was interrupted */
    INTERRUPTED("Interrupted"),
    
    /** Job has been canceled by the user */
    CANCELED("Canceled");
    
    private final String displayName;
    
    /**
     * Constructs a JobStatus with the given display name.
     * 
     * @param displayName The display name for the status
     */
    JobStatus(String displayName) {
        this.displayName = displayName;
    }
    
    /**
     * Returns the display name of this status.
     * 
     * @return The display name
     */
    public String getDisplayName() {
        return displayName;
    }
    
    /**
     * Converts a string representation to a JobStatus enum.
     * 
     * @param statusString The string representation of the status
     * @return The corresponding JobStatus enum, or WAITING if not found
     */
    public static JobStatus fromString(String statusString) {
        for (JobStatus status : values()) {
            if (status.displayName.equalsIgnoreCase(statusString)) {
                return status;
            }
        }
        return WAITING; // Default to WAITING if not found
    }
    
    @Override
    public String toString() {
        return displayName;
    }
} 