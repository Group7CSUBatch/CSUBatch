package com.project.unified;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import com.project.core.Job;
import com.project.core.JobQueue;
import com.project.core.JobStatus;
import com.project.logging.Logger;
import com.project.logging.LoggingSystem;
import com.project.management.JobQueueManager;
import com.project.management.JobStateManager;
import com.project.management.SystemController;
import com.project.scheduler.Dispatcher;
import com.project.scheduler.Scheduler;

/**
 * Test suite for the command handling functionality that interacts with the job queue.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class CommandHandlingTest {

    @Mock
    private SystemController mockSystemController;
    
    @Mock
    private JobQueueManager mockJobQueueManager;
    
    @Mock
    private JobQueue mockJobQueue;
    
    @Mock
    private JobStateManager mockJobStateManager;
    
    @Mock
    private Logger mockLogger;
    
    @Mock
    private Scheduler mockScheduler;
    
    @Mock
    private Dispatcher mockDispatcher;
    
    @Mock
    private LoggingSystem mockLoggingSystem;
    
    private ConsoleInterface consoleInterface;
    
    @BeforeEach
    public void setUp() {
        // Configure the mock system controller
        when(mockSystemController.getJobQueueManager()).thenReturn(mockJobQueueManager);
        when(mockSystemController.getScheduler()).thenReturn(mockScheduler);
        when(mockSystemController.getDispatcher()).thenReturn(mockDispatcher);
        when(mockSystemController.getJobStateManager()).thenReturn(mockJobStateManager);
        when(mockJobStateManager.getLogger()).thenReturn(mockLogger);
        when(mockLogger.getLoggingSystem()).thenReturn(mockLoggingSystem);
        when(mockJobQueueManager.getUnderlyingQueue()).thenReturn(mockJobQueue);
        
        // Create the console interface with the mock system controller
        consoleInterface = new ConsoleInterface(mockSystemController);
    }

    /**
     * Helper method to access the private handleCommand method using reflection.
     * 
     * @param command The command to handle
     * @throws Exception If reflection fails
     */
    private void invokeHandleCommand(String command) throws Exception {
        Method handleMethod = ConsoleInterface.class.getDeclaredMethod("handleCommand", String.class);
        handleMethod.setAccessible(true);
        handleMethod.invoke(consoleInterface, command);
    }
    
    /**
     * Helper method to access the private submitJob method using reflection.
     * 
     * @param parts The command parts
     * @throws Exception If reflection fails
     */
    private void invokeSubmitJob(String[] parts) throws Exception {
        Method submitMethod = ConsoleInterface.class.getDeclaredMethod("submitJob", String[].class);
        submitMethod.setAccessible(true);
        submitMethod.invoke(consoleInterface, (Object) parts);
    }
    
    @Test
    public void testSubmitJob_ValidJob() throws Exception {
        // Setup
        String[] validJobCommand = {"run", "job1", "10", "2"};
        when(mockSystemController.addJob(any(Job.class), eq("ConsoleInterface"))).thenReturn(true);
        
        // Execute
        invokeSubmitJob(validJobCommand);
        
        // Verify
        ArgumentCaptor<Job> jobCaptor = ArgumentCaptor.forClass(Job.class);
        verify(mockSystemController).addJob(jobCaptor.capture(), eq("ConsoleInterface"));
        
        Job capturedJob = jobCaptor.getValue();
        assertEquals("job1", capturedJob.getName());
        assertEquals(10, capturedJob.getCpuTime());
        assertEquals(2, capturedJob.getPriority());
        assertEquals(JobStatus.WAITING, capturedJob.getJobStatus());
    }
    
    @Test
    public void testSubmitJob_FailedToAdd() throws Exception {
        // Setup
        String[] validJobCommand = {"run", "job1", "10", "2"};
        when(mockSystemController.addJob(any(Job.class), eq("ConsoleInterface"))).thenReturn(false);
        
        // Execute
        invokeSubmitJob(validJobCommand);
        
        // Nothing to verify beyond the setup - just make sure no exceptions occur
    }
    
    @Test
    public void testSetPolicy_FCFS() throws Exception {
        // Execute
        invokeHandleCommand("fcfs");
        
        // Verify
        verify(mockScheduler).setPolicy(Scheduler.Policy.FCFS);
    }
    
    @Test
    public void testSetPolicy_SJF() throws Exception {
        // Execute
        invokeHandleCommand("sjf");
        
        // Verify
        verify(mockScheduler).setPolicy(Scheduler.Policy.SJF);
    }
    
    @Test
    public void testSetPolicy_PRIORITY() throws Exception {
        // Execute
        invokeHandleCommand("priority");
        
        // Verify
        verify(mockScheduler).setPolicy(Scheduler.Policy.PRIORITY);
    }
    
    @Test
    public void testListJobs() throws Exception {
        // Setup
        List<Job> mockJobs = new ArrayList<>();
        when(mockSystemController.listJobs()).thenReturn(mockJobs);
        when(mockJobQueueManager.getQueueSize()).thenReturn(0);
        
        // Execute
        invokeHandleCommand("list");
        
        // Verify
        verify(mockSystemController).listJobs();
    }
    
    @Test
    public void testListJobs_WithRunningJob() throws Exception {
        // Setup
        List<Job> mockJobs = new ArrayList<>();
        Job mockRunningJob = new Job("RunningJob", 10, 2, System.currentTimeMillis(), JobStatus.RUNNING);
        when(mockSystemController.listJobs()).thenReturn(mockJobs);
        when(mockJobQueueManager.getQueueSize()).thenReturn(0);
        when(mockJobQueue.getRunningJob()).thenReturn(mockRunningJob);
        
        // Execute
        invokeHandleCommand("list");
        
        // Verify
        verify(mockSystemController).listJobs();
        verify(mockJobQueue).getRunningJob();
    }
    
    @Test
    public void testHandleCommand_InvalidCommand() throws Exception {
        // Execute
        invokeHandleCommand("invalidCommand");
        
        // Nothing to verify - just make sure no exceptions occur
    }
    
    @Test
    public void testHandleCommand_EmptyCommand() throws Exception {
        // Execute
        invokeHandleCommand("");
        
        // Nothing to verify - just make sure no exceptions occur
    }
    
    @Test
    public void testHandleCommand_Version() throws Exception {
        // Execute
        invokeHandleCommand("version");
        
        // Verify
        verify(mockLogger).info("Version command executed");
    }
    
    @Test
    public void testHandleCommand_Status() throws Exception {
        // Setup
        when(mockJobQueueManager.getQueueSize()).thenReturn(0);
        
        // Execute
        invokeHandleCommand("status");
        
        // Verify
        verify(mockLogger).info("Status command executed");
    }
    
    @Test
    public void testHandleCommand_Quit() throws Exception {
        // Execute
        invokeHandleCommand("quit");
        
        // Verify the interface stops
        verify(mockLogger).info("User requested to quit");
    }
} 