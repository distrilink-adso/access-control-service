# access-control-service

Microservicio de control de acceso de DistriLink: **CRUD de Usuario** (Fase 1) e **inicio de sesión con JWT**.
Puerto `8081` · Base de datos `auth_db` · Spring Boot 4.1.2 + Java 21.

## Endpoints (Swagger: http://localhost:8081/swagger-ui.html)

| Método | Ruta | Token | Qué hace |
|---|---|---|---|
| POST | `/api/auth/login` | Público | Inicia sesión y devuelve el JWT. 401 si el correo o la contraseña no coinciden; 403 si la cuenta no está `ACTIVO` |
| GET | `/api/auth/me` | Sí | Devuelve el usuario dueño del token |
| GET | `/.well-known/jwks.json` | Público | Llave pública para que los otros servicios verifiquen el token |
| POST | `/api/users` | Público | Registra un usuario (201). 409 si el correo ya existe |
| GET | `/api/users?rol=CLIENTE&estado=ACTIVO` | Sí | Lista usuarios; los filtros son opcionales |
| GET | `/api/users/{id}` | Solo el dueño | Consulta un usuario (404 si no existe) |
| PUT | `/api/users/{id}` | Solo el dueño | Actualiza nombre, correo, rol y estado. La contraseña es opcional |
| DELETE | `/api/users/{id}` | Solo el dueño | **Desactiva** la cuenta (estado `INACTIVO`, 204). No borra el registro |

Sin token se responde **401**; con el token de otro usuario, **403**. Los dos con el mismo formato JSON del resto de errores.

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

## Seguridad: login con JWT (RS256)

1. `POST /api/auth/login` con `{ "correo": "tienda@correo.com", "contrasena": "Clave1234" }` devuelve:
   ```json
   { "accessToken": "eyJ...", "tokenType": "Bearer", "expiresIn": 3600, "usuario": { "id": 15, "...": "..." } }
   ```
2. Las demás peticiones llevan el header `Authorization: Bearer eyJ...`. En Swagger: botón **Authorize**.
3. El token dura **60 minutos** y trae: `sub` (id del usuario), `email`, `name`, `role`, `iss`, `iat` y `exp`.
   El id del usuario se toma **siempre** de `sub`, nunca de un parámetro.

**Cómo lo verifican catalog, order y notification** (sin compartir ningún secreto): agregan
`spring-boot-starter-security-oauth2-resource-server` y en su `application.properties`:
```properties
spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://localhost:8081/.well-known/jwks.json
```
Spring descarga la llave pública de access-control y valida la firma de cada token.

**Llaves.** Si no se configuran, el servicio genera un par en memoria al arrancar (sirve para desarrollo, pero
los tokens dejan de valer cada vez que se reinicia). Para llaves fijas:
```
mkdir keys
openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -out keys/private.pem
openssl rsa -in keys/private.pem -pubout -out keys/public.pem
```
y antes de arrancar: `$env:JWT_PRIVATE_KEY_PATH="keys/private.pem"; $env:JWT_PUBLIC_KEY_PATH="keys/public.pem"`.
La carpeta `keys/` y los `.pem` están en `.gitignore`: **la llave privada nunca se sube**.

## Reglas que ya cumple

- **RF1.2** correo único (se guarda en minúsculas; 409 si se repite).
- **RF1.3** contraseña de 8 a 72 caracteres, con letras y números.
- **RNF5.2** la contraseña se guarda con **BCrypt**; la API nunca la devuelve.
- **RF1.12** "eliminar" desactiva la cuenta y conserva la información: el `id` de este usuario lo guardan
  `order-service` y `notification-service` (`recipient_id`) en otras bases de datos, así que el registro no se borra.
- Una cuenta nueva siempre nace `ACTIVO`.
- **RF1.4** login con JWT firmado con RS256. Un correo inexistente y una contraseña incorrecta dan el **mismo** error,
  para que no se pueda averiguar qué correos están registrados. Una cuenta `INACTIVO` o `BLOQUEADO` no recibe token.

## Pruebas

`.\mvnw test` — 15 pruebas de Services (Mockito), 3 del token (firma real RS256) y 19 de Controllers con seguridad (MockMvc).

## Próximas fases (no incluidas aquí)

Bloqueo tras 5 intentos fallidos, recuperar contraseña, refresh token y cerrar sesión (RF1.5 a RF1.10).
Se agregarán con migraciones nuevas (`V2__...`); la `V1` no se edita.
