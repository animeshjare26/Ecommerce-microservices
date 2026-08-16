# AI Agent Instructions for Creatives House Microservices Platform

This document contains critical project instructions for AI agents working on the Creatives House microservices platform. All agents MUST read and follow these guidelines.

## 1. Coding Conventions
- **Lombok everywhere**: Use `@Data`, `@Builder`, `@RequiredArgsConstructor`, `@Slf4j`
- **Constructor injection** via `@RequiredArgsConstructor` (never `@Autowired` on fields for new code)
- **Interface-Implementation pattern**: All services have an interface + `Impl` class
- **GenericResponse wrapper**: ALL API responses must be wrapped in `GenericResponse<T>`
- **Package structure**: `controllers/`, `service/` (with `impl/`), `repository/`, `models/`, `dtos/request/`, `dtos/response/`, `security/`, `config/`, `utils/`
- **Entity naming**: Singular (`Post`, `User`, `Role`); table naming: plural (`posts`, `users`, `roles`)

## 2. Architectural Rules
1. **ALL external requests MUST go through the Gateway** (port 8080). Never expose downstream services directly.
2. **Distributed JWT Validation**: Downstream services (e.g., product, order, user) validate JWTs independently using their local `JwtValidator` and `JwtAuthenticationFilter`. The Gateway simply routes the `Authorization: Bearer <token>` header.
3. **12-Factor Configuration**: Environment variables are strictly injected via `.env` files into `application.yml` placeholders. Do not hardcode secrets, ports, or URLs in `.yml` files.
4. **Each microservice owns its database**. No cross-service database access. Use Feign/REST for data.
5. **Feign clients must have fallbacks** for resilience.
6. **Soft-delete pattern** must be used for all data entities (use `@SQLDelete` + `@SQLRestriction`).
7. **Pagination** must use `PageResponse<T>` wrapper for list endpoints.

## 3. General Directives
- Keep changes minimal and focused on the task.
- Reuse existing patterns and utility classes instead of creating new ones (e.g., `GenericResponse`, Feign clients, and distributed JWT validation via `JwtValidator`).
- Verify your work by running relevant tests and type checks.
- Always check `.env` files for configuration before adding hardcoded values.
