# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

A Spring Boot 3.2.3 / Spring Cloud 2023.0.0 e-commerce backend split into 7 independent Maven modules (Java 17, groupId `com.ecommerce`). There is **no aggregator/parent POM and no Maven wrapper** — each module is a standalone Spring Boot app built and run on its own.

Modules and default ports (all ports/URLs are injected via env vars, see below):

| Module | Port | Role |
|---|---|---|
| `config-server` | 8888 | Spring Cloud Config (native profile) |
| `eureka-server` | 8761 | Service registry |
| `api-gateway` | 8080 | Single entry point; routing, rate limiting, circuit breaking |
| `auth-service` | (env) | Issues JWTs, owns credentials/roles/permissions |
| `user-service` | 8081 | User profiles (no passwords) |
| `product-service` | 8082 | Catalog + inventory |
| `order-service` | 8083 | Checkout; calls user/product via Feign |

## Build, run, test

Each module builds independently. From a module directory (e.g. `product-service/`):

```bash
mvn clean package
```
```bash
mvn spring-boot:run
```

There is **no `mvnw`** — a system `mvn` (targeting JDK 17) is required. **There are currently no tests** in any module; `mvn test` is a no-op. When adding tests, add `spring-boot-starter-test` to that module's POM first.

**Startup order matters** (services fail or mis-register if started out of order):
`config-server` → `eureka-server` → `auth-service` / `user-service` / `product-service` → `order-service` → `api-gateway`. Wait ~30s after boot for Eureka registration. The gateway also needs **Redis** running (used by the rate limiter).

All external API calls go through the gateway on **8080**; never hit downstream service ports directly. See [WiseWrites/04_API_Testing_Guide.md](WiseWrites/04_API_Testing_Guide.md) for a full end-to-end request walkthrough.

## Configuration (env-var driven — this is the #1 source of startup failures)

Service `application.yml` files contain **only env-var placeholders** (`${SERVER_PORT}`, `${DB_URL}`, `${JWT_SECRET}`, etc.) with no defaults. These must be provided at runtime.

- **`.env` files are gitignored and not committed.** No `.env.example` exists — you must know/create the variables.
- **Only `auth-service` auto-loads a `.env`** file (via `spring.config.import: optional:file:.env[.properties]`). The other services (`user`, `product`, `order`, `gateway`) do **not** import a `.env` — their env vars must come from the actual process environment (shell export or IDE run-configuration).
- `config-server` uses the `native` profile with `search-locations: classpath:/config/`, but **that `config/` directory is currently empty** — centralized config is wired up but unused, so each service effectively reads its own `application.yml`. Config imports use `optional:` so services still boot if the config server is down.

Required env vars by service: `SERVER_PORT`, `CONFIG_SERVER_URL`, `EUREKA_URL`, `DB_URL`/`DB_USERNAME`/`DB_PASSWORD` (Postgres, one DB per service), `REDIS_HOST`/`REDIS_PORT` (gateway), and the JWT vars below.

## Authentication architecture (distributed JWT validation)

- **`auth-service` is the only token issuer.** It signs **separate access and refresh tokens** with **separate secrets** (`JWT_ACCESS_SECRET` / `JWT_REFRESH_SECRET`) and expirations (`JWT_ACCESS_EXPIRATION` / `JWT_REFRESH_EXPIRATION`, in minutes). It also seeds an admin from `ADMIN_USERNAME` / `ADMIN_PASSWORD`.
- **Downstream services validate JWTs locally and independently.** Each of `user`/`product`/`order` has its own copy of `security/JwtValidator` + `JwtAuthenticationFilter` + `SecurityConfig`. They are stateless (`SessionCreationPolicy.STATELESS`), CSRF disabled, `anyRequest().authenticated()`, with `@EnableMethodSecurity` so controllers use `@PreAuthorize("hasRole('ADMIN')")`.
- **The gateway does NOT validate JWTs** — it only routes and forwards the `Authorization: Bearer <token>` header. Auth enforcement happens in each downstream service.
- **Key-derivation gotcha:** `auth-service` derives its HMAC key from `Decoders.BASE64.decode(secret)`, while downstream `JwtValidator`s derive it from `secret.getBytes(UTF_8)` on a single `JWT_SECRET`. These derivations differ, so the downstream `JWT_SECRET` must be configured to produce the *same key bytes* as the access-token secret, or signature validation will silently fail (filter clears context → 401). Keep this in mind whenever touching JWT secrets or signing code.
- The filter puts the token `subject` (user id) as the principal and maps the `roles` claim to `SimpleGrantedAuthority`. Invalid tokens are swallowed (context cleared → downstream 401), not thrown.

## Inter-service communication

`order-service` orchestrates checkout using **OpenFeign** clients (`feign/UserClient`, `feign/ProductClient`) that resolve targets by Eureka service name (e.g. `@FeignClient(name = "PRODUCT-SERVICE")`) — never hardcoded URLs. Resilience is applied with **Resilience4j `@CircuitBreaker` at the service-method level** (`OrderService.placeOrder`, instance `productServiceCB`, with a `productFallback(...)` method) — the Feign interfaces themselves have no `fallback` attribute. The gateway additionally wraps each route in its own circuit breaker (`forward:/fallback` → `FallbackController`) plus a Redis-backed `RequestRateLimiter` keyed by client IP (`@ipKeyResolver`).

Checkout flow: gateway → `order-service` → Feign call to `user-service` (validate user + get address) → Feign call to `product-service` (check stock, then `PUT /products/{id}/reduce-inventory`) → persist `Order` locally.

## Conventions

Project conventions are documented in [AGENTS.md](AGENTS.md) — read it before adding code. Notable rules: Lombok (`@Data`, `@Builder`, `@RequiredArgsConstructor`, `@Slf4j`), **constructor injection only** (`@RequiredArgsConstructor`, never field `@Autowired`), interface + `Impl` service pattern (see `auth-service`'s `AuthService`/`AuthServiceImpl`), database-per-service (no cross-service DB access), and the standard package layout (`controllers`/`service`/`repository`/`models`/`dtos`/`security`/`config`/`utils`).

**Be aware AGENTS.md is partly aspirational — current code diverges from it:**
- The `GenericResponse<T>` wrapper is used **only in `auth-service`**; `user`/`product`/`order` controllers return raw DTOs directly. Match the surrounding service's existing style rather than assuming one global convention.
- No entities currently use the soft-delete pattern (`@SQLDelete`/`@SQLRestriction`), and there is no `PageResponse` wrapper in the codebase yet, despite AGENTS.md prescribing both.

When in doubt, follow the pattern already present in the specific module you're editing.

## Reference docs

The [WiseWrites/](WiseWrites) directory contains a tutorial-style architecture walkthrough (`01_Infrastructure_Services.md`, `02_Business_Services.md`, `03_System_Architecture_Flow.md`, `04_API_Testing_Guide.md`). It explains intent and request flow well, but its code snippets are simplified — trust the actual source files over the docs for current behavior.

## CodeGraph

This repo has a `.codegraph/` index. Prefer `codegraph_explore` (MCP tool or `codegraph explore "<query>"` shell) over grep/find when locating or understanding code.
