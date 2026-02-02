package it.demo.fabrick.unit.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import it.demo.fabrick.exception.ExceptionMessageIn;
import it.demo.fabrick.utils.MessageParserUtil;

/**
 * Unit tests for MessageParserUtil.
 * Tests message parsing, configuration parsing, and URL substitution logic.
 */
@DisplayName("MessageParserUtil Tests")
class MessageParserUtilTest {

	// ==================== parseConfiguration Tests ====================

	@Test
	@DisplayName("parseConfiguration - valid input with multiple fields")
	void testParseConfiguration_validInput() {
		String config = "accountNumber=10;amount=15;currency=EUR";
		Map<String, String> result = MessageParserUtil.parseConfiguration(config);

		assertEquals(3, result.size());
		assertEquals("10", result.get("accountNumber"));
		assertEquals("15", result.get("amount"));
		assertEquals("EUR", result.get("currency"));
	}

	@Test
	@DisplayName("parseConfiguration - single field")
	void testParseConfiguration_singleField() {
		String config = "operation=5";
		Map<String, String> result = MessageParserUtil.parseConfiguration(config);

		assertEquals(1, result.size());
		assertEquals("5", result.get("operation"));
	}

	// ==================== decodeMessage Tests ====================

	@Test
	@DisplayName("decodeMessage - NULLIFEMTPY with non-empty string returns value")
	void testDecodeMessage_NULLIFEMPTY_nonEmpty() throws ExceptionMessageIn {
		String messageIn = "LIS1234567890";
		Map<String, String> config = new java.util.LinkedHashMap<>();
		config.put("operazione", "NULLIFEMTPY3");
		config.put("accountNumber", "NULLIFEMTPY10");

		Map<String, String> result = MessageParserUtil.decodeMessage(messageIn, config);

		assertEquals("LIS", result.get("operazione"));
		assertEquals("1234567890", result.get("accountNumber"));
	}

	@Test
	@DisplayName("decodeMessage - NULLIFEMTPY with all-spaces string returns null")
	void testDecodeMessage_NULLIFEMPTY_allSpaces() throws ExceptionMessageIn {
		String messageIn = "LIS          ";
		Map<String, String> config = new java.util.LinkedHashMap<>();
		config.put("operazione", "3");
		config.put("emptyField", "NULLIFEMTPY10");

		Map<String, String> result = MessageParserUtil.decodeMessage(messageIn, config);

		assertEquals("LIS", result.get("operazione"));
		assertNull(result.get("emptyField"));
	}

	@Test
	@DisplayName("decodeMessage - NOTRIM preserves whitespace")
	void testDecodeMessage_NOTRIM() throws ExceptionMessageIn {
		String messageIn = "LIS   1234567";
		Map<String, String> config = new java.util.LinkedHashMap<>();
		config.put("operazione", "3");
		config.put("accountNumber", "NOTRIM10");

		Map<String, String> result = MessageParserUtil.decodeMessage(messageIn, config);

		assertEquals("LIS", result.get("operazione"));
		assertEquals("   1234567", result.get("accountNumber"));
	}

	@Test
	@DisplayName("decodeMessage - numeric width with trimming (removes leading zeros)")
	void testDecodeMessage_numericWidth() throws ExceptionMessageIn {
		String messageIn = "LIS0000000001";
		Map<String, String> config = new java.util.LinkedHashMap<>();
		config.put("operazione", "LIS3");
		config.put("account", "LIS10");

		Map<String, String> result = MessageParserUtil.decodeMessage(messageIn, config);

		assertEquals("LIS", result.get("operazione"));
		assertEquals("1", result.get("account"));
	}

	@Test
	@DisplayName("decodeMessage - invalid input throws ExceptionMessageIn")
	void testDecodeMessage_invalidInput() {
		String messageIn = "LIS"; // Too short for config
		Map<String, String> config = new java.util.LinkedHashMap<>();
		config.put("operazione", "3");
		config.put("accountNumber", "10");

		assertThrows(ExceptionMessageIn.class, () -> {
			MessageParserUtil.decodeMessage(messageIn, config);
		});
	}

	@Test
	@DisplayName("decodeMessage - multiple fields with different types")
	void testDecodeMessage_multipleFields() throws ExceptionMessageIn {
		// Test that all field types work together
		String messageIn = "LIS1234567890  EUR";
		Map<String, String> config = new java.util.LinkedHashMap<>();
		config.put("operazione", "3");
		config.put("accountNumber", "10");
		config.put("spacer", "NOTRIM1");
		config.put("currency", "NULLIFEMTPY3");

		Map<String, String> result = MessageParserUtil.decodeMessage(messageIn, config);

		// Verify all fields are parsed correctly
		assertEquals(4, result.size());
	}

