package com.writesmith.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Comparator;

/**
 * Session-based persistent logger optimized for AI analysis.
 * 
 * Structure:
 *   logs/
 *     session_2026-01-04_12-34-56/
 *       session.log     - EVERYTHING in one file, timestamped with component tags
 *       errors.log      - Just errors (subset for quick scanning)
 *       requests/       - Detailed per-request logs (verbose streaming data)
 *         openrouter_12-35-00_user123.log
 * 
 * Log format: [HH:mm:ss.SSS] [COMPONENT] [LEVEL] message
 * This makes it easy for AI to parse and understand context.
 */
public class PersistentLogger {

    private static final String LOG_ROOT = "logs";
    private static final int MAX_SESSIONS_TO_KEEP = 1000;
    private static final DateTimeFormatter SESSION_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");
    private static final DateTimeFormatter FULL_TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    private static final ObjectMapper objectMapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    // Component name constants
    public static final String SERVER = "SERVER";
    public static final String CONSOLE = "CONSOLE";
    public static final String IMAGE = "IMAGE";
    public static final String OPENROUTER = "OPENROUTER";
    public static final String DATABASE = "DATABASE";
    public static final String AUTH = "AUTH";
    public static final String WEBSOCKET = "WEBSOCKET";
    public static final String APPLE = "APPLE";
    public static final String SPEECH = "SPEECH";
    public static final String TRANSACTION = "TRANSACTION";
    public static final String API = "API";
    public static final String REALTIME = "REALTIME";

    // Session state
    private static String sessionFolder;
    private static String requestsFolder;
    private static PrintWriter sessionWriter;
    private static PrintWriter errorsWriter;
    private static final Object writeLock = new Object();
    private static boolean initialized = false;

    // Original streams for passthrough
    private static PrintStream originalOut;
    private static PrintStream originalErr;

    /**
     * Initializes the persistent logger for a new session.
     * Creates a session folder and sets up log files.
     * Should be called once at application startup.
     */
    public static synchronized void initialize() {
        if (initialized) {
            return;
        }

        try {
            // Store original streams
            originalOut = System.out;
            originalErr = System.err;

            // Create session folder
            String sessionId = LocalDateTime.now().format(SESSION_FORMAT);
            sessionFolder = LOG_ROOT + "/session_" + sessionId;
            requestsFolder = sessionFolder + "/requests";

            Path sessionPath = Paths.get(sessionFolder);
            Path requestsPath = Paths.get(requestsFolder);
            Files.createDirectories(sessionPath);
            Files.createDirectories(requestsPath);

            // Open log files
            sessionWriter = new PrintWriter(new FileWriter(sessionFolder + "/session.log"), true);
            errorsWriter = new PrintWriter(new FileWriter(sessionFolder + "/errors.log"), true);

            // Write session header
            writeSessionHeader();

            // Intercept System.out
            System.setOut(new PrintStream(new OutputStream() {
                private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();

                @Override
                public void write(int b) throws IOException {
                    buffer.write(b);
                    if (b == '\n') {
                        String line = buffer.toString();
                        buffer.reset();
                        String trimmed = line.trim();
                        if (!trimmed.isEmpty()) {
                            writeLog(CONSOLE, "OUT", trimmed);
                        }
                        originalOut.print(line);
                    }
                }

                @Override
                public void flush() throws IOException {
                    if (buffer.size() > 0) {
                        String line = buffer.toString();
                        buffer.reset();
                        if (!line.trim().isEmpty()) {
                            writeLog(CONSOLE, "OUT", line.trim());
                            originalOut.print(line);
                        }
                    }
                    originalOut.flush();
                }
            }, true));

            // Intercept System.err
            System.setErr(new PrintStream(new OutputStream() {
                private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();

                @Override
                public void write(int b) throws IOException {
                    buffer.write(b);
                    if (b == '\n') {
                        String line = buffer.toString();
                        buffer.reset();
                        String trimmed = line.trim();
                        if (!trimmed.isEmpty()) {
                            writeLog(CONSOLE, "ERR", trimmed);
                            writeError(CONSOLE, "ERR", trimmed);
                        }
                        originalErr.print(line);
                    }
                }

                @Override
                public void flush() throws IOException {
                    if (buffer.size() > 0) {
                        String line = buffer.toString();
                        buffer.reset();
                        if (!line.trim().isEmpty()) {
                            writeLog(CONSOLE, "ERR", line.trim());
                            writeError(CONSOLE, "ERR", line.trim());
                            originalErr.print(line);
                        }
                    }
                    originalErr.flush();
                }
            }, true));

            initialized = true;
            info(SERVER, "Session started: " + sessionFolder);
            info(SERVER, "Log format: [TIME] [COMPONENT] [LEVEL] message");

            // Clean up old sessions
            cleanupOldSessions();

        } catch (Exception e) {
            if (originalErr != null) {
                originalErr.println("[PersistentLogger] Failed to initialize: " + e.getMessage());
                e.printStackTrace(originalErr);
            } else {
                e.printStackTrace();
            }
        }
    }

