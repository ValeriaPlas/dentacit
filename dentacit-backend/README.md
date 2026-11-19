# DentaCit — Backend de microservicios

Backend del sistema DentaCit (Proyectos Integradores de Aplicaciones Empresariales 2026-2).
Cuatro microservicios REST detrás de un API Gateway, una base de datos MySQL centralizada,
todo en contenedores Docker.

```
            Cliente web (paciente)        App Android (dentista)
                       \                     /
                        \   HTTP · JSON     /
                    ┌──────────────────────────────┐
                    │  API Gateway · Nginx  :8080  │
                    └──────────────────────────────┘
         /api/auth      /api/servicios     /api/citas, /api/horarios     /api/qr
            │                 │                     │                      │
     ┌──────────────┐ ┌────────────────┐ ┌──────────────────┐   ┌────────────────┐
     │ auth-service │ │catalogo-service│ │  citas-service   │──▶│ notif-service  │──▶ SMTP
     │   Node.js    │ │     PHP 8      │ │      PHP 8       │   │    Node.js     │
     └──────────────┘ └────────────────┘ └──────────────────┘   └────────────────┘
             │                 │          │ consulta servicio       ▲
             │                 │◀─────────┘                         │ correo de
             │                 │                                    │ recuperación
             └────────┬────────┴─────────┬──────────────────────────┘ (auth-service)
                      ▼                  ▼
                 ┌─────────────────────────────┐
                 │   MySQL 8 (centralizada)    │
                 └─────────────────────────────┘
```

| Servicio | Plataforma | Responsabilidad | Tablas |
| --- | --- | --- | --- |
| auth-service | Node.js 22 + Express | Login del dentista, JWT, recuperación de contraseña | dentistas, password_resets |
| catalogo-service | PHP 8.3 | CRUD del catálogo de servicios y precios | servicios |
| citas-service | PHP 8.3 | Solicitudes de cita, respuesta del dentista, horarios y disponibilidad | pacientes, horarios, citas |
| notif-service | Node.js 22 + Nodemailer | Correos al paciente y al dentista; QR del consultorio | — |
| gateway | Nginx | Punto único de entrada, enrutamiento, límite de intentos de login | — |

## Cómo cumple la arquitectura pedida

| Requisito | Implementación |
| --- | --- |
| Orientado a objetos | Entidades, repositorios, casos de uso y controladores como clases (PHP y JS) |
| En capas | Cada servicio separa `Domain` → `Application` → `Infrastructure` → `Api` |
| Centrado en el dominio | Reglas en `Cita`, `Agenda`, `Horario` y `Servicio`; los controladores solo traducen HTTP |
| Microservicios | 4 servicios independientes, cada uno con su contenedor y sus tablas; se comunican por HTTP |
| BD centralizada | MySQL 8 compartida, cada servicio es dueño de sus tablas |
| Multiplataforma | PHP, Node.js (backend) y Kotlin (app Android) |
| Contenedores | Docker Compose: 6 contenedores |

### Reglas de negocio implementadas

| Regla | Dónde vive |
| --- | --- |
| Un paciente solo tiene una solicitud pendiente a la vez | `SolicitarCita` |
| Las citas solo se agendan dentro de los horarios del dentista | `Agenda::validarEspacio` |
| Dos citas activas no pueden ocupar la misma hora (RNF-05) | `Agenda::validarEspacio` |
| Los bloques de horario no se empalman ni quedan en el pasado | `Agenda::validarNuevoBloque` |
| Una cita aceptada o rechazada ya no se modifica | `Cita::responder` |
| Al aceptar o rechazar se envía correo al paciente (RF-06) | `ResponderCita` → notif-service |
| Solo el dentista autenticado gestiona citas, horarios y catálogo | JWT verificado en cada servicio |
| Los pacientes solo ven servicios activos | `GestionarCatalogo::listar` |
| Contraseñas cifradas (RNF-02) | bcrypt en auth-service |
| Sin inyección SQL | PDO / mysql2 con consultas parametrizadas |

## Levantar el sistema (local)

