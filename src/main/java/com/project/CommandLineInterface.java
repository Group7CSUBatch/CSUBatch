package com.project;

import java.util.List;
import java.util.Scanner;
import java.util.stream.Collectors;

/**
 * CommandLineInterface class responsible for handling user commands and interactions.
 * Provides methods for executing commands, listing jobs, and running tests.
 */
public class CommandLineInterface {
    private static final int MIN_RUN_ARGS = 3;
    private final Scheduler scheduler;
    private final Dispatcher dispatcher;
    private final LoggingSystem loggingSystem;
    private Thread dispatcherThread;

    /**
     * Constructs a new CommandLineInterface with the specified scheduler and dispatcher.
     *
     * @param scheduler The scheduler to be used for job scheduling
     * @param dispatcher The dispatcher to be used for job execution
     * @param loggingSystem The logging system to be used for logging events
     */
    public CommandLineInterface(Scheduler scheduler, Dispatcher dispatcher, LoggingSystem loggingSystem) {
        this.scheduler = scheduler;
        this.dispatcher = dispatcher;
        this.loggingSystem = loggingSystem;
    }

    /**
     * Starts the command-line interface and processes user commands.
     */
    public void start() {
        Scanner scanner = new Scanner(System.in);
        String command;
        System.out.println("Welcome to Dr. Zhou's batch job scheduler Version 1.0 by Group X students");
        System.out.println("Type 'help' to find more about CSUbatch commands.");
        loggingSystem.logTransaction("Command-line interface started");
        loggingSystem.logJobTransaction("Command-line interface started");

        while (true) {
            System.out.print("> ");
            command = scanner.nextLine().trim();
            if ("quit".equalsIgnoreCase(command)) {
                loggingSystem.logTransaction("User requested to quit");
                loggingSystem.logJobTransaction("User requested to quit");
                break;
            }
            loggingSystem.logTransaction("Command received: " + command);
            executeCommand(command);
        }
        scanner.close();
        System.out.println("CSUbatch system is shutting down...");
        loggingSystem.logTransaction("Command-line interface terminated");
        loggingSystem.logJobTransaction("Command-line interface terminated");
    }

    /**
     * Executes a command based on the user input.
     *
     * @param command The command to execute
     */
    private void executeCommand(String command) {
        String[] parts = command.split(" ");
        switch (parts[0].toLowerCase()) {
            case "help":
                printHelp();
                break;
            case "run":
                if (parts.length < MIN_RUN_ARGS) {
                    System.out.println("Usage: run <job_name> <cpu_time> <priority>");
                    loggingSystem.logTransaction("Invalid run command: insufficient arguments");
                    loggingSystem.logJobTransaction("Invalid run command: insufficient arguments");
                } else {
                    String jobName = parts[1];
                    int cpuTime = Integer.parseInt(parts[2]);
                    int priority = Integer.parseInt(parts[MIN_RUN_ARGS]);
                    Job job = new Job(jobName, cpuTime, priority, System.currentTimeMillis(), "Waiting");
                    scheduler.getJobQueue().addJob(job);
                    System.out.println("Job " + jobName + " was submitted.");
                    System.out.println("Total number of jobs in the queue: " + scheduler.getJobQueue().size());
                    // Estimate waiting time (placeholder)
                    System.out.println("Expected waiting time: "
                            + (cpuTime * scheduler.getJobQueue().size()) + " seconds");
                    System.out.println("Scheduling Policy: " + scheduler.getCurrentPolicy());
                    loggingSystem.logTransaction("Job submitted: " + jobName + ", CPU Time: " + cpuTime
                            + ", Priority: " + priority);

                    // Log job submission to the job log file
                    loggingSystem.logJobTransaction("Job submitted: " + jobName + ", CPU Time: " + cpuTime
                            + ", Priority: " + priority);
                    loggingSystem.logJobDetails(job, 0, 0, 0);

                    // Update the job queue view file with the new job
                    loggingSystem.updateJobQueueView(job);
                }
                break;
            case "list":
                listJobs();
                break;
            case "fcfs":
                scheduler.setPolicy(Scheduler.SchedulingPolicy.FCFS);
                loggingSystem.logTransaction("Scheduling policy changed to FCFS");
                loggingSystem.logJobTransaction("Scheduling policy changed to FCFS");
                break;
            case "sjf":
                scheduler.setPolicy(Scheduler.SchedulingPolicy.SJF);
                loggingSystem.logTransaction("Scheduling policy changed to SJF");
                loggingSystem.logJobTransaction("Scheduling policy changed to SJF");
                break;
            case "priority":
                scheduler.setPolicy(Scheduler.SchedulingPolicy.PRIORITY);
                loggingSystem.logTransaction("Scheduling policy changed to PRIORITY");
                loggingSystem.logJobTransaction("Scheduling policy changed to PRIORITY");
                break;
            case "test":
                runTests();
                break;
            case "quit":
                System.out.println("Exiting CSUbatch...");
                loggingSystem.logTransaction("System exit requested");
                loggingSystem.logJobTransaction("System exit requested");
                // Display performance statistics (placeholder)
                System.exit(0);
                break;
            default:
                System.out.println("Unknown command. Type 'help' for a list of commands.");
                loggingSystem.logTransaction("Unknown command: " + parts[0]);
                loggingSystem.logJobTransaction("Unknown command: " + parts[0]);
        }
    }

