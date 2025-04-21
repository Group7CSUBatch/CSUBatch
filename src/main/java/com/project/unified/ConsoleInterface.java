package com.project.unified;

import com.project.core.Job;
import com.project.core.JobStatus;
import com.project.logging.LogLevel;
import com.project.logging.Logger;
import com.project.logging.LoggingSystem;
import com.project.management.JobQueueManager;
import com.project.management.JobStateManager;
import com.project.management.PerformanceMetrics;
import com.project.management.SystemController;
import com.project.scheduler.Dispatcher;
import com.project.scheduler.Scheduler;

import java.util.Scanner;
import java.util.concurrent.ConcurrentMap;
import java.util.List;
import java.util.stream.Collectors;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.io.File;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Map;
import java.util.HashMap;

/**
 * ConsoleInterface class is a unified interface for the CSUbatch system.
 * It replaces both UserInterface and CommandLineInterface with a single,
 * cleaner implementation that separates UI concerns from command logic.
 */
public class ConsoleInterface implements JobStateManager.JobStateListener {
    // Constants
    private static final int MIN_RUN_ARGS = 4;
    private static final int PRIORITY_INDEX = 3;
    private static final int MAX_WAITING_ESTIMATE = 3600;
    private static final String VERSION = "1.0";
    private static final String BUILD_DATE = "2025-04-20";
    
    // Help text map
    private final Map<String, String> helpTexts = new HashMap<>();
    
    // System components
    private final Scheduler scheduler;
    private final Dispatcher dispatcher;
    private final Logger logger;
    private final LoggingSystem loggingSystem; // Keep reference for job updates
    private final SystemController systemController;
    private final JobQueueManager jobQueueManager;
    
    // UI state
    private boolean isRunning = true;
    private Thread dispatcherThread;

    /**
     * Constructs a new ConsoleInterface with the specified components.
     *
     * @param systemController The system controller that manages all components
     */
    public ConsoleInterface(SystemController systemController) {
        this.systemController = systemController;
        this.jobQueueManager = systemController.getJobQueueManager();
        this.scheduler = systemController.getScheduler();
        this.dispatcher = systemController.getDispatcher();
        this.logger = systemController.getJobStateManager().getLogger();
        this.loggingSystem = logger != null ? logger.getLoggingSystem() : null;
        
        // Register as a listener for job state changes
        systemController.addJobStateListener(this);
        
        // Initialize help texts
        initializeHelpTexts();
    }

