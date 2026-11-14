# access-control-service

Microservicio de control de acceso de DistriLink: **CRUD de Usuario** (Fase 1).
Puerto `8081` · Base de datos `auth_db` · Spring Boot 4.1.2 + Java 21.

## Endpoints (Swagger: http://localhost:8081/swagger-ui.html)

| Método | Ruta | Qué hace |
|---|---|---|
| POST | `/api/users` | Registra un usuario (201). 409 si el correo ya existe |
| GET | `/api/users?rol=CLIENTE&estado=ACTIVO` | Lista usuarios; los filtros son opcionales |
| GET | `/api/users/{id}` | Consulta un usuario (404 si no existe) |
| PUT | `/api/users/{id}` | Actualiza nombre, correo, rol y estado. La contraseña es opcional |
| DELETE | `/api/users/{id}` | **Desactiva** la cuenta (estado `INACTIVO`, 204). No borra el registro |

**JSON de prueba para el POST** (demostración):
```json
{
  "nombre": "Tienda La Esquina",
  "correo": "tienda@correo.com",
  "contrasena": "Clave1234",
  "rol": "CLIENTE"
}
```

**JSON de prueba para el PUT**:
```json
{
  "nombre": "Tienda La Esquina SAS",
  "correo": "tienda@correo.com",
  "rol": "CLIENTE",
  "estado": "ACTIVO"
}
```

## Reglas que ya cumple

- **RF1.2** correo único (se guarda en minúsculas; 409 si se repite).
- **RF1.3** contraseña de 8 a 72 caracteres, con letras y números.
- **RNF5.2** la contraseña se guarda con **BCrypt**; la API nunca la devuelve.
- **RF1.12** "eliminar" desactiva la cuenta y conserva la información: el `id` de este usuario lo guardan
  `order-service` y `notification-service` (`recipient_id`) en otras bases de datos, así que el registro no se borra.
- Una cuenta nueva siempre nace `ACTIVO`.

## Pruebas

`.\mvnw test` — 10 pruebas del Service (Mockito) y 7 del Controller (MockMvc).

## Próximas fases (no incluidas aquí)

Login con JWT, bloqueo tras 5 intentos fallidos, recuperar contraseña y cerrar sesión (RF1.4 a RF1.10).
Se agregarán con migraciones nuevas (`V2__...`); la `V1` no se edita.
