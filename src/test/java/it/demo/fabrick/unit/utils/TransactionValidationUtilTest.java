package it.demo.fabrick.unit.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import it.demo.fabrick.dto.ListaTransactionDto;
import it.demo.fabrick.utils.MessageParserUtil;
import it.demo.fabrick.utils.TransactionValidationUtil;

/**
 * Unit tests for TransactionValidationUtil and MessageParserUtil validation.
 * Tests the validation enquiry logic for finding matching money transfers and input validation.
 */
@DisplayName("TransactionValidationUtil and Input Validation Tests")
class TransactionValidationUtilTest {

	// ==================== findMatchingTransaction Tests ====================

	@Test
	@DisplayName("findMatchingTransaction - should find exact match")
	void testFindMatchingTransaction_exactMatch() {
		// Arrange
		List<ListaTransactionDto> transactions = createTransactionList(
				"100.00", "EUR", "Test payment", "txn-001"
		);

		// Act
		ListaTransactionDto result = TransactionValidationUtil.findMatchingTransaction(
				transactions, new BigDecimal("100.00"), "EUR", "Test payment", "John Doe"
		);

		// Assert
		assertNotNull(result, "Should find matching transaction");
		assertEquals("txn-001", result.getTransactionId());
	}

	@Test
	@DisplayName("findMatchingTransaction - should return null when amount differs")
	void testFindMatchingTransaction_amountDiffers() {
		// Arrange
		List<ListaTransactionDto> transactions = createTransactionList(
				"100.00", "EUR", "Test payment", "txn-001"
		);

		// Act
		ListaTransactionDto result = TransactionValidationUtil.findMatchingTransaction(
				transactions, new BigDecimal("200.00"), "EUR", "Test payment", "John Doe"
		);

		// Assert
		assertNull(result, "Should not find transaction with different amount");
	}

	@Test
	@DisplayName("findMatchingTransaction - should return null when currency differs")
	void testFindMatchingTransaction_currencyDiffers() {
		// Arrange
		List<ListaTransactionDto> transactions = createTransactionList(
				"100.00", "EUR", "Test payment", "txn-001"
		);

		// Act
		ListaTransactionDto result = TransactionValidationUtil.findMatchingTransaction(
				transactions, new BigDecimal("100.00"), "USD", "Test payment", "John Doe"
		);

		// Assert
		assertNull(result, "Should not find transaction with different currency");
	}

	@Test
	@DisplayName("findMatchingTransaction - should return null when description differs")
	void testFindMatchingTransaction_descriptionDiffers() {
		// Arrange
		List<ListaTransactionDto> transactions = createTransactionList(
				"100.00", "EUR", "Test payment", "txn-001"
		);

		// Act
		ListaTransactionDto result = TransactionValidationUtil.findMatchingTransaction(
				transactions, new BigDecimal("100.00"), "EUR", "Different payment", "John Doe"
		);

		// Assert
		assertNull(result, "Should not find transaction with different description");
	}

	@Test
	@DisplayName("findMatchingTransaction - should return null for empty list")
	void testFindMatchingTransaction_emptyList() {
		// Arrange
		List<ListaTransactionDto> transactions = new ArrayList<>();

		// Act
		ListaTransactionDto result = TransactionValidationUtil.findMatchingTransaction(
				transactions, new BigDecimal("100.00"), "EUR", "Test payment", "John Doe"
		);

		// Assert
		assertNull(result, "Should not find transaction in empty list");
	}

	@Test
	@DisplayName("findMatchingTransaction - should find correct transaction in multiple")
	void testFindMatchingTransaction_multipleTransactions() {
		// Arrange
		List<ListaTransactionDto> transactions = new ArrayList<>();
		transactions.add(createTransaction("50.00", "EUR", "Payment 1", "txn-001"));
		transactions.add(createTransaction("100.00", "EUR", "Test payment", "txn-002"));
		transactions.add(createTransaction("150.00", "EUR", "Payment 3", "txn-003"));

		// Act
		ListaTransactionDto result = TransactionValidationUtil.findMatchingTransaction(
				transactions, new BigDecimal("100.00"), "EUR", "Test payment", "John Doe"
		);

		// Assert
		assertNotNull(result, "Should find matching transaction");
		assertEquals("txn-002", result.getTransactionId());
	}