Requisito: [Docker Desktop](https://www.docker.com/products/docker-desktop/).

```bash
cp .env.example .env        # en Windows: copy .env.example .env
# edita .env y cambia las claves
docker compose up -d --build
```

La primera vez tarda unos minutos (descarga imágenes y crea la base con datos de prueba).

| Qué | Dónde |
| --- | --- |
| Cliente web del paciente | http://localhost:8080 |
| API Gateway | http://localhost:8080/api/salud |
| Login de prueba | `dentista@dentacit.com` / `dentacit123` |
| Ver correos enviados (sin SMTP) | `docker compose logs -f notif-service` |
| Reiniciar la base con los datos de prueba | `docker compose down -v && docker compose up -d` |

### Conectar la app Android

En `dentacit-android/app/build.gradle.kts`:

```kotlin
buildConfigField("String", "BASE_URL", "\"http://10.0.2.2:8080/api/\"")   // emulador
buildConfigField("boolean", "USE_FAKE_API", "false")
```

En un celular físico usa la IP de la computadora en la misma red Wi-Fi
(por ejemplo `http://192.168.1.50:8080/api/`). Después de cambiarlo, vuelve a sincronizar Gradle y ejecuta la app.

### Enviar correos reales

Por defecto los correos solo se imprimen en el log de `notif-service`. Para enviarlos de verdad con Gmail:
crea una *contraseña de aplicación* en tu cuenta de Google y en `.env` pon
`SMTP_HOST=smtp.gmail.com`, `SMTP_PORT=587`, `SMTP_USER=tu_correo@gmail.com`, `SMTP_PASS=<contraseña de aplicación>`.

## Pruebas

| Tipo | Cómo correrlas | Cantidad |
| --- | --- | --- |
| Unitarias citas-service | `cd services/citas-service && composer install && composer test` | 29 |
| Unitarias catalogo-service | `cd services/catalogo-service && composer install && composer test` | 6 |
| Unitarias auth-service | `cd services/auth-service && npm install && npm test` | 8 |
| Unitarias notif-service | `cd services/notif-service && npm install && npm test` | 7 |
| Integración (front ↔ back) | Postman: importa `postman/DentaCit.postman_collection.json` y ejecútala con *Run collection* sobre una base recién creada | 25 peticiones, 34 verificaciones |

## API (resumen)

Todas las rutas van detrás del gateway: `http://<servidor>:8080/api/...`. Las marcadas como
**Dentista** requieren `Authorization: Bearer <token>`. Los errores responden con el código HTTP
correspondiente (401, 404, 422, 500) y `{"ok": false, "msg": "..."}`.

| Método | Ruta | Acceso | Descripción |
| --- | --- | --- | --- |
| POST | `/auth/login` | Público | `{correo, password}` → `{token, nombre}` |
| GET | `/auth/verificar` | Dentista | Datos del token |
| POST | `/auth/recuperar` | Público | `{correo}` → envía código por correo |
| POST | `/auth/restablecer` | Público | `{token, password}` |
| GET | `/servicios` | Público | Activos (con token del dentista: todos) |
| GET | `/servicios/{id}` | Público | Un servicio |
| POST · PUT · DELETE | `/servicios[/{id}]` | Dentista | `{nombre, descripcion, costo, activo}` |
| GET | `/horarios/proximos` | Público | Días con lugar en los próximos 30 días: `[{fecha, horas: [...]}]` |
| GET | `/horarios/disponibles?fecha=AAAA-MM-DD` | Público | `{fecha, horas: ["09:00", ...]}` |
| GET · POST | `/horarios` | Dentista | `{fecha, hora_inicio, hora_fin}` |
| DELETE | `/horarios/{id}` | Dentista | Elimina un bloque |
| POST | `/citas` | Público | `{nombre, correo, telefono, id_servicio, fecha, hora, sintomas}` → Pendiente |
| GET | `/citas/paciente?correo=` | Público | Citas de un paciente |
| GET | `/citas?estatus=Pendiente` | Dentista | Solicitudes filtradas |
| GET | `/citas/{id}` | Dentista | Detalle |
| PATCH | `/citas/{id}/estatus` | Dentista | `{estatus: "Aceptada"\|"Rechazada"}` → envía correo |
| GET | `/qr` | Público | PNG con el QR hacia el cliente web |

## Despliegue en AWS (EC2)

1. **Crear la instancia:** EC2 → *Launch instance* → Ubuntu 24.04, tipo `t3.small` (o `t2.micro` de capa gratuita),
   par de llaves nuevo. En el *Security group* abre SSH (22) solo para tu IP y HTTP en el puerto **8080** (o 80).
2. **Conectarte e instalar Docker:**
   ```bash
   ssh -i tu-llave.pem ubuntu@<IP-pública>
   curl -fsSL https://get.docker.com | sudo sh
   sudo usermod -aG docker ubuntu && exit   # vuelve a conectarte
   ```
3. **Subir el proyecto** (con Git o `scp -r dentacit-backend ubuntu@<IP>:~`).
4. **Configurar y levantar:**
   ```bash
   cd dentacit-backend
   cp .env.example .env && nano .env      # claves seguras y WEB_URL=http://<IP-pública>:8080/
   docker compose up -d --build
   curl http://localhost:8080/api/salud
   ```
5. **App Android:** `BASE_URL = "http://<IP-pública>:8080/api/"` y `USE_FAKE_API = false`.

Con un dominio propio se recomienda poner HTTPS (por ejemplo con Certbot o un balanceador de AWS con certificado de ACM).

## Estructura

```
dentacit-backend/
├── docker-compose.yml        Orquestación de los 6 contenedores
├── .env.example              Variables de configuración (copiar a .env)
├── gateway/nginx.conf        API Gateway
├── db/                       Esquema y datos de prueba (se cargan al crear el contenedor)
├── web/                      Cliente web del paciente: HTML + CSS + JavaScript (servido por el gateway)
├── postman/                  Colección de pruebas de integración
└── services/
    ├── citas-service/        PHP · src/{Domain, Application, Infrastructure, Api} · tests/
    ├── catalogo-service/     PHP · misma estructura
    ├── auth-service/         Node · src/{domain, application, infrastructure} · tests/
    └── notif-service/        Node · src/{application, infrastructure} · tests/
```

## Clientes web

Páginas en `web/` servidas por el gateway. Conservan el diseño de las vistas originales de Dentacit
(`catalogo.php`, `login.php`, `dashboard.php`), pero ya no usan PHP: son HTML + CSS + JavaScript que
consumen la API. Todo el texto que viene del servidor se escapa antes de mostrarse (prevención de XSS).

### Paciente — `http://<servidor>:8080/` (la dirección que abre el QR)

| Pantalla | Qué hace | Endpoints |
| --- | --- | --- |
| Inicio (pasos 1-2-3) | Servicio → fecha y bloque (Libre / Ocupado) → datos (RF-02, RF-03) | `GET /servicios`, `GET /horarios/proximos`, `GET /horarios/disponibles`, `POST /citas` |
| Confirmación | Muestra la cita en estado Pendiente (RF-04) | — |
| Mis citas | Activas e historial con su estado | `GET /citas/paciente` |
| Perfil | Datos y estadísticas del paciente (guardados solo en su navegador) | `GET /citas/paciente` |

### Dentista — `http://<servidor>:8080/dentista/`

| Pantalla | Qué hace | Endpoints |
| --- | --- | --- |
| Login | Inicio de sesión, recuperación y restablecimiento de contraseña (RF-01) | `POST /auth/login`, `/auth/recuperar`, `/auth/restablecer` |
| Dashboard | Métricas, solicitudes recientes, calendario con días con citas | `GET /citas` |
| Solicitudes | Filtro por estado; aceptar o rechazar con correo automático (RF-05, RF-06) | `GET /citas`, `PATCH /citas/{id}/estatus` |
| Horarios | Alta y baja de bloques; rechaza empalmes (RNF-05) | `GET/POST/DELETE /horarios` |
| Servicios | CRUD del catálogo (RF-07) | `GET/POST/PUT/DELETE /servicios` |
| Correos enviados | Historial de notificaciones enviadas | `GET /citas` |
| Mi código QR | QR generado por notif-service, descargable | `GET /qr` |

El dentista puede usar el panel web o la app Android indistintamente: ambos consumen la misma API.
