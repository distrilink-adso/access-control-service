# Base de los 4 microservicios de DistriLink

Cada carpeta es un proyecto de **Spring Boot 4.1.2 + Java 21** listo para subir a su propio repositorio (Multi-Repo).

| Carpeta | Dueño | Puerto | Base de datos | Paquete |
|---|---|---|---|---|
| `access-control-service` | Sebastián Calderón | 8081 | `auth_db` | `com.distrilink.accesscontrol` |
| `catalog-service` | Luis Alfredo Corrales | 8082 | `catalog_db` | `com.distrilink.catalog` |
| `order-service` | Jhojan Melo | 8083 | `order_db` | `com.distrilink.order` |
| `notification-service` | Juan José Cuellar | 8084 | `notification_db` | `com.distrilink.notification` |

## Qué trae el `pom.xml`

- `spring-boot-starter-webmvc`: la API REST. En Spring Boot 4 el antiguo `starter-web` se llama así.
- `spring-boot-starter-data-jpa`: la capa de entidades (Models) y repositorios (Repositories).
- `spring-boot-starter-validation`: valida los DTOs.
- `spring-boot-starter-flyway` + `flyway-mysql`: las migraciones. En Spring Boot 4 se necesitan **las dos**.
- `mysql-connector-j`: el conector de MySQL.
- `springdoc-openapi-starter-webmvc-ui` **3.2.0**: Swagger. Esta versión está hecha sobre Spring Boot 4.1.2. **No usen la 2.x.**
- `spring-boot-starter-actuator`: expone `/actuator/health`.
- `lombok` y `devtools`.
- Starters de pruebas para TDD: JUnit 5, Mockito y MockMvc.

## Cómo arrancar cada servicio

1. Instalar **JDK 21** (o una versión más nueva) y tener **MySQL 8** encendido.
2. Definir el usuario y la clave de MySQL como variables de entorno. Nunca se escriben en el código.
   - En IntelliJ: *Run > Edit Configurations > Environment variables*: `DB_USER=root;DB_PASSWORD=tu_clave`
   - En PowerShell, antes de arrancar: `$env:DB_USER="root"; $env:DB_PASSWORD="tu_clave"`
3. Crear la primera migración en `src/main/resources/db/migration/`, por ejemplo `V1__create_products_table.sql`.
4. Crear la `@Entity` con los **mismos** nombres de columnas que la migración. Si no coinciden, `ddl-auto=validate` impide que el servicio arranque.
5. Ejecutar el servicio:
   ```
   .\mvnw spring-boot:run
   ```
6. Revisar que en el log aparezca `Successfully applied 1 migration` y que el servicio arranque sin errores.
7. Abrir Swagger: `http://localhost:<puerto>/swagger-ui.html`

La base de datos se crea sola la primera vez gracias a `createDatabaseIfNotExist=true` en la URL. Las **tablas** las crea Flyway, nunca Hibernate.

## Estructura de capas que pide la guía

```
com.distrilink.<servicio>
├── model        ← @Entity
├── repository   ← interfaces JpaRepository
├── service      ← lógica de negocio
├── controller   ← @RestController
└── dto          ← Request / Response (nunca devolver la @Entity)
```

## CRUD de ejemplo: `notification-service`

`notification-service` ya trae un CRUD completo para que los demás lo tomen como modelo:

| Capa | Archivo |
|---|---|
| Migración | `db/migration/V1__create_notifications_table.sql` |
| Model | `model/Notification.java` (y los enums `NotificationType`, `NotificationChannel`, `NotificationStatus`) |
| Repository | `repository/NotificationRepository.java` |
| Service | `service/NotificationService.java` |
| Controller | `controller/NotificationController.java` |
| DTO | `dto/NotificationRequest.java` (entrada, con validaciones) y `dto/NotificationResponse.java` (salida) |
| Errores | `exception/GlobalExceptionHandler.java` (404 y 400 en JSON claro) |
| Pruebas | `NotificationServiceTest` (6) y `NotificationControllerTest` (4): `.\mvnw test` |

**Endpoints** (en `http://localhost:8084/swagger-ui.html`):

| Método | Ruta | Qué hace |
|---|---|---|
| POST | `/api/notifications` | Crea una notificación (201) |
| GET | `/api/notifications?recipientId=15` | Lista las notificaciones; el filtro por usuario es opcional |
| GET | `/api/notifications/{id}` | Consulta una notificación (404 si no existe) |
| PUT | `/api/notifications/{id}` | La actualiza |
| PATCH | `/api/notifications/{id}/read` | La marca como leída |
| DELETE | `/api/notifications/{id}` | La elimina (204) |

**JSON de prueba para el POST** (para la demostración con la instructora):
```json
{
  "recipientId": 15,
  "orderId": 120,
  "type": "ORDER_CONFIRMED",
  "title": "Pedido aprobado",
  "message": "Tu pedido #120 fue aprobado por Distribuidora El Sol."
}
```

> ⚠️ **Enums en MySQL:** Hibernate 7 espera columnas `ENUM` para los campos `@Enumerated`. Como en la migración usamos `VARCHAR`, cada campo enum lleva además `@JdbcTypeCode(SqlTypes.VARCHAR)`. Si lo quitan, `ddl-auto=validate` no deja arrancar el servicio.