    /**
     * Initializes the help text map with command documentation.
     */
    private void initializeHelpTexts() {
        // General help text
        helpTexts.put("general", 
            "run <job> <time> <pri>: submit a job named <job>,\n" +
            "                 execution time is <time>,\n   priority is <pri>.\n" +
            "list: display the job status.\n" +
            "fcfs: change the scheduling policy to FCFS.\n" +
            "sjf: change the scheduling policy to SJF.\n" +
            "priority: change the scheduling policy to priority.\n" +
            "test <benchmark> <policy> <num_of_jobs> <priority_levels>\n" +
            "      <min_CPU_time> <max_CPU_time>\n" +
            "quit: exit CSUbatch");
        
        // Help text for individual commands
        helpTexts.put("run", 
            "run <job> <time> <pri>: submit a job named <job>,\n" +
            "                 execution time is <time>, priority is <pri>.\n\n" +
            "Parameters:\n" +
            "  <job>: Name for the job\n" +
            "  <time>: CPU time in seconds\n" +
            "  <pri>: Priority level (lower number means higher priority)\n\n" +
            "Example: run job1 10 2");
            
        helpTexts.put("list", 
            "list: display the job status.\n\n" +
            "Shows all jobs in the system with their status, CPU time, priority,\n" +
            "and arrival time.");
            
        helpTexts.put("fcfs", 
            "fcfs: change the scheduling policy to FCFS.\n\n" +
            "FCFS (First Come First Served) executes jobs in the order they arrive.");
            
        helpTexts.put("sjf", 
            "sjf: change the scheduling policy to SJF.\n\n" +
            "SJF (Shortest Job First) executes jobs with shortest CPU time first.");
            
        helpTexts.put("priority", 
            "priority: change the scheduling policy to priority.\n\n" +
            "Priority scheduling executes jobs with highest priority first\n" +
            "(lower number means higher priority).");
            
        helpTexts.put("test", 
            "test <benchmark> <policy> <num_of_jobs> <priority_levels>\n" +
            "     <min CPU time> <max CPU time>\n\n" +
            "Parameters:\n" +
            "  <benchmark>: Name of the benchmark test to run\n" +
            "  <policy>: Scheduling policy to use (fcfs, sjf, priority)\n" +
            "  <num_of_jobs>: Number of jobs to generate for the test\n" +
            "  <priority_levels>: Number of priority levels to use\n" +
            "  <min CPU time>: Minimum CPU time for generated jobs (seconds)\n" +
            "  <max CPU time>: Maximum CPU time for generated jobs (seconds)\n\n" +
            "Example: test benchmark1 sjf 10 3 5 20\n" +
            "  (Runs benchmark1 with SJF policy, 10 jobs, 3 priority levels,\n" +
            "   with CPU times between 5 and 20 seconds)");
            
        helpTexts.put("quit", 
            "quit: exit CSUbatch\n\n" +
            "Safely terminates the CSUbatch system.");
            
        helpTexts.put("exit", 
            "exit: exit CSUbatch\n\n" +
            "Safely terminates the CSUbatch system (alias for quit).");
            
        helpTexts.put("status", 
            "status: Show current system status and job statistics.\n\n" +
            "Displays information about scheduler policy, queue size, and\n" +
            "active threads.");
            
        helpTexts.put("version", 
            "version: Show version information.\n\n" +
            "Displays CSUbatch version and build date.");
            
        helpTexts.put("checklocks", 
            "checklocks: Check for any held locks (debugging).\n\n" +
            "Verifies the state of locks in the system. Useful for debugging.");
            
        helpTexts.put("load", 
            "load <file>: Load jobs from a file.\n\n" +
            "Parameters:\n" +
            "  <file>: Path to a job file, relative to current directory\n" +
            "          or from the 'jobloads' directory\n\n" +
            "Each line in the file should contain: <job_name> <cpu_time> <priority>");
    }

    /**
     * Starts the console interface.
     */
    public void start() {
        Scanner scanner = new Scanner(System.in);
        
        System.out.println("Welcome to Group 7's CSU batch job scheduler Version " + VERSION);
        System.out.println("Type 'help' to find more about CSUbatch commands.");
        logger.info("Console interface started");
        
        while (isRunning) {
            System.out.print("> ");
            String input = scanner.nextLine().trim();
            
            if ("quit".equalsIgnoreCase(input)) {
                isRunning = false;
                handleQuit();
                continue;
            }
            
            handleCommand(input);
        }
        
        scanner.close();
        // System.out.println("CSUbatch system is shutting down...");
        // logger.info("Console interface shutting down");
        
        // Unregister as a listener
        systemController.removeJobStateListener(this);
    }

    /**
     * Stops the console interface.
     */
    public void stop() {
        isRunning = false;
    }

    /**
     * Handles a command from the user.
     *
     * @param command The command to handle
     */
    public void handleCommand(String command) {
        if (command == null || command.trim().isEmpty()) {
            return;
        }

        String[] parts = command.trim().split("\\s+");
        String cmd = parts[0].toLowerCase();

        try {
            // Validate command before execution
            String validationError = validateCommand(parts);
            if (validationError != null) {
                System.out.println(validationError);
                return;
            }
            
            switch (cmd) {
                case "run":
                    submitJob(parts);
                    break;
                case "list":
                    listJobs();
                    break;
                case "fcfs":
                    setPolicy(Scheduler.Policy.FCFS);
                    break;
                case "sjf":
                    setPolicy(Scheduler.Policy.SJF);
                    break;
                case "priority":
                    setPolicy(Scheduler.Policy.PRIORITY);
                    break;
                case "test":
                    runTests(parts);
                    break;
                case "help":
                    if (parts.length > 1) {
                        printHelp(parts[1]);
                    } else {
                        printHelp(null);
                    }
                    break;
                case "quit":
                case "exit":
                    handleQuit();
                    break;
                case "version":
                    showVersion();
                    break;
                case "status":
                    showSystemStatus();
                    break;
                case "checklocks":
                    checkLocks();
                    break;
                case "load":
                    loadJobFile(parts);
                    break;
                default:
                    System.out.println("Unknown command: " + cmd);
                    System.out.println("Type 'help' for available commands.");
                    break;
            }
        } catch (Exception e) {
            System.out.println("Error executing command: " + e.getMessage());
            logger.error("Command error: " + e.getMessage() + " for command: " + command);
        }
    }
    
