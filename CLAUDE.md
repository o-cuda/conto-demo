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
- `unit/utils/MessageParserUtilTest.java` - Tests for message parsing logic (NULLIFEMPTY, NOTRIM, URL parameter substitution)
- `unit/verticle/` - Verticle initialization and event bus subscription tests
  - `GestisciRequestVerticleTest.java`
  - `SocketServerVerticleTest.java`
  - `ListaTransazioniVerticleTest.java`
  - `SaldoVerticleTest.java`
- `unit/dto/DtoSerializationTest.java` - Jackson JSON serialization tests
- `unit/testutil/VerticleTestUtils.java` - Shared test utilities

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

- `MessageParserUtil.java` - Contains message parsing, configuration parsing, and URL substitution logic (extracted from `GestisciRequestVerticle`)
- `SocketResponseFormatter.java` - Contains response formatting logic for socket responses (extracted from `SocketServerVerticle`)

## Code Conventions

- Italian is used for comments and some variable names
- Verticles are Spring components (autowired in `ContoDemoApplication`)
- Message formats use positional fields (fixed-width, pipe-separated)
- Event bus communication uses `ContoDemoApplication.getDefaultDeliverOptions()` for 10-second timeout
- Utility classes in `utils/` package contain reusable business logic extracted from Verticles for better testability

## Known Limitations

- H2 database is in-memory only
- No decimal validation for transfer amounts
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
- All 29 unit tests pass successfully

**Files Modified During Upgrade:**
- `pom.xml` - Spring Boot parent, Java version, Vert.x version, compiler plugin
- `src/main/java/it/demo/fabrick/ContoDemoApplication.java` - Jakarta import
- `src/main/java/it/demo/fabrick/ActuatorSecurity.java` - Complete Spring Security 6 rewrite
- `src/main/resources/logging-extend.xml` - Jakarta namespace