	@Test
	@DisplayName("findMatchingTransaction - should handle decimal amount with US locale format")
	void testFindMatchingTransaction_usLocaleAmount() {
		// Arrange - Create transaction with amount 1234.56
		List<ListaTransactionDto> transactions = new ArrayList<>();
		ListaTransactionDto transaction = new ListaTransactionDto();
		transaction.setTransactionId("txn-001");
		transaction.setAmount(new BigDecimal("1234.56"));
		transaction.setCurrency("EUR");
		transaction.setDescription("Test payment");
		transactions.add(transaction);

		// Act - Search with US locale format (decimal point, no thousands separator)
		ListaTransactionDto result = TransactionValidationUtil.findMatchingTransaction(
				transactions, new BigDecimal("1234.56"), "EUR", "Test payment", "John Doe"
		);

		// Assert
		assertNotNull(result, "Should find matching transaction with BigDecimal amount");
		assertEquals("txn-001", result.getTransactionId());
	}

	@Test
	@DisplayName("findMatchingTransaction - should handle different BigDecimal scales")
	void testFindMatchingTransaction_differentScales() {
		// Arrange - Test that BigDecimal comparison handles different scales correctly
		List<ListaTransactionDto> transactions = new ArrayList<>();
		ListaTransactionDto transaction = new ListaTransactionDto();
		transaction.setTransactionId("txn-001");
		transaction.setAmount(new BigDecimal("100.00")); // scale 2
		transaction.setCurrency("EUR");
		transaction.setDescription("Test payment");
		transactions.add(transaction);

		// Act - Search with amount as "100.0" (scale 1)
		ListaTransactionDto result = TransactionValidationUtil.findMatchingTransaction(
				transactions, new BigDecimal("100.0"), "EUR", "Test payment", "John Doe"
		);

		// Assert
		assertNotNull(result, "Should find matching transaction with different BigDecimal scale");
		assertEquals("txn-001", result.getTransactionId());
	}

	@Test
	@DisplayName("findMatchingTransaction - should return null when transaction amount is null")
	void testFindMatchingTransaction_nullTransactionAmount() {
		// Arrange
		List<ListaTransactionDto> transactions = new ArrayList<>();
		ListaTransactionDto transaction = new ListaTransactionDto();
		transaction.setTransactionId("txn-001");
		transaction.setAmount(null); // null amount
		transaction.setCurrency("EUR");
		transaction.setDescription("Test payment");
		transactions.add(transaction);

		// Act
		ListaTransactionDto result = TransactionValidationUtil.findMatchingTransaction(
				transactions, new BigDecimal("100.00"), "EUR", "Test payment", "John Doe"
		);

		// Assert
		assertNull(result, "Should not match when transaction amount is null");
	}

	@Test
	@DisplayName("findMatchingTransaction - should match when description is null")
	void testFindMatchingTransaction_nullDescription() {
		// Arrange
		List<ListaTransactionDto> transactions = new ArrayList<>();
		ListaTransactionDto txn = createTransaction("100.00", "EUR", null, "txn-001");
		transactions.add(txn);

		// Act - searching with null description should not match
		ListaTransactionDto result = TransactionValidationUtil.findMatchingTransaction(
				transactions, new BigDecimal("100.00"), "EUR", "Test payment", "John Doe"
		);

		// Assert
		assertNull(result, "Should not match when transaction description is null");
	}

	// ==================== Input Validation Tests ====================

	@Test
	@DisplayName("validateMoneyTransferRequest - should pass with valid input")
	void testValidateMoneyTransferRequest_validInput() {
		// Arrange
		BigDecimal amount = new BigDecimal("100.50");
		String currency = "EUR";
		String creditorName = "John Doe";
		String description = "Payment for services";

		// Act & Assert - should not throw
		assertDoesNotThrow(() ->
			MessageParserUtil.validateMoneyTransferRequest(amount, currency, creditorName, description)
		);
	}