    /**
     * Validates a command and its arguments.
     * 
     * @param parts The command and its arguments
     * @return null if the command is valid, or an error message if it's invalid
     */
    private String validateCommand(String[] parts) {
        if (parts == null || parts.length == 0) {
            return "Error: Empty command";
        }
        
        String cmd = parts[0].toLowerCase();
        
        switch (cmd) {
            case "run":
                if (parts.length != MIN_RUN_ARGS) {
                    return "Error: 'run' command requires 3 arguments.\n" +
                           "Usage: run <job_name> <cpu_time> <priority>";
                }
                
                try {
                    int cpuTime = Integer.parseInt(parts[2]);
                    if (cpuTime <= 0) {
                        return "Error: CPU time must be a positive integer";
                    }
                    
                    int priority = Integer.parseInt(parts[PRIORITY_INDEX]);
                    if (priority < 0) {
                        return "Error: Priority must be a non-negative integer";
                    }
                } catch (NumberFormatException e) {
                    return "Error: CPU time and priority must be integers";
                }
                break;
                
            case "fcfs":
            case "sjf":
            case "priority":
            case "list":
            case "version":
            case "status": 
            case "checklocks":
            case "quit":
            case "exit":
                if (parts.length > 1) {
                    return "Error: '" + cmd + "' command takes no arguments";
                }
                break;
                
            case "help":
                if (parts.length > 2) {
                    return "Error: 'help' command takes at most one argument";
                }
                // second argument if present, should start with '-'
                if (parts.length == 2 && !parts[1].startsWith("-")) {
                    return "Error: 'help' command second argument should start with '-'";
                }
                break;
                
            case "load":
                if (parts.length < 2) {
                    return "Error: 'load' command requires a filename\n" +
                           "Usage: load <filename>";
                }
                break;
                
            case "test":
                // Test command validation will depend on how detailed your test command implementation is
                // Basic validation for now
                if (parts.length > 1 && parts.length < 7) {
                    return "Error: 'test' command requires all parameters or none\n" +
                           "Usage: test <benchmark> <policy> <num_of_jobs> <priority_levels> <min-CPU-time> <max-CPU-time>";
                }
                break;
                
            default:
                return "Unknown command: " + cmd + "\nType 'help' for available commands.";
        }
        
        return null; // No validation errors
    }
    
    /**
     * Handles the quit command.
     */
    private void handleQuit() {
        logger.info("User requested to quit");
        
        // Display performance metrics before exiting
        displayPerformanceMetrics();
    }
    
