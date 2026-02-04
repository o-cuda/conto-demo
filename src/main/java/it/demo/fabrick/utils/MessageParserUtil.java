package it.demo.fabrick.utils;

import it.demo.fabrick.exception.ExceptionMessageIn;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class for message parsing, URL parameter substitution, and input validation.
 * Extracted from GestisciRequestVerticle for better testability.
 */
public class MessageParserUtil {

	private static final String REGEX_URL_PARAMETER = "(\\{.+?\\})";

	// Constants for message parsing patterns
	public static final String PATTERN_NULLIFEMPTY = "^NULLIFEMTPY.*";
	public static final String PATTERN_NOTRIM = "^NOTRIM.*";
	public static final String PATTERN_THREE_LETTER_CODE = "^[A-Z]{3}.*";

	// Constants for operation codes
	public static final String OPERATION_LIS = "LIS";
	public static final String OPERATION_SAL = "SAL";
	public static final String OPERATION_BON = "BON";

	// Validation constants
	public static final BigDecimal MIN_TRANSFER_AMOUNT = new BigDecimal("0.01");
	public static final BigDecimal MAX_TRANSFER_AMOUNT = new BigDecimal("999999999.99");
	public static final int MAX_DESCRIPTION_LENGTH = 500;
	public static final int MAX_CREDITOR_NAME_LENGTH = 140;

	/**
	 * Parse configuration string into key-value map.
	 * Format: "key1=value1;key2=value2"
	 *
	 * @param configurazione configuration string in semicolon-separated key=value format
	 * @return map of configuration keys to values
	 */
	public static Map<String, String> parseConfiguration(String configurazione) {

		Map<String, String> mappaConfigurazione = new LinkedHashMap<>();

		String[] split = configurazione.split(";");
		for (String configurazineDiUnCampo : split) {

			String[] split2 = configurazineDiUnCampo.split("=");

			mappaConfigurazione.put(split2[0], split2[1]);
		}

		return mappaConfigurazione;
	}

	/**
	 * Decode input message based on configuration map.
	 * Supports multiple parsing patterns:
	 * - NULLIFEMTPY<N>: Takes N characters, converts empty string to null
	 * - NOTRIM<N>: Takes N characters without trimming whitespace
	 * - <AAA><N>: Takes N characters, removes leading zeros (where AAA is 3-letter code)
	 * - <N>: Takes N characters and trims whitespace (default)
	 *
	 * @param messageInInput the raw input message string
	 * @param creaConfigurazioneInput configuration map defining field parsing rules
	 * @return map of field names to parsed values
	 * @throws ExceptionMessageIn if message format doesn't match configuration
	 */
	public static Map<String, String> decodeMessage(String messageInInput, Map<String, String> creaConfigurazioneInput)
			throws ExceptionMessageIn {

		Map<String, String> mappaMessageIn = new HashMap<>();

		int start = 0;
		int end = 0;

		Set<String> keySet = creaConfigurazioneInput.keySet();
		for (String key : keySet) {

			String valore = creaConfigurazioneInput.get(key);
			String substring = null;

			try {

				if (Pattern.matches(PATTERN_NULLIFEMPTY, valore)) {

					start = end;
					end = start + Integer.valueOf(valore.substring(11));
					substring = messageInInput.substring(start, end).trim();

					if (substring.isEmpty()) {
						substring = null;
					}

				} else if (Pattern.matches(PATTERN_NOTRIM, valore)) {

					start = end;
					end = start + Integer.valueOf(valore.substring(6));
					substring = messageInInput.substring(start, end);

				} else if (Pattern.matches(PATTERN_THREE_LETTER_CODE, valore)) {

					start = end;
					end = start + Integer.valueOf(valore.substring(3));
					substring = messageInInput.substring(start, end).replaceFirst("^0+(?!$)", Constants.EMPTY_STRING)
							.trim();

				} else {

					start = end;
					end = start + Integer.valueOf(valore);
					substring = messageInInput.substring(start, end).trim();
				}

			} catch (StringIndexOutOfBoundsException e) {
				throw new ExceptionMessageIn();
			}

			mappaMessageIn.put(key, substring);
		}

		return mappaMessageIn;
	}

