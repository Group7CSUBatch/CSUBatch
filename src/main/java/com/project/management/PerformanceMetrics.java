package com.project.management;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

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
        }
        
        /**
         * Sets the start time for the job.
         * 
         * @param startTime The time when the job started execution
         */
        public void setStartTime(long startTime) {
            this.startTime = startTime;
            this.waitTime = startTime - arrivalTime;
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
    }
    
    /**
     * Resets all metrics.
     */
    public void reset() {
        jobMetricsMap.clear();
        totalJobsCompleted.set(0);
        totalJobsSubmitted.set(0);
        lastResetTime = System.currentTimeMillis();
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
        }
    }
    
    /**
     * Records when a job completes execution.
     * 
     * @param jobName The name of the job
     * @param completionTime The time when the job completed
     */
    public void recordJobCompletion(String jobName, long completionTime) {
        JobMetrics metrics = jobMetricsMap.get(jobName);
        if (metrics != null) {
            metrics.setCompletionTime(completionTime);
            totalJobsCompleted.incrementAndGet();
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
            return 0.0;
        }
        
        double totalTurnaroundTime = completedJobs.stream()
            .mapToDouble(JobMetrics::getTurnaroundTime)
            .sum();
        
        return totalTurnaroundTime / completedJobs.size();
    }
    
    /**
     * Gets the average waiting time for all completed jobs.
     * 
     * @return The average waiting time in milliseconds
     */
    public double getAverageWaitingTime() {
        List<JobMetrics> completedJobs = getCompletedJobs();
        if (completedJobs.isEmpty()) {
            return 0.0;
        }
        
        double totalWaitingTime = completedJobs.stream()
            .mapToDouble(JobMetrics::getWaitTime)
            .sum();
        
        return totalWaitingTime / completedJobs.size();
    }
    
    /**
     * Gets the average CPU time for all completed jobs.
     * 
     * @return The average CPU time in milliseconds
     */
    public double getAverageCpuTime() {
        List<JobMetrics> completedJobs = getCompletedJobs();
        if (completedJobs.isEmpty()) {
            return 0.0;
        }
        
        double totalCpuTime = completedJobs.stream()
            .mapToDouble(JobMetrics::getActualCpuTime)
            .sum();
        
        return totalCpuTime / completedJobs.size();
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
            return 0.0;
        }
        
        return totalJobsCompleted.get() / elapsedTimeSeconds;
    }
    
    /**
     * Gets the total number of jobs completed.
     * 
     * @return The total number of jobs completed
     */
    public int getTotalJobsCompleted() {
        return totalJobsCompleted.get();
    }
    
    /**
     * Gets the total number of jobs submitted.
     * 
     * @return The total number of jobs submitted
     */
    public int getTotalJobsSubmitted() {
        return totalJobsSubmitted.get();
    }
    
    /**
     * Gets all completed jobs.
     * 
     * @return A list of all completed jobs
     */
    private List<JobMetrics> getCompletedJobs() {
        return jobMetricsMap.values().stream()
            .filter(metrics -> metrics.getCompletionTime() > 0)
            .collect(Collectors.toList());
    }
    
    /**
     * Gets all jobs, including those not yet completed.
     * 
     * @return A list of all jobs
     */
    public List<JobMetrics> getAllJobs() {
        return new ArrayList<>(jobMetricsMap.values());
    }
    
    /**
     * Gets the system uptime in milliseconds.
     * 
     * @return The system uptime in milliseconds
     */
    public long getSystemUptime() {
        return System.currentTimeMillis() - systemStartTime;
    }
} 