	// ==================== substituteUrlParameters Tests ====================

	@Test
	@DisplayName("substituteUrlParameters - single parameter replacement")
	void testSubstituteUrlParameters_singleParam() {
		Map<String, String> params = Map.of("accountId", "1234567890");
		String url = "https://api.example.com/accounts/{accountId}/transactions";

		String result = MessageParserUtil.substituteUrlParameters(params, url);

		assertEquals("https://api.example.com/accounts/1234567890/transactions", result);
	}

	@Test
	@DisplayName("substituteUrlParameters - multiple parameters")
	void testSubstituteUrlParameters_multipleParams() {
		Map<String, String> params = Map.of(
			"accountId", "12345",
			"fromDate", "2023-01-01",
			"toDate", "2023-12-31"
		);
		String url = "https://api.example.com/accounts/{accountId}/transactions?from={fromDate}&to={toDate}";

		String result = MessageParserUtil.substituteUrlParameters(params, url);

		assertEquals("https://api.example.com/accounts/12345/transactions?from=2023-01-01&to=2023-12-31", result);
	}

	@Test
	@DisplayName("substituteUrlParameters - no parameters in URL")
	void testSubstituteUrlParameters_noMatch() {
		Map<String, String> params = Map.of("accountId", "12345");
		String url = "https://api.example.com/accounts/list";

		String result = MessageParserUtil.substituteUrlParameters(params, url);

		assertEquals(url, result);
	}

	@Test
	@DisplayName("substituteUrlParameters - empty result returns original URL")
	void testSubstituteUrlParameters_emptyResult() {
		Map<String, String> params = Map.of("param", "");
		String url = "https://api.example.com/{param}";

		String result = MessageParserUtil.substituteUrlParameters(params, url);

		// Empty string is returned as-is when substitution results in empty
		assertEquals("https://api.example.com/", result);
	}

	// ==================== padRight Tests ====================

	@Test
	@DisplayName("padRight - pads string to specified length")
	void testPadRight() {
		String input = "test";
		String result = MessageParserUtil.padRight(input, 10);

		assertEquals(10, result.length());
		assertTrue(result.startsWith("test"));
		assertTrue(result.endsWith("      "));
	}

	@Test
	@DisplayName("padRight - longer string unchanged")
	void testPadRight_longerString() {
		String input = "this is a long string";
		String result = MessageParserUtil.padRight(input, 5);

		assertEquals(input.length(), result.length());
		assertEquals(input, result);
	}

	// ==================== padLeft Tests ====================

	@Test
	@DisplayName("padLeft - pads string to specified length")
	void testPadLeft() {
		String input = "test";
		String result = MessageParserUtil.padLeft(input, 10);

		assertEquals(10, result.length());
		assertTrue(result.endsWith("test"));
		assertTrue(result.startsWith("      "));
	}

	@Test
	@DisplayName("padLeft - longer string unchanged")
	void testPadLeft_longerString() {
		String input = "this is a long string";
		String result = MessageParserUtil.padLeft(input, 5);

		assertEquals(input.length(), result.length());
		assertEquals(input, result);
	}

	// ==================== Edge Case Tests ====================

	@Test
	@DisplayName("decodeMessage - empty message throws exception")
	void testDecodeMessage_emptyMessage() {
		String messageIn = "";
		Map<String, String> config = Map.of("field", "5");

		assertThrows(ExceptionMessageIn.class, () -> {
			MessageParserUtil.decodeMessage(messageIn, config);
		});
	}

	@Test
	@DisplayName("decodeMessage - all NULLIFEMPTY fields return nulls")
	void testDecodeMessage_allEmptyFields() throws ExceptionMessageIn {
		String messageIn = "          "; // 10 spaces
		Map<String, String> config = Map.of("field1", "NULLIFEMTPY5", "field2", "NULLIFEMTPY5");

		Map<String, String> result = MessageParserUtil.decodeMessage(messageIn, config);

		assertNull(result.get("field1"));
		assertNull(result.get("field2"));
	}

	@Test
	@DisplayName("parseConfiguration - maintains order")
	void testParseConfiguration_maintainsOrder() {
		String config = "first=1;second=2;third=3";
		Map<String, String> result = MessageParserUtil.parseConfiguration(config);

		// LinkedHashMap should maintain insertion order
		Object[] keys = result.keySet().toArray();
		assertEquals("first", keys[0]);
		assertEquals("second", keys[1]);
		assertEquals("third", keys[2]);
	}
}