	@Test
	@DisplayName("validateMoneyTransferRequest - should fail when amount is null")
	void testValidateMoneyTransferRequest_nullAmount() {
		// Arrange
		String currency = "EUR";
		String creditorName = "John Doe";
		String description = "Payment for services";

		// Act & Assert
		Exception exception = assertThrows(IllegalArgumentException.class, () ->
			MessageParserUtil.validateMoneyTransferRequest(null, currency, creditorName, description)
		);
		assertEquals("Amount is required", exception.getMessage());
	}

	@Test
	@DisplayName("validateMoneyTransferRequest - should fail when amount is too small")
	void testValidateMoneyTransferRequest_amountTooSmall() {
		// Arrange
		BigDecimal amount = new BigDecimal("0.001");
		String currency = "EUR";
		String creditorName = "John Doe";
		String description = "Payment for services";

		// Act & Assert
		Exception exception = assertThrows(IllegalArgumentException.class, () ->
			MessageParserUtil.validateMoneyTransferRequest(amount, currency, creditorName, description)
		);
		assertEquals("Amount must be at least 0.01 (was: 0.001)", exception.getMessage());
	}

	@Test
	@DisplayName("validateMoneyTransferRequest - should fail when amount is too large")
	void testValidateMoneyTransferRequest_amountTooLarge() {
		// Arrange
		BigDecimal amount = new BigDecimal("1000000000.00");
		String currency = "EUR";
		String creditorName = "John Doe";
		String description = "Payment for services";

		// Act & Assert
		Exception exception = assertThrows(IllegalArgumentException.class, () ->
			MessageParserUtil.validateMoneyTransferRequest(amount, currency, creditorName, description)
		);
		assertTrue(exception.getMessage().contains("Amount exceeds maximum"),
			"Error message should mention maximum amount limit");
	}

	@Test
	@DisplayName("validateMoneyTransferRequest - should fail when currency is null")
	void testValidateMoneyTransferRequest_nullCurrency() {
		// Arrange
		BigDecimal amount = new BigDecimal("100.00");
		String creditorName = "John Doe";
		String description = "Payment for services";

		// Act & Assert
		Exception exception = assertThrows(IllegalArgumentException.class, () ->
			MessageParserUtil.validateMoneyTransferRequest(amount, null, creditorName, description)
		);
		assertEquals("Currency must be a valid ISO 4217 code (3 letters)", exception.getMessage());
	}

	@Test
	@DisplayName("validateMoneyTransferRequest - should fail when currency is wrong length")
	void testValidateMoneyTransferRequest_currencyWrongLength() {
		// Arrange
		BigDecimal amount = new BigDecimal("100.00");
		String creditorName = "John Doe";
		String description = "Payment for services";

		// Act & Assert
		Exception exception = assertThrows(IllegalArgumentException.class, () ->
			MessageParserUtil.validateMoneyTransferRequest(amount, "US", creditorName, description)
		);
		assertEquals("Currency must be a valid ISO 4217 code (3 letters)", exception.getMessage());
	}

	@Test
	@DisplayName("validateMoneyTransferRequest - should fail when currency has lowercase letters")
	void testValidateMoneyTransferRequest_currencyLowercase() {
		// Arrange
		BigDecimal amount = new BigDecimal("100.00");
		String creditorName = "John Doe";
		String description = "Payment for services";

		// Act & Assert
		Exception exception = assertThrows(IllegalArgumentException.class, () ->
			MessageParserUtil.validateMoneyTransferRequest(amount, "eur", creditorName, description)
		);
		assertEquals("Currency must contain only uppercase letters (ISO 4217 format)", exception.getMessage());
	}

	@Test
	@DisplayName("validateMoneyTransferRequest - should fail when creditor name is null")
	void testValidateMoneyTransferRequest_nullCreditorName() {
		// Arrange
		BigDecimal amount = new BigDecimal("100.00");
		String currency = "EUR";
		String description = "Payment for services";

		// Act & Assert
		Exception exception = assertThrows(IllegalArgumentException.class, () ->
			MessageParserUtil.validateMoneyTransferRequest(amount, currency, null, description)
		);
		assertEquals("Creditor name is required", exception.getMessage());
	}

