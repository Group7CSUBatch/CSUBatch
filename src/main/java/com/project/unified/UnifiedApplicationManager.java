package com.project.unified;

import com.project.core.JobQueue;
import com.project.management.JobQueueManager;
import com.project.logging.Logger;
import com.project.logging.LoggingSystem;
import com.project.scheduler.Scheduler;
import com.project.management.SystemController;
import com.project.scheduler.Dispatcher;

import java.util.HashMap;
import java.util.Map;

/**
 * UnifiedApplicationManager class is a simplified version of ApplicationManager
 * that uses the new ConsoleInterface.
 * Implements the Singleton pattern.
 */
public class UnifiedApplicationManager {
    private static UnifiedApplicationManager instance;
    private JobQueue jobQueue;
    private Scheduler scheduler;
    private Dispatcher dispatcher;
    private LoggingSystem loggingSystem;
    private ConsoleInterface consoleInterface;
    private SystemController systemController;
    private JobQueueManager jobQueueManager;
    private boolean initialized;
    private Map<String, Logger> loggers = new HashMap<>();

    /**
     * Default constructor.
     */
    private UnifiedApplicationManager() {
        // Private constructor for singleton pattern
    }

    /**
     * Gets the singleton instance of UnifiedApplicationManager.
     *
     * @return The singleton instance
     */
    public static synchronized UnifiedApplicationManager getInstance() {
        if (instance == null) {
            instance = new UnifiedApplicationManager();
        }
        return instance;
    }

    /**
     * Initializes all system components.
     */
    public void initialize() {
        // Create core components
        jobQueue = new JobQueue();
        loggingSystem = new LoggingSystem();

        // Create loggers for each component
        Logger schedulerLogger = getOrCreateLogger("Scheduler");
        Logger dispatcherLogger = getOrCreateLogger("Dispatcher");
        Logger consoleLogger = getOrCreateLogger("ConsoleInterface");
        Logger systemLogger = getOrCreateLogger("System");
        Logger queueLogger = getOrCreateLogger("JobQueue");

        // check if consoleLogger or dispatcherLogger or queueLogger or systemLogger is null
        if (consoleLogger == null || dispatcherLogger == null || queueLogger == null || systemLogger == null) {
            throw new IllegalStateException("Logger initialization failed");
        }
        
        // Create the system controller with a job state manager
        systemController = SystemController.getInstance(jobQueue, null, null, systemLogger);
        
        // Get references to managers
        jobQueueManager = systemController.getJobQueueManager();
        
        // Create scheduler and dispatcher with proper managers
        scheduler = new Scheduler(jobQueueManager, systemController.getJobStateManager(), schedulerLogger);
        dispatcher = new Dispatcher(scheduler);
        dispatcher.setLogger(dispatcherLogger);
        dispatcher.setJobStateManager(systemController.getJobStateManager());
        dispatcher.setJobQueueManager(jobQueueManager);
        
        // Set the scheduler and dispatcher in the system controller
        systemController.setScheduler(scheduler);
        systemController.setDispatcher(dispatcher);

        // Create the console interface with the system controller
        consoleInterface = new ConsoleInterface(systemController);

        initialized = true;
        loggingSystem.logTransaction("System initialized with centralized job state management and job queue access");
    }

    /**
     * Gets or creates a logger for a component.
     * 
     * @param componentName The name of the component
     * @return The logger for the component
     */
    public Logger getOrCreateLogger(String componentName) {
        return loggers.computeIfAbsent(componentName, name -> new Logger(name, loggingSystem));
    }

    /**
     * Starts the system after initialization.
     *
     * @throws IllegalStateException if system is not initialized
     */
    public void startup() {
        if (!initialized) {
            throw new IllegalStateException("System needs to be initialized first");
        }

        // Start the system controller
        systemController.startSystem();
        
        // Start the scheduler thread
        Thread schedulerThread = new Thread(scheduler);
        schedulerThread.setDaemon(true);
        schedulerThread.start();
        loggingSystem.logTransaction("Scheduler thread started");

        // Start the console interface
        consoleInterface.start();
    }

    /**
     * Shuts down the system.
     */
    public void shutdown() {
        if (!initialized) {
            return; // Already shut down or not initialized
        }
        
        // Stop the system controller
        systemController.stopSystem();
        
        // Stop the scheduler and dispatcher
        scheduler.stop();
        dispatcher.stop();
        
        // Stop the console interface
        consoleInterface.stop();
        
        // Stop lock monitoring
        if (jobQueueManager != null) {
            jobQueueManager.stopMonitoring();
        }
        
        // Close the logging system
        loggingSystem.close();
        
        // Clear the job queue
        if (jobQueueManager != null) {
            jobQueueManager.clearQueue("SystemShutdown");
        } else {
            jobQueue.clear();
        }
        
        // Set status to not initialized
        initialized = false;
        
        // System.out.println("CSUbatch system has been shut down.");
        loggingSystem.logTransaction("System shutdown complete");
    }

    

    /**
     * Gets the scheduler.
     *
     * @return The scheduler
     */
    public Scheduler getScheduler() {
        return scheduler;
    }

    /**
     * Gets the dispatcher.
     *
     * @return The dispatcher
     */
    public Dispatcher getDispatcher() {
        return dispatcher;
    }

    /**
     * Gets the logging system.
     *
     * @return The logging system
     */
    public LoggingSystem getLoggingSystem() {
        return loggingSystem;
    }

    /**
     * Gets the console interface.
     *
     * @return The console interface
     */
    public ConsoleInterface getConsoleInterface() {
        return consoleInterface;
    }
    
    /**
     * Gets the system controller.
     *
     * @return The system controller
     */
    public SystemController getSystemController() {
        return systemController;
    }
    
    /**
     * Gets the job queue manager.
     *
     * @return The job queue manager
     */
    public JobQueueManager getJobQueueManager() {
        return jobQueueManager;
    }
} 