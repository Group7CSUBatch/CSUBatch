package com.project.core;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

/**
 * Test suite for the JobStatus enum.
 */
public class JobStatusTest {

    @Test
    public void testGetDisplayName() {
        assertEquals("Waiting", JobStatus.WAITING.getDisplayName());
        assertEquals("Selected", JobStatus.SELECTED.getDisplayName());
        assertEquals("Running", JobStatus.RUNNING.getDisplayName());
        assertEquals("Completed", JobStatus.COMPLETED.getDisplayName());
        assertEquals("Interrupted", JobStatus.INTERRUPTED.getDisplayName());
        assertEquals("Canceled", JobStatus.CANCELED.getDisplayName());
    }
    
    @Test
    public void testFromString() {
        // Test that each display name maps to the correct status
        assertEquals(JobStatus.WAITING, JobStatus.fromString("Waiting"));
        assertEquals(JobStatus.SELECTED, JobStatus.fromString("Selected"));
        assertEquals(JobStatus.RUNNING, JobStatus.fromString("Running"));
        assertEquals(JobStatus.COMPLETED, JobStatus.fromString("Completed"));
        assertEquals(JobStatus.INTERRUPTED, JobStatus.fromString("Interrupted"));
        assertEquals(JobStatus.CANCELED, JobStatus.fromString("Canceled"));
        
        // Test case insensitivity
        assertEquals(JobStatus.WAITING, JobStatus.fromString("waiting"));
        assertEquals(JobStatus.RUNNING, JobStatus.fromString("RUNNING"));
        
        // Test default behavior for unknown status
        assertEquals(JobStatus.WAITING, JobStatus.fromString("NonExistentStatus"));
        assertEquals(JobStatus.WAITING, JobStatus.fromString(null));
    }
    
    @Test
    public void testToString() {
        assertEquals("Waiting", JobStatus.WAITING.toString());
        assertEquals("Selected", JobStatus.SELECTED.toString());
        assertEquals("Running", JobStatus.RUNNING.toString());
        assertEquals("Completed", JobStatus.COMPLETED.toString());
        assertEquals("Interrupted", JobStatus.INTERRUPTED.toString());
        assertEquals("Canceled", JobStatus.CANCELED.toString());
    }
} 