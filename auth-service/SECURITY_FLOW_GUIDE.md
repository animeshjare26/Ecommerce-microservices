# Auth-Service — Security Flow Guide

A step-by-step, file-by-file walkthrough of the `auth-service`. Read this top-to-bottom
with the source open beside you; every step links to the exact file (and line) it describes,
so you can navigate the real code as you go.

> **What this service is.** `auth-service` is the **only token issuer** in the whole system.
> It owns user *identities* (username, email, password hash) and *authorization* data
> (roles + permissions). It hands out JWTs on register/login, refreshes them, and — as a
> side job — tells the `user-service` to create a matching **profile** for every new identity.
> It does **not** store profile data (name, address); that lives in `user-service`.

---

## 0. The 30-second mental model

```
          ┌─────────────────────────────────────────────────────────────┐
  client  │                       AUTH-SERVICE                          │
  ──────► │                                                             │
 /auth/*  │  AuthController → AuthService(Impl) → { DB, JwtUtils,       │
          │        │              │                 AuthenticationMgr } │
          │        │              └── UserProfileClient ──(Feign)──► user-service
          │        ▼                                                    │
          │  SecurityFilterChain (JwtAuthenticationFilter guards        │
          │   every non-public request using JwtValidator)              │
          └─────────────────────────────────────────────────────────────┘
```

Two things happen in this service:

1. **Issuing identity** — `register`, `login`, `refresh` create/verify users and mint JWTs.
2. **Guarding itself** — a security filter chain protects any *other* endpoint by validating
   the JWT on the way in. (Downstream services like `user`/`product`/`order` do their own
   validation independently; the gateway just forwards the token.)

The three layers you'll see repeated everywhere:

| Layer | Responsibility | Files |
|---|---|---|
| **Controller** | HTTP in/out, validation, wrapping responses | [`AuthController`](src/main/java/com/ecommerce/authservice/controllers/AuthController.java) |
| **Service** | Business logic (the "what") | [`AuthService`](src/main/java/com/ecommerce/authservice/service/AuthService.java) + [`AuthServiceImpl`](src/main/java/com/ecommerce/authservice/service/impl/AuthServiceImpl.java) |
| **Repository / Entity** | Database access + table mapping | [`AuthUserRepository`](src/main/java/com/ecommerce/authservice/repository/AuthUserRepository.java), [`AuthUser`](src/main/java/com/ecommerce/authservice/models/AuthUser.java) |

Supporting cast: **security** (JWT machinery), **config** (beans + seeding), **dtos** (request/response
envelopes), **feign** (the phone line to user-service).

---

## 1. What happens at startup (before any request)

Understanding boot order makes the request flows obvious later.

**Step 1 — The engine starts.**
[`AuthServiceApplication`](src/main/java/com/ecommerce/authservice/AuthServiceApplication.java:31)
is the entry point. Three annotations matter:
- `@SpringBootApplication` — scans this package and auto-wires all controllers/services/repos.
- `@EnableDiscoveryClient` — registers this service with **Eureka** so others can find it by name.
- `@EnableFeignClients` — turns the [`UserProfileClient`](src/main/java/com/ecommerce/authservice/feign/UserProfileClient.java)
  interface into a real HTTP client at runtime.

**Step 2 — Config is bound from `application.yml` / env vars.**
[`application.yml`](src/main/resources/application.yml) contains **only env-var placeholders**
(`${SERVER_PORT}`, `${DB_URL}`, `${JWT_ACCESS_SECRET}`, …). This service is the only one that also
imports a local `.env` file (line 10). Two config classes bind these values into objects:
- [`JwtConfig`](src/main/java/com/ecommerce/authservice/config/JwtConfig.java:48) reads the `jwt.access.*`
  and `jwt.refresh.*` blocks (secret + expiration) into a typed object, and in
  [`init()`](src/main/java/com/ecommerce/authservice/config/JwtConfig.java:67) indexes them by
  [`TokenType`](src/main/java/com/ecommerce/authservice/enums/TokenType.java).
- [`AuthConfig`](src/main/java/com/ecommerce/authservice/config/AuthConfig.java:31) exposes the
  **BCrypt** `PasswordEncoder` bean used to hash/verify passwords.

**Step 3 — The security perimeter is built.**
[`SecurityConfig`](src/main/java/com/ecommerce/authservice/security/SecurityConfig.java:59) assembles
the `SecurityFilterChain`: CORS on, CSRF off, **stateless** sessions, `/auth/login` + `/auth/register`
+ `/auth/internal/**` public, everything else authenticated, and our custom
[`JwtAuthenticationFilter`](src/main/java/com/ecommerce/authservice/security/JwtAuthenticationFilter.java)
inserted into the chain. It also wires the `AuthenticationManager` and `AuthenticationProvider`
used by login.

**Step 4 — The database is seeded.**
[`AdminSeeder`](src/main/java/com/ecommerce/authservice/config/AdminSeeder.java:33) implements
`CommandLineRunner`, so its [`run()`](src/main/java/com/ecommerce/authservice/config/AdminSeeder.java:52)
executes once on boot. It creates (if missing) the permissions `ALL_ACCESS`/`READ_ONLY`, the roles
`ROLE_ADMIN`/`ROLE_USER`, and a seeded admin account from `ADMIN_USERNAME`/`ADMIN_PASSWORD`. This is
why `ROLE_USER` exists in the DB and can be attached to every new registrant.

