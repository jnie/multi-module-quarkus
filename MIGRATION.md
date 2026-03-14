# Quarkus Migration Guide

This document tracks the migration from `multi-module-architecture` (Spring Boot) to `multi-module-quarkus`.

## Migration Status

| Module | POM | Source Files | Status |
|--------|-----|--------------|--------|
| Parent | ✅ | N/A | Complete |
| Domain | ✅ | ✅ (7/7) | Complete |
| Service | ✅ | ✅ (1/1) | Complete |
| Repository | ✅ | ✅ (1/1) | Complete |
| Outbound | ✅ | ✅ (4/4) | Complete |
| Inbound/REST | ✅ | ✅ (4/4) | Complete |
| Application | ✅ | ✅ (2/2) | Complete |
| Arch-Tests | ✅ | ✅ (1/1) | Complete |
| Documentation | ✅ | N/A | Complete |
| Git Init | ✅ | N/A | Complete |

---

## completed Migrations

### Lombok Removal

Removed Lombok dependency entirely and replaced with:

| Lombok | Replacement |
|--------|-------------|
| `@Data` | Java `record` types |
| `@Slf4j` | SLF4J `LoggerFactory.getLogger()` |
| `@RequiredArgsConstructor` | Explicit constructor injection |

**Files changed:**
- `RequestDto.java` - converted to `record`
- `ResponseDto.java` - converted to `record`
- `AdviceResponse.java` - converted to `record` with nested `Slip` record
- `Slip.java` - converted to `record`
- `MainController.java` - explicit constructor with `LoggerFactory`
- `AdviceApiImpl.java` - explicit constructor with `LoggerFactory`
- `CacheRepositoryImpl.java` - explicit constructor with `LoggerFactory`
- `OurServiceImpl.java` - explicit constructor with `LoggerFactory`

**Annotation processors remaining:**
- MapStruct (`mapstruct-processor`)
- Immutables (`value`)

---

### YAML Configuration

Converted from `application.properties` to `application.yml`:

**Before (properties):**
```properties
quarkus.application.name=multi-module-quarkus
quarkus.http.port=8081
quarkus.rest-client.advice-slip.url=https://api.adviceslip.com
```

**After (YAML):**
```yaml
quarkus:
  application:
    name: multi-module-quarkus
  http:
    port: 8081
  rest-client:
    advice-slip:
      url: https://api.adviceslip.com
      connect-timeout: 15000
      read-timeout: 30000
```

**Dependency added:**
```xml
<dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-config-yaml</artifactId>
</dependency>
```

---

### CDI Bean Discovery

Added `beans.xml` markers to enable CDI bean discovery in modules:

| Module | Path |
|--------|------|
| service | `app/service/src/main/resources/META-INF/beans.xml` |
| repository | `app/repository/src/main/resources/META-INF/beans.xml` |
| inbound/rest | `app/inbound/rest/src/main/resources/META-INF/beans.xml` |
| outbound/advice-slip-api | `app/outbound/advice-slip-api/src/main/resources/META-INF/beans.xml` |

---

### Smoke Tests

Added `ApplicationSmokeTest.java` to verify CDI context loading:

```java
@QuarkusTest
class ApplicationSmokeTest {
    @Inject OurService ourService;
    @Inject AdviceApi adviceApi;
    @Inject CacheRepository cacheRepository;
    @Inject MainController mainController;
    @Inject RestMapper restMapper;

    @Test
    void applicationContextLoads() { /* ... */ }
    @Test
    void serviceLayerIsProperlyWired() { /* ... */ }
    @Test
    void repositoryLayerIsProperlyWired() { /* ... */ }
    @Test
    void outboundAdapterIsProperlyWired() { /* ... */ }
}
```

---

## Key Migration Decisions

### 1. Parent POM Structure

Used `dependencyManagement` with `scope=import` for Quarkus BOM (not as parent):

```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>io.quarkus.platform</groupId>
            <artifactId>quarkus-bom</artifactId>
            <version>${quarkus.platform.version}</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```

### 2. Java Version Configuration

Added explicit source/target configuration for Java 21:

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-compiler-plugin</artifactId>
    <configuration>
        <source>21</source>
        <target>21</target>
    </configuration>
</plugin>
```

### 3. Immutables Configuration

Removed unsupported `strictModularity` attribute:

```java
@Value.Style(
    typeAbstract = "*Def",
    typeImmutable = "*",
    of = "of",
    allMandatoryParameters = true,
    allParameters = true,
    strictBuilder = true
    // strictModularity removed - not supported in 2.12.1
)
```

### 4. REST Client Timeout Format

Used milliseconds instead of Duration strings:

```yaml
# Wrong
connect-timeout: 15S

# Correct
connect-timeout: 15000
```

### 5. Optional Injection Replacement

Replaced `Optional<CacheRepository>` with direct injection:

```java
// Wrong - CDI doesn't handle Optional well
@Inject
public AdviceApiImpl(Optional<CacheRepository> cacheRepository) { }

