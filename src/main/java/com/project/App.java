package com.project;

/**
 * Entry point for the CSUbatch Scheduling System.
 * Initializes the job queue, scheduler, dispatcher, and command-line interface.
 */
public final class App {

    /**
     * Private constructor to prevent instantiation of utility class.
     */
    private App() {
        // Utility class should not be instantiated
    }

    /**
     * Main method to start the application.
     *
     * @param args Command line arguments (not used)
     */
    public static void main(String[] args) {
        try {
            // Initialize the logging system
            LoggingSystem loggingSystem = new LoggingSystem();
            System.out.println("Logging system initialized.");
            System.out.println("System log file: " + loggingSystem.getCurrentLogFile());
            System.out.println("Job log file: " + loggingSystem.getCurrentJobLogFile());

            // Initialize the job queue
            JobQueue jobQueue = new JobQueue();
            loggingSystem.logTransaction("Job queue initialized");
            loggingSystem.logJobTransaction("Job queue initialized");

            // Initialize the scheduler with the job queue
            Scheduler scheduler = new Scheduler(jobQueue);
            scheduler.setLoggingSystem(loggingSystem);
            loggingSystem.logTransaction("Scheduler initialized with policy: " + scheduler.getCurrentPolicy());
            loggingSystem.logJobTransaction("Scheduler initialized with policy: " + scheduler.getCurrentPolicy());

            // Initialize the dispatcher with the scheduler
            Dispatcher dispatcher = new Dispatcher(scheduler);
            dispatcher.setLoggingSystem(loggingSystem);
            loggingSystem.logTransaction("Dispatcher initialized");
            loggingSystem.logJobTransaction("Dispatcher initialized");

            // Start the dispatcher thread
            Thread dispatcherThread = new Thread(dispatcher);
            dispatcherThread.setDaemon(true); // Set as daemon so it doesn't prevent JVM shutdown
            dispatcherThread.start();
            System.out.println("Dispatcher started successfully.");
            loggingSystem.logTransaction("Dispatcher thread started");
            loggingSystem.logJobTransaction("Dispatcher thread started");

            // Initialize the command-line interface with the scheduler and dispatcher
            CommandLineInterface cli = new CommandLineInterface(scheduler, dispatcher, loggingSystem);
            loggingSystem.logTransaction("Command-line interface initialized");
            loggingSystem.logJobTransaction("Command-line interface initialized");

            // Start the command-line interface
            loggingSystem.logTransaction("Starting command-line interface");
            loggingSystem.logJobTransaction("Starting command-line interface");
            cli.start();

            // This point is reached when the CLI exits
            loggingSystem.logTransaction("CSUbatch system shutting down");
            loggingSystem.logJobTransaction("CSUbatch system shutting down");
        } catch (IllegalArgumentException e) {
            // Handle specific exceptions
            System.err.println("Invalid argument: " + e.getMessage());
            e.printStackTrace();
        } catch (IllegalStateException e) {
            // Handle illegal state exceptions
            System.err.println("Illegal state: " + e.getMessage());
            e.printStackTrace();
        }
    }
}