---

## 2. Flow A — Registration (`POST /auth/register`)

**Goal:** create an identity, give it a default role, mint tokens, and (best-effort) create the
matching profile in `user-service`.

**Step 1 — Request arrives at the controller.**
[`AuthController.register()`](src/main/java/com/ecommerce/authservice/controllers/AuthController.java:52).
The JSON body is deserialized into a
[`RegisterRequest`](src/main/java/com/ecommerce/authservice/dtos/request/RegisterRequest.java:26).
`@Valid` triggers the DTO's constraints **before** your code runs: `username`/`email`/`password`
non-blank, `email` well-formed, `password` ≥ 8 chars. `name` and `address` are optional profile
seed fields — they are *not* stored here, only forwarded to `user-service`.

**Step 2 — Delegate to the service.** The controller does no logic; it calls
`authService.register(request)` — the interface
[`AuthService.register`](src/main/java/com/ecommerce/authservice/service/AuthService.java:35),
implemented by
[`AuthServiceImpl.register`](src/main/java/com/ecommerce/authservice/service/impl/AuthServiceImpl.java:67).

**Step 3 — Uniqueness checks.** Inside the impl:
- [`existsByUsername`](src/main/java/com/ecommerce/authservice/service/impl/AuthServiceImpl.java:70)
  → 409 CONFLICT if taken.
- [`existsByEmail`](src/main/java/com/ecommerce/authservice/service/impl/AuthServiceImpl.java:77)
  → 409 CONFLICT if taken (email is a unique **identity** field — see next note).

**Step 4 — Build and hash.** A new
[`AuthUser`](src/main/java/com/ecommerce/authservice/models/AuthUser.java:39) entity is populated.
The password is **hashed with BCrypt** at
[line 89](src/main/java/com/ecommerce/authservice/service/impl/AuthServiceImpl.java:89) — plain text
is never stored. The default `ROLE_USER` is fetched from the DB via
[`RoleRepository.findByName`](src/main/java/com/ecommerce/authservice/repository/RoleRepository.java:21)
and attached.

**Step 5 — Persist.**
[`repository.save(user)`](src/main/java/com/ecommerce/authservice/service/impl/AuthServiceImpl.java:99).
After this, `saved.getId()` is the DB-generated id. **This id is the linchpin of the whole system:**
it becomes the JWT `subject` *and* the primary key of the user-service profile, keeping the two
services correlated with zero id-translation.

**Step 6 — Sync the profile (best-effort dual write).**
[`provisionProfileSafely`](src/main/java/com/ecommerce/authservice/service/impl/AuthServiceImpl.java:128)
builds a [`ProvisionProfileRequest`](src/main/java/com/ecommerce/authservice/feign/ProvisionProfileRequest.java:33)
(carrying `id`, `email`, `name`, `address`) and calls
[`UserProfileClient.provisionProfile`](src/main/java/com/ecommerce/authservice/feign/UserProfileClient.java:47),
which Feign turns into `POST http://USER-SERVICE/users/internal/provision`. Two design choices to
notice:
- **The call is wrapped in try/catch and failures are swallowed** (logged, not thrown). The identity
  is already committed and the user can log in; a down `user-service` must not throw away a good
  account. The classic *dual-write* problem — two databases can't share one transaction.
- **The request carries a shared secret header.**
  [`FeignInternalAuthConfig`](src/main/java/com/ecommerce/authservice/config/FeignInternalAuthConfig.java:45)
  is a Feign `RequestInterceptor` (wired only into this client via `configuration =` on the
  `@FeignClient`) that stamps `X-Internal-Key` onto every outgoing call, so `user-service` can trust
  the caller.

**Step 7 — Mint tokens and respond.**
[`toResponse`](src/main/java/com/ecommerce/authservice/service/impl/AuthServiceImpl.java:237) flattens
the role/permission tree into a comma-separated string via
[`buildRolesString`](src/main/java/com/ecommerce/authservice/service/impl/AuthServiceImpl.java:259),
then calls [`JwtUtils.generateToken`](src/main/java/com/ecommerce/authservice/security/JwtUtils.java:44)
twice (access + refresh). The result is an
[`AuthResponse`](src/main/java/com/ecommerce/authservice/dtos/response/AuthResponse.java:26). Back in
the controller, it's wrapped in `GenericResponse.success(...)` and returned as **201 Created**.

```
register: Controller → Impl → [existsByUsername/Email] → save() → provisionProfileSafely()(Feign) → toResponse()(JwtUtils) → 201
```

---

## 3. Flow B — Login (`POST /auth/login`)

**Goal:** verify username + password, then mint fresh tokens.

**Step 1 — Controller.**
[`AuthController.login()`](src/main/java/com/ecommerce/authservice/controllers/AuthController.java:65)
validates a [`LoginRequest`](src/main/java/com/ecommerce/authservice/dtos/request/LoginRequest.java:24)
(`@NotBlank` username + password — no length check here, unlike register) and delegates.

