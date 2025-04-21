package com.project.management;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import com.project.logging.Logger;
import com.project.logging.LoggingSystem;


/**
 * PerformanceMetrics tracks and calculates various performance metrics for the CSUbatch system.
 * This includes turnaround time, waiting time, CPU time, and system throughput.
 */
public class PerformanceMetrics {
    // Map to store performance data for completed jobs
    private final ConcurrentMap<String, JobMetrics> jobMetricsMap = new ConcurrentHashMap<>();
    
    // System-wide metrics
    private final AtomicInteger totalJobsCompleted = new AtomicInteger(0);
    private final AtomicInteger totalJobsSubmitted = new AtomicInteger(0);
    private long systemStartTime;
    private volatile long lastResetTime;
    private final Logger logger;
    
    /**
     * Inner class to hold metrics for a single job.
     */
    public static class JobMetrics {
        private final String jobName;
        private final int cpuTime;            // Required CPU time in seconds
        private final int priority;
        private final long arrivalTime;       // Time when job was submitted
        private long startTime;               // Time when job started execution
        private long completionTime;          // Time when job finished
        private long waitTime;                // Time spent waiting
        private long actualCpuTime;           // Actual time spent executing
        private long turnaroundTime;          // Total time from submission to completion
        private Logger logger;
        
        /**
         * Creates a new JobMetrics object for tracking a job's performance.
         * 
         * @param jobName The name of the job
         * @param cpuTime The CPU time required for the job in seconds
         * @param priority The priority of the job
         * @param arrivalTime The time when the job was submitted
         */
        public JobMetrics(String jobName, int cpuTime, int priority, long arrivalTime) {
            this.jobName = jobName;
            this.cpuTime = cpuTime;
            this.priority = priority;
            this.arrivalTime = arrivalTime;
            LoggingSystem loggingSystem = new LoggingSystem();
            this.logger = new Logger("JobMetrics", loggingSystem);
            this.logger.info("Created new JobMetrics for job: " + jobName);
        }
        
        /**
         * Sets the start time for the job.
         * 
         * @param startTime The time when the job started execution
         */
        public void setStartTime(long startTime) {
            this.startTime = startTime;
            this.waitTime = startTime - arrivalTime;
            logger.info("Job " + jobName + " started. Wait time: " + waitTime + "ms");
        }
        
        /**
         * Sets the completion time for the job and calculates performance metrics.
         * 
         * @param completionTime The time when the job completed
         */
        public void setCompletionTime(long completionTime) {
            this.completionTime = completionTime;
            this.actualCpuTime = completionTime - startTime;
            this.turnaroundTime = completionTime - arrivalTime;
            logger.info("Job " + jobName + " completed. CPU time: " + actualCpuTime + 
                       "ms, Turnaround time: " + turnaroundTime + "ms");
        }
        
        /**
         * Gets the job name.
         * 
         * @return The job name
         */
        public String getJobName() {
            return jobName;
        }
        
        /**
         * Gets the required CPU time.
         * 
         * @return The CPU time in seconds
         */
        public int getCpuTime() {
            return cpuTime;
        }
        
        /**
         * Gets the job priority.
         * 
         * @return The job priority
         */
        public int getPriority() {
            return priority;
        }
        
        /**
         * Gets the arrival time.
         * 
         * @return The arrival time in milliseconds
         */
        public long getArrivalTime() {
            return arrivalTime;
        }
        
        /**
         * Gets the start time.
         * 
         * @return The start time in milliseconds
         */
        public long getStartTime() {
            return startTime;
        }
        
        /**
         * Gets the completion time.
         * 
         * @return The completion time in milliseconds
         */
        public long getCompletionTime() {
            return completionTime;
        }
        
        /**
         * Gets the wait time.
         * 
         * @return The wait time in milliseconds
         */
        public long getWaitTime() {
            return waitTime;
        }
        
        /**
         * Gets the actual CPU time.
         * 
         * @return The actual CPU time in milliseconds
         */
        public long getActualCpuTime() {
            return actualCpuTime;
        }
        
        /**
         * Gets the turnaround time.
         * 
         * @return The turnaround time in milliseconds
         */
        public long getTurnaroundTime() {
            return turnaroundTime;
        }
    }
    
    /**
     * Constructs a new PerformanceMetrics instance and sets the system start time.
     */
    public PerformanceMetrics() {
        systemStartTime = System.currentTimeMillis();
        lastResetTime = systemStartTime;
        LoggingSystem loggingSystem = new LoggingSystem();
        this.logger = new Logger("PerformanceMetrics", loggingSystem);
        logger.info("Performance metrics system initialized");
    }
    
    /**
     * Resets all metrics.
     */
    public void reset() {
        jobMetricsMap.clear();
        totalJobsCompleted.set(0);
        totalJobsSubmitted.set(0);
        lastResetTime = System.currentTimeMillis();
        logger.info("Performance metrics reset");
    }

    // get the job metrics map copy
    public ConcurrentMap<String, JobMetrics> getJobMetricsMap() {
        return new ConcurrentHashMap<>(jobMetricsMap);
    }

    // update the job metrics map
    public void updateJobMetricsMap(ConcurrentMap<String, JobMetrics> jobMetricsMap) {
        this.jobMetricsMap.putAll(jobMetricsMap);
    }
    
    /**
     * Records a job submission.
     * 
     * @param jobName The name of the job
     * @param cpuTime The CPU time required for the job in seconds
     * @param priority The priority of the job
     * @param arrivalTime The time when the job was submitted
     */
    public void recordJobSubmission(String jobName, int cpuTime, int priority, long arrivalTime) {
        JobMetrics metrics = new JobMetrics(jobName, cpuTime, priority, arrivalTime);
        jobMetricsMap.put(jobName, metrics);
        totalJobsSubmitted.incrementAndGet();
        logger.info("Job submitted: " + jobName + " (CPU time: " + cpuTime + "s, Priority: " + priority + ")");
    }
    
