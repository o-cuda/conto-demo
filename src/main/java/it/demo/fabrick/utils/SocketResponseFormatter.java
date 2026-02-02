package it.demo.fabrick.utils;

/**
 * Utility class for formatting socket responses.
 * Extracted from SocketServerVerticle for better testability.
 */
public class SocketResponseFormatter {

	private static final int RESPONSE_LENGTH = 500;

	/**
	 * Format success response with padding.
	 * Format: "0" + message, padded to RESPONSE_LENGTH characters.
	 *
	 * @param message the success message content
	 * @return formatted response string with prefix and padding
	 */
	public static String formatSuccessResponse(String message) {
		String stringOut = "0" + message;
		return String.format("%-" + RESPONSE_LENGTH + "s", stringOut);
	}

	/**
	 * Format error response with request ID.
	 * Format: "1[requestId] error message"
	 *
	 * @param requestId unique identifier for the request
	 * @param errorMessage the error message content
	 * @return formatted error response string
	 */
	public static String formatErrorResponse(String requestId, String errorMessage) {
		return String.format("1[%s] %s", requestId, errorMessage);
	}
}