**Step 2 — Authenticate.**
[`AuthServiceImpl.login()`](src/main/java/com/ecommerce/authservice/service/impl/AuthServiceImpl.java:172)
hands a `UsernamePasswordAuthenticationToken` to the **`AuthenticationManager`**
([line 178](src/main/java/com/ecommerce/authservice/service/impl/AuthServiceImpl.java:178)). This is
Spring Security's engine. It uses the `DaoAuthenticationProvider` configured in
[`SecurityConfig.authenticationProvider()`](src/main/java/com/ecommerce/authservice/security/SecurityConfig.java:108),
which:
1. calls a **`UserDetailsService`** to load the user by username from the DB, then
2. uses the **BCrypt** `PasswordEncoder` to compare the submitted password against the stored hash.

The loaded user is adapted to Spring's `UserDetails` contract by
[`UserDetailsImpl`](src/main/java/com/ecommerce/authservice/security/UserDetailsImpl.java:22)
(it exposes id, username, password hash, authorities, and active-flag). If credentials are wrong,
the manager throws, and the impl maps it to **401 Unauthorized**
([line 204](src/main/java/com/ecommerce/authservice/service/impl/AuthServiceImpl.java:204)).

> ⚠️ **Gap to be aware of (see §8).** The `DaoAuthenticationProvider` needs a concrete
> `UserDetailsServiceImpl` bean (referenced at
> [`SecurityConfig:52`](src/main/java/com/ecommerce/authservice/security/SecurityConfig.java:52)),
> but **that class is not present in the repository.** Until it's added, login can't load users.

**Step 3 — Mint tokens.** On success, the authenticated principal
([`UserDetailsImpl`](src/main/java/com/ecommerce/authservice/security/UserDetailsImpl.java)) is read
for id/username/roles, and
[`JwtUtils.generateToken`](src/main/java/com/ecommerce/authservice/security/JwtUtils.java:44) produces
access + refresh tokens. Returned as an `AuthResponse` wrapped in `GenericResponse`, **200 OK**.

```
login: Controller → Impl → AuthenticationManager → DaoAuthProvider → [UserDetailsService + BCrypt] → JwtUtils → 200
```

---

## 4. Flow C — Refresh (`POST /auth/refresh`)

**Goal:** trade a valid refresh token for a brand-new access+refresh pair (no password needed).

**Step 1 — Controller.**
[`AuthController.refresh()`](src/main/java/com/ecommerce/authservice/controllers/AuthController.java:78)
takes a [`TokenRequest`](src/main/java/com/ecommerce/authservice/dtos/request/TokenRequest.java:21)
(`{ "token": "..." }`).

**Step 2 — Validate + reissue.**
[`AuthServiceImpl.refreshAccessToken()`](src/main/java/com/ecommerce/authservice/service/impl/AuthServiceImpl.java:212):
- [`JwtUtils.getClaimsFromToken(REFRESH_TOKEN, ...)`](src/main/java/com/ecommerce/authservice/security/JwtUtils.java:89)
  verifies the refresh token's signature with the **refresh secret** and extracts the `subject`
  (user id). A tampered/expired token throws here.
- The user is re-loaded via
  [`repository.findById`](src/main/java/com/ecommerce/authservice/service/impl/AuthServiceImpl.java:218)
  (401 if gone), roles rebuilt, and a fresh token pair minted.
- Returns a `Map<TokenType,String>` of the two new tokens.

```
refresh: Controller → Impl → JwtUtils.getClaimsFromToken(REFRESH) → findById → JwtUtils.generateToken ×2 → 200
```

---

## 5. Flow D — Calling a *protected* endpoint (how the JWT gate works)

`/register`, `/login`, `/refresh` are public. Any *other* endpoint in this service is guarded by the
filter chain. This is the path an authenticated request takes:

**Step 1 — The filter intercepts.**
[`JwtAuthenticationFilter.doFilterInternal`](src/main/java/com/ecommerce/authservice/security/JwtAuthenticationFilter.java:44)
(a `OncePerRequestFilter`, so it runs exactly once per request). It reads the
`Authorization: Bearer <token>` header
([line 49](src/main/java/com/ecommerce/authservice/security/JwtAuthenticationFilter.java:49)) and
strips the `Bearer ` prefix.

**Step 2 — Validate the signature.**
[`JwtValidator.validateToken`](src/main/java/com/ecommerce/authservice/security/JwtValidator.java:41)
parses and verifies the token. If it's forged or expired, it throws.

**Step 3 — Populate the security context.** On success, the filter reads the `subject` (user id) and
`roles` claim, maps roles to `SimpleGrantedAuthority`, builds an authentication token, and places it
into the `SecurityContextHolder`
([line 82](src/main/java/com/ecommerce/authservice/security/JwtAuthenticationFilter.java:82)). Now the
request is "logged in" for the rest of the chain. **On any failure the context is cleared**
([line 89](src/main/java/com/ecommerce/authservice/security/JwtAuthenticationFilter.java:89)) — the
exception is swallowed, so the request simply continues as anonymous.

**Step 4 — Access decision.** If the endpoint required auth and the context is empty, Spring rejects
the request and routes it to
[`AuthEntryPointJwt.commence`](src/main/java/com/ecommerce/authservice/security/AuthEntryPointJwt.java:38),
which writes a clean **401 JSON** (via `GenericResponse.error`) instead of a default HTML page.
Because `@EnableMethodSecurity` is on, controllers can additionally use `@PreAuthorize("hasRole('ADMIN')")`.

