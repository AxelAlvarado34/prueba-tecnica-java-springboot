# Coworking Service

Microservicio de gestión de reservas de espacios de coworking. Spring Boot 3.5.16, Java 21, PostgreSQL, JWT, patrón State para el ciclo de vida de la reserva, Circuit Breaker (Resilience4j), notificaciones asíncronas, caché y despliegue con Docker.

## Ejecución

Requiere Docker Desktop corriendo.

```bash
docker-compose down -v
docker-compose up --build
```

Levanta PostgreSQL y la aplicación (puerto `8080`). El esquema se crea automáticamente al arrancar (Hibernate) y se siembra un usuario administrador.

**Verificación:** `curl http://localhost:8080/actuator/health`

**Documentación:** Swagger en `/swagger-ui.html` (botón "Authorize" para pegar el token una vez), y `coworking-service-requests.http` en la raíz del repo con ejemplos de todos los endpoints.

## Credenciales por defecto

| Campo | Valor |
|---|---|
| Email | `admin@coworking.com` |
| Contraseña | `Admin123!` |
| Rol | `ADMIN` |

Se crea automáticamente al arrancar (`AdminSeeder`). Para probar como usuario normal, regístrate con `POST /api/auth/register` (siempre asigna rol `USER`).

## Endpoints

### Autenticación

| Método | Endpoint | Rol requerido |
|---|---|---|
| POST | `/api/auth/register` | Público |
| POST | `/api/auth/login` | Público |

### Espacios

| Método | Endpoint | Rol requerido |
|---|---|---|
| POST | `/api/spaces` | ADMIN |
| PUT | `/api/spaces/{id}` | ADMIN |
| DELETE | `/api/spaces/{id}` | ADMIN |
| GET | `/api/spaces/{id}` | Autenticado |
| GET | `/api/spaces` | Autenticado |

### Reservas

| Método | Endpoint | Rol requerido |
|---|---|---|
| POST | `/api/reservations` | Autenticado |
| POST | `/api/reservations/{id}/cancel` | Dueño de la reserva o ADMIN |
| POST | `/api/reservations/{id}/confirm` | ADMIN |
| POST | `/api/reservations/{id}/complete` | ADMIN |
| GET | `/api/reservations/mine` | Autenticado |
| GET | `/api/reservations` | ADMIN |

### Reportes

| Método | Endpoint | Rol requerido |
|---|---|---|
| GET | `/api/reports/occupancy?start=...&end=...` | ADMIN |

### Observabilidad (Actuator)

| Método | Endpoint | Rol requerido |
|---|---|---|
| GET | `/actuator/health` | Público |
| GET | `/actuator/info` | Público |
| GET | `/actuator/metrics` | Público |
| GET | `/actuator/circuitbreakers` | Público |
| GET | `/actuator/circuitbreakerevents` | Público |

## Manejo centralizado de errores

`GlobalExceptionHandler` (`@RestControllerAdvice`) traduce cada excepción de negocio a una respuesta HTTP consistente:

| Excepción | Código HTTP |
|---|---|
| `ResourceNotFoundException` | 404 |
| `OverlappingReservationException` | 409 |
| `EmailAlreadyExistsException` | 409 |
| `InvalidReservationStateException` | 400 |
| `BadCredentialsException` | 401 |
| `AuthorizationDeniedException` (fallo de `@PreAuthorize`) | 403 |
| `MethodArgumentNotValidException` (validación de campos) | 400 |
| Cualquier otra excepción no controlada | 500 |

Todas las respuestas de error siguen el mismo formato (`ErrorResponse`), incluyendo timestamp, código de estado, mensaje y ruta.

## Decisiones de diseño y trade-offs

- **Patrón State** para el ciclo de vida de la reserva (`PENDING`, `PENDING_PAYMENT`, `CONFIRMED`, `CANCELLED`, `COMPLETED`): handlers sin estado + factory, en vez de `if/else` repetido en el servicio.
- **Concurrencia:** lock pesimista (`SELECT ... FOR UPDATE`) sobre el `Space` antes de validar solapamiento e insertar, para evitar doble reserva bajo peticiones simultáneas.
- **Circuit Breaker** (Resilience4j) sobre la validación de pago: éxito → `CONFIRMED`, fallo o circuito abierto → `PENDING_PAYMENT`. La URL del gateway apunta a un host inexistente a propósito, para que el comportamiento sea reproducible sin depender de un servicio externo real.
- **Notificación asíncrona** vía evento (`ApplicationEventPublisher` + `@TransactionalEventListener(AFTER_COMMIT)` + `@Async` con executor acotado), para no bloquear la respuesta HTTP y evitar notificar transacciones que terminaron en rollback.
- **`ddl-auto=update`** en vez de migraciones (Flyway/Liquibase): más simple para el alcance de la prueba; en producción se usaría `validate` + migraciones versionadas.
- **Actuator público** (`permitAll`) para facilitar la evaluación; en producción se restringiría por rol/red.
- Perfiles `dev`/`prod` con `@ConfigurationProperties` (nunca `@Value` disperso); secretos de producción vía variables de entorno.
- Sin refresh token ni paginación en los listados — suficiente para el alcance de esta prueba.

## Qué quedó fuera de alcance

- Prueba dedicada con WireMock (dependencia ya incluida) para verificar de forma automatizada la apertura/recuperación del circuit breaker — se validó manualmente contra `/actuator/circuitbreakers` y `/actuator/health`.
- Job `@Scheduled` para marcar `COMPLETED` automáticamente al vencer una reserva `CONFIRMED` — hoy esa transición es 100% manual (`POST /api/reservations/{id}/complete`).
- Cobertura de tests más amplia (`SpaceService`, `AuthService`, handlers de estado por separado). Se incluyeron tests unitarios (Mockito) sobre la lógica de solapamiento/pago en `ReservationService`, y 2 de integración (`@SpringBootTest` + H2) sobre el flujo de reserva y control de acceso por rol.
- Refresh token, paginación en los listados, migraciones versionadas (Flyway/Liquibase).