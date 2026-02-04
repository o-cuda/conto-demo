# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a reactive Spring Boot + Vert.x microservice that exposes a TCP socket server for banking operations through the Fabrick API. The application uses an event-driven architecture where Verticles communicate via the Vert.x event bus in a publisher/subscriber pattern.

**Key Technologies:**
- Spring Boot 3.2.5 (for dependency injection, actuator, and infrastructure)
- Vert.x 4.5.11 (reactive framework)
- H2 in-memory database
- Java 21 (compiled to Java 21 bytecode)
- Lombok (managed by Spring Boot, code generation for getters/setters/logging)
- JUnit 5 + Mockito (unit testing)
- Vert.x JUnit5 (async testing support)

**Ports:**
- Spring Boot/Actuator: 9090
- Vert.x TCP Server: 9221

## Running the Application

The application requires a properties file path to be specified for local development:

```bash
# Local development with custom properties location
mvn spring-boot:run -Dspring-boot.run.jvmArguments="-DapplicationPropertiesPath=file:/path/to/conto-demo/config-map/local/application.properties"

# Build
mvn clean package

# Build Docker image (automatically builds during package phase)
mvn clean package docker:build

# Run tests (integration tests - requires server to be running first)
mvn test
```

**Important**: The default properties location is `/data/application.properties` for Kubernetes deployments. For local development, override with `applicationPropertiesPath` VM argument.

## Architecture

### Message Flow

1. TCP socket receives messages on port 9221
2. First 3 characters identify the operation (LIS, BON, SAL)
3. `SocketServerVerticle` generates UUID and passes messages to `GestisciRequestVerticle`
4. `GestisciRequestVerticle` queries `ConfigVerticle` for routing configuration
5. `GestisciRequestVerticle` uses `MessageParserUtil` to decode the message based on configuration
6. URL parameters are substituted using `MessageParserUtil.substituteUrlParameters()`
7. Message is routed to appropriate Verticle via event bus:
   - LIS → `lista_bus` → `ListaTransazioniVerticle`
   - BON → `bonifico_bus` → `BonificoVerticle`
   - SAL → `saldo_bus` → `SaldoVerticle`
8. Verticles call Fabrick APIs and return responses
9. `SocketServerVerticle` formats responses using `SocketResponseFormatter`

### Response Format

All responses are plain strings:
- `0` + message = success
- `1` + requestId + error message = error

The requestId is included in error responses to facilitate log lookup.

### Request ID Propagation

The application uses `reactiverse-contextual-logging` to maintain request context across the reactive event bus. In `ContoDemoApplication`, outbound/inbound interceptors propagate the `requestId` through event bus headers, allowing logs to be correlated across Verticles.

See: `ContoDemoApplication.java:73-89`

### Database Access

**Important**: Database operations use Vert.x JDBC Client (async), NOT Spring Data JPA. The JPA dependency is present but database access in Verticles uses `JDBCClient` directly to avoid blocking threads.

Configuration tables:
- `CONTO_CONFIGURATION` - operation routing configuration
- `CONTO_INDIRIZZI` - API endpoints per environment
- `CONTO_TRANSACTION` - transaction history (not yet used for writes)

Database is H2 in-memory - data is lost on restart.

### Configuration Management

The application is designed for multi-environment Kubernetes deployments:
- Local: `config-map/local/application.properties`
- Kubernetes default: `/data/application.properties`

Override with `-DapplicationPropertiesPath` VM argument.

**Required Properties:**
- `fabrick.apiKey` - API key for Fabrick API authentication
- `fabrick.baseUrl` - Base URL for Fabrick API (default: `https://sandbox.platfr.io/api/gbs/banking/v4.0`)

### Money Transfer Validation Enquiry

The `BonificoVerticle` implements a validation enquiry mechanism to handle ambiguous error responses from money transfers:

**Behavior:**
- When the Fabrick API returns HTTP 500 or 504 errors, the service MAY have executed the transfer
- The verticle automatically performs a validation enquiry by searching the transactions list
- It searches for a transaction matching: amount, currency, and description
- Based on the search results, it determines whether the transfer was executed

**Timeout Configuration:**
- Money transfer requests use a 120-second timeout (more than the 100 seconds recommended by Fabrick)
- Validation enquiry requests use a 30-second timeout

**Response Messages:**
| Scenario | Response |
|----------|----------|
| Matching transaction found | `"Transfer executed - Transaction ID: {id}"` |
| No matching transaction | `"Transfer not executed. You may safely retry."` |
| Validation enquiry failed | `"Transfer execution uncertain - validation enquiry failed. Please manually check before retrying."` |