    /**
     * Prints the help message with available commands.
     */
    private void printHelp() {
        System.out.println("Available commands:");
        System.out.println("help - Show this help message");
        System.out.println("run <job_name> <cpu_time> <priority> - Submit a job with the given parameters");
        System.out.println("list - List all jobs in the queue");
        System.out.println("fcfs - Set scheduling policy to First Come First Served");
        System.out.println("sjf - Set scheduling policy to Shortest Job First");
        System.out.println("priority - Set scheduling policy to Priority");
        System.out.println("test - Run automated tests");
        System.out.println("quit - Exit the system");
        loggingSystem.logTransaction("Help command executed");
        loggingSystem.logJobTransaction("Help command executed");
    }

    /**
     * Starts the dispatcher thread to execute jobs if not already running.
     * This method is kept for backward compatibility but may not be needed
     * if the dispatcher is already started in the App class.
     */
    private void runJobs() {
        // Check if dispatcher thread is already running
        if (dispatcherThread == null || !dispatcherThread.isAlive()) {
            dispatcherThread = new Thread(dispatcher);
            dispatcherThread.setDaemon(true);
            dispatcherThread.start();
            System.out.println("Jobs are being executed...");
            loggingSystem.logTransaction("Dispatcher thread started from CLI");
        } else {
            System.out.println("Dispatcher is already running.");
            loggingSystem.logTransaction("Dispatcher thread already running");
        }
    }

    /**
     * Lists all jobs in the queue with their details.
     */
    private void listJobs() {
        // List all jobs in the queue
        System.out.println("Listing all jobs in the queue:");
        loggingSystem.logTransaction("List command executed");
        loggingSystem.logJobTransaction("List command executed");

        synchronized (scheduler.getJobQueue()) {
            List<Job> jobs = scheduler.getJobQueue().stream().collect(Collectors.toList());
            if (jobs.isEmpty()) {
                System.out.println("No jobs in the queue.");
                loggingSystem.logQueueStatus("Queue is empty");
                loggingSystem.logJobQueueUpdate("Queue is empty");
            } else {
                loggingSystem.logQueueStatus("Queue contains " + jobs.size() + " jobs");
                loggingSystem.logJobQueueUpdate("Queue contains " + jobs.size() + " jobs");
                for (Job job : jobs) {
                    System.out.println("Job: " + job.getName()
                            + ", CPU Time: " + job.getCpuTime()
                            + ", Priority: " + job.getPriority()
                            + ", Status: " + job.getStatus());
                    loggingSystem.logQueueStatus("Job: " + job.getName()
                            + ", CPU Time: " + job.getCpuTime()
                            + ", Priority: " + job.getPriority()
                            + ", Status: " + job.getStatus());
                    loggingSystem.logJobQueueUpdate("Job: " + job.getName()
                            + ", CPU Time: " + job.getCpuTime()
                            + ", Priority: " + job.getPriority()
                            + ", Status: " + job.getStatus());
                }
            }
        }
    }

    /**
     * Runs automated tests for the scheduling system.
     */
    private void runTests() {
        // Placeholder for running automated tests
        System.out.println("Running automated tests...");
        loggingSystem.logTransaction("Test command executed");
        // Implement test logic here
    }
}
