# Coworking Service

Microservicio backend para la gestión de reservas de espacios de coworking, desarrollado como prueba técnica para la posición de Backend Developer en Banco Cuscatlán.

Construido con Spring Boot 3.5.16 y Java 21, cubriendo modelado de dominio, autenticación con JWT, control de concurrencia, un patrón de diseño GoF aplicado al ciclo de vida de las reservas, resiliencia ante fallas de servicios externos, notificaciones asíncronas, reportes con caché y despliegue completo con Docker.

---

## Tabla de contenido

1. [Stack tecnológico](#stack-tecnológico)
2. [Arquitectura y modelo de dominio](#arquitectura-y-modelo-de-dominio)
3. [Patrón de diseño: State](#patrón-de-diseño-state)
4. [Seguridad y autenticación](#seguridad-y-autenticación)
5. [Concurrencia y validación de solapamiento](#concurrencia-y-validación-de-solapamiento)
6. [Resiliencia: Circuit Breaker](#resiliencia-circuit-breaker)
7. [Notificaciones asíncronas](#notificaciones-asíncronas)
8. [Reporte de ocupación y caché](#reporte-de-ocupación-y-caché)
9. [Perfiles de configuración](#perfiles-de-configuración)
10. [Cómo ejecutar el proyecto](#cómo-ejecutar-el-proyecto)
11. [Credenciales por defecto](#credenciales-por-defecto)
12. [Documentación de la API](#documentación-de-la-api)
13. [Referencia de endpoints](#referencia-de-endpoints)
14. [Manejo centralizado de errores](#manejo-centralizado-de-errores)
15. [Historial de ramas (Git Flow)](#historial-de-ramas-git-flow)
16. [Decisiones de diseño y trade-offs](#decisiones-de-diseño-y-trade-offs)
17. [Qué quedó fuera de alcance](#qué-quedó-fuera-de-alcance)

---

## Stack tecnológico

| Categoría | Tecnología |
|---|---|
| Lenguaje / Runtime | Java 21 |
| Framework | Spring Boot 3.5.16 |
| Persistencia | Spring Data JPA + Hibernate, PostgreSQL |
| Seguridad | Spring Security 6, JWT (jjwt 0.13.0), BCrypt |
| Validación | Bean Validation (jakarta.validation) |
| Resiliencia | Resilience4j (Circuit Breaker) vía Spring Cloud |
| Documentación de API | springdoc-openapi (Swagger UI) |
| Caché | Spring Cache (in-memory) |
| Observabilidad | Spring Boot Actuator |
| Contenedores | Docker, Docker Compose |
| Build | Maven |
| Utilidades de código | Lombok |

---

## Arquitectura y modelo de dominio

El proyecto sigue una arquitectura por capas clásica de Spring:

```
controller   -> expone los endpoints REST
service      -> logica de negocio y transacciones
repository   -> acceso a datos (Spring Data JPA)
model        -> entidades JPA
dto          -> records inmutables para entrada/salida (nunca se expone la entidad JPA directamente)
mapper       -> conversion entre entidad y DTO
security     -> JWT, filtros, configuracion de Spring Security
state         -> patron State para el ciclo de vida de la reserva
event / notification -> notificacion asincrona basada en eventos
exception    -> excepciones de negocio + manejador centralizado
config       -> configuracion transversal (async, cache, propiedades)
```

### Entidades principales

**Space** (`spaces`): representa un espacio reservable.
- `id`, `name`, `type` (`MEETING_ROOM`, `DESK`, `PRIVATE_OFFICE`), `capacity`, `location`, `hourlyRate`.

**User** (`users`): usuario del sistema.
- `id`, `email` (único), `password` (hash BCrypt), `role` (`ADMIN`, `USER`).

**Reservation** (`reservations`): una reserva de un espacio por un usuario.
- `id`, `space` (relación `@ManyToOne`, `LAZY`), `user` (relación `@ManyToOne`, `LAZY`), `startDateTime`, `endDateTime`, `status` (`ReservationState`), `createdAt`.

Las relaciones hacia `Space` y `User` se declararon explícitamente como `LAZY` (el default de JPA para `@ManyToOne` es `EAGER`) para evitar cargar datos que no siempre se necesitan y prevenir problemas de N+1 quando se listan reservas; donde sí se necesitan esos datos relacionados, se usa `@EntityGraph` para resolverlo en una sola consulta.

### Enum `ReservationState`

`PENDING`, `PENDING_PAYMENT`, `CONFIRMED`, `CANCELLED`, `COMPLETED`.

---

## Patrón de diseño: State

Se eligió el patrón **State** (Gang of Four) para modelar el ciclo de vida de una reserva, porque el comportamiento permitido (qué transiciones son válidas) depende completamente del estado actual, y la alternativa habría sido un bloque de `if/else` o `switch` repetido en cada operación del servicio.

Como una entidad JPA no puede persistir "objetos de comportamiento" (solo datos), el estado se modela en dos partes:

- La entidad `Reservation` guarda el estado como un dato simple (`enum ReservationState`).
- Un conjunto de **handlers sin estado** (`ReservationStateHandler` y sus implementaciones `PendingStateHandler`, `PendingPaymentStateHandler`, `ConfirmedStateHandler`, `CancelledStateHandler`, `CompletedStateHandler`) definen qué transiciones son válidas desde cada estado.
- Una **factory** (`ReservationStateHandlerFactory`) devuelve el handler correcto según el estado actual de la reserva.

La interfaz `ReservationStateHandler` define métodos por defecto que lanzan `InvalidReservationStateException` — así, cada handler concreto solo necesita sobreescribir las transiciones que sí permite, y cualquier transición no soportada falla automáticamente sin código adicional.

Este diseño elimina toda lógica condicional de estados del `ReservationService`: el servicio simplemente pide el handler correspondiente y le delega la transición.

---

## Seguridad y autenticación

Autenticación **stateless** basada en JWT:

- `POST /api/auth/register` y `POST /api/auth/login` son públicos; el resto de la API requiere un token válido.
- Las contraseñas se almacenan con **BCrypt** (`PasswordEncoder`).
- `JwtAuthenticationFilter` (un `OncePerRequestFilter`) valida el token en cada request y puebla el `SecurityContextHolder`, de donde el resto de la aplicación obtiene el usuario autenticado sin volver a tocar el header.
- Los roles (`ADMIN`, `USER`) se controlan con `@PreAuthorize` a nivel de método (`@EnableMethodSecurity`), y también con reglas a nivel de filtro en `SecurityFilterChain`.
- El registro público **siempre** asigna el rol `USER` — no existe un endpoint público para crear administradores, por diseño (evitar que cualquiera se autoasigne privilegios).

Para poder probar los endpoints de administrador sin necesitar acceso directo a la base de datos, se agregó `AdminSeeder` (`CommandLineRunner`): crea automáticamente un usuario ADMIN al arrancar la aplicación, si todavía no existe. Las credenciales están documentadas más abajo.

---

## Concurrencia y validación de solapamiento

El requisito de negocio más delicado es evitar que dos reservas se solapen sobre el mismo espacio, incluso bajo peticiones concurrentes.

Un simple `@Transactional` con una consulta de "¿existe solapamiento?" antes de insertar **no es suficiente**: dos transacciones pueden ejecutar esa verificación al mismo tiempo, ambas ven "sin conflicto" (porque ninguna ha insertado todavía), y ambas terminan reservando el mismo horario.

La solución implementada:

1. Antes de verificar solapamiento, se adquiere un **bloqueo pesimista** (`PESSIMISTIC_WRITE`, traducido a `SELECT ... FOR UPDATE`) sobre la fila del `Space` que se quiere reservar (`SpaceRepository.findByIdForUpdate`).
2. Mientras una transacción tiene ese bloqueo, cualquier otra transacción que intente reservar el **mismo espacio** debe esperar a que la primera termine (commit o rollback).
3. Solo entonces se ejecuta la verificación de solapamiento (`ReservationRepository.findOverlapping`, que excluye reservas `CANCELLED`) y, si no hay conflicto, se inserta la nueva reserva.

Todo esto ocurre dentro del mismo método `@Transactional`, garantizando atomicidad. Reservas sobre espacios **distintos** no se bloquean entre sí — el candado es por fila de `Space`, no global.

---

## Resiliencia: Circuit Breaker

Al crear una reserva, el sistema intenta validar el pago contra un gateway externo (simulado) antes de confirmarla. Este es exactamente el escenario para el que existe el patrón **Circuit Breaker**: si el servicio externo falla o responde lento de forma sostenida, seguir intentando puede degradar la aplicación completa.

Implementación (`PaymentGatewayClient`, con Resilience4j vía `spring-cloud-starter-circuitbreaker-resilience4j`):

- La llamada se hace con un `RestClient` configurado con timeouts cortos (2s) — así, tanto una falla dura como una respuesta lenta se convierten en una excepción que el circuit breaker puede contar.
- El método está anotado con `@CircuitBreaker(name = "paymentGateway", fallbackMethod = "fallback")`.
- Configuración del circuito (`application.properties`): ventana deslizante de 5 llamadas, mínimo 3 llamadas para evaluar, umbral de fallo del 50%, 10 segundos en estado abierto antes de reintentar, 2 llamadas de prueba en estado semi-abierto.
- El resultado de la validación decide la transición de estado (usando el mismo patrón State): pago exitoso → `CONFIRMED`; fallo o circuito abierto (fallback) → `PENDING_PAYMENT`.
- El estado del circuito se expone en `/actuator/health` y `/actuator/circuitbreakers`.

**Nota de diseño:** la URL del gateway de pago apunta, por defecto, a un host que no existe (`payment.gateway.url`). Esto es intencional: permite demostrar el comportamiento del circuit breaker (apertura, fallback, recuperación) de forma determinística y reproducible por el evaluador sin necesitar levantar un segundo servicio externo real. En un entorno productivo, esta URL apuntaría al gateway de pagos real.

---

## Notificaciones asíncronas

Cuando una reserva se confirma, el sistema debe notificar al usuario sin bloquear la respuesta HTTP mientras eso ocurre. Se implementó con un enfoque orientado a eventos:

- `ReservationService` publica un `ReservationConfirmedEvent` (un record inmutable) a través de `ApplicationEventPublisher`.
- `ReservationNotificationListener` escucha ese evento con `@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)` combinado con `@Async` — así, la notificación solo se dispara si la transacción realmente se confirmó (nunca se notifica algo que se revirtió), y corre en un hilo separado del que atendió el request.
- El executor asíncrono es un `ThreadPoolTaskExecutor` acotado, configurado explícitamente (`AsyncConfig`) en lugar de usar el executor por defecto de Spring, que crea un hilo nuevo sin límite por cada tarea.
- El evento **no** transporta la entidad `Reservation` completa: transporta un snapshot de datos simples (id, email, nombre del espacio, fechas), extraídos mientras la transacción y la sesión de Hibernate siguen abiertas. Esto evita un `LazyInitializationException`, ya que las relaciones `LAZY` de la entidad no se pueden resolver desde un hilo asíncrono una vez cerrada la sesión original.

---

## Reporte de ocupación y caché

`GET /api/reports/occupancy` calcula, para cada espacio, el porcentaje de tiempo ocupado dentro de un rango de fechas dado (excluyendo reservas `CANCELLED`, y recortando cada reserva a los límites del rango solicitado).

Este cálculo recorre todas las reservas superpuestas de todos los espacios, por lo que es un buen candidato para caché si se consulta repetidamente con el mismo rango (por ejemplo, un dashboard). Se usa `@Cacheable` sobre el método de reporte, e `@CacheEvict(allEntries = true)` sobre cada operación que modifica una reserva (crear, cancelar, confirmar, completar), para que el reporte cacheado nunca quede desactualizado.

---

## Perfiles de configuración

Toda la configuración sensible o dependiente del entorno usa `@ConfigurationProperties` (nunca `@Value` disperso).

- **`application.properties`**: configuración base compartida entre perfiles.
- **`application-dev.yml`** (perfil activo por defecto): `ddl-auto=update` para iterar rápido, logging en `DEBUG`, detalles de salud siempre visibles.
- **`application-prod.yml`**: `ddl-auto=validate` (nunca modifica el esquema solo), logging en `INFO`, detalles de salud restringidos, secretos (`jwt.secret`, `payment.gateway.url`) tomados de variables de entorno en lugar de estar escritos en el archivo.

---

## Cómo ejecutar el proyecto

### Opción recomendada: todo en Docker

Requiere Docker Desktop corriendo.

```bash
docker-compose down -v
docker-compose up --build
```

Esto construye la imagen de la aplicación (build multi-etapa: compila con JDK, corre con JRE), levanta PostgreSQL, espera a que la base de datos esté saludable (`healthcheck`), y arranca la aplicación en el puerto **8080**. Al iniciar, Hibernate crea el esquema automáticamente y se siembra el usuario administrador por defecto.

### Opción alterna: desarrollo local

1. Levanta solo la base de datos: `docker-compose up -d postgres`.
2. Corre la aplicación desde tu IDE o con `./mvnw spring-boot:run`.

### Verificación rápida

```bash
curl http://localhost:8080/actuator/health
```

---

## Credenciales por defecto

Al arrancar por primera vez, `AdminSeeder` crea automáticamente un usuario administrador si no existe uno:

| Campo | Valor |
|---|---|
| Email | `admin@coworking.com` |
| Contraseña | `Admin123!` |
| Rol | `ADMIN` |

Usa `POST /api/auth/login` con estas credenciales para obtener un token con rol de administrador. Para probar como usuario normal, regístrate con `POST /api/auth/register` (siempre asigna rol `USER`).

---

## Documentación de la API

- **Swagger UI:** `http://localhost:8080/swagger-ui.html`
- **OpenAPI JSON:** `http://localhost:8080/v3/api-docs`
- **Archivo de ejemplos `.http`:** `coworking-service-requests.http` (compatible con la extensión REST Client de VS Code, o importable en Postman), incluido en la raíz del repositorio. Contiene ejemplos de todos los endpoints, incluyendo el flujo completo de autenticación, gestión de espacios, reservas y el reporte de ocupación.

---

## Referencia de endpoints

### Autenticación

| Método | Endpoint | Acceso |
|---|---|---|
| POST | `/api/auth/register` | Público |
| POST | `/api/auth/login` | Público |

### Espacios

| Método | Endpoint | Acceso |
|---|---|---|
| POST | `/api/spaces` | ADMIN |
| PUT | `/api/spaces/{id}` | ADMIN |
| DELETE | `/api/spaces/{id}` | ADMIN |
| GET | `/api/spaces/{id}` | Autenticado |
| GET | `/api/spaces` | Autenticado |

### Reservas

| Método | Endpoint | Acceso |
|---|---|---|
| POST | `/api/reservations` | Autenticado (USER o ADMIN) |
| POST | `/api/reservations/{id}/cancel` | Dueño de la reserva o ADMIN |
| POST | `/api/reservations/{id}/confirm` | ADMIN |
| POST | `/api/reservations/{id}/complete` | ADMIN |
| GET | `/api/reservations/mine` | Autenticado |
| GET | `/api/reservations` | ADMIN |

### Reportes

| Método | Endpoint | Acceso |
|---|---|---|
| GET | `/api/reports/occupancy?start=...&end=...` | ADMIN |

### Observabilidad (Actuator)

| Método | Endpoint | Acceso |
|---|---|---|
| GET | `/actuator/health` | Público |
| GET | `/actuator/info` | Público |
| GET | `/actuator/metrics` | Público |
| GET | `/actuator/circuitbreakers` | Público |
| GET | `/actuator/circuitbreakerevents` | Público |

---

## Manejo centralizado de errores

`GlobalExceptionHandler` (`@RestControllerAdvice`) traduce cada excepción de negocio a una respuesta HTTP consistente:

| Excepción | Código HTTP |
|---|---|
| `ResourceNotFoundException` | 404 |
| `OverlappingReservationException` | 409 |
| `EmailAlreadyExistsException` | 409 |
| `InvalidReservationStateException` | 400 |
| `BadCredentialsException` | 401 |
| `MethodArgumentNotValidException` (validación de campos) | 400 |
| Cualquier otra excepción no controlada | 500 |

Todas las respuestas de error siguen el mismo formato (`ErrorResponse`), incluyendo timestamp, código de estado, mensaje y ruta.

---

## Historial de ramas (Git Flow)

El desarrollo siguió Git Flow: `main` se mantuvo intacto hasta el cierre final, todo el trabajo se integró contra `develop` a través de ramas `feature/*`, cada una correspondiente a un bloque funcional independiente:

- `feature/actuator-config`
- `feature/domain-model`
- `feature/security-jwt`
- `feature/reservation-business-logic`
- `feature/async-notification`
- `feature/payment-circuit-breaker`
- `feature/reporting-and-profiles`

---

## Decisiones de diseño y trade-offs

- **`ddl-auto=update` en lugar de una herramienta de migraciones** (Flyway/Liquibase): permite que el esquema se genere automáticamente sin pasos manuales adicionales, adecuado para el alcance de esta prueba. En un entorno productivo real se usarían migraciones versionadas, con `ddl-auto=validate` como red de seguridad.
- **Actuator completamente público** (`permitAll`): facilita la evaluación sin necesitar tokens adicionales. En producción, estos endpoints se restringirían por rol y/o red.
- **Gateway de pago simulado con una URL inexistente**: permite demostrar el circuit breaker de forma determinística sin depender de infraestructura externa adicional.
- **Sin mecanismo de refresh token**: el JWT expira en una hora sin renovación automática; suficiente para el alcance de la prueba.
- **Sin paginación** en los listados (`GET /api/spaces`, `GET /api/reservations`): aceptable para el volumen de datos de esta prueba; en producción se usaría `Pageable`.

---

## Qué quedó fuera de alcance

Por restricciones de tiempo, **no se implementaron pruebas automatizadas** (ni unitarias ni de integración). Con más tiempo, se habría priorizado:

- Pruebas unitarias con **Mockito** sobre `ReservationService`, cubriendo: rechazo de reservas solapadas, la bifurcación de confirmación según el resultado de la validación de pago, y las transiciones del patrón State.
- Al menos dos pruebas de integración con `@SpringBootTest` y una base de datos en memoria (H2), cubriendo el flujo completo de creación de una reserva contra una base de datos real, y la verificación de que los endpoints de administrador rechazan correctamente a usuarios sin el rol adecuado.
- Una prueba dedicada con **WireMock** (dependencia ya incluida en el proyecto) para simular respuestas HTTP controladas (200, 500, latencia) contra el `PaymentGatewayClient`, y verificar de forma automatizada la apertura y recuperación del circuit breaker — comportamiento que en esta entrega se validó manualmente contra `/actuator/circuitbreakers` y `/actuator/health`, con evidencia reproducible pero sin cobertura automatizada.