    private static void writeSessionHeader() {
        String startTime = LocalDateTime.now().format(FULL_TIMESTAMP_FORMAT);
        sessionWriter.println("════════════════════════════════════════════════════════════════════════════════");
        sessionWriter.println("SESSION LOG");
        sessionWriter.println("════════════════════════════════════════════════════════════════════════════════");
        sessionWriter.println("Started:  " + startTime);
        sessionWriter.println("Folder:   " + sessionFolder);
        sessionWriter.println("Format:   [TIME] [COMPONENT] [LEVEL] message");
        sessionWriter.println("════════════════════════════════════════════════════════════════════════════════");
        sessionWriter.println();

        errorsWriter.println("════════════════════════════════════════════════════════════════════════════════");
        errorsWriter.println("ERRORS LOG (subset of session.log)");
        errorsWriter.println("════════════════════════════════════════════════════════════════════════════════");
        errorsWriter.println("Started:  " + startTime);
        errorsWriter.println("════════════════════════════════════════════════════════════════════════════════");
        errorsWriter.println();
    }

    /**
     * Writes a log entry to session.log
     */
    private static void writeLog(String component, String level, String message) {
        if (sessionWriter == null) return;
        
        synchronized (writeLock) {
            String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
            String logLine = "[" + timestamp + "] [" + component + "] [" + level + "] " + message;
            sessionWriter.println(logLine);
        }
    }

    /**
     * Writes an error entry to errors.log
     */
    private static void writeError(String component, String level, String message) {
        if (errorsWriter == null) return;
        
        synchronized (writeLock) {
            String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
            String logLine = "[" + timestamp + "] [" + component + "] [" + level + "] " + message;
            errorsWriter.println(logLine);
        }
    }

    // ════════════════════════════════════════════════════════════════════════════════
    // PUBLIC LOGGING METHODS
    // ════════════════════════════════════════════════════════════════════════════════

    /**
     * Logs a message with component and level.
     */
    public static void log(String component, String level, String message) {
        writeLog(component, level, message);
        
        if ("ERROR".equals(level) || "ERR".equals(level)) {
            writeError(component, level, message);
        }
    }

    /**
     * Logs an info message.
     */
    public static void info(String component, String message) {
        log(component, "INFO", message);
    }

    /**
     * Logs a warning message.
     */
    public static void warn(String component, String message) {
        log(component, "WARN", message);
    }

    /**
     * Logs an error message.
     */
    public static void error(String component, String message) {
        log(component, "ERROR", message);
    }

    /**
     * Logs an error with full exception details.
     */
    public static void error(String component, String message, Throwable t) {
        StringBuilder sb = new StringBuilder();
        sb.append(message);
        sb.append(" | Exception: ").append(t.getClass().getName());
        sb.append(" | Message: ").append(t.getMessage());
        
        log(component, "ERROR", sb.toString());
        
        // Write full stack trace
        StringBuilder stackTrace = new StringBuilder();
        stackTrace.append("Stack trace for above error:");
        for (StackTraceElement element : t.getStackTrace()) {
            stackTrace.append("\n    at ").append(element.toString());
        }
        
        Throwable cause = t.getCause();
        while (cause != null) {
            stackTrace.append("\nCaused by: ").append(cause.getClass().getName()).append(": ").append(cause.getMessage());
            for (StackTraceElement element : cause.getStackTrace()) {
                stackTrace.append("\n    at ").append(element.toString());
            }
            cause = cause.getCause();
        }
        
        writeLog(component, "TRACE", stackTrace.toString());
        writeError(component, "TRACE", stackTrace.toString());
    }

    /**
     * Logs a debug message.
     */
    public static void debug(String component, String message) {
        log(component, "DEBUG", message);
    }

    /**
     * Logs an object as JSON.
     */
    public static void logJson(String component, String label, Object obj) {
        try {
            String json = objectMapper.writeValueAsString(obj);
            log(component, "JSON", label + ":\n" + json);
        } catch (Exception e) {
            log(component, "ERROR", label + ": Failed to serialize - " + e.getMessage());
        }
    }

