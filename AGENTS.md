# AGENTS.md guide

## 1. Repository Overview
- The multi-module-architecture is a multi-module Maven project demonstrating Clean Architecture and Hexagonal Architecture patterns.
- This is to showcase how separation of concerns is kept strict in separate modules, to give a low cognitive experience for humans.
- Low cognitive load is mostly relevant in integrated development environment(IDE). 

**Technology Stack**
- Java 21+
- Quarkus 3.15.x
- Maven 3.9+
- H2 (in-memory database)
- Quarkus RESTEasy Reactive

---

## 2. Project Structure, Code style & patterns 

```
multi-module-architecture/
├── app/
│   ├── inbound/
│   │   ├── rest/             # REST controllers, DTOs
│   │   └── consumer/         # Message consumers (future)
│   ├── outbound/
│   │   ├── advice-slip-api/  # External API client for Advice Slip
│   │   └── publisher/        # Message publishers (future)
│   ├── application/          # Quarkus application entry point
│   ├── architecture-tests/   # ArchUnit architecture tests
│   ├── domain/               # Domain models & interfaces
│   ├── repository/           # Database and infrastructure
│   └── service/              # Business logic
├── doc/                      # Documentation & diagrams
├── AGENTS.md                 # Agents guidelines
├── SECURITY.md               # Security policy
├── .github/                  # GitHub workflows
└── pom.xml                   # Parent POM
```

### General principles

1. **Always check existing modules first** - Understand the architecture before making changes
2. **Keep the module boundaries clean** - Don't mix concerns across modules
3. **Domain layer should be vendor-agnostic** - No framework annotations in domain/
4. **Use Immutables and MapStruct** - Already configured in POMs
5. **main branch** - is off limits, when changing code always create a branch
6. **Run tests before committing** - Use `./mvnw test`
7. **Port/adapter pattern** - External APIs go in `app/outbound/`, interfaces in `app/domain/`
8. **Port/adapter pattern** - Inbound APIs go in `app/inbound/`, no Interfaces, external parties should rely on 
     OpenAPI documentation, or negotiated message structures for async messaging

### Type safety & Code quality

- Language: Java 21+; use strict typing; avoid raw types and unchecked casts.
- Formatting: `./mvnw format` (or project formatter); run before commits.
- Verification: `./mvnw verify` runs all checks; fix violations, don't suppress.
- Never use `@SuppressWarnings` without justification; fix root causes instead.
- Never use reflection or runtime bytecode manipulation to share class behavior; use inheritance/composition.
- If reflection is needed, stop and get explicit approval; prefer interfaces and DI.
- In tests, use Mockito `@Mock` per-instance stubs; avoid static state mutation.
- Add brief comments for tricky logic (explain WHY, not WHAT).
- Keep files under ~500 LOC; extract helpers instead of "V2" copies.
- Immutables: use `@Value.Immutable` with `*Def` interface pattern; generates immutable implementations.
- MapStruct: use interfaces with `componentModel = "cdi"`; don't manually implement mappers.
- Reactive: avoid `.block()` in WebFlux code paths; embrace reactive patterns.
- Naming: follow Quarkus/CDI conventions (`@ApplicationScoped`, `@Singleton`, `@Path`, `@Inject`).

### Formatting
- Use the generic Google Java Style formatting
- Use 2 spaces for indentation.
- Always include curly braces, even for single-line `if` statements.

---

## 3. Agentic workflow (The "How-To")
When you are tasked with a feature or bug fix, follow this exact sequence:

### Phase 1: Exploration & Plan
1. **Search:** Locate relevant logic using `grep` or symbol search.
2. **Propose:** Briefly summarize your plan in the chat before writing code.

### Phase 2: Implementation & Testing
1. **Branch from a clean main** Make sure you have all the latest from main branch, create new branch from here
2. **Execute:** Modify files. Do not delete comments unless they are obsolete.
3. **Local Validation:**
   - Build command: `./mvnw clean compile`
   - Test command: `./mvnw test`
4. **Self-Correction:** If tests fail, analyze the logs, fix the code, and re-run tests until green. **Do not ask for help until you have attempted 2 logical fixes.**

---
### ⚡ Quick Reference

