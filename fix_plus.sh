#!/bin/bash

echo "Removing + signs from Java files..."

# Find all Java files and remove the leading + signs
find src -name "*.java" -type f | while read file; do
  echo "Processing $file"
  # Create a backup
  cp "$file" "${file}.bak"
  # Remove the leading + signs
  sed -i 's/^                + //' "$file"
done

echo "All + signs have been removed. Backup files have been saved with .bak extension." 