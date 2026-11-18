# DentaCit — App Android del dentista

Cliente móvil del sistema DentaCit (Proyectos Integradores de Aplicaciones Empresariales 2026-2).
El dentista inicia sesión, revisa las solicitudes de cita, las acepta o rechaza, administra sus
horarios disponibles y el catálogo de servicios.

## Tecnologías

Kotlin · Jetpack Compose (Material 3) · MVVM · Clean Architecture · Retrofit + OkHttp · DataStore ·
Navigation Compose · Coroutines/Flow · JUnit.

## Cómo abrirla

1. Android Studio → **File › Open** → selecciona la carpeta `dentacit-android`.
2. Espera a que Gradle sincronice (descarga dependencias la primera vez).
3. Ejecuta en un emulador o celular (Android 8.0 / API 26 o superior).

### Modo demostración vs. API real

En `app/build.gradle.kts`:

```kotlin
buildConfigField("String", "BASE_URL", "\"http://10.0.2.2:8080/api/\"")
buildConfigField("boolean", "USE_FAKE_API", "true")
```

| `USE_FAKE_API` | Comportamiento |
| --- | --- |
| `true` | Usa datos de prueba en memoria. No necesita backend. Login: `dentista@dentacit.com` / `dentacit123` |
| `false` | Consume el API Gateway en `BASE_URL` con el token JWT del dentista |

`10.0.2.2` es la PC anfitriona vista desde el emulador. En celular físico usa la IP de la PC en la
misma red (ej. `http://192.168.1.50:8080/api/`) y en AWS la IP o dominio de la instancia.

## Arquitectura

```
com.dentacit.admin
├── domain/            ← Núcleo: no depende de Android ni de Retrofit
│   ├── model/         Cita, Paciente, Servicio, Horario, Agenda, EstatusCita, Sesion, errores
│   ├── repository/    Interfaces (contratos) de cada microservicio
│   └── usecase/       IniciarSesion, ResponderCita, GuardarServicio, CrearHorario, …
├── data/              ← Implementaciones de los contratos
│   ├── remote/        Retrofit (DentaCitApi), DTOs, mappers, interceptor JWT, manejo de errores
│   ├── local/         SesionStore (DataStore) para guardar el token
│   ├── repository/    Repositorios que llaman al API Gateway
│   └── fake/          Repositorios de demostración con datos en memoria
├── di/                AppContainer: inyección de dependencias manual
└── presentation/      ← Compose + ViewModels (MVVM)
    ├── login/  home/  solicitudes/  detalle/  catalogo/  horarios/
    ├── navigation/    NavGraph y rutas
    ├── common/        Componentes y utilidades compartidas
    └── theme/         Colores y tema (misma paleta que el cliente web)
```

### Reglas de negocio en el dominio

| Regla | Dónde vive |
| --- | --- |
| Una cita solo pasa de Pendiente a Aceptada o Rechazada; después no se modifica | `Cita.responder()` |
| Los bloques de horario no se empalman (RNF-05) | `Agenda.validarNuevoBloque()` |
| No se crean horarios en fechas pasadas ni con fin antes del inicio | `Agenda.validarNuevoBloque()` |
| Un servicio requiere nombre y costo mayor a cero (RF-07) | `GuardarServicioUseCase` |

## Pruebas unitarias

```
./gradlew test
```

18 pruebas en `app/src/test` sobre `Cita`, `Agenda` y los casos de uso `ResponderCita`,
`GuardarServicio` y `CrearHorario`.

## Contrato de la API que consume la app

Todas las rutas van detrás del API Gateway (`BASE_URL`). Respuestas en JSON.
Las rutas marcadas como **Dentista** requieren el encabezado `Authorization: Bearer <token>`.
En caso de error, los servicios deben responder con el código HTTP adecuado y
`{"ok": false, "msg": "Texto del error"}`; la app muestra `msg` al usuario.

### auth-service

| Método | Ruta | Acceso | Cuerpo | Respuesta |
| --- | --- | --- | --- | --- |
| POST | `auth/login` | Público | `{"correo": "...", "password": "..."}` | `{"token": "<jwt>", "nombre": "Dra. ..."}` · 401 si falla |

### citas-service

| Método | Ruta | Acceso | Cuerpo | Respuesta |
| --- | --- | --- | --- | --- |
| GET | `citas?estatus=Pendiente` | Dentista | — | Lista de citas (estatus: `Pendiente`, `Aceptada`, `Rechazada`; sin parámetro = todas) |
| GET | `citas/{id}` | Dentista | — | Una cita |
| PATCH | `citas/{id}/estatus` | Dentista | `{"estatus": "Aceptada"}` | 200/204. Debe avisar a notif-service para el correo |
| GET | `horarios` | Dentista | — | Lista de horarios |
| POST | `horarios` | Dentista | `{"fecha": "2026-11-20", "hora_inicio": "09:00", "hora_fin": "13:00"}` | El horario creado |
| DELETE | `horarios/{id}` | Dentista | — | 200/204 |

Cita:

```json
{
  "id": 1,
  "paciente_nombre": "Ana López García",
  "paciente_correo": "ana.lopez@correo.com",
  "paciente_telefono": "477 123 4567",
  "servicio": "Limpieza dental",
  "fecha": "2026-11-20",
  "hora": "10:00",
  "sintomas": "Sangrado leve de encías",
  "estatus": "Pendiente"
}
```

Horario:

```json
{ "id": 1, "fecha": "2026-11-20", "hora_inicio": "09:00", "hora_fin": "13:00" }
```

(`hora`, `hora_inicio` y `hora_fin` aceptan `HH:mm` o `HH:mm:ss`, como las devuelve MySQL.)

### catalogo-service

| Método | Ruta | Acceso | Cuerpo | Respuesta |
| --- | --- | --- | --- | --- |
| GET | `servicios` | Público | — | Lista de servicios |
| POST | `servicios` | Dentista | Servicio sin `id` | El servicio creado con `id` |
| PUT | `servicios/{id}` | Dentista | Servicio | El servicio actualizado |
| DELETE | `servicios/{id}` | Dentista | — | 200/204 |

Servicio:

```json
{ "id": 1, "nombre": "Limpieza dental", "descripcion": "Profilaxis", "costo": 600.0, "activo": true }
```