> **JWT key derivation — the #1 silent-failure trap.**
> [`JwtUtils.getKey`](src/main/java/com/ecommerce/authservice/security/JwtUtils.java:65) derives the
> HMAC key with `Decoders.BASE64.decode(secret)`, while
> [`JwtValidator.init`](src/main/java/com/ecommerce/authservice/security/JwtValidator.java:34) uses
> `secret.getBytes(UTF_8)` on a *different* property (`jwt.secret`, not `jwt.access.secret`). These
> derivations differ. `JwtValidator`/`jwt.secret` is the pattern the **downstream** services use;
> within auth-service itself the issuing path (`JwtUtils`) is what register/login/refresh rely on.
> Whenever you touch secrets, make sure the *bytes* line up or signature checks fail silently
> (context cleared → 401).

---

## 6. Flow E — Internal reconciliation (`POST /auth/internal/reconcile`)

**Goal (ops/maintenance):** heal profiles that failed to sync during registration, or backfill
profiles for accounts created before the sync feature existed.

**Step 1 — Controller (with its own auth).**
[`AuthController.reconcile()`](src/main/java/com/ecommerce/authservice/controllers/AuthController.java:104).
This route is `permitAll()` in the filter chain
([`SecurityConfig:83`](src/main/java/com/ecommerce/authservice/security/SecurityConfig.java:83))
because it's not called by end users and has no JWT — instead the controller itself checks the
`X-Internal-Key` header against the configured secret and returns **403** on mismatch.

**Step 2 — Re-provision everyone.**
[`AuthServiceImpl.reconcileProfiles`](src/main/java/com/ecommerce/authservice/service/impl/AuthServiceImpl.java:157)
loads all identities and re-sends each through the same best-effort
`provisionProfileSafely(...)`. The user-service upsert is **idempotent** (keyed on `id`), so existing
profiles are untouched and only missing ones get created.

---

## 7. Complete file index (by layer)

Every file, what it does, and where it plugs into the flows above. The `#N` is the ordering number
written in each file's own header comment.

### Entry point & config
| # | File | Role |
|---|---|---|
| 16 | [`AuthServiceApplication`](src/main/java/com/ecommerce/authservice/AuthServiceApplication.java) | Boots Spring; enables Eureka discovery + Feign clients. |
| 2 | [`JwtConfig`](src/main/java/com/ecommerce/authservice/config/JwtConfig.java) | Binds `jwt.access.*` / `jwt.refresh.*` (secret + expiry) into a typed, `TokenType`-indexed object. |
| 10 | [`AuthConfig`](src/main/java/com/ecommerce/authservice/config/AuthConfig.java) | Provides the BCrypt `PasswordEncoder` bean. |
| 11 | [`AdminSeeder`](src/main/java/com/ecommerce/authservice/config/AdminSeeder.java) | On startup, seeds permissions, roles, and the admin account. |
| — | [`FeignInternalAuthConfig`](src/main/java/com/ecommerce/authservice/config/FeignInternalAuthConfig.java) | Interceptor that stamps `X-Internal-Key` onto outgoing user-service calls. |
| — | [`application.yml`](src/main/resources/application.yml) | Env-var-driven config (DB, Eureka, JWT secrets, internal key). |

### Security perimeter
| # | File | Role |
|---|---|---|
| 1 | [`SecurityConfig`](src/main/java/com/ecommerce/authservice/security/SecurityConfig.java) | The filter chain: public routes, stateless sessions, provider/manager wiring, CORS. |
| 2 | [`JwtAuthenticationFilter`](src/main/java/com/ecommerce/authservice/security/JwtAuthenticationFilter.java) | Per-request gate: reads Bearer token, validates, populates security context. |
| 3 | [`JwtUtils`](src/main/java/com/ecommerce/authservice/security/JwtUtils.java) | **Issues** & parses tokens (used by register/login/refresh). |
| — | [`JwtValidator`](src/main/java/com/ecommerce/authservice/security/JwtValidator.java) | **Validates** incoming tokens for the filter. |
| — | [`UserDetailsImpl`](src/main/java/com/ecommerce/authservice/security/UserDetailsImpl.java) | Adapts `AuthUser` to Spring Security's `UserDetails`. |
| 3 | [`AuthEntryPointJwt`](src/main/java/com/ecommerce/authservice/security/AuthEntryPointJwt.java) | Turns auth rejections into clean 401 JSON. |
| — | ⚠️ `UserDetailsServiceImpl` | **Referenced but missing** — see §8. Should load users by username for login. |

### Web layer
| # | File | Role |
|---|---|---|
| 6 | [`AuthController`](src/main/java/com/ecommerce/authservice/controllers/AuthController.java) | Endpoints: `/register`, `/login`, `/refresh`, `/internal/reconcile`. |
| 9 | [`GlobalExceptionHandler`](src/main/java/com/ecommerce/authservice/controllers/GlobalExceptionHandler.java) | Catches exceptions → uniform `GenericResponse` JSON. |

### Service layer
| # | File | Role |
|---|---|---|
| 4 | [`AuthService`](src/main/java/com/ecommerce/authservice/service/AuthService.java) | Interface / contract. |
| 5 | [`AuthServiceImpl`](src/main/java/com/ecommerce/authservice/service/impl/AuthServiceImpl.java) | All business logic for register/login/refresh/reconcile. |

