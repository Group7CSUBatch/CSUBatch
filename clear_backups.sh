#!/bin/bash

# Script to clear all .bak files in the src directory

echo "Searching for backup (.bak) files..."

# Find and count all .bak files
bak_files=$(find src -name "*.bak" -type f)
count=$(echo "$bak_files" | grep -v '^$' | wc -l)

if [ $count -eq 0 ]; then
    echo "No .bak files found."
    exit 0
fi

echo "Found $count .bak files. Removing..."

# Remove all .bak files
find src -name "*.bak" -type f -delete

echo "All .bak files have been removed successfully." 