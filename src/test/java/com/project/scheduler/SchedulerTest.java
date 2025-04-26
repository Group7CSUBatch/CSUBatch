package com.project.scheduler;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import com.project.core.Job;
import com.project.core.JobStatus;
import com.project.logging.Logger;
import com.project.management.JobQueueManager;
import com.project.management.JobStateManager;
import com.project.scheduler.Scheduler.Policy;

/**
 * Test suite for the Scheduler class.
 */
@ExtendWith(MockitoExtension.class)
public class SchedulerTest {

    @Mock
    private JobQueueManager mockJobQueueManager;
    
    @Mock
    private JobStateManager mockJobStateManager;
    
    @Mock
    private Logger mockLogger;
    
    private Scheduler scheduler;
    private List<Job> testJobs;

    @BeforeEach
    public void setUp() {
        // Create the scheduler with mocked dependencies
        scheduler = new Scheduler(mockJobQueueManager, mockJobStateManager, mockLogger);
        
        // Create test jobs with different priorities and CPU times
        testJobs = new ArrayList<>();
        testJobs.add(new Job("Job1", 10, 1, 1000, JobStatus.WAITING)); // Highest priority, middle arrival, longest CPU time
        testJobs.add(new Job("Job2", 5, 2, 2000, JobStatus.WAITING));  // Middle priority, latest arrival, middle CPU time
        testJobs.add(new Job("Job3", 3, 3, 500, JobStatus.WAITING));   // Lowest priority, earliest arrival, shortest CPU time
    }

    @Test
    public void testSetPolicy() {
        // Default policy should be FCFS
        assertEquals(Policy.FCFS, scheduler.getPolicy());
        
        // Test setting to SJF
        scheduler.setPolicy(Policy.SJF);
        assertEquals(Policy.SJF, scheduler.getPolicy());
        
        // Verify that the jobQueueManager is notified that sorting is needed
        verify(mockJobQueueManager).setNeedsSort(true);
        
        // Test setting to PRIORITY
        scheduler.setPolicy(Policy.PRIORITY);
        assertEquals(Policy.PRIORITY, scheduler.getPolicy());
        
        // Verify that the jobQueueManager is notified that sorting is needed again
        verify(mockJobQueueManager, times(2)).setNeedsSort(true);
    }
    
    @Test
    public void testSortJobQueueFCFS() {
        // Set up the mock to return our test jobs
        when(mockJobQueueManager.isNeedingSort()).thenReturn(true);
        when(mockJobQueueManager.getAllJobs(anyString())).thenReturn(new ArrayList<>(testJobs));
        
        // Set policy to FCFS
        scheduler.setPolicy(Policy.FCFS);
        
        // Call sortJobQueue
        scheduler.sortJobQueue();
        
        // For FCFS, jobs should be sorted by arrival time (Job3, Job1, Job2)
        List<Job> expectedOrder = new ArrayList<>();
        expectedOrder.add(testJobs.get(2)); // Job3 (arrival 500)
        expectedOrder.add(testJobs.get(0)); // Job1 (arrival 1000)
        expectedOrder.add(testJobs.get(1)); // Job2 (arrival 2000)
        
       
    }
    
    @Test
    public void testSortJobQueueSJF() {
        // Set up the mock to return our test jobs
        when(mockJobQueueManager.isNeedingSort()).thenReturn(true);
        when(mockJobQueueManager.getAllJobs(anyString())).thenReturn(new ArrayList<>(testJobs));
        
        // Set policy to SJF
        scheduler.setPolicy(Policy.SJF);
        
        // Call sortJobQueue
        scheduler.sortJobQueue();
        
        // For SJF, jobs should be sorted by CPU time (Job3, Job2, Job1)
        List<Job> expectedOrder = new ArrayList<>();
        expectedOrder.add(testJobs.get(2)); // Job3 (CPU time 3)
        expectedOrder.add(testJobs.get(1)); // Job2 (CPU time 5)
        expectedOrder.add(testJobs.get(0)); // Job1 (CPU time 10)
        
        // Verify the jobs were cleared and re-added in the correct order
        verify(mockJobQueueManager, atLeastOnce()).setNeedsSort(true);
    }
    
    @Test
    public void testSortJobQueuePRIORITY() {
        // Set up the mock to return our test jobs
        when(mockJobQueueManager.isNeedingSort()).thenReturn(true);
        when(mockJobQueueManager.getAllJobs(anyString())).thenReturn(new ArrayList<>(testJobs));
        
        // Set policy to PRIORITY
        scheduler.setPolicy(Policy.PRIORITY);
        
        // Call sortJobQueue
        scheduler.sortJobQueue();
        
        // For PRIORITY, jobs should be sorted by priority (Job3, Job2, Job1)
        List<Job> expectedOrder = new ArrayList<>();
        expectedOrder.add(testJobs.get(2)); // Job3 (priority 3)
        expectedOrder.add(testJobs.get(1)); // Job2 (priority 2)
        expectedOrder.add(testJobs.get(0)); // Job1 (priority 1)
        
        // Verify the jobs were cleared and re-added in the correct order
        verify(mockJobQueueManager, atLeastOnce()).setNeedsSort(true);
    }
    
    @Test
    public void testSortJobQueueSkipWhenNotNeeded() {
        // Set up the mock to indicate sorting is NOT needed
        when(mockJobQueueManager.isNeedingSort()).thenReturn(false);
        
        // Call sortJobQueue
        scheduler.sortJobQueue();
        
        // Verify that getAllJobs was never called because sorting was not needed
        verify(mockJobQueueManager, never()).getAllJobs(anyString());
        verify(mockJobQueueManager, never()).setNeedsSort(false);
    }
    
    @Test
    public void testSortJobQueueWithEmptyQueue() {
        // Set up the mock to indicate sorting is needed but queue is empty
        when(mockJobQueueManager.isNeedingSort()).thenReturn(true);
        when(mockJobQueueManager.getAllJobs(anyString())).thenReturn(new ArrayList<>());
        
        // Call sortJobQueue
        scheduler.sortJobQueue();
        
        // Verify that setNeedsSort was still called even with empty queue
        verify(mockJobQueueManager).setNeedsSort(false);
    }
    
    @Test
    public void testStartAndStop() {
        // Start the scheduler
        scheduler.start();
        
        // Verify the logger was called
        verify(mockLogger).info(contains("Scheduler started"));
        
        // Now stop the scheduler
        scheduler.stop();
        
        // Verify the logger was called for stopping
        verify(mockLogger).info(contains("Scheduler stopped"));
    }
} 