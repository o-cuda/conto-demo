package it.demo.fabrick.dto;

/**
 * Enumeration of semantic error codes for the application.
 * Each error code has a unique numeric identifier for easier logging and tracking.
 */
public enum ErrorCode {
    /**
     * Input validation failed (e.g., missing required fields, invalid format)
     */
    VALIDATION_ERROR(1000),

    /**
     * External API returned an error response
     */
    API_ERROR(1001),

    /**
     * Request timed out
     */
    TIMEOUT_ERROR(1002),

    /**
     * Failed to parse or decode a message
     */
    PARSE_ERROR(1003),

    /**
     * Network connectivity issue
     */
    NETWORK_ERROR(1004),

    /**
     * Configuration or setup error
     */
    CONFIGURATION_ERROR(1005),

    /**
     * Unknown or unexpected error
     */
    UNKNOWN_ERROR(1999);

    private final int code;

    ErrorCode(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
