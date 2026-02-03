package it.demo.fabrick.unit.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import it.demo.fabrick.dto.ListaTransactionDto;
import it.demo.fabrick.utils.TransactionValidationUtil;

/**
 * Unit tests for TransactionValidationUtil.
 * Tests the validation enquiry logic for finding matching money transfers.
 */
@DisplayName("TransactionValidationUtil Tests")
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
				transactions, "100.00", "EUR", "Test payment", "John Doe"
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
				transactions, "200.00", "EUR", "Test payment", "John Doe"
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
				transactions, "100.00", "USD", "Test payment", "John Doe"
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
				transactions, "100.00", "EUR", "Different payment", "John Doe"
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
				transactions, "100.00", "EUR", "Test payment", "John Doe"
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
				transactions, "100.00", "EUR", "Test payment", "John Doe"
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
		transaction.setAmount(1234.56);
		transaction.setCurrency("EUR");
		transaction.setDescription("Test payment");
		transactions.add(transaction);

		// Act - Search with US locale format (decimal point, no thousands separator)
		ListaTransactionDto result = TransactionValidationUtil.findMatchingTransaction(
				transactions, "1234.56", "EUR", "Test payment", "John Doe"
		);

		// Assert
		assertNotNull(result, "Should find matching transaction with US locale amount");
		assertEquals("txn-001", result.getTransactionId());
	}

	@Test
	@DisplayName("findMatchingTransaction - should return null for invalid amount format")
	void testFindMatchingTransaction_invalidAmountFormat() {
		// Arrange
		List<ListaTransactionDto> transactions = createTransactionList(
				"100.00", "EUR", "Test payment", "txn-001"
		);

		// Act - Invalid amount format
		ListaTransactionDto result = TransactionValidationUtil.findMatchingTransaction(
				transactions, "invalid", "EUR", "Test payment", "John Doe"
		);

		// Assert
		assertNull(result, "Should return null for invalid amount format");
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
				transactions, "100.00", "EUR", "Test payment", "John Doe"
		);

		// Assert
		assertNull(result, "Should not match when transaction description is null");
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
		transaction.setAmount(Double.parseDouble(amount));
		transaction.setCurrency(currency);
		transaction.setDescription(description);
		transaction.setAccountingDate("2025-01-15");
		transaction.setValueDate("2025-01-15");
		return transaction;
	}
}