### Data layer
| # | File | Role |
|---|---|---|
| 7 | [`AuthUser`](src/main/java/com/ecommerce/authservice/models/AuthUser.java) | Identity entity (`auth_users`): username, email, hash, roles, soft-delete. |
| — | [`Role`](src/main/java/com/ecommerce/authservice/models/Role.java) | Role entity + its permissions (`roles`, `role_permissions`). |
| — | [`Permission`](src/main/java/com/ecommerce/authservice/models/Permission.java) | Granular permission entity (`permissions`). |
| 8 | [`AuthUserRepository`](src/main/java/com/ecommerce/authservice/repository/AuthUserRepository.java) | `findByUsername`, `existsByUsername`, `existsByEmail`. |
| — | [`RoleRepository`](src/main/java/com/ecommerce/authservice/repository/RoleRepository.java) | `findByName` (used to attach `ROLE_USER`). |
| — | [`PermissionRepository`](src/main/java/com/ecommerce/authservice/repository/PermissionRepository.java) | `findByName` (used by the seeder). |

### DTOs & enums
| # | File | Role |
|---|---|---|
| 12 | [`LoginRequest`](src/main/java/com/ecommerce/authservice/dtos/request/LoginRequest.java) | Login payload. |
| 13 | [`RegisterRequest`](src/main/java/com/ecommerce/authservice/dtos/request/RegisterRequest.java) | Register payload (+ optional `name`/`address` profile seeds). |
| 14 | [`TokenRequest`](src/main/java/com/ecommerce/authservice/dtos/request/TokenRequest.java) | Refresh payload. |
| 15 | [`AuthResponse`](src/main/java/com/ecommerce/authservice/dtos/response/AuthResponse.java) | Token + basic-user response. |
| 1 | [`TokenType`](src/main/java/com/ecommerce/authservice/enums/TokenType.java) | `ACCESS_TOKEN` / `REFRESH_TOKEN` / `FORGOT_PASSWORD`. |
| — | [`GenericResponse`](src/main/java/com/ecommerce/authservice/utils/GenericResponse.java) | Uniform `{success, message, data}` envelope. |

### Inter-service (Feign)
| File | Role |
|---|---|
| [`UserProfileClient`](src/main/java/com/ecommerce/authservice/feign/UserProfileClient.java) | Declarative HTTP client → `user-service` provisioning endpoint. |
| [`ProvisionProfileRequest`](src/main/java/com/ecommerce/authservice/feign/ProvisionProfileRequest.java) | The wire contract for profile creation (`id`, `email`, `name`, `address`). |

---

## 8. Key concepts & gotchas (read before changing anything)

1. **The `id` is the correlation key.** `auth_users.id` == the JWT `subject` == the user-service
   profile's primary key. This is deliberate and load-bearing — don't break it.
   ([save](src/main/java/com/ecommerce/authservice/service/impl/AuthServiceImpl.java:99),
   [ProvisionProfileRequest](src/main/java/com/ecommerce/authservice/feign/ProvisionProfileRequest.java:35))

2. **Passwords are BCrypt hashes, always.** Never store or log plaintext.
   ([hash](src/main/java/com/ecommerce/authservice/service/impl/AuthServiceImpl.java:89),
   [encoder bean](src/main/java/com/ecommerce/authservice/config/AuthConfig.java:31))

3. **Two secrets, two token types.** Access and refresh tokens are signed with **different** secrets
   and expirations (`JwtConfig`). Refresh validation uses the refresh secret specifically.

4. **JWT key-derivation mismatch (silent-failure trap).** `JwtUtils` uses `BASE64.decode`;
   `JwtValidator` uses `getBytes(UTF_8)` on a separate `jwt.secret`. See §5. Align the *bytes*, or
   validation silently fails.

5. **Dual-write is best-effort by design.** Profile provisioning is swallowed on failure and healed
   later by `reconcileProfiles()`; the user-service upsert must stay idempotent for this to be safe.

6. **Stateless everywhere.** No sessions; every request must carry its own JWT. CSRF is off because
   there are no cookies to protect.

7. **RBAC is roles → permissions.** `AuthUser` → many `Role` → many `Permission`. All of it is
   flattened into a single comma-separated `roles` claim in the JWT by
   [`buildRolesString`](src/main/java/com/ecommerce/authservice/service/impl/AuthServiceImpl.java:259).

8. **⚠️ Missing class: `UserDetailsServiceImpl`.** It is injected in
   [`SecurityConfig:52`](src/main/java/com/ecommerce/authservice/security/SecurityConfig.java:52)
   and named in the login comment at
   [`AuthServiceImpl:175`](src/main/java/com/ecommerce/authservice/service/impl/AuthServiceImpl.java:175),
   but **no such file exists** in the repo or git history. It's the piece that loads a user by
   username (returning a `UserDetailsImpl`) so `DaoAuthenticationProvider` can compare passwords.
   Until it's added, the auth-service won't compile / login won't work. If you're studying the login
   flow, this is the one link that's currently absent.

---

## 9. Suggested reading order (first time through)