    /**
     * Displays the system performance metrics.
     */
    private void displayPerformanceMetrics() {
        PerformanceMetrics metrics = systemController.getPerformanceMetrics();
        
        // System.out.println("\n============== PERFORMANCE METRICS ==============");
        
        // Get metrics data
        double avgTurnaroundTime = metrics.getAverageTurnaroundTime();
        double avgWaitingTime = metrics.getAverageWaitingTime();
        double avgCpuTime = metrics.getAverageCpuTime();
        double throughput = metrics.getThroughput();
        int totalJobsCompleted = metrics.getTotalJobsCompleted();
        int totalJobsSubmitted = metrics.getTotalJobsSubmitted();
        
        // Display metrics
        System.out.println("Total number of jobs submitted: " + totalJobsSubmitted);
        // System.out.println("Total jobs completed: " + totalJobsCompleted);
        // Format times from milliseconds to seconds with 2 decimal places
        System.out.printf("Average Turnaround Time:    %.2f seconds\n", avgTurnaroundTime / 1000.0);
        System.out.printf("Average CPU Time:           %.2f seconds\n", avgCpuTime / 1000.0);
        System.out.printf("Average Waiting Time:       %.2f seconds\n", avgWaitingTime / 1000.0);
        System.out.printf("Throughput:                 %.3f No./second\n", throughput);
        
        // Format elapsed time
        long uptime = metrics.getSystemUptime();
        long seconds = uptime / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        
        seconds %= 60;
        minutes %= 60;
        
        // System.out.printf("System Uptime: %02d:%02d:%02d (HH:MM:SS)\n", hours, minutes, seconds);
        
        // Show currently active policy
        // System.out.println("Final Scheduling Policy: " + scheduler.getPolicy());
        
        // System.out.println("==================================================\n");
        
        // Log performance data
        if (logger != null) {
            String perfData = String.format(
                "PERF_METRICS: TurnTime=%.2fs, WaitTime=%.2fs, CPUTime=%.2fs, Throughput=%.2f jobs/s, Completed=%d, Total=%d",
                avgTurnaroundTime / 1000.0,
                avgWaitingTime / 1000.0,
                avgCpuTime / 1000.0,
                throughput,
                totalJobsCompleted,
                totalJobsSubmitted
            );
            logger.info(perfData);
        }
    }

    /**
     * Prints help information for available commands.
     * 
     * @param command The specific command to show help for, or null for general help
     */
    private void printHelp(String command) {
        if (command == null) {
            // Show general help if no specific command is requested
            System.out.println(helpTexts.get("general"));
        } else {
            // Remove any leading dash if present (e.g., "-test" -> "test")
            if (command.startsWith("-")) {
                command = command.substring(1);
            }
            
            // Look up the specific help text
            String helpText = helpTexts.get(command.toLowerCase());
            if (helpText != null) {
                System.out.println(helpText);
            } else {
                System.out.println("No help available for command: " + command);
                System.out.println("Type 'help' for a list of available commands.");
            }
        }
        
        if (logger != null) {
            logger.info("Help command executed" + (command != null ? " for " + command : ""));
        }
    }

    /**
     * Submits a job to the queue.
     *
     * @param parts The command parts
     */
    private void submitJob(String[] parts) {
        // We no longer need validation here as it's done in validateCommand
        String jobName = parts[1];
        
        try {
            int cpuTime = Integer.parseInt(parts[2]);
            int priority = Integer.parseInt(parts[PRIORITY_INDEX]);
            
            // Create a new job with WAITING status
            Job job = new Job(jobName, cpuTime, priority, System.currentTimeMillis(), JobStatus.WAITING);
            
            // Add the job to the queue through the SystemController
            boolean added = systemController.addJob(job, "ConsoleInterface");
            
            if (!added) {
                System.out.println("Error: Failed to add job to queue");
                return;
            }
            
            System.out.println("Job " + jobName + " added to queue.");
            // System.out.println("Job details: CPU Time = " + cpuTime + " seconds, Priority = " + priority);
            
            // Estimate waiting time based on scheduling policy
            int queueSize = jobQueueManager.getQueueSize();
            int waitEstimate = Math.min(queueSize * cpuTime, MAX_WAITING_ESTIMATE);
            // out Total number of jobs in the queue: <number>
            System.out.println("Total number of jobs in the queue: " + queueSize);
            System.out.println("Expected waiting time: " + (waitEstimate-cpuTime) + " seconds");
            // out Scheduling Policy: FCFS
            System.out.println("Scheduling Policy: " + scheduler.getPolicyName()+".");
            
            if (loggingSystem != null) {
                loggingSystem.logJobDetails(job, 0, 0, 0);
                loggingSystem.updateJobQueueView(job);
            }
            
            // Ensure the dispatcher is running
            ensureDispatcherRunning();
            
        } catch (NumberFormatException e) {
            System.out.println("Error: CPU time and priority must be integers");
            logger.error("Invalid job parameters: CPU time and priority must be integers");
        }
    }

