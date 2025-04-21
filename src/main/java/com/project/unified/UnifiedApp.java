package com.project.unified;

/**
 * Entry point for the unified CSUbatch Scheduling System.
 * Uses the new UnifiedApplicationManager and ConsoleInterface.
 */
public final class UnifiedApp {

    /**
     * Private constructor to prevent instantiation of utility class.
     */
    private UnifiedApp() {
        // Utility class should not be instantiated
    }

    /**
     * Main method to start the application.
     *
     * @param args Command line arguments (not used)
     */
    public static void main(String[] args) {
        try {
            // Get the UnifiedApplicationManager instance
            UnifiedApplicationManager appManager = UnifiedApplicationManager.getInstance();

            // Initialize all components
            // System.out.println("Initializing CSUbatch system...");
            appManager.initialize();
            // System.out.println("All components initialized.");

            // Log information about initialization
            // System.out.println("System log file: " + 
                    // appManager.getLoggingSystem().getCurrentLogFile());

            // Start the application
            // System.out.println("Starting CSUbatch system...");
            appManager.startup();
            
            // The application will continue to run in the ConsoleInterface
            // until the user quits
            // shutdown the application when the user quits
            appManager.shutdown();
            
        } catch (IllegalArgumentException e) {
            System.err.println("Invalid argument: " + e.getMessage());
            e.printStackTrace();
        } catch (IllegalStateException e) {
            System.err.println("Illegal state: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("Unexpected error: " + e.getMessage());
            e.printStackTrace();
        }
    }
} 