See: `BonificoVerticle.java:94-224`, `TransactionValidationUtil.java`

## Testing

The project has two types of tests:

### Unit Tests

Unit tests are located in `src/test/java/it/demo/fabrick/unit/` and can be run independently without starting the server:

```bash
# Run all unit tests (fast, no external dependencies)
mvn test -Dtest="**/unit/**/*Test"

# Run specific unit test class
mvn test -Dtest="MessageParserUtilTest"
```

**Unit Test Structure:**
- `unit/utils/MessageParserUtilTest.java` - 19 tests for message parsing logic (NULLIFEMPTY, NOTRIM, URL parameter substitution, input validation)
- `unit/utils/TransactionValidationUtilTest.java` - 23 tests for transaction matching logic (validation enquiry) and input validation
- `unit/verticle/` - Verticle initialization and event bus subscription tests
  - `GestisciRequestVerticleTest.java` - 1 test
  - `SocketServerVerticleTest.java` - 1 test
  - `ListaTransazioniVerticleTest.java` - 2 tests
  - `SaldoVerticleTest.java` - 2 tests
  - `BonificoVerticleTest.java` - 5 tests
- `unit/dto/DtoSerializationTest.java` - 6 tests for Jackson JSON serialization with BigDecimal
- `unit/testutil/VerticleTestUtils.java` - Shared test utilities

**Total:** 59 unit tests (all passing)

**Key Test Dependencies:**
- `vertx-junit5` - Vert.x JUnit 5 extension for async testing
- `mockito-core` and `mockito-junit-jupiter` - Mocking framework

### Integration Tests

Integration tests require the application to be running first:

1. Start the application with appropriate properties
2. Run test classes as Java applications (they contain `main` methods)
3. Tests are TCP clients that send formatted messages to port 9221

Test classes extend `AbstractTestClient` and are in `src/test/java/it/demo/fabrick/vertx/client/`.

### Refactored Utility Classes

To improve testability, some logic was extracted from Verticles into utility classes:

- `MessageParserUtil.java` - Contains message parsing, configuration parsing, URL substitution logic, and input validation (extracted from `GestisciRequestVerticle`)
- `TransactionValidationUtil.java` - Contains transaction matching logic for validation enquiry (extracted from `BonificoVerticle`)

## Code Quality Standards

### Monetary Values
All monetary values use `BigDecimal` for precision (never `double`):
- `BonificoRequestDto.amount`
- `ListaTransactionDto.amount`
- `BalanceDto.balance` and `BalanceDto.availableBalance`

### Generics
All collections use proper generics (`List<E>` instead of `ArrayList`):
- DTOs use `List<Error>` instead of `ArrayList<Error>`
- This ensures better type safety and flexibility

### JSON Libraries
The project uses Jackson exclusively - no other JSON libraries (jsoniter, org.json) are present.

### Error Handling
Structured error handling with semantic error codes:
- `ErrorCode` enum defines error types (VALIDATION_ERROR, API_ERROR, TIMEOUT_ERROR, PARSE_ERROR, NETWORK_ERROR, CONFIGURATION_ERROR)
- Error responses include error code, message, requestId, and details
- All Verticles use `ErrorCode` for consistent error reporting

### Constants
Hardcoded strings are defined as constants in appropriate classes:
- `MessageParserUtil`: Operation codes (`OPERATION_LIS`, `OPERATION_SAL`, `OPERATION_BON`), message parsing patterns, validation limits
- `BonificoRequestDto`: `REMITTANCE_INFORMATION_URI`
- `ErrorCode`: All error codes and their numeric values

### Input Validation
All inputs are validated before processing in `MessageParserUtil.validateMoneyTransferRequest()`:
- **Amount**: Between 0.01 and 999999999.99
- **Currency**: ISO 4217 format (3 uppercase letters)
- **Creditor name**: Required, max 140 characters
- **Description**: Required, max 500 characters

### Logging Standards
- **DEBUG level**: Detailed debugging information, sensitive data
- **INFO level**: Important business events, successful operations
- **WARN level**: Unexpected but recoverable situations
- **ERROR level**: Errors that prevent operation completion
- All log messages in English
- Context included in error logs (creditor name, account ID, etc.)

## Code Conventions

- Italian is used for comments and some variable names
- Verticles are Spring components (autowired in `ContoDemoApplication`)
- Message formats use positional fields (fixed-width, pipe-separated)
- Event bus communication uses `ContoDemoApplication.getDefaultDeliverOptions()` for 10-second timeout
- Utility classes in `utils/` package contain reusable business logic extracted from Verticles for better testability

