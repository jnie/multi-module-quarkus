# Quarkus Migration Guide

This document tracks the migration from `multi-module-architecture` (Spring Boot) to `multi-module-quarkus`.

## Migration Status

| Module | POM | Source Files | Status |
|--------|-----|--------------|--------|
| Parent | ✅ | N/A | Complete |
| Domain | ✅ | ✅ (7/7) | Complete |
| Service | ✅ | ✅ (1/1) | Complete |
| Repository | ✅ | ⏳ (0/1) | Pending |
| Outbound | ✅ | ⏳ (0/4) | Pending |
| Inbound/REST | ✅ | ⏳ (0/4) | Pending |
| Application | ✅ | ⏳ (0/2) | Pending |
| Arch-Tests | ✅ | ⏳ (0/1) | Pending |
| Documentation | ⏳ | N/A | Pending |
| Git Init | ⏳ | N/A | Pending |

---

## Module Details

### 1. Domain Module ✅ COMPLETE

**Location:** `app/domain/`

**Files Created:**
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

**Files Created:**
- `pom.xml` - Domain dependency, quarkus-arc, mutiny
- `OurServiceImpl.java` - @ApplicationScoped CDI bean

**Key Change:** 
- `@Service` → `@ApplicationScoped`
- `Mono` → `Uni`
- `@Inject` constructor injection

---

### 3. Repository Module ⏳ PENDING

**Location:** `app/repository/`

**POM:** ✅ Created

**Files Needed:**

#### `src/main/java/dk/jnie/example/repository/CacheRepositoryImpl.java`

```java
package dk.jnie.example.repository;

import dk.jnie.example.domain.repository.CacheRepository;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.sqlclient.SqlClient;
import io.vertx.mutiny.sqlclient.Row;
import io.vertx.mutiny.sqlclient.Tuple;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;

@Slf4j
@ApplicationScoped
public class CacheRepositoryImpl implements CacheRepository {
    
    private final SqlClient sqlClient;
    
    @Inject
    public CacheRepositoryImpl(SqlClient sqlClient) {
        this.sqlClient = sqlClient;
        initializeSchema();
    }
    
    private void initializeSchema() {
        sqlClient.query("""
            CREATE TABLE IF NOT EXISTS cache_entries (
                cache_key VARCHAR(512) PRIMARY KEY,
                cache_value TEXT NOT NULL,
                created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
            )
            """)
        .execute()
        .subscribe()
        .with(
            rowSet -> log.info("Cache schema initialized"),
            failure -> log.error("Failed to initialize cache schema", failure)
        );
    }
    
    @Override
    public Uni<String> get(String key) {
        return sqlClient
                .preparedQuery("SELECT cache_value FROM cache_entries WHERE cache_key = $1")
                .execute(Tuple.of(key))
                .onItem().transformToUni(rowSet -> {
                    if (rowSet.iterator().hasNext()) {
                        String value = rowSet.iterator().next().getString("cache_value");
                        log.debug("Cache hit for key: {}", key);
                        return Uni.createFrom().item(value);
                    }
                    log.debug("Cache miss for key: {}", key);
                    return Uni.createFrom().nullItem();
                });
    }
    
    @Override
    public Uni<Boolean> put(String key, String value) {
        return sqlClient
                .preparedQuery("""
                    MERGE INTO cache_entries (cache_key, cache_value, created_at)
                    VALUES ($1, $2, $3)
                    """)
                .execute(Tuple.of(key, value, Instant.now()))
                .onItem().transform(rowSet -> {
                    log.debug("Cached value for key: {}", key);
                    return true;
                })
                .onFailure().recoverWithItem(false);
    }
    
    @Override
    public Uni<Boolean> evict(String key) {
        return sqlClient
                .preparedQuery("DELETE FROM cache_entries WHERE cache_key = $1")
                .execute(Tuple.of(key))
                .onItem().transform(rowSet -> {
                    log.debug("Evicted cache for key: {}", key);
                    return true;
                })
                .onFailure().recoverWithItem(false);
    }
    
    @Override
    public Uni<Boolean> exists(String key) {
        return sqlClient
                .preparedQuery("SELECT COUNT(*) as cnt FROM cache_entries WHERE cache_key = $1")
                .execute(Tuple.of(key))
                .onItem().transform(rowSet -> {
                    Row row = rowSet.iterator().next();
                    return row.getLong("cnt") > 0;
                });
    }
}
```

**Need to configure datasource in application.properties:**
```properties
quarkus.datasource.db-kind=h2
quarkus.datasource.jdbc.url=jdbc:h2:mem:default;DB_CLOSE_DELAY=-1
quarkus.datasource.jdbc.driver=org.h2.Driver
```

