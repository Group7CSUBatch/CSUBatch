package com.project.core;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.concurrent.TimeUnit;

/**
 * Test suite for the JobQueue class.
 */
public class JobQueueTest {
    private JobQueue jobQueue;
    private Job testJob1;
    private Job testJob2;
    private Job testJob3;

    @BeforeEach
    public void setUp() {
        jobQueue = new JobQueue();
        // Create test jobs with different priorities and CPU times
        testJob1 = new Job("TestJob1", 5, 1, System.currentTimeMillis(), JobStatus.WAITING);
        testJob2 = new Job("TestJob2", 3, 2, System.currentTimeMillis() + 1, JobStatus.WAITING);
        testJob3 = new Job("TestJob3", 7, 3, System.currentTimeMillis() + 2, JobStatus.WAITING);
    }

    @Test
    public void testAddJob() {
        assertTrue(jobQueue.isEmpty());
        jobQueue.addJob(testJob1);
        assertFalse(jobQueue.isEmpty());
        assertEquals(1, jobQueue.size());
    }

    @Test
    public void testSize() {
        assertEquals(0, jobQueue.size());
        jobQueue.addJob(testJob1);
        assertEquals(1, jobQueue.size());
        jobQueue.addJob(testJob2);
        assertEquals(2, jobQueue.size());
    }

    @Test
    public void testIsEmpty() {
        assertTrue(jobQueue.isEmpty());
        jobQueue.addJob(testJob1);
        assertFalse(jobQueue.isEmpty());
        jobQueue.pollJob(); // Remove the job
        assertTrue(jobQueue.isEmpty());
    }

    @Test
    public void testPeekJob() {
        assertNull(jobQueue.peekJob());
        jobQueue.addJob(testJob1);
        assertSame(testJob1, jobQueue.peekJob());
        assertEquals(1, jobQueue.size()); // Peek shouldn't remove the job
    }

    @Test
    public void testPollJob() {
        assertNull(jobQueue.pollJob());
        jobQueue.addJob(testJob1);
        Job polledJob = jobQueue.pollJob();
        assertSame(testJob1, polledJob);
        assertEquals(0, jobQueue.size()); // Poll should remove the job
    }

    @Test
    public void testRetrieveJob() throws InterruptedException {
        jobQueue.addJob(testJob1);
        Job retrievedJob = jobQueue.retrieveJob();
        assertSame(testJob1, retrievedJob);
        assertEquals(0, jobQueue.size());
        assertEquals(retrievedJob, jobQueue.getRunningJob()); // Running job should be updated
    }

    @Test
    @Timeout(value = 1, unit = TimeUnit.SECONDS) // Test should timeout if retrieveJob() doesn't wait correctly
    public void testRetrieveJobWaitsWhenEmpty() {
        Thread adderThread = new Thread(() -> {
            try {
                Thread.sleep(100); // Wait a bit before adding a job
                jobQueue.addJob(testJob1);
            } catch (InterruptedException e) {
                fail("Thread was interrupted");
            }
        });
        
        adderThread.start();
        
        try {
            Job retrievedJob = jobQueue.retrieveJob(); // Should wait until job is added
            assertSame(testJob1, retrievedJob);
        } catch (InterruptedException e) {
            fail("Thread was interrupted");
        }
    }

    @Test
    public void testClear() {
        jobQueue.addJob(testJob1);
        jobQueue.addJob(testJob2);
        assertFalse(jobQueue.isEmpty());
        
        jobQueue.clear();
        assertTrue(jobQueue.isEmpty());
        assertEquals(0, jobQueue.size());
        assertNull(jobQueue.getRunningJob()); // Running job should also be cleared
    }

    @Test
    public void testRescheduleJob() {
        jobQueue.addJob(testJob1);
        Job job = jobQueue.pollJob();
        assertEquals(0, jobQueue.size());
        
        jobQueue.rescheduleJob(job);
        assertEquals(1, jobQueue.size());
        assertSame(job, jobQueue.peekJob());
    }

    @Test
    public void testStream() {
        jobQueue.addJob(testJob1);
        jobQueue.addJob(testJob2);
        jobQueue.addJob(testJob3);
        
        long count = jobQueue.stream().count();
        assertEquals(3, count);
        
        // Test that we can find jobs by name using the stream
        boolean containsTestJob2 = jobQueue.stream()
                .anyMatch(job -> "TestJob2".equals(job.getName()));
        assertTrue(containsTestJob2);
    }

    @Test
    public void testRunningJobOperations() {
        assertNull(jobQueue.getRunningJob());
        
        jobQueue.setRunningJob(testJob1);
        assertSame(testJob1, jobQueue.getRunningJob());
        
        jobQueue.clearRunningJob();
        assertNull(jobQueue.getRunningJob());
    }

    @Test
    public void testFIFOBehavior() throws InterruptedException {
        // Add jobs in a specific order
        jobQueue.addJob(testJob1);
        jobQueue.addJob(testJob2);
        jobQueue.addJob(testJob3);
        
        // Retrieve jobs and verify they come out in FIFO order
        Job retrieved1 = jobQueue.retrieveJob();
        Job retrieved2 = jobQueue.retrieveJob();
        Job retrieved3 = jobQueue.retrieveJob();
        
        assertSame(testJob1, retrieved1);
        assertSame(testJob2, retrieved2);
        assertSame(testJob3, retrieved3);
    }
} 