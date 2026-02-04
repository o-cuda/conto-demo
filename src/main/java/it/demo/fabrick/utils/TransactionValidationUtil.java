package it.demo.fabrick.utils;

import java.math.BigDecimal;
import java.util.List;

import it.demo.fabrick.dto.ListaTransactionDto;
import lombok.extern.slf4j.Slf4j;

/**
 * Utility class for transaction validation operations.
 * Extracted from BonificoVerticle for better testability.
 */
@Slf4j
public class TransactionValidationUtil {

	/**
	 * Searches the transactions list for a money transfer matching the specified criteria.
	 * Used for validation enquiry when HTTP 500/504 errors occur during money transfer.
	 *
	 * @param transactions the list of transactions to search
	 * @param amount the transfer amount as BigDecimal for precise comparison
	 * @param currency the transfer currency
	 * @param description the transfer description
	 * @param creditorName the beneficiary name (logged but not used for matching since it's not in transaction response)
	 * @return the matching transaction or null if not found
	 */
	public static ListaTransactionDto findMatchingTransaction(List<ListaTransactionDto> transactions,
																BigDecimal amount, String currency,
																String description, String creditorName) {

		log.debug("Searching through {} transactions", transactions.size());
		log.debug("Search criteria - amount: {}, currency: {}, description: {}, creditor: {}",
				amount, currency, description, creditorName);

		for (ListaTransactionDto transaction : transactions) {
			log.debug("Checking transaction: amount={}, currency={}, description={}",
					transaction.getAmount(), transaction.getCurrency(), transaction.getDescription());

			// Check for matching amount (using compareTo for BigDecimal), currency, and description
			if (transaction.getAmount() != null &&
					transaction.getAmount().compareTo(amount) == 0 &&
					currency.equals(transaction.getCurrency()) &&
					description != null && description.equals(transaction.getDescription())) {

				log.info("Found matching transaction: {}", transaction.getTransactionId());
				return transaction;
			}
		}

		log.debug("No matching transaction found");
		return null;
	}

	private TransactionValidationUtil() {
		// Utility class - prevent instantiation
	}
}
