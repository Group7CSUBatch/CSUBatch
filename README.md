# CSUbatch Job Scheduling System

CSUbatch is a job scheduling system that allows users to submit jobs with different CPU time requirements and priorities. The system uses different scheduling policies to determine the order of job execution.

## Features

- **Job Submission:** Submit jobs with customizable CPU time and priority.
- **Scheduling Policies:** Choose from multiple scheduling policies:
  - First Come First Served (FCFS)
  - Shortest Job First (SJF)
  - Priority-based scheduling
- **System Management:** View detailed job status and system information.
- **Command-Line Interface:** Interact with the system via console commands.
- **Testing:** Built-in testing capabilities using JUnit and Mockito.
- **Logging:** Comprehensive logging for system events and errors.

## Prerequisites

Before you can build and run CSUbatch, you need to have the following installed:

- **Java Development Kit (JDK):** Version 1.8 or higher is required. You can download OpenJDK from [Adoptium](https://adoptium.net/) or use your system's package manager.
- **Apache Maven:** This project uses Maven for dependency management and building. You can download it from the [Maven website](https://maven.apache.org/download.cgi) or install it using a package manager (like `apt` for Debian/Ubuntu or `brew` for macOS).

**Linux (Debian/Ubuntu) Command-Line Setup:**

If you are on a fresh Debian-based Linux system (like Ubuntu), you can install both the default JDK and Maven using `apt` after cloning this repository:

```bash
# Update package list
sudo apt update

# Install default JDK and Maven
sudo apt install default-jdk maven -y

chmod +x ./CSUbatch
```

Make sure both `java -version` and `mvn -v` commands work and show appropriate versions before proceeding.

## Project Structure

- `src/main/java/com/project/`
  - `core/`: Core data structures (`Job`, `JobQueue`, `JobStatus`).
  - `logging/`: Logging components (`Logger`, `LoggingSystem`).
  - `management/`: System control and job management (`SystemController`, `JobQueueManager`, `JobStateManager`).
  - `scheduler/`: Job scheduling logic (`Scheduler`, `Dispatcher`).
  - `unified/`: Console interface and application entry point (`ConsoleInterface`).
  - `App.java`: Main application entry point.
  - `TestRunner.java`: Utility for running performance tests.
- `src/test/java/com/project/`: Unit and integration tests.
- `logs/`: Directory for log file output.
- `target/`: Compiled code and packaged JAR file.
- `pom.xml`: Maven project configuration.
- `Makefile`: Makefile for build, run, and utility tasks.

## Building and Running

You can build and run the project using either Maven or the provided Makefile.

**Using Maven:**

```bash
# Clean the project and build the executable JAR
mvn clean package

# Run the application
java -jar target/CSUbatch.jar

# Run tests
mvn test
```

**Using Makefile:**

```bash
# Build everything (clean, compile, test, create JAR)
make build

# Run the application (after building)
make run

# Quickly build without Checkstyle/JaCoCo (useful for development)
make quick-build

# Run the application after a quick build
make quick-run

# Compile source code
make compile

# Run tests
make test

# Clean build artifacts
make clean
```

See `make help` for more Makefile targets.

## Application start


```
# compile and test application
mvn clean package

# run application
./CSUbatch
```


## Available Application Commands

When the application is running, you can use the following commands:

- `help [-command]` - Show help message (optionally for a specific command).
- `run <job_name> <cpu_time> <priority>` - Submit a job.
- `list` - List all jobs in the queue and the currently running job.
- `fcfs` - Set scheduling policy to First Come First Served.
- `sjf` - Set scheduling policy to Shortest Job First.
- `priority` - Set scheduling policy to Priority-based scheduling.
- `load <filename>` - Load jobs from a file.
- `test [benchmark_name policy num_jobs max_cpu_time max_priority arrival_interval]` - Run performance tests.
- `version` - Display system version.
- `status` - Display current system status, policy, and queue size.
- `quit` or `exit` - Exit the system.

## Makefile Development Targets

The `Makefile` provides additional utility targets for development:

- `make help`: Show all available Makefile targets.
- `make clean-logs`: Remove all files from the `logs/` directory.
- `make clean-backups`: Remove backup (`.bak`) files created by fix tasks.
- `make fix-indent`: Attempt to automatically fix indentation in Java source files.
- `make clean-all`: Run `clean`, `clean-logs`, and `clean-backups`.

## Dependencies

- **JUnit 5:** For unit testing.
- **Mockito:** For creating mock objects in tests.

(See `pom.xml` for detailed dependency information)

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Contributors

Group 7 students 