| Action | Command |
|--------|---------|
| Full build | `./mvnw clean package` |
| Run app | `./mvnw quarkus:dev -pl app/application` |
| Run tests | `./mvnw test` |
| Build specific module | `./mvnw install -pl <module> -am` |
| Check dependencies | `./mvnw dependency:tree` |
| Default port | 8080, but local is 8081 |
| Swagger UI | http://localhost:8081/q/swagger-ui/ |

### 🔧 Common Issues & Solutions

**Problem:** "Unable to find main class"
```bash
# Make sure to run from application module
./mvnw quarkus:dev -pl app/application
```

**Problem:** MapStruct/Immutables conflicts
```bash
# Ensure annotation processor order is correct (already configured in pom.xml)
# If issues persist, run:
./mvnw clean compile
```

**Problem:** Port already in use
```bash
# Kill process on port 8081
lsof -ti:8081 | xargs kill -9
# Or run on different port
./mvnw quarkus:dev -Dquarkus.http.port=8082
```


## Testing Guidelines

### Test Framework & Coverage
- Framework: JUnit 5 (Jupiter) with Mockito and AssertJ.
- Coverage: JaCoCo thresholds (config in parent pom.xml); run `./mvnw test` before commits.
- Unit tests: `*Test.java` suffix; integration tests: `*IT.java` suffix.
- Run `./mvnw test` (or `./mvnw verify` for integration tests) before pushing when you touch logic.

### Test Execution & Performance
- Do not set Surefire/Failsafe forkCount above 6; tried already.
- If local test runs cause memory pressure, use: `MAVEN_OPTS="-Xmx1g -XX:MaxMetaspaceSize=512m" ./mvnw test`.
- Test profiles: use `-Dquarkus.profile=test` for test configuration.

### Live Tests (Real APIs/Keys)
- Live tests: `@TestProfile(LiveTestProfile.class)` annotation; run with `./mvnw test -Dquarkus.profile=live`.
- Full integration test docs: `doc/testing.md` (if exists) or project README.

### Test Patterns & Best Practices
- Mocking: Use Mockito `@Mock`, `@InjectMock`, `@QuarkusTest` with `@Inject`.
- Integration tests: `@QuarkusTest`, configure H2 in `application-test.properties`, Dev Services for external deps.
- Reactive tests: Use Mutiny patterns with `await()` or AssertJ fluent assertions.
- Assertions: Prefer AssertJ fluent assertions (`assertThat(...)`).
- Method naming: Use `@DisplayName("descriptive test name")` for clarity.

---

## 4. Pull Request (PR) requirements

### Communication within the repo
- **Repo:** https://github.com/jnie/multi-module-architecture
- **In chat replies** file references must be repo-root relative only (example: `app/inbound/rest/src/main/java/dk/jnie/example/controllers/MainController.java`); never absolute paths or `~/...`.
- **GitHub PR Summary:** What was changed and why.
- **Breaking Changes:** Explicitly state if any APIs or asset formats were modified. 
- **GitHub issues/comments/PR comments** use literal multiline strings never embed "\\n".
- **GitHub comments** never use `gh issue/pr comment -b "..."` when body contains backticks or shell chars.
- **GitHub linking** don’t wrap issue/PR refs like `#24643` in backticks when you want auto-linking. Use plain `#24643` (optionally add full URL).
- **PR landing comments** always make commit SHAs clickable with full commit links (both landed SHA + source SHA when present).
- **PR review conversations** if a bot leaves review conversations on your PR, address them and resolve those conversations yourself once fixed. Leave a conversation unresolved only when reviewer or maintainer judgment is still needed; do not leave bot-conversation cleanup to maintainers.
- **Security advisory analysis** before triage/severity decisions, read `SECURITY.md` to align with agreed trust model and design boundaries.
- **Risk Assessment:** Label as [Low/Medium/High] risk.

---

## 5. Constraints & Boundaries
- **Dependencies:** Do not add new external libraries without explicit user approval.
- **Secrets:** Never commit `.env` files or API keys.
- The project uses **reactive programming** (Reactor) style, keep that style.
- **NO-GO zone** External Advice Slip API (https://api.adviceslip.com/) is off limits, auto generation of classes are part of build