    /**
     * Logs a detailed error with request/response context.
     * Useful for API debugging.
     */
    public static void logDetailedError(String component, String operation, 
                                         Object request, Object response, 
                                         Throwable error) {
        StringBuilder sb = new StringBuilder();
        sb.append("\n════════════════════════════════════════════════════════════════════════════════\n");
        sb.append("DETAILED ERROR: ").append(operation).append("\n");
        sb.append("════════════════════════════════════════════════════════════════════════════════\n");
        
        // Request
        sb.append("REQUEST:\n");
        if (request != null) {
            try {
                sb.append(objectMapper.writeValueAsString(request));
            } catch (Exception e) {
                sb.append("(serialization failed: ").append(e.getMessage()).append(")");
            }
        } else {
            sb.append("null");
        }
        
        // Response
        sb.append("\n\nRESPONSE:\n");
        if (response != null) {
            try {
                sb.append(objectMapper.writeValueAsString(response));
            } catch (Exception e) {
                sb.append("(serialization failed: ").append(e.getMessage()).append(")");
            }
        } else {
            sb.append("null");
        }
        
        // Error
        sb.append("\n\nERROR: ").append(error.getClass().getName()).append(": ").append(error.getMessage());
        sb.append("\nStack trace:");
        for (StackTraceElement element : error.getStackTrace()) {
            sb.append("\n    at ").append(element.toString());
        }
        
        if (error.getCause() != null) {
            sb.append("\nCaused by: ").append(error.getCause().getClass().getName())
              .append(": ").append(error.getCause().getMessage());
        }
        
        sb.append("\n════════════════════════════════════════════════════════════════════════════════");
        
        log(component, "ERROR", sb.toString());
    }

    // ════════════════════════════════════════════════════════════════════════════════
    // SESSION MANAGEMENT
    // ════════════════════════════════════════════════════════════════════════════════

    /**
     * Gets the current session's requests folder path.
     * Used by OpenRouterRequestLogger to store detailed request logs.
     */
    public static String getRequestsFolder() {
        return requestsFolder;
    }

    /**
     * Gets the current session folder path.
     */
    public static String getSessionFolder() {
        return sessionFolder;
    }

    /**
     * Checks if the logger has been initialized.
     */
    public static boolean isInitialized() {
        return initialized;
    }

    /**
     * Cleans up old session folders, keeping only the most recent.
     */
    private static void cleanupOldSessions() {
        try {
            File logRoot = new File(LOG_ROOT);
            if (!logRoot.exists() || !logRoot.isDirectory()) {
                return;
            }

            File[] sessionFolders = logRoot.listFiles(file -> 
                file.isDirectory() && file.getName().startsWith("session_")
            );

            if (sessionFolders == null || sessionFolders.length <= MAX_SESSIONS_TO_KEEP) {
                return;
            }

            // Sort by name (includes timestamp) - oldest first
            Arrays.sort(sessionFolders, Comparator.comparing(File::getName));

            // Delete oldest folders
            int foldersToDelete = sessionFolders.length - MAX_SESSIONS_TO_KEEP;
            for (int i = 0; i < foldersToDelete; i++) {
                File oldFolder = sessionFolders[i];
                deleteDirectory(oldFolder);
                info(SERVER, "Deleted old session: " + oldFolder.getName());
            }
        } catch (Exception e) {
            if (originalErr != null) {
                originalErr.println("[PersistentLogger] Error cleaning up old sessions: " + e.getMessage());
            }
        }
    }

    /**
     * Recursively deletes a directory.
     */
    private static void deleteDirectory(File directory) {
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    deleteDirectory(file);
                } else {
                    file.delete();
                }
            }
        }
        directory.delete();
    }

    /**
     * Shuts down the logger gracefully.
     */
    public static synchronized void shutdown() {
        if (!initialized) {
            return;
        }

        try {
            info(SERVER, "Shutting down logger...");
            
            String endTime = LocalDateTime.now().format(FULL_TIMESTAMP_FORMAT);
            
            synchronized (writeLock) {
                if (sessionWriter != null) {
                    sessionWriter.println();
                    sessionWriter.println("════════════════════════════════════════════════════════════════════════════════");
                    sessionWriter.println("SESSION ENDED: " + endTime);
                    sessionWriter.println("════════════════════════════════════════════════════════════════════════════════");
                    sessionWriter.close();
                    sessionWriter = null;
                }
                
                if (errorsWriter != null) {
                    errorsWriter.println();
                    errorsWriter.println("════════════════════════════════════════════════════════════════════════════════");
                    errorsWriter.println("SESSION ENDED: " + endTime);
                    errorsWriter.println("════════════════════════════════════════════════════════════════════════════════");
                    errorsWriter.close();
                    errorsWriter = null;
                }
            }

            // Restore original streams
            if (originalOut != null) {
                System.setOut(originalOut);
            }
            if (originalErr != null) {
                System.setErr(originalErr);
            }

            initialized = false;
        } catch (Exception e) {
            if (originalErr != null) {
                originalErr.println("[PersistentLogger] Error during shutdown: " + e.getMessage());
            }
        }
    }
}
