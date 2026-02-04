package it.demo.fabrick.dto;

import java.util.ArrayList;

import lombok.Data;

/**
 * DTO for Fabrick money transfer (bonifico) API response.
 * Follows the standard Fabrick response structure with status, payload, and optional errors.
 */
@Data
public class BonificoResponseDto {

	private String status;
	private ArrayList<Error> error;
	private Payload payload;

	@Data
	public static class Payload {
		/**
		 * Unique identifier for the money transfer transaction
		 */
		private String transactionId;
		private String status;
		private String description;
	}

	@Data
	public static class Error {
		private String code;
		private String description;
		private String params;
	}
}
