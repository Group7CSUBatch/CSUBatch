package com.project.core;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

/**
 * Test suite for the Job class.
 */
public class JobTest {

    @Test
    public void testConstructorWithStringStatus() {
        String name = "TestJob";
        int cpuTime = 5;
        int priority = 2;
        long arrivalTime = System.currentTimeMillis();
        String status = "Waiting";
        
        Job job = new Job(name, cpuTime, priority, arrivalTime, status);
        
        assertEquals(name, job.getName());
        assertEquals(cpuTime, job.getCpuTime());
        assertEquals(priority, job.getPriority());
        assertEquals(arrivalTime, job.getArrivalTime());
        assertEquals(status, job.getStatus());
    }
    
    @Test
    public void testConstructorWithEnumStatus() {
        String name = "TestJob";
        int cpuTime = 5;
        int priority = 2;
        long arrivalTime = System.currentTimeMillis();
        JobStatus status = JobStatus.WAITING;
        
        Job job = new Job(name, cpuTime, priority, arrivalTime, status);
        
        assertEquals(name, job.getName());
        assertEquals(cpuTime, job.getCpuTime());
        assertEquals(priority, job.getPriority());
        assertEquals(arrivalTime, job.getArrivalTime());
        assertEquals(status.getDisplayName(), job.getStatus());
        assertEquals(status, job.getJobStatus());
    }
    
    @Test
    public void testGetters() {
        String name = "TestJob";
        int cpuTime = 5;
        int priority = 2;
        long arrivalTime = System.currentTimeMillis();
        JobStatus status = JobStatus.WAITING;
        
        Job job = new Job(name, cpuTime, priority, arrivalTime, status);
        
        assertEquals(name, job.getName());
        assertEquals(cpuTime, job.getCpuTime());
        assertEquals(priority, job.getPriority());
        assertEquals(arrivalTime, job.getArrivalTime());
        assertEquals(status.getDisplayName(), job.getStatus());
    }
    
    @Test
    public void testSetStatus() {
        String name = "TestJob";
        int cpuTime = 5;
        int priority = 2;
        long arrivalTime = System.currentTimeMillis();
        JobStatus status = JobStatus.WAITING;
        
        Job job = new Job(name, cpuTime, priority, arrivalTime, status);
        
        // Test setting status with string
        String newStatus = "Running";
        job.setStatus(newStatus);
        assertEquals(newStatus, job.getStatus());
        assertEquals(JobStatus.RUNNING, job.getJobStatus());
        
        // Test setting status with enum
        job.setStatus(JobStatus.COMPLETED);
        assertEquals(JobStatus.COMPLETED.getDisplayName(), job.getStatus());
        assertEquals(JobStatus.COMPLETED, job.getJobStatus());
    }
    
    @Test
    public void testGetJobStatus() {
        String name = "TestJob";
        int cpuTime = 5;
        int priority = 2;
        long arrivalTime = System.currentTimeMillis();
        
        // Create job with string status
        Job job = new Job(name, cpuTime, priority, arrivalTime, "Running");
        assertEquals(JobStatus.RUNNING, job.getJobStatus());
        
        // Test with invalid status string - should return WAITING by default
        job.setStatus("InvalidStatus");
        assertEquals(JobStatus.WAITING, job.getJobStatus());
    }
} 