// Correct - use direct injection
@Inject
public AdviceApiImpl(CacheRepository cacheRepository) { }
```

---

## Module Details

### 1. Domain Module ✅ COMPLETE

**Location:** `app/domain/`

**Files:**
- `pom.xml` - Quarkus BOM parent, Mutiny dependency
- `ObjectStyle.java` - Immutables annotation
- `DomainRequestDef.java` - Domain request interface
- `DomainResponseDef.java` - Domain response interface
- `MultiAggregateDef.java` - Multi aggregate interface
- `OurService.java` - Service interface (Uni<DomainResponse>)
- `AdviceApi.java` - Outbound API interface (Uni<MultiAggregate>)
- `CacheRepository.java` - Repository interface (Uni<String>, etc.)

**Key Change:** Mono → Uni (Mutiny)

---

### 2. Service Module ✅ COMPLETE

**Location:** `app/service/`

**Files:**
- `pom.xml` - Domain dependency, quarkus-arc, mutiny
- `OurServiceImpl.java` - @ApplicationScoped CDI bean

**Key Change:** 
- `@Service` → `@ApplicationScoped`
- `Mono` → `Uni`
- `@Inject` constructor injection
- SLF4J `LoggerFactory` instead of `@Slf4j`

---

### 3. Repository Module ✅ COMPLETE

**Location:** `app/repository/`

**Files:**
- `pom.xml` - Domain dependency, mutiny
- `CacheRepositoryImpl.java` - In-memory ConcurrentHashMap cache

**Implementation:**
- In-memory cache using `ConcurrentHashMap`
- SLF4J logging
- Direct implementation of domain interface

---

### 4. Outbound Module (advice-slip-api) ✅ COMPLETE

**Location:** `app/outbound/advice-slip-api/`

**Files:**
- `pom.xml` - REST Client dependencies
- `AdviceSlipClient.java` - MicroProfile REST Client interface
- `AdviceApiImpl.java` - Adapter implementing domain interface
- `AdviceObjectMapper.java` - Maps external API to domain
- `AdviceResponse.java` - External API DTO (record)
- `Slip.java` - External API DTO (record)

**Key Change:**
- `WebClient` → MicroProfile REST Client
- `@RestClient` injection
- Java records for DTOs instead of `@Data`

---

### 5. Inbound Module (REST) ✅ COMPLETE

**Location:** `app/inbound/rest/`

**Files:**
- `pom.xml` - REST dependencies
- `MainController.java` - JAX-RS controller
- `RequestDto.java` - Request DTO (record)
- `ResponseDto.java` - Response DTO (record)
- `RestMapper.java` - Maps DTOs to domain

**Key Change:**
- `@RestController` → `@Path` with JAX-RS
- `Mono` → `Uni`
- Java records instead of `@Data`
- Explicit constructor injection

---

### 6. Application Module ✅ COMPLETE

**Location:** `app/application/`

**Files:**
- `pom.xml` - Quarkus dependencies
- `Application.java` - Main entry point
- `application.yml` - Configuration (YAML)
- `application-test.yml` - Test configuration

---

### 7. Architecture Tests ✅ COMPLETE

**Location:** `app/architecture-tests/`

**Files:**
- `pom.xml` - ArchUnit dependency
- `ModuleDependencyRules.java` - Enforces Clean Architecture boundaries

**Tests:**
- Domain module isolation
- Service module dependencies
- Inbound module dependencies
- Outbound module dependencies

---

## Verification Commands

```bash
# Compile all modules
mvn clean compile

# Run all tests
mvn test

# Run application in dev mode
mvn quarkus:dev -pl app/application

# Run smoke tests only
mvn test -pl app/application

# Run architecture tests
mvn test -pl app/architecture-tests
```

---

## API Endpoints

| Method | Path | Description |
|--------|------|-------------|
| POST | `/api/v1/advice` | Get random advice |

## Running the Application

```bash
# Build
mvn clean package

# Run in dev mode
mvn quarkus:dev -pl app/application

# Run tests
mvn test

# Access Swagger UI
open http://localhost:8081/q/swagger-ui/
```

---

## Dependencies Summary

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

---

## Project Structure

```
multi-module-quarkus/
├── app/
│   ├── domain/               # Framework-agnostic models & interfaces
│   ├── service/              # Business logic implementation
│   ├── repository/           # Infrastructure persistence
│   ├── inbound/rest/         # REST API endpoints
│   ├── outbound/advice-slip-api/  # External API client
│   ├── application/           # Quarkus entry point
│   └── architecture-tests/    # ArchUnit tests
├── doc/                       # Documentation
├── AGENTS.md                  # Agent guidelines
├── CHANGELOG.md               # Change history
├── MIGRATION.md               # This file
├── README.md                  # Project overview
└── pom.xml                    # Parent POM
```

---

## Lessons Learned

1. **CDI Bean Discovery**: Modules need `beans.xml` for CDI to discover beans
2. **Optional Injection**: CDI doesn't handle `Optional<T>` well - use direct injection
3. **Timeout Format**: Use milliseconds, not Duration strings for REST client config
4. **YAML Configuration**: Requires `quarkus-config-yaml` dependency
5. **Immutables**: Some attributes may not be supported in newer versions
6. **Lombok Removal**: Java 21 records provide good alternative for DTOs