## Known Limitations

- H2 database is in-memory only
- Transaction history not persisted to database

## Version History

### Spring Boot 3.2.5 + Java 21 + Vert.x 4.5.11 Upgrade (February 2025)

Upgraded from Spring Boot 2.7.2 / Java 8 / Vert.x 4.2.1 to modern versions:

**Major Changes:**
- **Spring Boot**: 2.7.2 → 3.2.5
- **Java**: 1.8 → 21 (with `release=21` compiler flag)
- **Vert.x**: 4.2.1 → 4.5.11
- **Jakarta EE**: Full migration from `javax.*` to `jakarta.*` namespace (required by Spring Boot 3)
- **Spring Security**: Rewritten `ActuatorSecurity` class to use `SecurityFilterChain` bean pattern (removed deprecated `WebSecurityConfigurerAdapter`)
- **Maven Compiler Plugin**: 3.8.1 → 3.13.0

**Breaking Changes Addressed:**
1. All `javax.*` imports replaced with `jakarta.*` (e.g., `javax.annotation.PostConstruct` → `jakarta.annotation.PostConstruct`)
2. Spring Security configuration migrated from `WebSecurityConfigurerAdapter` to `SecurityFilterChain` bean (required by Spring Security 6)
3. Actuator security matcher updated to use `securityMatcher()` with `authorizeHttpRequests()` for `/actuator` endpoints
4. Logger configuration updated from `javax.servlet` to `jakarta.servlet`

**Compatibility Notes:**
- Lombok version is now managed by Spring Boot 3 parent POM
- Application requires Java 21+ to compile and run
- All 43 unit tests pass successfully (including 14 new tests for validation enquiry)

**Files Modified During Upgrade:**
- `pom.xml` - Spring Boot parent, Java version, Vert.x version, compiler plugin
- `src/main/java/it/demo/fabrick/ContoDemoApplication.java` - Jakarta import
- `src/main/java/it/demo/fabrick/ActuatorSecurity.java` - Complete Spring Security 6 rewrite
- `src/main/resources/logging-extend.xml` - Jakarta namespace

### Money Transfer Validation Enquiry (February 2025)

Added validation enquiry behavior for money transfers to handle HTTP 500/504 errors as recommended by Fabrick API documentation.

