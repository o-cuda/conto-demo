package it.demo.fabrick.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Structured error response DTO.
 * Contains error details including code, message, request context, and additional information.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ErrorResponse {
    /**
     * The semantic error code categorizing the error type
     */
    private ErrorCode code;

    /**
     * Human-readable error message
     */
    private String message;

    /**
     * Unique request identifier for log correlation (nullable)
     */
    private String requestId;

    /**
     * Additional error details or debugging information (nullable)
     */
    private String details;
}
