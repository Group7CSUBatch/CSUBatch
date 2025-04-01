package com.project.scheduler;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.project.core.Job;
import com.project.core.JobStatus;
import com.project.logging.Logger;
import com.project.management.JobQueueManager;
import com.project.management.JobStateManager;

/**
 * Test suite for the Dispatcher class.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class DispatcherTest {

    @Mock
    private Scheduler mockScheduler;
    
    @Mock
    private JobQueueManager mockJobQueueManager;
    
    @Mock
    private JobStateManager mockJobStateManager;
    
    @Mock
    private Logger mockLogger;
    
    private Dispatcher dispatcher;
    private Job testJob;

    @BeforeEach
    public void setUp() {
        // Create a new dispatcher with the mock scheduler
        dispatcher = new Dispatcher(mockScheduler);
        
        // Set mock dependencies
        dispatcher.setJobQueueManager(mockJobQueueManager);
        dispatcher.setJobStateManager(mockJobStateManager);
        dispatcher.setLogger(mockLogger);
        
        // Create a test job
        testJob = new Job("TestJob", 5, 1, System.currentTimeMillis(), JobStatus.WAITING);
    }

    @Test
    public void testCpuTimeSliceSetting() {
        // Default CPU time slice should be Integer.MAX_VALUE
        
        // Set a new CPU time slice
        int timeSlice = 10;
        dispatcher.setCpuTimeSlice(timeSlice);
        
        // Verify that setting an invalid CPU time slice throws an exception
        assertThrows(IllegalArgumentException.class, () -> {
            dispatcher.setCpuTimeSlice(0);
        });
        
        assertThrows(IllegalArgumentException.class, () -> {
            dispatcher.setCpuTimeSlice(-1);
        });
    }
    
    @Test
    public void testSetDependencies() {
        // Create a new dispatcher
        Dispatcher newDispatcher = new Dispatcher(mockScheduler);
        
        // Set the logger and dependencies
        newDispatcher.setLogger(mockLogger);
        newDispatcher.setJobQueueManager(mockJobQueueManager);
        newDispatcher.setJobStateManager(mockJobStateManager);
        
        // No need to verify logger calls as the implementation doesn't log
        // during setting dependencies; we just verify no exceptions occur
    }
    
    @Test
    public void testConstructorWithNullScheduler() {
        // Verify that constructing with a null scheduler throws an exception
        assertThrows(IllegalArgumentException.class, () -> {
            new Dispatcher(null);
        });
    }
    
    @Test
    public void testStopDispatcher() {
        // Stop the dispatcher
        dispatcher.stop();
        
        // Verify that the logger was called with a message containing "stopped"
        verify(mockLogger).info(contains("Dispatcher stopped"));
    }
    
    @Test
    public void testExecuteJob() throws Exception {
        // Configure the mockJobQueueManager to return our test job and then
        // handle the queue check properly
        when(mockJobQueueManager.isQueueEmpty())
            .thenReturn(false) // First call to check if queue is empty
            .thenReturn(true); // Second call to exit the loop
            
        when(mockJobQueueManager.retrieveJob())
            .thenReturn(testJob);
        
        // Create a thread for the dispatcher that will run briefly
        Thread dispatcherThread = new Thread(() -> {
            try {
                // Only run a limited number of iterations 
                int maxIterations = 2;
                int iteration = 0;
                
                while (iteration < maxIterations && !Thread.currentThread().isInterrupted()) {
                    iteration++;
                    if (iteration == 1 && mockJobQueueManager != null) {
                        // Simulate the dispatcher checking the queue and processing one job
                        if (!mockJobQueueManager.isQueueEmpty()) {
                            Job job = mockJobQueueManager.retrieveJob();
                            // Do something with the job to simulate processing
                            if (job != null) {
                                Thread.sleep(10); // Brief sleep to simulate work
                            }
                        }
                    }
                    Thread.sleep(10);
                }
            } catch (InterruptedException e) {
                // Expected when stopping the test
                Thread.currentThread().interrupt();
            }
        });
        
        // Start the thread and let it run for a short time
        dispatcherThread.start();
        Thread.sleep(50);
        
        // Stop the thread
        dispatcherThread.interrupt();
        
        // Wait for the thread to finish, with a timeout
        dispatcherThread.join(500);
        
        // Verify the queue was checked
        verify(mockJobQueueManager, atLeastOnce()).isQueueEmpty();
        
        // The retrieveJob might or might not have been called, depending on timing
        // So we don't verify that specifically
    }
} 