1. [`TokenType`](src/main/java/com/ecommerce/authservice/enums/TokenType.java) — the vocabulary.
2. [`AuthUser`](src/main/java/com/ecommerce/authservice/models/AuthUser.java) →
   [`Role`](src/main/java/com/ecommerce/authservice/models/Role.java) →
   [`Permission`](src/main/java/com/ecommerce/authservice/models/Permission.java) — the data model.
3. [`RegisterRequest`](src/main/java/com/ecommerce/authservice/dtos/request/RegisterRequest.java) /
   [`AuthResponse`](src/main/java/com/ecommerce/authservice/dtos/response/AuthResponse.java) — the wire shapes.
4. [`AuthController`](src/main/java/com/ecommerce/authservice/controllers/AuthController.java) →
   [`AuthServiceImpl`](src/main/java/com/ecommerce/authservice/service/impl/AuthServiceImpl.java) — follow §2 (register) then §3 (login).
5. [`SecurityConfig`](src/main/java/com/ecommerce/authservice/security/SecurityConfig.java) →
   [`JwtAuthenticationFilter`](src/main/java/com/ecommerce/authservice/security/JwtAuthenticationFilter.java) →
   [`JwtUtils`](src/main/java/com/ecommerce/authservice/security/JwtUtils.java) /
   [`JwtValidator`](src/main/java/com/ecommerce/authservice/security/JwtValidator.java) — follow §5 (the gate).
6. [`UserProfileClient`](src/main/java/com/ecommerce/authservice/feign/UserProfileClient.java) +
   [`FeignInternalAuthConfig`](src/main/java/com/ecommerce/authservice/config/FeignInternalAuthConfig.java) — the profile-sync side story (§6).

---

## 10. Complete end-to-end sequence flows

The sections above explain *why* each step exists. These diagrams show the *exact call chain* —
every hop annotated with `file:line` so you can single-step through the code. Read them like a
stack trace from top to bottom. `▶` = a method call, `◀` = a return, `✗` = an error branch.

### 10.0 Init flow (application startup — happens once)

```
main(args)                                     AuthServiceApplication.java:33
 ▶ SpringApplication.run(...)                   AuthServiceApplication.java:34
    │  @SpringBootApplication  → component-scan controllers/services/repos/config
    │  @EnableDiscoveryClient  → will register with Eureka
    │  @EnableFeignClients     → build UserProfileClient proxy
    │
    ├─▶ bind @ConfigurationProperties("jwt")    JwtConfig.java:48
    │     └─▶ @PostConstruct init()             JwtConfig.java:67
    │           → tokenConfigMap{ ACCESS→{secret,exp}, REFRESH→{secret,exp} }
    │
    ├─▶ @Bean passwordEncoder() = BCrypt        AuthConfig.java:31
    │
    ├─▶ @Bean securityFilterChain(http)         SecurityConfig.java:59
    │     • CORS on, CSRF off, STATELESS
    │     • permitAll: /auth/login, /auth/register, /auth/internal/**   SecurityConfig.java:78-83
    │     • anyRequest().authenticated()        SecurityConfig.java:86
    │     • addFilterBefore(JwtAuthenticationFilter)   SecurityConfig.java:98
    │     └─▶ @Bean authenticationProvider()    SecurityConfig.java:108
    │           = DaoAuthenticationProvider( UserDetailsServiceImpl, BCrypt )   ⚠️ see §8
    │     └─▶ @Bean authenticationManager(cfg)  SecurityConfig.java:119
    │
    └─▶ CommandLineRunner.run(...)              AdminSeeder.java:52   (runs after context is ready)
          • createPermissionIfNotFound ALL_ACCESS / READ_ONLY      AdminSeeder.java:55-56
          • createRoleIfNotFound ROLE_ADMIN / ROLE_USER            AdminSeeder.java:59-60
          • if admin missing → save seeded admin (BCrypt password)  AdminSeeder.java:63-73
 ◀ context refreshed → registers with Eureka → "Auth Service Started"   AuthServiceApplication.java:35
```

**Why order matters:** `JwtConfig.init()` must run before any token is minted; `AdminSeeder` must run
after the repositories + `PasswordEncoder` beans exist; the filter chain must be built before the
first request is served.

---

### 10.1 Register flow (`POST /auth/register`)