	@Test
	@DisplayName("validateMoneyTransferRequest - should fail when creditor name is empty")
	void testValidateMoneyTransferRequest_emptyCreditorName() {
		// Arrange
		BigDecimal amount = new BigDecimal("100.00");
		String currency = "EUR";
		String description = "Payment for services";

		// Act & Assert
		Exception exception = assertThrows(IllegalArgumentException.class, () ->
			MessageParserUtil.validateMoneyTransferRequest(amount, currency, "   ", description)
		);
		assertEquals("Creditor name is required", exception.getMessage());
	}

	@Test
	@DisplayName("validateMoneyTransferRequest - should fail when creditor name is too long")
	void testValidateMoneyTransferRequest_creditorNameTooLong() {
		// Arrange
		BigDecimal amount = new BigDecimal("100.00");
		String currency = "EUR";
		String creditorName = "A".repeat(141); // 141 characters
		String description = "Payment for services";

		// Act & Assert
		Exception exception = assertThrows(IllegalArgumentException.class, () ->
			MessageParserUtil.validateMoneyTransferRequest(amount, currency, creditorName, description)
		);
		assertEquals("Creditor name exceeds maximum length of 140 characters", exception.getMessage());
	}

	@Test
	@DisplayName("validateMoneyTransferRequest - should fail when description is null")
	void testValidateMoneyTransferRequest_nullDescription() {
		// Arrange
		BigDecimal amount = new BigDecimal("100.00");
		String currency = "EUR";
		String creditorName = "John Doe";

		// Act & Assert
		Exception exception = assertThrows(IllegalArgumentException.class, () ->
			MessageParserUtil.validateMoneyTransferRequest(amount, currency, creditorName, null)
		);
		assertEquals("Description is required", exception.getMessage());
	}

	@Test
	@DisplayName("validateMoneyTransferRequest - should fail when description is empty")
	void testValidateMoneyTransferRequest_emptyDescription() {
		// Arrange
		BigDecimal amount = new BigDecimal("100.00");
		String currency = "EUR";
		String creditorName = "John Doe";

		// Act & Assert
		Exception exception = assertThrows(IllegalArgumentException.class, () ->
			MessageParserUtil.validateMoneyTransferRequest(amount, currency, creditorName, "")
		);
		assertEquals("Description is required", exception.getMessage());
	}

	@Test
	@DisplayName("validateMoneyTransferRequest - should fail when description is too long")
	void testValidateMoneyTransferRequest_descriptionTooLong() {
		// Arrange
		BigDecimal amount = new BigDecimal("100.00");
		String currency = "EUR";
		String creditorName = "John Doe";
		String description = "A".repeat(501); // 501 characters

		// Act & Assert
		Exception exception = assertThrows(IllegalArgumentException.class, () ->
			MessageParserUtil.validateMoneyTransferRequest(amount, currency, creditorName, description)
		);
		assertEquals("Description exceeds maximum length of 500 characters", exception.getMessage());
	}

	// ==================== Helper Methods ====================

	/**
	 * Create a list with a single transaction.
	 */
	private List<ListaTransactionDto> createTransactionList(String amount, String currency, String description, String txnId) {
		List<ListaTransactionDto> list = new ArrayList<>();
		list.add(createTransaction(amount, currency, description, txnId));
		return list;
	}

	/**
	 * Create a test transaction with the specified values.
	 */
	private ListaTransactionDto createTransaction(String amount, String currency, String description, String txnId) {
		ListaTransactionDto transaction = new ListaTransactionDto();
		transaction.setTransactionId(txnId);
		transaction.setAmount(new BigDecimal(amount));
		transaction.setCurrency(currency);
		transaction.setDescription(description);
		transaction.setAccountingDate("2025-01-15");
		transaction.setValueDate("2025-01-15");
		return transaction;
	}
}