---

### 4. Outbound Module (advice-slip-api) ⏳ PENDING

**Location:** `app/outbound/advice-slip-api/`

**POM:** ✅ Created

**Files Needed:**

#### 4.1 `src/main/java/dk/jnie/example/advice/client/AdviceSlipClient.java`

```java
package dk.jnie.example.advice.client;

import io.smallrye.mutiny.Uni;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@RegisterRestClient(configKey = "advice-slip")
public interface AdviceSlipClient {
    
    @GET
    @Path("/advice")
    @Produces(MediaType.TEXT_HTML)
    Uni<String> getRandomAdvice();
}
```

#### 4.2 `src/main/java/dk/jnie/example/advice/AdviceApiImpl.java`

```java
package dk.jnie.example.advice;

import com.fasterxml.jackson.databind.ObjectMapper;
import dk.jnie.example.advice.client.AdviceSlipClient;
import dk.jnie.example.advice.mappers.AdviceObjectMapper;
import dk.jnie.example.advice.model.AdviceResponse;
import dk.jnie.example.domain.model.MultiAggregate;
import dk.jnie.example.domain.outbound.AdviceApi;
import dk.jnie.example.domain.repository.CacheRepository;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import java.util.Optional;

@Slf4j
@ApplicationScoped
public class AdviceApiImpl implements AdviceApi {
    
    private static final String CACHE_KEY = "advice:random";
    
    private final ObjectMapper objectMapper;
    private final AdviceObjectMapper mapper;
    private final AdviceSlipClient adviceClient;
    private final Optional<CacheRepository> cacheRepository;
    private final boolean cacheEnabled;
    
    @Inject
    public AdviceApiImpl(
            ObjectMapper objectMapper,
            AdviceObjectMapper mapper,
            @RestClient AdviceSlipClient adviceClient,
            Optional<CacheRepository> cacheRepository,
            @ConfigProperty(name = "mma.cache.enabled", defaultValue = "false") boolean cacheEnabled) {
        this.objectMapper = objectMapper;
        this.mapper = mapper;
        this.adviceClient = adviceClient;
        this.cacheRepository = cacheRepository;
        this.cacheEnabled = cacheEnabled;
    }
    
    @Override
    public Uni<MultiAggregate> getRandomAdvice() {
        if (cacheEnabled && cacheRepository.isPresent()) {
            return getFromCache()
                    .onItem().ifNull().switchTo(fetchAndCache());
        }
        return fetchFromApi();
    }
    
    private Uni<MultiAggregate> getFromCache() {
        return cacheRepository.get().get(CACHE_KEY)
                .onItem().transformToUni(this::deserializeFromCache)
                .onItem().invoke(aggregate -> log.info("Retrieved advice from cache"));
    }
    
    private Uni<MultiAggregate> fetchAndCache() {
        return fetchFromApi()
                .onItem().transformToUni(aggregate -> 
                    cacheRepository.get().put(CACHE_KEY, serializeForCache(aggregate))
                        .onItem().transform(ignored -> aggregate)
                );
    }
    
    private Uni<MultiAggregate> fetchFromApi() {
        log.info("Calling the advice API");
        
        return adviceClient.getRandomAdvice()
                .onItem().transformToUni(this::convertToAdviceResponse)
                .onItem().transform(mapper::toDomain);
    }
    
    private Uni<MultiAggregate> deserializeFromCache(String cachedValue) {
        try {
            return Uni.createFrom().item(objectMapper.readValue(cachedValue, MultiAggregate.class));
        } catch (Exception e) {
            log.warn("Failed to deserialize cached value", e);
            return Uni.createFrom().nullItem();
        }
    }
    
    private String serializeForCache(MultiAggregate aggregate) {
        try {
            return objectMapper.writeValueAsString(aggregate);
        } catch (Exception e) {
            log.error("Failed to serialize aggregate", e);
            return null;
        }
    }
    
    private Uni<AdviceResponse> convertToAdviceResponse(String responseBody) {
        try {
            AdviceResponse response = objectMapper.readValue(responseBody, AdviceResponse.class);
            return Uni.createFrom().item(response);
        } catch (Exception e) {
            return Uni.createFrom().failure(new RuntimeException("Failed to parse response", e));
        }
    }
}
```

#### 4.3 `src/main/java/dk/jnie/example/advice/mappers/AdviceObjectMapper.java`

```java
package dk.jnie.example.advice.mappers;

import dk.jnie.example.advice.model.AdviceResponse;
import dk.jnie.example.domain.model.MultiAggregate;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class AdviceObjectMapper {
    
    public MultiAggregate toDomain(AdviceResponse response) {
        // Map from external API response to domain
        return MultiAggregate.builder()
                .answer(response.getSlip().getAdvice())
                .build();
    }
}
```