```
CLIENT ─── POST /auth/register {username,email,password,name?,address?} ───▶ (gateway → auth-service)
│
▶ AuthController.register(@Valid RegisterRequest)                 AuthController.java:52
│    └─ @Valid runs RegisterRequest constraints FIRST            RegisterRequest.java:28-48
│         @NotBlank username/email/password · @Email · @Size(min=8)
│         ✗ any violation → 400 Bad Request (never reaches service)
│
▶ authService.register(request)                                  AuthService.java:35
│    └─(interface → impl)                                        AuthServiceImpl.java:67
│
│  ┌─ AuthServiceImpl.register ─────────────────────────────────────────────┐
│  │ ▶ repository.existsByUsername(username)   AuthUserRepository.java:51     │
│  │      ✗ true → throw 409 CONFLICT "Username already taken"  :70           │
│  │ ▶ repository.existsByEmail(email)         AuthUserRepository.java:65     │
│  │      ✗ true → throw 409 CONFLICT "Email already registered" :77         │
│  │ ▶ new AuthUser(); setUsername/setEmail    AuthUser.java:39               │
│  │ ▶ passwordEncoder.encode(password)  ── BCrypt hash ──       :89          │
│  │ ▶ roleRepository.findByName("ROLE_USER")  RoleRepository.java:21         │
│  │      ✗ empty → throw 500 "Default role not found"          :92-93        │
│  │ ▶ AuthUser saved = repository.save(user)                    :99          │
│  │      └─ INSERT auth_users → saved.getId() (THE correlation key)          │
│  │ ▶ provisionProfileSafely(saved, name, address)              :105         │
│  │      │  try {                                               :128         │
│  │      │   ▶ userProfileClient.provisionProfile(              UserProfileClient.java:47
│  │      │        ProvisionProfileRequest{id,email,name,address})  ProvisionProfileRequest.java:33
│  │      │        └─ FeignInternalAuthConfig stamps X-Internal-Key  FeignInternalAuthConfig.java:45
│  │      │        └─ Eureka resolves USER-SERVICE → POST /users/internal/provision
│  │      │  } catch(Exception) → log.error, SWALLOW              :138-142    │
│  │      │     (identity already saved; reconcile() heals later)             │
│  │ ▶ return toResponse(saved)                                  :108 → :237  │
│  │      └─ buildRolesString(user)  flatten roles+perms → "ROLE_USER,READ_ONLY"  :259
│  │      └─ jwtUtils.generateToken(ACCESS_TOKEN, id, claims)     :241  ─▶ see 10.3
│  │      └─ jwtUtils.generateToken(REFRESH_TOKEN, id, claims)    :244  ─▶ see 10.3
│  │      └─ new AuthResponse(access,refresh,"Bearer",id,username,roles)  AuthResponse.java:26
│  └───────────────────────────────────────────────────────────────────────┘
│
◀ ResponseEntity 201 CREATED  GenericResponse.success(AuthResponse)   AuthController.java:58
CLIENT ◀── {success:true, message:"success", data:{accessToken, refreshToken, ...}}
```

---

### 10.2 Login flow (`POST /auth/login`)

```
CLIENT ─── POST /auth/login {username,password} ───▶ auth-service
│
▶ AuthController.login(@Valid LoginRequest)                      AuthController.java:65
│    └─ @Valid: @NotBlank username/password (NO length check)    LoginRequest.java:24-30
│
▶ authService.login(request)  →(impl)                           AuthServiceImpl.java:172
│
│  ┌─ AuthServiceImpl.login ────────────────────────────────────────────────┐
│  │ ▶ authenticationManager.authenticate(                       :178         │
│  │      new UsernamePasswordAuthenticationToken(username,password))          │
│  │   └─▶ DaoAuthenticationProvider  (wired in SecurityConfig.java:108)      │
│  │        ▶ UserDetailsServiceImpl.loadUserByUsername(username) ⚠️ MISSING §8│
│  │             ▶ AuthUserRepository.findByUsername(...)  AuthUserRepository.java:38
│  │             ◀ UserDetailsImpl.build(user, authorities)  UserDetailsImpl.java:34
│  │        ▶ passwordEncoder.matches(raw, storedHash)  ── BCrypt verify ──    │
│  │             ✗ mismatch/not found → BadCredentialsException                │
│  │   ✗ any auth failure → catch → throw 401 "Invalid credentials"  :202-204 │
│  │ ◀ Authentication (authenticated=true)                                     │
│  │ ▶ userDetails = authentication.getPrincipal()               :183         │
│  │ ▶ roles = authorities joined by ","                         :186-188     │
│  │ ▶ jwtUtils.generateToken(ACCESS_TOKEN,  id, claims)         :191  ─▶ 10.3 │
│  │ ▶ jwtUtils.generateToken(REFRESH_TOKEN, id, claims)         :192  ─▶ 10.3 │
│  │ ◀ new AuthResponse(...)                                      :194         │
│  └───────────────────────────────────────────────────────────────────────┘
│
◀ ResponseEntity 200 OK  GenericResponse.success(AuthResponse)   AuthController.java:71
CLIENT ◀── {success:true, data:{accessToken, refreshToken, tokenType:"Bearer", userId, username, roles}}
```

**Register vs Login — the key difference:** register *creates* the user (uniqueness + hash + save +
profile sync) and trusts the input; login *verifies* an existing user through Spring Security's
`AuthenticationManager` (which loads the user and BCrypt-compares the password). Both end the same
way: `generateToken ×2 → AuthResponse`.

---

### 10.3 Token generation flow (`JwtUtils.generateToken`, shared by register/login/refresh)

```
▶ jwtUtils.generateToken(tokenType, subject, claims)            JwtUtils.java:44
│   ▶ jwtConfig.getTokenConfigByType(tokenType)                 JwtConfig.java:78
│        ◀ TokenConfig{ secret, expiration(minutes) }   (from ACCESS or REFRESH)
│   ▶ Jwts.builder()                                            JwtUtils.java:49
│        .subject(subject)          → "sub" = user id           :51
│        .claims(claims)            → id(UUID), username, roles  :52
│        .issuedAt(now)             → "iat"                      :53
│        .expiration(now + exp*60*1000)  → "exp"                 :55
│        .signWith( getKey(secret), HS256 )                      :57
│              └─▶ getKey(secret): Keys.hmacShaKeyFor(           JwtUtils.java:65
│                     Decoders.BASE64.decode(secret) )   ◀── BASE64 decode!
│        .compact()                 → "<header>.<payload>.<signature>"  :58
◀ signed JWT string
```

