package com.project.core;

/**
 * Represents a job in the CSUbatch scheduling system.
 * Contains information about the job's name, CPU time, priority, arrival time,
 * and status.
 */
public class Job {
        private final String name;
        private final int cpuTime;
        private final int priority;
        private final long arrivalTime;
        private String status;

        /**
         * Constructs a new Job with the specified parameters.
         *
         * @param name        The name of the job
         * @param cpuTime     The CPU time required for the job in seconds
         * @param priority    The priority of the job (lower value means higher
         *                    priority)
         * @param arrivalTime The time when the job arrived in the system
         * @param status      The initial status of the job
         */
        public Job(String name, int cpuTime, int priority, long arrivalTime, String status) {
                this.name = name;
                this.cpuTime = cpuTime;
                this.priority = priority;
                this.arrivalTime = arrivalTime;
                this.status = status;
        }

        /**
         * Constructs a new Job with the specified parameters.
         *
         * @param name        The name of the job
         * @param cpuTime     The CPU time required for the job in seconds
         * @param priority    The priority of the job (lower value means higher
         *                    priority)
         * @param arrivalTime The time when the job arrived in the system
         * @param status      The initial status of the job as JobStatus enum
         */
        public Job(String name, int cpuTime, int priority, long arrivalTime, JobStatus status) {
                this.name = name;
                this.cpuTime = cpuTime;
                this.priority = priority;
                this.arrivalTime = arrivalTime;
                this.status = status.getDisplayName();
        }

        public String getName() {
                return name;
        }

        public int getCpuTime() {
                return cpuTime;
        }

        public int getPriority() {
                return priority;
        }

        public long getArrivalTime() {
                return arrivalTime;
        }

        public String getStatus() {
                return status;
        }

        /**
         * Gets the status as a JobStatus enum.
         *
         * @return The job status as a JobStatus enum
         */
        public JobStatus getJobStatus() {
                return JobStatus.fromString(status);
        }

        public void setStatus(String status) {
                this.status = status;
        }

        /**
         * Sets the status using a JobStatus enum.
         *
         * @param status The new job status
         */
        public void setStatus(JobStatus status) {
                this.status = status.getDisplayName();
        }
}
