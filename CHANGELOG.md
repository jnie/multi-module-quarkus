# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [0.0.1-SNAPSHOT] - 2026-03-14

### Added

#### Project Structure
- Multi-module Maven project with Clean Architecture and Hexagonal Architecture patterns
- Domain module with framework-agnostic models and interfaces
- Service module for business logic implementation
- Repository module for infrastructure persistence
- Inbound REST module for API endpoints
- Outbound advice-slip-api module for external API integration
- Application module as Quarkus entry point
- Architecture tests module with ArchUnit for module dependency validation

#### Domain Module
- `CacheRepository` interface for reactive caching operations
- `AdviceApi` interface for outbound advice retrieval
- `OurService` interface for business logic contract
- `DomainRequest`, `DomainResponse`, `MultiAggregate` immutable models using Immutables
- `ObjectStyle` custom annotation for Immutables configuration

#### Service Module
- `OurServiceImpl` - Business logic implementation using CDI annotations

#### Repository Module
- `CacheRepositoryImpl` - In-memory ConcurrentHashMap-based cache implementation

#### Outbound Module (advice-slip-api)
- `AdviceSlipClient` - MicroProfile REST Client for Advice Slip API
- `AdviceApiImpl` - Adapter implementing domain `AdviceApi` interface
- `AdviceObjectMapper` - Maps external API responses to domain models
- `AdviceResponse`, `Slip` - External API DTO models (Java records)

#### Inbound Module (REST)
- `MainController` - JAX-RS controller with OpenAPI annotations
- `RequestDto`, `ResponseDto` - Request/Response DTOs (Java records)
- `RestMapper` - Maps between DTOs and domain models

#### Application Module
- `Application` - Quarkus application entry point
- `application.yml` - YAML-based configuration
- `application-test.yml` - Test profile configuration
- `ApplicationSmokeTest` - Smoke test verifying CDI context loading

#### Configuration
- Quarkus 3.18.x with RESTEasy Reactive
- Mutiny for reactive programming
- MicroProfile REST Client for external API calls
- MicroProfile OpenAPI for Swagger UI
- H2 in-memory database support
- YAML configuration format (`quarkus-config-yaml`)

#### Architecture Tests
- `ModuleDependencyRules` - ArchUnit tests enforcing Clean Architecture boundaries

### Changed

#### Migration from Spring Boot to Quarkus
- Spring Boot 3.5.11 → Quarkus 3.18.x
- `@RestController` → `@Path` with JAX-RS annotations
- `@Service` → `@ApplicationScoped`
- `@SpringBootTest` → `@QuarkusTest`
- Spring WebFlux → Quarkus RESTEasy Reactive with Mutiny
- Springdoc OpenAPI → SmallRye OpenAPI

#### Removed Lombok
- Replaced `@Data` with Java records for DTOs
- Replaced `@Slf4j` with SLF4J `LoggerFactory.getLogger()`
- Replaced `@RequiredArgsConstructor` with explicit constructors
- Removed Lombok from all pom.xml files

#### Parent POM
- Fixed quarkus-bom as dependencyManagement import (not parent)
- Added Java 21 source/target configuration
- Configured annotation processors for Immutables and MapStruct

#### Configuration
- Converted `application.properties` → `application.yml`
- Converted `application-test.properties` → `application-test.yml`

### Fixed
- Immutables `strictModularity` removed (not supported in version 2.12.1)
- REST client timeout format: `15S` → `15000` (milliseconds)
- CDI bean discovery: added `beans.xml` to service, repository, rest, and outbound modules
- `Optional<CacheRepository>` injection replaced with direct injection

### Dependencies

| Technology | Version |
|------------|---------|
| Java | 21+ |
| Quarkus | 3.18.x |
| Maven | 3.9+ |
| Immutables | 2.12.1 |
| MapStruct | 1.6.3 |
| H2 Database | Latest |
| Mutiny | Latest |
| ArchUnit | 1.3.0 |

### Project Structure

```
multi-module-quarkus/
├── app/
│   ├── domain/               # Framework-agnostic models & interfaces
│   ├── service/              # Business logic implementation
│   ├── repository/           # Infrastructure persistence
│   ├── inbound/rest/          # REST API endpoints
│   ├── outbound/advice-slip-api/  # External API client
│   ├── application/           # Quarkus entry point
│   └── architecture-tests/    # ArchUnit tests
├── doc/                       # Documentation
├── AGENTS.md                  # Agent guidelines
├── CHANGELOG.md               # This file
├── MIGRATION.md               # Migration guide
├── README.md                  # Project overview
└── pom.xml                    # Parent POM
```

### API Endpoints

| Method | Path | Description |
|--------|------|-------------|
| POST | `/api/v1/advice` | Get random advice |

### Running the Application

```bash
# Build
mvn clean package

# Run in dev mode
mvn quarkus:dev -pl app/application

# Run tests
mvn test

# Run architecture tests
mvn test -pl app/architecture-tests
```

### Swagger UI

Access the Swagger UI at: `http://localhost:8081/q/swagger-ui/`