    /**
     * Lists all jobs in the queue.
     */
    private void listJobs() {
        // Get jobs from the JobQueueManager through SystemController
        List<Job> jobs = systemController.listJobs();
        
        // Display total job count and current scheduling policy
        // Even if queue is empty, check if there's a running job
        Job runningJob = null;
        if (jobQueueManager != null && jobQueueManager.getUnderlyingQueue() != null) {
            runningJob = jobQueueManager.getUnderlyingQueue().getRunningJob();
        }
        System.out.println("Total number of jobs in the queue: " + (runningJob != null ? jobs.size() + 1 : jobs.size()));
        System.out.println("Scheduling Policy: " + scheduler.getPolicy() + ".");
        
        if (jobs.isEmpty()) {
            
            if (runningJob != null) {
                System.out.println("Queue is empty, but there is 1 job currently running.");
                System.out.printf("%-10s %-8s %-4s %-12s %-10s%n", "Name", "CPU_Time", "Pri", "Arrival_time", "Progress");
                // System.out.println("------ RUNNING JOBS ------");
                System.out.println("-------------------------");
                SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");
                String arrivalTime = timeFormat.format(new Date(runningJob.getArrivalTime()));
                
                System.out.printf("%-10s %-8d %-4d %-12s %-10s%n",
                    runningJob.getName(),
                    runningJob.getCpuTime(),
                    runningJob.getPriority(),
                    arrivalTime,
                    "Run");
                
                // System.out.println("-------------------------");
            } else {
                System.out.println("Queue is empty.");
            }
        } else {
            // Define column widths for better alignment
            System.out.printf("%-10s %-8s %-4s %-12s %-10s%n", "Name", "CPU_Time", "Pri", "Arrival_time", "Progress");
            System.out.println("-------------------------");
            // Format and print each job - first sort so running jobs appear at top
            SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");
            
            // First, find and print any running jobs
            List<Job> runningJobs = jobs.stream()
                .filter(job -> job.getJobStatus() == JobStatus.RUNNING)
                .collect(Collectors.toList());
            
            // Check for the tracked running job that might be missing from the queue
            Job trackedRunningJob = null;
            if (jobQueueManager != null && jobQueueManager.getUnderlyingQueue() != null) {
                trackedRunningJob = jobQueueManager.getUnderlyingQueue().getRunningJob();
                
                // If the running job is not already in our list, add it
                if (trackedRunningJob != null && 
                    trackedRunningJob.getJobStatus() == JobStatus.RUNNING) {
                    final Job finalTrackedRunningJob = trackedRunningJob;
                    if (runningJobs.stream().noneMatch(j -> j.getName().equals(finalTrackedRunningJob.getName()))) {
                        runningJobs.add(finalTrackedRunningJob);
                    }
                }
            }
                
            if (!runningJobs.isEmpty()) {
                // System.out.println("------ RUNNING JOBS ------");
                printJobList(runningJobs, timeFormat);
                // System.out.println("-------------------------");
            }
            
            // Then print all other jobs
            List<Job> otherJobs = jobs.stream()
                .filter(job -> job.getJobStatus() != JobStatus.RUNNING)
                .collect(Collectors.toList());
                
            if (!otherJobs.isEmpty()) {
                if (!runningJobs.isEmpty()) {
                    // System.out.println("------ QUEUED JOBS ------");
                }
                printJobList(otherJobs, timeFormat);
            }
        }
        
        logger.info("List command executed");
    }
    
    /**
     * Helper method to print a list of jobs with formatting
     * 
     * @param jobList The list of jobs to print
     * @param timeFormat The date format to use for arrival time
     */
    private void printJobList(List<Job> jobList, SimpleDateFormat timeFormat) {
        for (Job job : jobList) {
            String arrivalTime = timeFormat.format(new Date(job.getArrivalTime()));
            String progress = "";
            
            // Determine job progress based on status
            if (job.getJobStatus() == JobStatus.RUNNING) {
                progress = "Run";
            } else if (job.getJobStatus() == JobStatus.COMPLETED) {
                progress = "Done";
            } else if (job.getJobStatus() == JobStatus.SELECTED) {
                progress = "Selected";
            } else if (job.getJobStatus() == JobStatus.WAITING) {
                progress = "Waiting";
            } else if (job.getJobStatus() == JobStatus.INTERRUPTED) {
                progress = "Interrupted";
            } else if (job.getJobStatus() == JobStatus.CANCELED) {
                progress = "Canceled";
            }
            
            // Print job details in the required format
            System.out.printf("%-10s %-8d %-4d %-12s %-10s%n",
                    job.getName(),
                    job.getCpuTime(),
                    job.getPriority(),
                    arrivalTime,
                    progress);
        }
    }
    
