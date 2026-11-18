-- =====================================================================
-- DentaCit — Esquema de la base de datos centralizada (MySQL 8)
-- Cada microservicio es dueño de sus tablas:
--   auth-service      → dentistas, password_resets
--   catalogo-service  → servicios
--   citas-service     → pacientes, horarios, citas
-- =====================================================================
SET NAMES utf8mb4;
SET time_zone = '-06:00';

CREATE TABLE IF NOT EXISTS dentistas (
    id            INT AUTO_INCREMENT PRIMARY KEY,
    nombre        VARCHAR(120) NOT NULL,
    correo        VARCHAR(150) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    creado_en     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS password_resets (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    id_dentista INT NOT NULL,
    token_hash  CHAR(64) NOT NULL,
    expira      DATETIME NOT NULL,
    INDEX idx_token (token_hash),
    FOREIGN KEY (id_dentista) REFERENCES dentistas(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS servicios (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    nombre      VARCHAR(120) NOT NULL,
    descripcion TEXT,
    costo       DECIMAL(10,2) NOT NULL,
    activo      TINYINT(1) NOT NULL DEFAULT 1,
    eliminado   TINYINT(1) NOT NULL DEFAULT 0,
    creado_en   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS pacientes (
    id        INT AUTO_INCREMENT PRIMARY KEY,
    nombre    VARCHAR(120) NOT NULL,
    correo    VARCHAR(150) NOT NULL UNIQUE,
    telefono  VARCHAR(20),
    creado_en TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS horarios (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    id_dentista INT NOT NULL DEFAULT 1,
    fecha       DATE NOT NULL,
    hora_inicio TIME NOT NULL,
    hora_fin    TIME NOT NULL,
    INDEX idx_fecha (fecha)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS citas (
    id              INT AUTO_INCREMENT PRIMARY KEY,
    id_paciente     INT NOT NULL,
    id_dentista     INT NOT NULL DEFAULT 1,
    id_servicio     INT NOT NULL,
    servicio_nombre VARCHAR(120) NOT NULL,
    fecha           DATE NOT NULL,
    hora            TIME NOT NULL,
    sintomas        TEXT,
    estatus         ENUM('Pendiente','Aceptada','Rechazada') NOT NULL DEFAULT 'Pendiente',
    correo_enviado  TINYINT(1) NOT NULL DEFAULT 0,
    creado_en       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    actualizado_en  TIMESTAMP NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_fecha_hora (fecha, hora),
    INDEX idx_estatus (estatus),
    FOREIGN KEY (id_paciente) REFERENCES pacientes(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
