# CSUbatch Job Scheduling System

CSUbatch is a job scheduling system that allows users to submit jobs with different CPU time requirements and priorities. The system uses different scheduling policies to determine the order of job execution.

## Unified Implementation

The system now exclusively uses the unified implementation in the `src/main/java/com/project/unified` package. This implementation offers improved design, better error handling, and enhanced features. See the [Unified Implementation README](src/main/java/com/project/unified/README.md) for details.

The legacy implementation has been completely removed. All code should use the unified implementation classes.

## Features

- Submit jobs with customizable CPU time and priority
- Choose from multiple scheduling policies:
  - First Come First Served (FCFS)
  - Shortest Job First (SJF)
  - Priority-based scheduling
- View detailed job status and system information
- Built-in testing capabilities
- Comprehensive logging

## Building and Running

Using the unified implementation's build script:

```bash
# To build the application
./build.sh

# To build and run the application
./build.sh run

# To run tests
./build.sh test
```

Or using Maven:

```bash
# Build with Maven
mvn clean package

# Run the application
java -jar target/CSUbatch.jar
```

## Available Commands

- `help` - Show the help message
- `run <job_name> <cpu_time> <priority>` - Submit a job
- `list` - List all jobs in the queue
- `fcfs` - Set scheduling policy to First Come First Served
- `sjf` - Set scheduling policy to Shortest Job First
- `priority` - Set scheduling policy to Priority
- `test` - Run automated tests
- `version` - Display system version (unified implementation only)
- `status` - Display system status (unified implementation only)
- `quit` - Exit the system

## Project Structure

- `src/main/java/com/project/` - Core components (Job, JobQueue, Scheduler, Dispatcher)
- `src/main/java/com/project/unified/` - Unified implementation (UnifiedApp, UnifiedApplicationManager, ConsoleInterface)
- `src/test/java/com/project/` - Test classes
- `logs/` - Log files directory

## Recent Improvements

1. Unified the interface classes into a single, cleaner ConsoleInterface
2. Removed the legacy implementation (App.java and ApplicationManager.java)
3. Created a more maintainable and better organized application structure
4. Added new commands: `version` and `status`
5. Improved error handling throughout the codebase
6. Enhanced the job listing display with better formatting
7. Added a proper implementation of the test command

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Contributors

Group 7 students 