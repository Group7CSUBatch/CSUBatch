package com.project.unified;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import com.project.core.Job;
import com.project.core.JobQueue;
import com.project.logging.Logger;
import com.project.logging.LoggingSystem;
import com.project.management.JobQueueManager;
import com.project.management.JobStateManager;
import com.project.management.SystemController;
import com.project.scheduler.Dispatcher;
import com.project.scheduler.Scheduler;

/**
 * Test suite for the ConsoleInterface class focusing on command validation.
 */
@ExtendWith(MockitoExtension.class)
public class ConsoleInterfaceTest {

    @Mock
    private SystemController mockSystemController;
    
    @Mock
    private JobQueueManager mockJobQueueManager;
    
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
        
        // Create the console interface with the mock system controller
        consoleInterface = new ConsoleInterface(mockSystemController);
    }
    
    /**
     * Helper method to access the private validateCommand method using reflection.
     * 
     * @param parts The command parts to validate
     * @return The validation result (null means no errors)
     * @throws Exception If reflection fails
     */
    private String invokeValidateCommand(String[] parts) throws Exception {
        Method validateMethod = ConsoleInterface.class.getDeclaredMethod("validateCommand", String[].class);
        validateMethod.setAccessible(true);
        return (String) validateMethod.invoke(consoleInterface, (Object) parts);
    }
    
    @Test
    public void testValidateCommand_NullOrEmptyCommand() throws Exception {
        // Test null command
        assertNotNull(invokeValidateCommand(null));
        
        // Test empty command
        assertNotNull(invokeValidateCommand(new String[0]));
    }
    
    @Test
    public void testValidateCommand_RunCommand() throws Exception {
        // Test valid 'run' command
        String[] validRun = {"run", "job1", "10", "2"};
        assertNull(invokeValidateCommand(validRun));
        
        // Test 'run' command with missing arguments
        String[] missingArgs = {"run", "job1", "10"};
        assertNotNull(invokeValidateCommand(missingArgs));
        
        // Test 'run' command with too many arguments
        String[] tooManyArgs = {"run", "job1", "10", "2", "extra"};
        assertNotNull(invokeValidateCommand(tooManyArgs));
        
        // Test 'run' command with invalid CPU time (non-numeric)
        String[] invalidCpuTime = {"run", "job1", "abc", "2"};
        assertNotNull(invokeValidateCommand(invalidCpuTime));
        
        // Test 'run' command with invalid CPU time (negative)
        String[] negativeCpuTime = {"run", "job1", "-10", "2"};
        assertNotNull(invokeValidateCommand(negativeCpuTime));
        
        // Test 'run' command with invalid CPU time (zero)
        String[] zeroCpuTime = {"run", "job1", "0", "2"};
        assertNotNull(invokeValidateCommand(zeroCpuTime));
        
        // Test 'run' command with invalid priority (non-numeric)
        String[] invalidPriority = {"run", "job1", "10", "abc"};
        assertNotNull(invokeValidateCommand(invalidPriority));
        
        // Test 'run' command with invalid priority (negative)
        String[] negativePriority = {"run", "job1", "10", "-2"};
        assertNotNull(invokeValidateCommand(negativePriority));
    }
    
    @Test
    public void testValidateCommand_NoArgumentCommands() throws Exception {
        // Test commands that should have no arguments
        String[][] validCommands = {
            {"fcfs"},
            {"sjf"},
            {"priority"},
            {"list"},
            {"version"},
            {"status"},
            {"checklocks"},
            {"quit"},
            {"exit"}
        };
        
        for (String[] cmd : validCommands) {
            assertNull(invokeValidateCommand(cmd), "Command " + cmd[0] + " should be valid with no arguments");
            
            // Test with extra arguments (should be invalid)
            String[] withExtraArg = {cmd[0], "extraArg"};
            assertNotNull(invokeValidateCommand(withExtraArg), "Command " + cmd[0] + " should be invalid with arguments");
        }
    }
    
    @Test
    public void testValidateCommand_HelpCommand() throws Exception {
        // Test help command with no arguments
        String[] helpNoArgs = {"help"};
        assertNull(invokeValidateCommand(helpNoArgs));
        
        // Test help command with one argument (valid)
        String[] helpOneArg = {"help", "-run"};
        assertNull(invokeValidateCommand(helpOneArg));
        
        // Test help command with argument not starting with '-' (invalid)
        String[] helpInvalidArg = {"help", "run"};
        assertNotNull(invokeValidateCommand(helpInvalidArg));
        
        // Test help command with too many arguments
        String[] helpTooManyArgs = {"help", "-run", "extra"};
        assertNotNull(invokeValidateCommand(helpTooManyArgs));
    }
    
    @Test
    public void testValidateCommand_LoadCommand() throws Exception {
        // Test load command with filename (valid)
        String[] loadValid = {"load", "jobs.txt"};
        assertNull(invokeValidateCommand(loadValid));
        
        // Test load command with no filename (invalid)
        String[] loadNoFilename = {"load"};
        assertNotNull(invokeValidateCommand(loadNoFilename));
    }
    
    @Test
    public void testValidateCommand_TestCommand() throws Exception {
        // Test test command with no arguments (valid)
        String[] testNoArgs = {"test"};
        assertNull(invokeValidateCommand(testNoArgs));
        
        // Test test command with all arguments (valid)
        String[] testAllArgs = {"test", "benchmark1", "sjf", "10", "3", "5", "20"};
        assertNull(invokeValidateCommand(testAllArgs));
        
        // Test test command with some arguments but not all (invalid)
        String[] testSomeArgs = {"test", "benchmark1", "sjf", "10"};
        assertNotNull(invokeValidateCommand(testSomeArgs));
    }
    
    @Test
    public void testValidateCommand_UnknownCommand() throws Exception {
        // Test unknown command
        String[] unknownCmd = {"unknownCommand"};
        assertNotNull(invokeValidateCommand(unknownCmd));
    }
} 