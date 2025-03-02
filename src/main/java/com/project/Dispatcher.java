package com.project;

/**
 * Dispatcher class responsible for executing jobs from the scheduler.
 * Implements the Runnable interface to operate in its own thread.
 */
public class Dispatcher implements Runnable {
    private static final long MILLISECONDS_PER_SECOND = 1000L;
    private final Scheduler scheduler;
    private LoggingSystem loggingSystem;

    /**
     * Constructs a new Dispatcher with the specified scheduler.
     *
     * @param scheduler The scheduler that provides jobs to be executed
     * @throws IllegalArgumentException if scheduler is null
     */
    public Dispatcher(Scheduler scheduler) {
        if (scheduler == null) {
            throw new IllegalArgumentException("Scheduler cannot be null");
        }
        this.scheduler = scheduler;
    }

    /**
     * Sets the logging system for this dispatcher.
     *
     * @param loggingSystem The logging system to use
     */
    public void setLoggingSystem(LoggingSystem loggingSystem) {
        this.loggingSystem = loggingSystem;
    }

    @Override
    public void run() {
        if (loggingSystem != null) {
            loggingSystem.logTransaction("Dispatcher thread started execution");
        }

        while (true) {
            try {
                // Wait for jobs to be available in the queue
                Job job = scheduler.getNextJob();
                if (job != null) {
                    if (loggingSystem != null) {
                        loggingSystem.logTransaction("Starting execution of job: " + job.getName());
                        loggingSystem.logJobTransaction("Starting execution of job: " + job.getName());
                    }

                    long startTime = System.currentTimeMillis();
                    executeJob(job);
                    long endTime = System.currentTimeMillis();

                    // Measure performance metrics
                    long turnaroundTime = endTime - job.getArrivalTime();
                    long waitingTime = startTime - job.getArrivalTime();

                    System.out.println("Job " + job.getName() + " completed. Turnaround Time: "
                            + turnaroundTime + " ms, Waiting Time: " + waitingTime + " ms");

                    if (loggingSystem != null) {
                        loggingSystem.logTransaction("Job completed: " + job.getName()
                                + ", Turnaround Time: " + turnaroundTime
                                + " ms, Waiting Time: " + waitingTime + " ms");

                        // Log detailed job information to the job log file
                        loggingSystem.logJobDetails(job, endTime, waitingTime, turnaroundTime);
                        loggingSystem.logJobTransaction("Job completed: " + job.getName()
                                + ", Turnaround Time: " + turnaroundTime
                                + " ms, Waiting Time: " + waitingTime + " ms");
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                if (loggingSystem != null) {
                    loggingSystem.logTransaction("Dispatcher thread interrupted: " + e.getMessage());
                    loggingSystem.logJobTransaction("Dispatcher thread interrupted: " + e.getMessage());
                }
                break;
            } catch (IllegalStateException e) {
                System.err.println("Illegal state: " + e.getMessage());
                if (loggingSystem != null) {
                    loggingSystem.logTransaction("Dispatcher error - Illegal state: " + e.getMessage());
                    loggingSystem.logJobTransaction("Dispatcher error - Illegal state: " + e.getMessage());
                }
                e.printStackTrace();
            } catch (IllegalArgumentException e) {
                System.err.println("Invalid argument: " + e.getMessage());
                if (loggingSystem != null) {
                    loggingSystem.logTransaction("Dispatcher error - Invalid argument: " + e.getMessage());
                    loggingSystem.logJobTransaction("Dispatcher error - Invalid argument: " + e.getMessage());
                }
                e.printStackTrace();
            }
        }
    }

    /**
     * Executes a job by simulating CPU time.
     *
     * @param job The job to execute
     */
    private void executeJob(Job job) {
        // Simulate job execution
        // System.out.println("Executing job: " + job.getName());

        // Update job status to "Running"
        job.setStatus("Running");

        // Update the job queue view with the status change to "Running"
        if (loggingSystem != null) {
            loggingSystem.updateJobQueueView(job);
            loggingSystem.logTransaction("Executing job: " + job.getName()
                    + ", CPU Time: " + job.getCpuTime() + " seconds");
            loggingSystem.logJobTransaction("Executing job: " + job.getName()
                    + ", CPU Time: " + job.getCpuTime() + " seconds");
        }

        try {
            // Simulate CPU time
            Thread.sleep(job.getCpuTime() * MILLISECONDS_PER_SECOND);
            // Update job status
            job.setStatus("Completed");

            // Update the job queue view with the status change to "Completed"
            if (loggingSystem != null) {
                loggingSystem.updateJobQueueView(job);
                loggingSystem.logTransaction("Job execution completed: " + job.getName());
                loggingSystem.logJobTransaction("Job execution completed: " + job.getName());
            }

            // Log transaction
            System.out.println("Job completed: " + job.getName());
        } catch (InterruptedException e) {
            // Update job status to "Interrupted"
            job.setStatus("Interrupted");

            // Update the job queue view with the status change to "Interrupted"
            if (loggingSystem != null) {
                loggingSystem.updateJobQueueView(job);
                loggingSystem.logTransaction("Job execution interrupted: " + job.getName());
                loggingSystem.logJobTransaction("Job execution interrupted: " + job.getName());
            }

            Thread.currentThread().interrupt();
        }
    }
}
