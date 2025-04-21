#!/bin/bash

# Build script for CSUbatch Unified Application
# This script compiles and runs the unified CSUbatch application

# Colors for terminal output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[0;33m'
NC='\033[0m' # No Color

# Create necessary directories
mkdir -p build
mkdir -p logs

echo -e "${YELLOW}Compiling CSUbatch Unified Application...${NC}"

# Compile the application
javac -d build src/main/java/com/project/*.java src/main/java/com/project/unified/*.java

# Check if compilation was successful
if [ $? -eq 0 ]; then
    echo -e "${GREEN}Compilation successful!${NC}"
    
    # Run the application
    if [ "$1" == "run" ]; then
        echo -e "${YELLOW}Running CSUbatch Unified Application...${NC}"
        java -cp build com.project.unified.UnifiedApp
    elif [ "$1" == "test" ]; then
        echo -e "${YELLOW}Running CSUbatch Tests...${NC}"
        java -cp build com.project.unified.UnifiedApp test
    else
        echo -e "${YELLOW}Application successfully built.${NC}"
        echo -e "To run the application, use: ${GREEN}./build.sh run${NC}"
        echo -e "To run the tests, use: ${GREEN}./build.sh test${NC}"
    fi
else
    echo -e "${RED}Compilation failed.${NC}"
    exit 1
fi 