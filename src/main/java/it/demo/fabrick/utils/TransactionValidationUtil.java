package it.demo.fabrick.utils;

import java.text.NumberFormat;
import java.text.ParseException;
import java.util.List;
import java.util.Locale;

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
	 * @param amountStr the transfer amount as string (US number format)
	 * @param currency the transfer currency
	 * @param description the transfer description
	 * @param creditorName the beneficiary name (logged but not used for matching since it's not in transaction response)
	 * @return the matching transaction or null if not found
	 */
	public static ListaTransactionDto findMatchingTransaction(List<ListaTransactionDto> transactions,
																String amountStr, String currency,
																String description, String creditorName) {

		log.debug("Searching through {} transactions", transactions.size());
		log.debug("Search criteria - amount: {}, currency: {}, description: {}, creditor: {}",
				amountStr, currency, description, creditorName);

		// Parse amount for comparison
		double amount;
		try {
			NumberFormat format = NumberFormat.getInstance(Locale.US);
			Number value = format.parse(amountStr);
			amount = value.doubleValue();
		} catch (ParseException e) {
			log.error("Failed to parse amount: {}", amountStr, e);
			return null;
		}

		for (ListaTransactionDto transaction : transactions) {
			log.debug("Checking transaction: amount={}, currency={}, description={}",
					transaction.getAmount(), transaction.getCurrency(), transaction.getDescription());

			// Check for matching amount, currency, and description
			if (transaction.getAmount() == amount &&
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
