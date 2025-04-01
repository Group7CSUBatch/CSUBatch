package com.project;

import com.project.core.JobQueue;
import com.project.scheduler.Scheduler;
import com.project.scheduler.Dispatcher;
import com.project.logging.LoggingSystem;
import com.project.logging.Logger;
import com.project.core.Job;
import com.project.core.JobStatus;
import com.project.management.JobQueueManager;
import com.project.management.JobStateManager;
import com.project.management.SystemController;
/**
 * A simple driver program to run tests for the CSUbatch system.
 * Utility class for running automated tests on the scheduler system.
 */
final class TestRunner {
        // Constants for test job parameters
        private static final int CPU_TIME_JOB1 = 5;
        private static final int PRIORITY_JOB1 = 3;
        private static final int CPU_TIME_JOB2 = 3;
        private static final int PRIORITY_JOB2 = 2;
        private static final int CPU_TIME_JOB3 = 7;
        private static final int PRIORITY_JOB3 = 1;
        private static final int CPU_TIME_SLICE = 2;
        private static final int TEST_WAIT_TIME = 20000; // 20 seconds

        /**
         * Private constructor to prevent instantiation of utility class.
         */
        private TestRunner() {
                // Private constructor to prevent instantiation
        }

        /**
         * Main method that sets up the CSUbatch system components and runs automated
         * tests.
         *
         * @param args Command line arguments (not used)
         */
        public static void main(String[] args) {
                // Set up components
                JobQueue jobQueue = new JobQueue();
                LoggingSystem loggingSystem = new LoggingSystem();

                // Create loggers for each component
                Logger schedulerLogger = new Logger("TestScheduler", loggingSystem);
                Logger dispatcherLogger = new Logger("TestDispatcher", loggingSystem);
                Logger jobStateLogger = new Logger("TestJobState", loggingSystem);

                // Create managers
                JobStateManager jobStateManager = new JobStateManager(jobStateLogger);
                JobQueueManager jobQueueManager = new JobQueueManager(jobQueue, jobStateManager, jobStateLogger);

                // Connect managers
                jobStateManager.setJobQueueManager(jobQueueManager);

                // Create scheduler and dispatcher
                Scheduler scheduler = new Scheduler(jobQueueManager, jobStateManager, schedulerLogger);
                Dispatcher dispatcher = new Dispatcher(scheduler);

                // Configure dispatcher
                dispatcher.setLogger(dispatcherLogger);
                dispatcher.setJobStateManager(jobStateManager);
                dispatcher.setJobQueueManager(jobQueueManager);

                // Run our own tests
                runAutomatedTests(jobQueueManager, scheduler, dispatcher, loggingSystem);
        }

        /**
         * Runs automated tests for the scheduling system.
         *
         * @param jobQueueManager The job queue manager for storing jobs
         * @param scheduler       The scheduler for job scheduling
         * @param dispatcher      The dispatcher for job execution
         * @param loggingSystem   The logging system for recording test events
         */
        private static void runAutomatedTests(JobQueueManager jobQueueManager, Scheduler scheduler,
                        Dispatcher dispatcher, LoggingSystem loggingSystem) {
                loggingSystem.logTransaction("Running automated tests...");
                loggingSystem.logTransaction("Test execution started");

                // Add test implementation here
                // For example, submitting test jobs and measuring performance

                // Set CPU time slice for dispatcher
                dispatcher.setCpuTimeSlice(CPU_TIME_SLICE);

                // Submit test jobs
                Job job1 = new Job("TestJob1", CPU_TIME_JOB1, PRIORITY_JOB1, System.currentTimeMillis(), JobStatus.WAITING);
                Job job2 = new Job("TestJob2", CPU_TIME_JOB2, PRIORITY_JOB2, System.currentTimeMillis(), JobStatus.WAITING);
                Job job3 = new Job("TestJob3", CPU_TIME_JOB3, PRIORITY_JOB3, System.currentTimeMillis(), JobStatus.WAITING);

                SystemController.getInstance().addJob(job1, "TestRunner");
                SystemController.getInstance().addJob(job2, "TestRunner");
                SystemController.getInstance().addJob(job3, "TestRunner");

                loggingSystem.logTransaction("Test jobs submitted. Starting dispatcher...");

                // Start scheduler
                scheduler.start();

                // Start dispatcher in a new thread
                Thread dispatcherThread = new Thread(dispatcher);
                dispatcherThread.start();

                // Wait for jobs to complete
                try {
                        Thread.sleep(TEST_WAIT_TIME); // Wait up to 20 seconds
                } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                }

                // Stop scheduler and dispatcher
                scheduler.stop();
                dispatcher.stop();

                loggingSystem.logTransaction("Test execution completed");
        }
}