	/**
	 * Validate money transfer request parameters.
	 *
	 * @param amount the transfer amount
	 * @param currency the currency code (should be ISO 4217)
	 * @param creditorName the beneficiary name
	 * @param description the payment description
	 * @throws IllegalArgumentException if validation fails
	 */
	public static void validateMoneyTransferRequest(BigDecimal amount, String currency, String creditorName, String description) {
		// Validate amount
		if (amount == null) {
			throw new IllegalArgumentException("Amount is required");
		}
		if (amount.compareTo(MIN_TRANSFER_AMOUNT) < 0) {
			throw new IllegalArgumentException("Amount must be at least " + MIN_TRANSFER_AMOUNT + " (was: " + amount + ")");
		}
		if (amount.compareTo(MAX_TRANSFER_AMOUNT) > 0) {
			throw new IllegalArgumentException("Amount exceeds maximum of " + MAX_TRANSFER_AMOUNT + " (was: " + amount + ")");
		}

		// Validate currency (ISO 4217 - should be 3 letter currency code)
		if (currency == null || currency.length() != 3) {
			throw new IllegalArgumentException("Currency must be a valid ISO 4217 code (3 letters)");
		}
		if (!currency.matches("[A-Z]{3}")) {
			throw new IllegalArgumentException("Currency must contain only uppercase letters (ISO 4217 format)");
		}

		// Validate creditor name
		if (creditorName == null || creditorName.trim().isEmpty()) {
			throw new IllegalArgumentException("Creditor name is required");
		}
		if (creditorName.length() > MAX_CREDITOR_NAME_LENGTH) {
			throw new IllegalArgumentException("Creditor name exceeds maximum length of " + MAX_CREDITOR_NAME_LENGTH + " characters");
		}

		// Validate description
		if (description == null || description.trim().isEmpty()) {
			throw new IllegalArgumentException("Description is required");
		}
		if (description.length() > MAX_DESCRIPTION_LENGTH) {
			throw new IllegalArgumentException("Description exceeds maximum length of " + MAX_DESCRIPTION_LENGTH + " characters");
		}
	}

	/**
	 * Replace URL placeholders {key} with values from params map.
	 * Uses regex to find and replace parameters in URL.
	 *
	 * @param mappaMessageIn map of parameter names to values
	 * @param indirizzo URL string containing {placeholder} parameters
	 * @return URL with placeholders replaced by actual values, or original URL if result is empty
	 */
	public static String substituteUrlParameters(Map<String, String> mappaMessageIn, String indirizzo) {

		String result = null;
		StringBuffer builder = new StringBuffer();
		Matcher matcher = Pattern.compile(REGEX_URL_PARAMETER).matcher(indirizzo);
		while (matcher.find()) {

			String key = matcher.group();
			key = key.substring(1, key.length() - 1);
			String value = mappaMessageIn.get(key);

			matcher.appendReplacement(builder, value);
		}

		matcher.appendTail(builder);

		result = builder.toString();
		if (Constants.EMPTY_STRING.equals(result)) {
			result = indirizzo;
		}

		return result;
	}

	/**
	 * Pad string right to specified length.
	 *
	 * @param stringa string to pad
	 * @param lunghezza target length
	 * @return right-padded string
	 */
	public static String padRight(String stringa, int lunghezza) {
		return String.format("%-" + lunghezza + "s", stringa);
	}

	/**
	 * Pad string left to specified length.
	 *
	 * @param stringa string to pad
	 * @param lunghezza target length
	 * @return left-padded string
	 */
	public static String padLeft(String stringa, int lunghezza) {
		return String.format("%" + lunghezza + "s", stringa);
	}
}