    /**
     * Records when a job starts execution.
     * 
     * @param jobName The name of the job
     * @param startTime The time when the job started execution
     */
    public void recordJobStart(String jobName, long startTime) {
        JobMetrics metrics = jobMetricsMap.get(jobName);
        if (metrics != null) {
            metrics.setStartTime(startTime);
            logger.info("Job started: " + jobName);
        } else {
            logger.debug("Attempted to record start for unknown job: " + jobName);
        }
    }
    
    /**
     * Records when a job completes execution.
     * 
     * @param jobName The name of the job
     * @param completionTime The time when the job completed
     */
    public void recordJobCompletion(String jobName, long completionTime) {
        logger.info("Recording job completion for job: " + jobName);
        JobMetrics metrics = jobMetricsMap.get(jobName);
        if (metrics != null) {
            metrics.setCompletionTime(completionTime);
            totalJobsCompleted.incrementAndGet();
            logger.info("Job completed: " + jobName);
        } else {
            logger.debug("Attempted to record completion for unknown job: " + jobName);
        }
    }
    
    /**
     * Gets the average turnaround time for all completed jobs.
     * 
     * @return The average turnaround time in milliseconds
     */
    public double getAverageTurnaroundTime() {
        List<JobMetrics> completedJobs = getCompletedJobs();
        if (completedJobs.isEmpty()) {
            logger.info("No completed jobs for turnaround time calculation");
            return 0.0;
        }
        
        double totalTurnaroundTime = completedJobs.stream()
            .mapToDouble(JobMetrics::getTurnaroundTime)
            .sum();
        
        double avgTurnaround = totalTurnaroundTime / completedJobs.size();
        logger.info("Average turnaround time: " + avgTurnaround + "ms");
        return avgTurnaround;
    }
    
    /**
     * Gets the average waiting time for all completed jobs.
     * 
     * @return The average waiting time in milliseconds
     */
    public double getAverageWaitingTime() {
        List<JobMetrics> completedJobs = getCompletedJobs();
        if (completedJobs.isEmpty()) {
            logger.info("No completed jobs for waiting time calculation");
            return 0.0;
        }
        
        double totalWaitingTime = completedJobs.stream()
            .mapToDouble(JobMetrics::getWaitTime)
            .sum();
        
        double avgWaiting = totalWaitingTime / completedJobs.size();
        logger.info("Average waiting time: " + avgWaiting + "ms");
        return avgWaiting;
    }
    
    /**
     * Gets the average CPU time for all completed jobs.
     * 
     * @return The average CPU time in milliseconds
     */
    public double getAverageCpuTime() {
        List<JobMetrics> completedJobs = getCompletedJobs();
        if (completedJobs.isEmpty()) {
            logger.info("No completed jobs for CPU time calculation");
            return 0.0;
        }
        
        double totalCpuTime = completedJobs.stream()
            .mapToDouble(JobMetrics::getActualCpuTime)
            .sum();
        
        double avgCpu = totalCpuTime / completedJobs.size();
        logger.info("Average CPU time: " + avgCpu + "ms");
        return avgCpu;
    }
    
    /**
     * Gets the system throughput (jobs per time unit).
     * 
     * @return The throughput in jobs per second
     */
    public double getThroughput() {
        long currentTime = System.currentTimeMillis();
        double elapsedTimeSeconds = (currentTime - lastResetTime) / 1000.0;
        
        if (elapsedTimeSeconds <= 0) {
            logger.info("No elapsed time for throughput calculation");
            return 0.0;
        }
        
        double throughput = totalJobsCompleted.get() / elapsedTimeSeconds;
        logger.info("System throughput: " + throughput + " jobs/second");
        return throughput;
    }
    
    /**
     * Gets the total number of jobs completed.
     * 
     * @return The total number of jobs completed
     */
    public int getTotalJobsCompleted() {
        int completed = totalJobsCompleted.get();
        logger.info("Total jobs completed: " + completed);
        return completed;
    }
    
    /**
     * Gets the total number of jobs submitted.
     * 
     * @return The total number of jobs submitted
     */
    public int getTotalJobsSubmitted() {
        int submitted = totalJobsSubmitted.get();
        logger.info("Total jobs submitted: " + submitted);
        return submitted;
    }
    
    /**
     * Gets all completed jobs.
     * 
     * @return A list of all completed jobs
     */
    private List<JobMetrics> getCompletedJobs() {
        List<JobMetrics> completed = jobMetricsMap.values().stream()
            .filter(metrics -> metrics.getCompletionTime() > 0)
            .collect(Collectors.toList());
        logger.debug("Retrieved " + completed.size() + " completed jobs");
        return completed;
    }
    
    /**
     * Gets all jobs, including those not yet completed.
     * 
     * @return A list of all jobs
     */
    public List<JobMetrics> getAllJobs() {
        List<JobMetrics> allJobs = new ArrayList<>(jobMetricsMap.values());
        logger.debug("Retrieved " + allJobs.size() + " total jobs");
        return allJobs;
    }
    
    /**
     * Gets the system uptime in milliseconds.
     * 
     * @return The system uptime in milliseconds
     */
    public long getSystemUptime() {
        long uptime = System.currentTimeMillis() - systemStartTime;
        logger.info("System uptime: " + uptime + "ms");
        return uptime;
    }
} 