    /**
     * Sets the scheduling policy.
     * 
     * @param policy The policy to set
     */
    private void setPolicy(Scheduler.Policy policy) {
        scheduler.setPolicy(policy);
        int queueSize = jobQueueManager.getQueueSize();
        String message = "Scheduling policy is switched to " + policy+".";
        if (queueSize > 0) {
            message += " All the " + queueSize + " waiting jobs have been rescheduled.";
        }else{
            message += " No waiting jobs.";
        }
        System.out.println(message);
        logger.info(message);
    }
    
    /**
     * Ensures the dispatcher thread is running.
     */
    private void ensureDispatcherRunning() {
        if (dispatcherThread == null || !dispatcherThread.isAlive()) {
            dispatcherThread = new Thread(dispatcher);
            dispatcherThread.setDaemon(true);
            dispatcherThread.start();
            // System.out.println("Dispatcher started.");
            logger.info("Dispatcher thread started");
        }
    }
    
    /**
     * Runs automated tests.
     */
    private void runTests(String[] parts) {
        logger.info("Test command executed with benchmark: " + parts[1]);
        
        // Set the scheduling policy
        Scheduler.Policy policy = Scheduler.Policy.valueOf(parts[2].toUpperCase());
        scheduler.setPolicy(policy);
        // System.out.println("  - " + policy + " policy set: SUCCESS");

        // get current job metrics map
        ConcurrentMap<String, PerformanceMetrics.JobMetrics> mainJobMetricsMap = systemController.getPerformanceMetrics().getJobMetricsMap();

        // reset the job metrics map
        systemController.getPerformanceMetrics().reset();
        
        // Generate and submit jobs
        int numOfJobs = Integer.parseInt(parts[3]);
        int priorityLevels = Integer.parseInt(parts[4]);
        int minCpuTime = Integer.parseInt(parts[5]);
        int maxCpuTime = Integer.parseInt(parts[6]);
        
        for (int i = 0; i < numOfJobs; i++) {
            String jobName = parts[1] + "_Job" + (i + 1);
            int cpuTime = minCpuTime + (int)(Math.random() * ((maxCpuTime - minCpuTime) + 1));
            int priority = (int)(Math.random() * priorityLevels);
            
            Job job = new Job(jobName, cpuTime, priority, System.currentTimeMillis(), JobStatus.WAITING);
            boolean added = systemController.addJob(job, "ConsoleInterface");
            
            if (!added) {
                logger.error("  - Job submission failed for " + jobName);
                continue;
            }
            
            logger.info("  - Job " + jobName + " submission: SUCCESS");
        }
        
        // Ensure the dispatcher is running
        ensureDispatcherRunning();
        logger.info("  - Dispatcher startup: SUCCESS");
        
        // wait for jobs to complete
        while (jobQueueManager.getQueueSize() > 0) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                logger.error("Test interrupted: " + e.getMessage());    
            }
        }
        
        // Measure performance metrics
        displayPerformanceMetrics();

        // reset the job metrics map
        systemController.getPerformanceMetrics().reset();

        // update the job metrics map
        systemController.getPerformanceMetrics().updateJobMetricsMap(mainJobMetricsMap);
        
        logger.info("All tests completed successfully!");
    }
    
    /**
     * Shows the version information of the system.
     */
    private void showVersion() {
        System.out.println("CSUbatch Job Scheduling System");
        System.out.println("Version: " + VERSION);
        System.out.println("Build date: " + BUILD_DATE);
        System.out.println("Developed by: Group 7 students");
        
        logger.info("Version command executed");
    }
    
    /**
     * Shows the current system status.
     */
    private void showSystemStatus() {
        System.out.println("CSUbatch System Status");
        System.out.println("----------------------");
        System.out.println("Current scheduling policy: " + scheduler.getPolicy());
        System.out.println("Jobs in queue: " + jobQueueManager.getQueueSize());
        System.out.println("Dispatcher running: " + (dispatcherThread != null && dispatcherThread.isAlive()));
        
        if (loggingSystem != null) {
            System.out.println("Log file: " + loggingSystem.getCurrentLogFile());
            System.out.println("Lock log file: " + loggingSystem.getCurrentLockLogFile());
            System.out.println("Lock monitoring: Active (logs every second)");
        }
        
        // Show thread information
        try {
            ThreadGroup rootGroup = Thread.currentThread().getThreadGroup();
            while (rootGroup.getParent() != null) {
                rootGroup = rootGroup.getParent();
            }
            
            // Get estimate of active threads
            int estimatedCount = rootGroup.activeCount();
            Thread[] threads = new Thread[estimatedCount * 2]; // Double size to ensure we get all
            int actualCount = rootGroup.enumerate(threads, true);
            
            System.out.println("\nActive threads (" + actualCount + "):");
            System.out.println("------------------------------");
            for (int i = 0; i < actualCount; i++) {
                Thread t = threads[i];
                if (t.getName().contains("JobQueue") || t.getName().contains("Scheduler") || 
                    t.getName().contains("Dispatcher")) {
                    System.out.println(t.getName() + " (ID: " + t.getId() + ", State: " + t.getState() + ")");
                }
            }
        } catch (Exception e) {
            System.out.println("Error getting thread information: " + e.getMessage());
        }
        
        logger.info("Status command executed");
    }
    
    /**
     * Checks the current state of locks in the system.
     * This is helpful for debugging lock issues.
     */
    private void checkLocks() {
        System.out.println("Checking lock status in the system...");
        
        JobQueueManager queueManager = systemController.getJobQueueManager();
        if (queueManager == null) {
            System.out.println("Error: JobQueueManager not available");
            return;
        }
        
        // Force a check of lock state using a dummy job name
        queueManager.checkLockStateAfterJobCompletion("MANUAL_CHECK", "MANUAL");
        
        System.out.println("Lock check completed. Check logs for details.");
        logger.info("Manual lock check performed");
    }
    
    /**
     * Handles job state change events.
     * This method is called whenever a job's state changes.
     *
     * @param event The event containing information about the state change
     */
    @Override
    public void onJobStateChanged(JobStateManager.JobStateEvent event) {
        // Update the UI with the new job state
        Job job = event.getJob();
        JobStatus newStatus = event.getNewStatus();
        
        // For now, we'll just log that we received an event
        // In a more sophisticated UI, we could update UI elements here
        if (logger != null) {
            logger.infoJob(job, "ConsoleInterface notified of job state change to " + newStatus);
        }
        
        // Update the logging system view if available
        if (loggingSystem != null) {
            loggingSystem.updateJobQueueView(job);
        }
        
        // Check for lock issues on terminal job states (completed, interrupted, canceled)
        boolean isTerminalState = (newStatus == JobStatus.COMPLETED || 
                                 newStatus == JobStatus.INTERRUPTED || 
                                 newStatus == JobStatus.CANCELED);
        
        if (isTerminalState) {
            // Log terminal state transition
            logger.infoJob(job, "Notification: Job '" + job.getName() + 
                              "' state changed to " + newStatus + 
                              " by " + event.getSource());
            
            // After a brief delay, verify that locks are properly released
            new Thread(() -> {
                try {
                    // Wait a bit to allow any lock releases to complete
                    Thread.sleep(200);
                    
                    // Get the JobQueueManager through the systemController
                    JobQueueManager queueManager = systemController.getJobQueueManager();
                    if (queueManager != null) {
                        // Check if any locks are still held
                        queueManager.checkLockStateAfterJobCompletion(
                            job.getName(), newStatus.getDisplayName());
                    }
                } catch (InterruptedException e) {
                    // Ignore
                } catch (Exception e) {
                    logger.error("Error checking lock state: " + e.getMessage());
                }
            }, "LockChecker-" + job.getName()).start();
        }
    }

    /**
     * Loads jobs from a file.
     *
     * @param parts The command parts
     */
    private void loadJobFile(String[] parts) {
        // No need to check parts.length since it's validated in validateCommand
        String fileName = parts[1];
        
        // Allow for files in the jobloads directory without specifying the full path
        File file = new File(fileName);
        if (!file.exists()) {
            // Try looking in the 'jobloads' directory
            file = new File(Paths.get("jobloads", fileName).toString());
            
            // If still not found, try adding .txt extension
            if (!file.exists() && !fileName.endsWith(".txt")) {
                file = new File(Paths.get("jobloads", fileName + ".txt").toString());
            }
        }
        
        if (!file.exists()) {
            System.out.println("Error: File not found: " + fileName);
            System.out.println("Available job load files:");
            
            // List available job files
            File jobloadsDir = new File("jobloads");
            if (jobloadsDir.exists() && jobloadsDir.isDirectory()) {
                File[] jobFiles = jobloadsDir.listFiles((dir, name) -> name.endsWith(".txt"));
                if (jobFiles != null && jobFiles.length > 0) {
                    for (File jobFile : jobFiles) {
                        System.out.println("  - " + jobFile.getName());
                    }
                } else {
                    System.out.println("  No job files found in 'jobloads' directory");
                }
            }
            
            logger.warning("Job file loading failed: File not found: " + fileName);
            return;
        }
        
        try {
            System.out.println("Loading jobs from file: " + file.getPath());
            logger.info("Loading jobs from file: " + file.getPath());
            
            BufferedReader reader = new BufferedReader(new FileReader(file));
            String line;
            int lineNumber = 0;
            int jobsAdded = 0;
            int jobsSkipped = 0;
            
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                
                // Skip empty lines and comments
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                
                String[] jobParts = line.split("\\s+");
                if (jobParts.length < 3) {
                    System.out.println("Warning: Line " + lineNumber + " has invalid format, skipping");
                    jobsSkipped++;
                    continue;
                }
                
                try {
                    String jobName = jobParts[0];
                    int cpuTime = Integer.parseInt(jobParts[1]);
                    int priority = Integer.parseInt(jobParts[2]);
                    
                    if (cpuTime <= 0) {
                        System.out.println("Warning: Line " + lineNumber + ": CPU time must be positive, skipping job " + jobName);
                        jobsSkipped++;
                        continue;
                    }
                    
                    if (priority < 0) {
                        System.out.println("Warning: Line " + lineNumber + ": Priority must be non-negative, skipping job " + jobName);
                        jobsSkipped++;
                        continue;
                    }
                    
                    // Create a new job with WAITING status
                    Job job = new Job(jobName, cpuTime, priority, System.currentTimeMillis(), JobStatus.WAITING);
                    
                    // Add the job to the queue through the SystemController
                    boolean added = systemController.addJob(job, "ConsoleInterface-LoadFile");
                    
                    if (!added) {
                        System.out.println("Warning: Line " + lineNumber + ": Failed to add job " + jobName + " to queue");
                        jobsSkipped++;
                        continue;
                    }
                    
                    jobsAdded++;
                    
                    // Update logging system if available
                    if (loggingSystem != null) {
                        loggingSystem.logJobDetails(job, 0, 0, 0);
                        loggingSystem.updateJobQueueView(job);
                    }
                } catch (NumberFormatException e) {
                    System.out.println("Warning: Line " + lineNumber + ": Invalid number format, skipping");
                    jobsSkipped++;
                }
            }
            
            reader.close();
            
            // Summary of jobs loaded
            System.out.println("Job loading complete: " + jobsAdded + " jobs added, " + jobsSkipped + " jobs skipped");
            logger.info("Job loading complete: " + jobsAdded + " jobs added, " + jobsSkipped + " jobs skipped from " + file.getPath());
            
            // Ensure the dispatcher is running if we loaded jobs
            if (jobsAdded > 0) {
                ensureDispatcherRunning();
            }
            
        } catch (IOException e) {
            System.out.println("Error reading file: " + e.getMessage());
            logger.error("Job file loading failed: " + e.getMessage());
        }
    }
} 