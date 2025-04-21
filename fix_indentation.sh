#!/bin/bash

echo "Fixing indentation issues in Java files..."

# Find all Java files and process them
find src -name "*.java" -type f | while read file; do
  echo "Processing $file"
  # Create a backup
  cp "$file" "${file}.bak"
  
  # Create a temporary file
  temp_file="${file}.temp"
  
  # Process each line
  while IFS= read -r line; do
    # Skip package, import declarations and class declarations which should not be indented
    if [[ $line =~ ^package ]] || [[ $line =~ ^import ]] || [[ $line =~ ^\/\*\* ]] || 
       [[ $line =~ ^\* ]] || [[ $line =~ ^public\ class ]] || [[ $line =~ ^public\ enum ]] || 
       [[ $line =~ ^class ]] || [[ $line =~ ^@SuppressWarnings ]] || [[ $line == "}" ]] || 
       [[ -z $line ]]; then
      echo "$line" >> "$temp_file"
    else
      echo "    $line" >> "$temp_file"
    fi
  done < "$file"
  
  # Replace the original file with the fixed version
  mv "$temp_file" "$file"
done

echo "Indentation fixes completed. Backup files have been saved with .bak extension."
echo "Please run your build again to check for remaining indentation issues." 