Access and refresh tokens differ only by which `TokenConfig` (secret + expiry) is selected — same
builder, same algorithm (HMAC-SHA256).

---

### 10.4 Refresh flow (`POST /auth/refresh`)

```
CLIENT ─── POST /auth/refresh {token:"<refreshJWT>"} ───▶ auth-service
│
▶ AuthController.refresh(@Valid TokenRequest)                   AuthController.java:78
▶ authService.refreshAccessToken(request) →(impl)              AuthServiceImpl.java:212
│   ▶ jwtUtils.getClaimsFromToken(REFRESH_TOKEN, token)        JwtUtils.java:89
│        └─▶ getAllClaimsFromToken → Jwts.parser()             JwtUtils.java:77-83
│               .verifyWith(getKey(refreshSecret)).parseSignedClaims(token)
│               ✗ tampered/expired → throws → 500 handler / 401
│        ◀ {subject=userId, id}
│   ▶ repository.findById(subject)                              AuthServiceImpl.java:218
│        ✗ empty → throw 401 "Unauthorized"                     :219
│   ▶ buildRolesString(user)                                   :221
│   ▶ jwtUtils.generateToken(REFRESH_TOKEN, id, claims)        :224  ─▶ 10.3
│   ▶ jwtUtils.generateToken(ACCESS_TOKEN,  id, claims)        :225  ─▶ 10.3
◀ Map{ REFRESH_TOKEN:…, ACCESS_TOKEN:… } → 200 OK              AuthController.java:83
```

---

### 10.5 Validation flow (any protected request carrying a Bearer token)

```
CLIENT ─── GET /auth/<protected>  Header: Authorization: Bearer <accessJWT> ───▶ auth-service
│
▶ SecurityFilterChain  (built in SecurityConfig.java:59)
│   ▶ JwtAuthenticationFilter.doFilterInternal(req,res,chain)  JwtAuthenticationFilter.java:44
│        ▶ authHeader = req.getHeader("Authorization")          :49
│        │   ── no header / not "Bearer " → skip auth, stay anonymous ──  :53
│        ▶ token = authHeader.substring(7)                      :56
│        ▶ try {                                                :58
│        │    ▶ jwtValidator.validateToken(token)              JwtValidator.java:41
│        │         Jwts.parser().verifyWith(key)                :42-46
│        │            key = hmacShaKeyFor(secret.getBytes(UTF_8))  JwtValidator.java:34-38  ◀── UTF-8 bytes!
│        │         .parseSignedClaims(token).getPayload()
│        │         ✗ forged/expired → throws
│        │    ◀ Claims{ subject=userId, roles:[...] }           :61
│        │    ▶ userId = claims.getSubject()                    :64
│        │    ▶ roles = claims.get("roles", List)               :66
│        │    ▶ authorities = roles → SimpleGrantedAuthority     :71-73
│        │    ▶ auth = UsernamePasswordAuthenticationToken(userId,null,authorities)  :77
│        │    ▶ SecurityContextHolder.getContext().setAuthentication(auth)  :82  ◀ REQUEST NOW "LOGGED IN"
│        │  } catch(Exception) → SecurityContextHolder.clearContext()  :89  (stay anonymous)
│        ▶ chain.doFilter(req,res)                              :96
│
│   ── access decision ──
│   ├─ context set + rule satisfied → ▶ Controller ( + @PreAuthorize("hasRole(...)") if present )
│   └─ context empty / unauthorized → ▶ AuthEntryPointJwt.commence(...)   AuthEntryPointJwt.java:38
│            → 401 JSON  GenericResponse.error(null, message)   :50-53
```

> **Two different key derivations (§4/§5 restated at the call site):** the **issuer** path
> (`JwtUtils.getKey`, [line 65](src/main/java/com/ecommerce/authservice/security/JwtUtils.java:65))
> uses `Decoders.BASE64.decode(secret)` on the per-type `jwt.access.secret`; the **validator** path
> (`JwtValidator.init`, [line 34](src/main/java/com/ecommerce/authservice/security/JwtValidator.java:34))
> uses `secret.getBytes(UTF_8)` on a single `jwt.secret`. For a token minted here to also validate
> here (or downstream), the resulting **key bytes must match** — otherwise `parseSignedClaims` throws,
> the context is cleared, and you get a silent 401.

---

### 10.6 One-glance summary — where every flow ends up

| Flow | Public? | Verifies via | Mints tokens? | Success code |
|---|---|---|---|---|
| Register (10.1) | ✅ | uniqueness checks | ✅ ×2 | 201 |
| Login (10.2) | ✅ | `AuthenticationManager` + BCrypt | ✅ ×2 | 200 |
| Refresh (10.4) | ✅ | `JwtUtils` (refresh secret) | ✅ ×2 | 200 |
| Validation (10.5) | ❌ (guarded) | `JwtValidator` (per request) | ❌ | 200 / 401 |
| Reconcile (§6) | ✅ (key-guarded) | `X-Internal-Key` header | ❌ | 200 / 403 |