**Major Changes:**
- **Timeout**: Added 120-second timeout to money transfer requests (exceeds Fabrick's recommended 100 seconds)
- **Validation Enquiry**: Implemented automatic validation enquiry when receiving HTTP 500 or 504 errors
- **Transaction Search**: Searches transactions list by amount, currency, and description to determine if transfer executed
- **Utility Class**: Extracted `TransactionValidationUtil` for better testability

**New Features:**
- When money transfer fails with HTTP 500/504, automatically queries transactions list
- Matches transactions by amount, currency, and description
- Returns appropriate response based on whether matching transaction was found
- Prevents duplicate transfers by advising user whether retry is safe

**Configuration:**
- Added `fabrick.baseUrl` property to `application.properties`

**Tests Added:**
- `TransactionValidationUtilTest.java` - 9 tests for transaction matching logic
- `BonificoVerticleTest.java` - 5 tests for verticle initialization and configuration

**Files Added:**
- `src/main/java/it/demo/fabrick/utils/TransactionValidationUtil.java`
- `src/test/java/it/demo/fabrick/unit/utils/TransactionValidationUtilTest.java`
- `src/test/java/it/demo/fabrick/unit/verticle/BonificoVerticleTest.java`

**Files Modified:**
- `src/main/java/it/demo/fabrick/vertx/BonificoVerticle.java` - Added timeout, validation enquiry logic
- `config-map/local/application.properties` - Added `fabrick.baseUrl`

### Code Quality Improvements (February 2025)

Comprehensive code quality improvements addressing error handling, logging, data types, and validation:

**Error Handling:**
- Created `ErrorCode` enum with semantic error codes (VALIDATION_ERROR, API_ERROR, TIMEOUT_ERROR, PARSE_ERROR, NETWORK_ERROR, CONFIGURATION_ERROR)
- Created `ErrorResponse` DTO for structured error responses
- Updated all Verticles to use `ErrorCode` instead of hardcoded "1"
- Completed TODO in `BonificoVerticle` to parse successful money transfer responses

**Logging Improvements:**
- Changed logging level from TRACE to DEBUG for better performance
- Removed redundant method entry/exit logs
- Standardized all log messages to English
- Moved sensitive data from INFO to DEBUG level
- Added context to error logs (creditor names, account IDs, etc.)

**File Cleanup:**
- Deleted unused `ExceptionCoda.java`
- Completed refactoring of `GestisciRequestVerticle` to use `MessageParserUtil` (reduced from 235 to 125 lines)

**Files Added:**
- `src/main/java/it/demo/fabrick/dto/ErrorCode.java`
- `src/main/java/it/demo/fabrick/dto/ErrorResponse.java`
- `src/main/java/it/demo/fabrick/utils/SocketResponseFormatter.java` (later deleted)

**Files Modified:**
- All Verticles - Updated to use ErrorCode enum and improved logging
- `src/main/java/it/demo/fabrick/vertx/GestisciRequestVerticle.java` - Refactored to use MessageParserUtil methods
- `src/main/resources/logging-extend.xml` - Changed level to DEBUG
- Unit tests updated to test new error codes

### BigDecimal Migration and Input Validation (February 2025)

Replaced all `double` types with `BigDecimal` for monetary values and added comprehensive input validation:

**Data Type Changes:**
- `BonificoRequestDto.amount`: `double` → `BigDecimal`
- `ListaTransactionDto.amount`: `double` → `BigDecimal`
- `BalanceDto.balance` and `availableBalance`: `double` → `BigDecimal`
- Updated all related code to use `BigDecimal.compareTo()` for comparisons

**Input Validation:**
- Added `MessageParserUtil.validateMoneyTransferRequest()` method
- Validates amount range (0.01 to 999999999.99)
- Validates currency format (ISO 4217 - 3 uppercase letters)
- Validates creditor name (required, max 140 characters)
- Validates description (required, max 500 characters)
- Validation is automatically called in `BonificoRequestDto` constructor

**Constants Added:**
- Operation codes: `OPERATION_LIS`, `OPERATION_SAL`, `OPERATION_BON`
- Message parsing patterns: `PATTERN_NULLIFEMPTY`, `PATTERN_NOTRIM`, `PATTERN_THREE_LETTER_CODE`
- Validation limits: `MIN_TRANSFER_AMOUNT`, `MAX_TRANSFER_AMOUNT`, `MAX_DESCRIPTION_LENGTH`, `MAX_CREDITOR_NAME_LENGTH`

**Dependency Cleanup:**
- Removed jsoniter dependency (use Jackson only)

**Test Coverage:**
- Added 13 new validation tests in `TransactionValidationUtilTest` (23 total tests)
- Updated `DtoSerializationTest` to use `BigDecimal.compareTo()`

**Files Modified:**
- `src/main/java/it/demo/fabrick/dto/BalanceDto.java`
- `src/main/java/it/demo/fabrick/dto/BonificoRequestDto.java`
- `src/main/java/it/demo/fabrick/dto/ListaTransactionDto.java`
- `src/main/java/it/demo/fabrick/utils/MessageParserUtil.java`
- `src/main/java/it/demo/fabrick/utils/TransactionValidationUtil.java`
- `src/main/java/it/demo/fabrick/vertx/BonificoVerticle.java`
- `src/test/java/it/demo/fabrick/unit/dto/DtoSerializationTest.java`
- `src/test/java/it/demo/fabrick/unit/utils/TransactionValidationUtilTest.java`
- `pom.xml` - Removed jsoniter

### Generics and Hardcoded String Fixes (February 2025)

Improved code quality through better generics usage and elimination of hardcoded strings:

**Generics:**
- Replaced all `ArrayList` with `List<E>` in DTOs for better type safety
- `BalanceDto`: `ArrayList<Error>` → `List<Error>`
- `BonificoResponseDto`: `ArrayList<Error>` → `List<Error>`
- `ErrorDto`: `ArrayList<AnErrorDto>` → `List<AnErrorDto>`
- `TransactionDto`: `ArrayList<Error>` → `List<Error>`, `ArrayList<ListaTransactionDto>` → `List<ListaTransactionDto>`

**Hardcoded Strings:**
- Fixed hardcoded `"LIS"` in `BonificoVerticle` to use `MessageParserUtil.OPERATION_LIS`

**Files Modified:**
- `src/main/java/it/demo/fabrick/dto/BalanceDto.java`
- `src/main/java/it/demo/fabrick/dto/BonificoResponseDto.java`
- `src/main/java/it/demo/fabrick/dto/ErrorDto.java`
- `src/main/java/it/demo/fabrick/dto/TransactionDto.java`
- `src/main/java/it/demo/fabrick/vertx/BonificoVerticle.java`
