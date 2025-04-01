package com.project.logging;

/**
 * Enum representing different logging levels.
 * This allows for filtering of log messages based on their severity.
 */
public enum LogLevel {
    /** Debug-level information, most verbose */
    DEBUG(1, "DEBUG"),
    
    /** Information messages */
    INFO(2, "INFO"),
    
    /** Warning messages */
    WARNING(3, "WARNING"),
    
    /** Error messages */
    ERROR(4, "ERROR"),
    
    /** Fatal error messages, most severe */
    FATAL(5, "FATAL");
    
    private final int priority;
    private final String displayName;
    
    /**
     * Constructs a LogLevel with the specified priority and display name.
     * 
     * @param priority The priority level (higher means more severe)
     * @param displayName The display name for the level
     */
    LogLevel(int priority, String displayName) {
        this.priority = priority;
        this.displayName = displayName;
    }
    
    /**
     * Gets the priority of this log level.
     * Higher priority means more severe.
     * 
     * @return The priority
     */
    public int getPriority() {
        return priority;
    }
    
    /**
     * Gets the display name of this log level.
     * 
     * @return The display name
     */
    public String getDisplayName() {
        return displayName;
    }
    
    /**
     * Converts a string to a LogLevel.
     * Case-insensitive.
     * 
     * @param levelString The string representation of the level
     * @return The corresponding LogLevel, or INFO if not found
     */
    public static LogLevel fromString(String levelString) {
        for (LogLevel level : values()) {
            if (level.displayName.equalsIgnoreCase(levelString)) {
                return level;
            }
        }
        return INFO; // Default to INFO if not found
    }
    
    @Override
    public String toString() {
        return displayName;
    }
} 