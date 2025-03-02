# CSUbatch Scheduling System

## Overview
The CSUbatch Scheduling System is a Java-based application designed to manage and execute jobs using various scheduling policies such as First Come First Served (FCFS), Shortest Job First (SJF), and Priority-based scheduling. The system includes a command-line interface for user interaction and a logging system to track job execution and system performance.

## Project Structure
- **src/main/java**: Contains the main application source code.
- **src/test/java**: Contains test source code.
- **docs**: Contains project documentation.
- **pom.xml**: Maven configuration file.

## How to Run the Project
1. Ensure you have Java and Maven installed on your system.
2. Navigate to the project root directory.
3. Use the following command to compile and run the project:
   ```
   mvn clean compile exec:java -Dexec.mainClass="com.project.App"
   ```

## Installation

### Prerequisites
1. **Java 8**: Ensure you have Java Development Kit (JDK) 8 installed on your system. You can download it from [Oracle's official website](https://www.oracle.com/java/technologies/javase-jdk8-downloads.html).
2. **Maven 3.6.3 or higher**: Ensure you have Apache Maven installed on your system. You can download it from [Maven's official website](https://maven.apache.org/download.cgi).

### Quickstart
1. **Install Java 8 (OpenJDK)**
   - For Windows, download and run the installer from [OpenJDK's official website](https://adoptopenjdk.net/).
   - For macOS, you can use Homebrew:
     ```sh
     brew install openjdk@8
     ```
   - For Linux, you can use your package manager. For example, on Ubuntu:
     ```sh
     sudo apt-get update
     sudo apt-get install openjdk-8-jdk
     ```

2. **Install Maven**
   - For Windows, download and unzip the binary from [Maven's official website](https://maven.apache.org/download.cgi), and add the `bin` directory to your system's PATH.
   - For macOS, you can use Homebrew:
     ```sh
     brew install maven
     ```
   - For Linux, you can use your package manager. For example, on Ubuntu:
     ```sh
     sudo apt-get update
     sudo apt-get install maven
     ```

3. **Verify Installation**
   - To verify that Java and Maven are installed correctly, run the following commands in your terminal:
     ```sh
     java -version
     mvn -version
     ```

### Project Setup
1. **Clone the repository**
   ```sh
   git clone https://github.com/Group7CSUBatch/CSUBatch.git
   cd CSUBatch
   ```

2. **Install Dependencies**
   - Navigate to the project root directory and run the following command to install the dependencies and build the project:
     ```sh
     mvn clean install
     ```

3. **Run the Project**
   - After the dependencies are installed, you can compile and run the project using the following command:
     ```sh
     mvn clean compile exec:java -Dexec.mainClass="com.project.App"
     ```

## Incomplete Tasks
- Implement detailed performance metrics calculation (turnaround time, CPU time, waiting time, throughput).
- Complete the automated test logic in the `runTests` method of `CommandLineInterface`.
- Enhance the logging system to include more detailed job and system information.
- Add more comprehensive test cases in `AppTest.java`.

## Commands
- **help**: Displays available commands.
- **run <job_name> <cpu_time> <priority>**: Submits a job with specified execution time and priority.
- **list**: Displays all jobs in the queue with their status.
- **fcfs**: Changes scheduling policy to First Come First Served.
- **sjf**: Changes scheduling policy to Shortest Job First.
- **priority**: Changes scheduling policy to Priority-based scheduling.
- **test**: Runs automated performance tests.
- **quit**: Exits the system and displays performance statistics.

## License
This project is developed for Coursework CPSC 6179
