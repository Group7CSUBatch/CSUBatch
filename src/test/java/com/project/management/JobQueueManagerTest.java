package com.project.management;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;

import com.project.core.Job;
import com.project.core.JobQueue;
import com.project.core.JobStatus;
import com.project.logging.Logger;

/**
 * Test suite for the JobQueueManager class.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class JobQueueManagerTest {

    @Mock
    private JobStateManager mockJobStateManager;
    
    @Mock
    private Logger mockLogger;
    
    @Mock
    private SystemController mockSystemController;
    
    private JobQueue jobQueue;
    private JobQueueManager jobQueueManager;
    private Job testJob1;
    private Job testJob2;
    private Job testJob3;

    @BeforeEach
    public void setUp() {
        jobQueue = new JobQueue();
        
        // Mock the SystemController singleton before creating JobQueueManager
        try {
            // Use reflection to set the SystemController instance
            java.lang.reflect.Field instanceField = SystemController.class.getDeclaredField("instance");
            instanceField.setAccessible(true);
            instanceField.set(null, mockSystemController);
            
            // Set up returns for mockSystemController methods that might be called
            when(mockSystemController.updateJobStatus(any(Job.class), any(JobStatus.class), anyString(), anyString()))
                .thenReturn(true);
        } catch (Exception e) {
            fail("Failed to set up SystemController mock: " + e.getMessage());
        }
        
        jobQueueManager = new JobQueueManager(jobQueue, mockJobStateManager, mockLogger);
        
        // Create test jobs with different priorities and CPU times
        testJob1 = new Job("TestJob1", 5, 1, System.currentTimeMillis(), JobStatus.WAITING);
        testJob2 = new Job("TestJob2", 3, 2, System.currentTimeMillis() + 1, JobStatus.WAITING);
        testJob3 = new Job("TestJob3", 7, 3, System.currentTimeMillis() + 2, JobStatus.WAITING);
    }

    @Test
    public void testAddJob() {
        boolean result = jobQueueManager.addJob(testJob1, "Test");
        
        assertTrue(result);
        assertEquals(1, jobQueueManager.getQueueSize());
        assertFalse(jobQueueManager.isQueueEmpty());
    }
    
    @Test
    public void testAddJobNullJob() {
        boolean result = jobQueueManager.addJob(null, "Test");
        
        assertFalse(result);
        assertEquals(0, jobQueueManager.getQueueSize());
        assertTrue(jobQueueManager.isQueueEmpty());
    }
    
    @Test
    public void testRetrieveJob() throws InterruptedException {
        jobQueueManager.addJob(testJob1, "Test");
        Job retrievedJob = jobQueueManager.retrieveJob();
        
        assertSame(testJob1, retrievedJob);
        assertEquals(0, jobQueueManager.getQueueSize());
        assertTrue(jobQueueManager.isQueueEmpty());
    }
    
    @Test
    public void testRescheduleJob() {
        jobQueueManager.addJob(testJob1, "Test");
        assertEquals(1, jobQueueManager.getQueueSize());
        
        Job job = null;
        try {
            job = jobQueueManager.retrieveJob();
        } catch (InterruptedException e) {
            fail("Unexpected interruption");
        }
        assertEquals(0, jobQueueManager.getQueueSize());
        
        boolean result = jobQueueManager.rescheduleJob(job, "Test");
        assertTrue(result);
        assertEquals(1, jobQueueManager.getQueueSize());
    }
    
    @Test
    public void testRescheduleJobNullJob() {
        boolean result = jobQueueManager.rescheduleJob(null, "Test");
        
        assertFalse(result);
        assertEquals(0, jobQueueManager.getQueueSize());
    }
    
    @Test
    public void testGetJobByName() {
        jobQueueManager.addJob(testJob1, "Test");
        jobQueueManager.addJob(testJob2, "Test");
        
        Optional<Job> foundJob = jobQueueManager.getJobByName("TestJob1");
        assertTrue(foundJob.isPresent());
        assertSame(testJob1, foundJob.get());
        
        // Test for a job that doesn't exist
        Optional<Job> notFoundJob = jobQueueManager.getJobByName("NonExistentJob");
        assertFalse(notFoundJob.isPresent());
    }
    
    @Test
    public void testRemoveJob() {
        jobQueueManager.addJob(testJob1, "Test");
        jobQueueManager.addJob(testJob2, "Test");
        
        boolean result = jobQueueManager.removeJob(testJob1, "Test");
        
        assertTrue(result);
        assertEquals(1, jobQueueManager.getQueueSize());
        
        // Try to remove a job that's not in the queue
        result = jobQueueManager.removeJob(testJob3, "Test");
        assertFalse(result);
    }
    
    @Test
    public void testRemoveJobNullJob() {
        boolean result = jobQueueManager.removeJob(null, "Test");
        
        assertFalse(result);
    }
    
    @Test
    public void testRemoveJobByName() {
        jobQueueManager.addJob(testJob1, "Test");
        jobQueueManager.addJob(testJob2, "Test");
        
        boolean result = jobQueueManager.removeJobByName("TestJob1", "Test");
        
        assertTrue(result);
        assertEquals(1, jobQueueManager.getQueueSize());
        
        // Try to remove a job that's not in the queue
        result = jobQueueManager.removeJobByName("NonExistentJob", "Test");
        assertFalse(result);
    }
    
    @Test
    public void testGetShortestJob() {
        // Add jobs with different CPU times
        jobQueueManager.addJob(testJob1, "Test"); // CPU time 5
        jobQueueManager.addJob(testJob2, "Test"); // CPU time 3
        jobQueueManager.addJob(testJob3, "Test"); // CPU time 7
        
        Job shortestJob = jobQueueManager.getShortestJob();
        
        assertNotNull(shortestJob);
        assertSame(testJob2, shortestJob); // TestJob2 has the shortest CPU time (3)
    }
    
    @Test
    public void testGetHighestPriorityJob() {
        // Add jobs with different priorities
        jobQueueManager.addJob(testJob1, "Test"); // Priority 1
        jobQueueManager.addJob(testJob2, "Test"); // Priority 2
        jobQueueManager.addJob(testJob3, "Test"); // Priority 3
        
        Job highestPriorityJob = jobQueueManager.getHighestPriorityJob();
        
        assertNotNull(highestPriorityJob);
        // The current implementation returns the job with the lowest priority value
        // In the system, priority is inverted: lower numbers = higher priority
        assertEquals(testJob1.getName(), highestPriorityJob.getName());
        assertEquals(testJob1.getPriority(), highestPriorityJob.getPriority());
    }
    
    @Test
    public void testListJobs() {
        jobQueueManager.addJob(testJob1, "Test");
        jobQueueManager.addJob(testJob2, "Test");
        
        List<Job> jobs = jobQueueManager.listJobs();
        
        assertNotNull(jobs);
        assertEquals(2, jobs.size());
        assertTrue(jobs.contains(testJob1));
        assertTrue(jobs.contains(testJob2));
    }
    
    @Test
    public void testGetAllJobs() {
        jobQueueManager.addJob(testJob1, "Test");
        jobQueueManager.addJob(testJob2, "Test");
        
        List<Job> jobs = jobQueueManager.getAllJobs("Test");
        
        assertNotNull(jobs);
        assertEquals(2, jobs.size());
        assertTrue(jobs.contains(testJob1));
        assertTrue(jobs.contains(testJob2));
    }
    
    @Test
    public void testClearQueue() {
        jobQueueManager.addJob(testJob1, "Test");
        jobQueueManager.addJob(testJob2, "Test");
        
        assertFalse(jobQueueManager.isQueueEmpty());
        
        jobQueueManager.clearQueue("Test");
        
        assertTrue(jobQueueManager.isQueueEmpty());
        assertEquals(0, jobQueueManager.getQueueSize());
    }
    
    @Test
    public void testQueueSizeAndIsEmpty() {
        assertTrue(jobQueueManager.isQueueEmpty());
        assertEquals(0, jobQueueManager.getQueueSize());
        
        jobQueueManager.addJob(testJob1, "Test");
        
        assertFalse(jobQueueManager.isQueueEmpty());
        assertEquals(1, jobQueueManager.getQueueSize());
    }
    
    @Test
    public void testNeedsSort() {
        // Initially, needsSort should be false
        assertFalse(jobQueueManager.isNeedingSort());
        
        // Set needsSort to true
        jobQueueManager.setNeedsSort(true);
        assertTrue(jobQueueManager.isNeedingSort());
        
        // Set needsSort back to false
        jobQueueManager.setNeedsSort(false);
        assertFalse(jobQueueManager.isNeedingSort());
    }
} 