#### 4.4 `src/main/java/dk/jnie/example/advice/model/AdviceResponse.java`

```java
package dk.jnie.example.advice.model;

import lombok.Data;

@Data
public class AdviceResponse {
    private Slip slip;
    
    @Data
    public static class Slip {
        private Long id;
        private String advice;
    }
}
```

---

### 5. Inbound Module (REST) ⏳ PENDING

**Location:** `app/inbound/rest/`

**POM:** ✅ Created

**Files Needed:**

#### 5.1 `src/main/java/dk/jnie/example/rest/model/RequestDto.java`

```java
package dk.jnie.example.rest.model;

import lombok.Data;

@Data
public class RequestDto {
    private String please;
}
```

#### 5.2 `src/main/java/dk/jnie/example/rest/model/ResponseDto.java`

```java
package dk.jnie.example.rest.model;

import lombok.Data;

@Data
public class ResponseDto {
    private String advice;
}
```

#### 5.3 `src/main/java/dk/jnie/example/rest/mappers/RestMapper.java`

```java
package dk.jnie.example.rest.mappers;

import dk.jnie.example.domain.model.DomainRequest;
import dk.jnie.example.domain.model.DomainResponse;
import dk.jnie.example.rest.model.RequestDto;
import dk.jnie.example.rest.model.ResponseDto;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class RestMapper {
    
    public DomainRequest requestDTOToDomain(RequestDto dto) {
        return DomainRequest.builder()
                .question(dto.getPlease())
                .build();
    }
    
    public ResponseDto domainToResponseDto(DomainResponse response) {
        ResponseDto dto = new ResponseDto();
        dto.setAdvice(response.getAnswer());
        return dto;
    }
}
```

#### 5.4 `src/main/java/dk/jnie/example/rest/controllers/MainController.java`

```java
package dk.jnie.example.rest.controllers;

import dk.jnie.example.rest.mappers.RestMapper;
import dk.jnie.example.rest.model.RequestDto;
import dk.jnie.example.rest.model.ResponseDto;
import dk.jnie.example.domain.services.OurService;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.ExampleObject;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

@ApplicationScoped
@RequiredArgsConstructor
@Slf4j
@Path("/api/v1/advice")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Main API", description = "Call for an advice")
public class MainController {

    private final OurService ourService;
    private final RestMapper restMapper;

    @POST
    @Operation(summary = "Ask for an advice", description = "Usage: POST with request body")
    @APIResponse(responseCode = "200", description = "Success",
                 content = @Content(schema = @Schema(implementation = ResponseDto.class)))
    @APIResponse(responseCode = "400", description = "Invalid input")
    public Uni<Response> getAdvice(
            @Valid @RequestBody(
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = RequestDto.class),
                            examples = @ExampleObject(value = "{\"please\": \"anything\"}")
                    )
            ) RequestDto request) {
        
        return ourService.getAnAdvice(restMapper.requestDTOToDomain(request))
                .onItem().transform(restMapper::domainToResponseDto)
                .onItem().transform(response -> Response.ok(response).build());
    }
}
```

---

### 6. Application Module ⏳ PENDING

**Location:** `app/application/`

**POM:** ✅ Created

**Files Needed:**

#### 6.1 `src/main/java/dk/jnie/example/Application.java`

```java
package dk.jnie.example;

import io.quarkus.runtime.Quarkus;

public class Application {
    public static void main(String[] args) {
        Quarkus.run(args);
    }
}
```

#### 6.2 `src/main/resources/application.properties`

```properties
# Application
quarkus.application.name=multi-module-quarkus
quarkus.http.port=8081

# OpenAPI/Swagger
quarkus.smallrye-openapi.info-title=Multi-Module Quarkus API
quarkus.smallrye-openapi.info-version=1.0.0
quarkus.smallrye-openapi.info-description=Clean Architecture with Quarkus and Mutiny
quarkus.http.cors.enabled=true

# REST Client for Advice Slip API
quarkus.rest-client.advice-slip.url=https://api.adviceslip.com
quarkus.rest-client.advice-slip.connect-timeout=15s
quarkus.rest-client.advice-slip.read-timeout=30s

# Cache configuration
mma.cache.enabled=false

# H2 Database
quarkus.datasource.db-kind=h2
quarkus.datasource.jdbc.url=jdbc:h2:mem:default;DB_CLOSE_DELAY=-1
quarkus.datasource.jdbc.driver=org.h2.Driver
quarkus.h2.devservices.enabled=false

# Development mode
%dev.quarkus.http.port=8081
%dev.quarkus.log.level=DEBUG
%dev.quarkus.live-reload.instrumentation=true
```

