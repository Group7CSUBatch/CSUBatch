#!/bin/bash

# Script to clear all log files in the logs directory

LOG_DIR="logs"

echo "Clearing all files in $LOG_DIR directory..."

# Check if logs directory exists
if [ -d "$LOG_DIR" ]; then
    # Remove all files in the logs directory
    rm -f "$LOG_DIR"/*
    echo "All log files have been removed."
else
    # Create the logs directory if it doesn't exist
    mkdir -p "$LOG_DIR"
    echo "Created $LOG_DIR directory (it didn't exist)."
fi

echo "Log cleanup completed successfully." 