---

### 7. Architecture Tests ⏳ PENDING

**Location:** `app/architecture-tests/`

**POM:** ✅ Created

**Files Needed:**

#### `src/test/java/dk/jnie/example/architecture/ModuleDependencyRules.java`

```java
package dk.jnie.example.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAnyPackage;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@AnalyzeClasses(packages = "dk.jnie.example..", importOptions = ImportOption.DoNotIncludeTests.class)
public class ModuleDependencyRules {

    @ArchTest
    public static final ArchRule domainModuleShouldNotDependOnOtherModules =
        noClasses()
            .that(resideInAPackage("dk.jnie.example.domain.."))
            .should()
            .dependOnClassesThat(resideInAnyPackage(
                "dk.jnie.example.application..",
                "dk.jnie.example.service..",
                "dk.jnie.example.rest..",
                "dk.jnie.example.inbound..",
                "dk.jnie.example.outbound..",
                "dk.jnie.example.advice..",
                "jakarta.enterprise..",
                "io.quarkus.."
            ))
            .because("Domain module should not depend on any framework or other modules");

    @ArchTest
    public static final ArchRule serviceModuleShouldOnlyDependOnDomain =
        noClasses()
            .that(resideInAPackage("dk.jnie.example.service.."))
            .should()
            .dependOnClassesThat(resideInAnyPackage(
                "dk.jnie.example.application..",
                "dk.jnie.example.rest..",
                "dk.jnie.example.inbound..",
                "dk.jnie.example.outbound..",
                "dk.jnie.example.advice..",
                "io.quarkus.."
            ))
            .because("Service module should only depend on domain (CDI annotations are OK)");

    @ArchTest
    public static final ArchRule inboundModuleShouldOnlyDependOnDomain =
        noClasses()
            .that(resideInAnyPackage("dk.jnie.example.rest..", "dk.jnie.example.inbound.."))
            .should()
            .dependOnClassesThat(resideInAnyPackage(
                "dk.jnie.example.application..",
                "dk.jnie.example.service..",
                "dk.jnie.example.outbound..",
                "dk.jnie.example.advice.."
            ))
            .because("Inbound/REST module should only depend on domain");

    @ArchTest
    public static final ArchRule outboundModuleShouldOnlyDependOnDomain =
        noClasses()
            .that(resideInAnyPackage("dk.jnie.example.outbound..", "dk.jnie.example.advice.."))
            .should()
            .dependOnClassesThat(resideInAnyPackage(
                "dk.jnie.example.application..",
                "dk.jnie.example.service..",
                "dk.jnie.example.rest..",
                "dk.jnie.example.inbound.."
            ))
            .because("Outbound module should only depend on domain");
}
```

---

### 8. Documentation ⏳ PENDING

#### `README.md`

```markdown
# multi-module-quarkus

Clean Architecture implementation with Quarkus and Mutiny.

## Technology Stack

| Technology | Version | Purpose |
|------------|---------|---------|
| Java | 21+ | Programming language |
| Quarkus | 3.18.4 | Application framework |
| Mutiny | (managed) | Reactive types |
| Maven | 3.9+ | Build tool |

## Quick Start

```bash
./mvnw quarkus:dev
```

Access: http://localhost:8081/q/swagger-ui

## Architecture

See MIGRATION.md for details on the module structure.

## Running Tests

```bash
./mvnw test
```
```

#### `AGENTS.md`

Copy from original with Quarkus-specific updates.

---

### 9. Git Initialize ⏳ PENDING

```bash
cd ~/projects/github/multi-module-quarkus
git init
git add .
git commit -m "feat: initial Quarkus migration from Spring Boot

- Create multi-module structure with Quarkus 3.18.4
- Migrate domain module (Mono → Uni)
- Migrate service module (@ApplicationScoped CDI)
- Add module POMs with Quarkus dependencies
- Create MIGRATION.md tracking progress"
```

---

## Verification Commands

After completing each module:

```bash
# Compile
./mvnw clean compile

# Test single module
./mvnw test -pl app/domain

# Run application
./mvnw quarkus:dev -pl app/application
```

---

## Next Steps

1. Complete Repository module (`CacheRepositoryImpl.java`)
2. Complete Outbound module (`AdviceSlipClient`, `AdviceApiImpl`, mappers, models)
3. Complete Inbound module (`MainController`, DTOs, mappers)
4. Complete Application module (`Application.java`, `application.properties`)
5. Complete Architecture tests
6. Create documentation
7. Initialize Git repository

Call me with: **"Complete Repository module"** to continue.