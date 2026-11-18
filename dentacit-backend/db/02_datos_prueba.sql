-- =====================================================================
-- DentaCit — Datos de prueba
-- Las fechas son relativas al día en que se crea la base de datos,
-- para que siempre haya citas y horarios "a futuro" en la demo.
-- Dentista: dentista@dentacit.com / dentacit123
-- =====================================================================
SET NAMES utf8mb4;
SET time_zone = '-06:00';

INSERT INTO dentistas (id, nombre, correo, password_hash) VALUES
(1, 'Dentista DentaCit', 'dentista@dentacit.com', '$2a$10$TPjE4y7uW3x1UIh1Nq8zTOeBqBo5XJl13JiQIxZ/Jok1eOCsDYZQy');

INSERT INTO servicios (id, nombre, descripcion, costo, activo) VALUES
(1, 'Limpieza dental',   'Profilaxis y eliminación de sarro',      600.00, 1),
(2, 'Resina',            'Restauración estética de caries',        850.00, 1),
(3, 'Extracción simple', 'Extracción de pieza sin cirugía',        900.00, 1),
(4, 'Blanqueamiento',    'Blanqueamiento en consultorio',         3500.00, 1),
(5, 'Valoración',        'Revisión general y diagnóstico',         300.00, 1),
(6, 'Ortodoncia',        'Consulta inicial de ortodoncia',        1200.00, 0);

INSERT INTO horarios (id_dentista, fecha, hora_inicio, hora_fin) VALUES
(1, CURDATE() + INTERVAL 1 DAY, '09:00', '14:00'),
(1, CURDATE() + INTERVAL 1 DAY, '16:00', '19:00'),
(1, CURDATE() + INTERVAL 2 DAY, '09:00', '13:00'),
(1, CURDATE() + INTERVAL 3 DAY, '10:00', '18:00'),
(1, CURDATE() + INTERVAL 4 DAY, '09:00', '14:00'),
(1, CURDATE() + INTERVAL 7 DAY, '09:00', '14:00');

INSERT INTO pacientes (id, nombre, correo, telefono) VALUES
(1, 'Ana López García',    'ana.lopez@correo.com',  '477 123 4567'),
(2, 'Luis Hernández Ruiz', 'luis.hr@correo.com',    '477 987 6543'),
(3, 'María Torres Díaz',   'maria.td@correo.com',   '462 555 0101'),
(4, 'Jorge Ramírez Soto',  'jorge.rs@correo.com',   '477 222 3344'),
(5, 'Sofía Mendoza Cruz',  'sofia.mc@correo.com',   '477 444 8899');

INSERT INTO citas (id_paciente, id_servicio, servicio_nombre, fecha, hora, sintomas, estatus, correo_enviado) VALUES
(1, 1, 'Limpieza dental',   CURDATE() + INTERVAL 1 DAY, '10:00', 'Sangrado leve de encías al cepillarme.',            'Pendiente', 0),
(2, 3, 'Extracción simple', CURDATE() + INTERVAL 1 DAY, '12:00', 'Dolor fuerte en muela del juicio inferior derecha.', 'Pendiente', 0),
(3, 2, 'Resina',            CURDATE() + INTERVAL 2 DAY, '09:00', 'Sensibilidad al frío en un diente frontal.',         'Pendiente', 0),
(4, 5, 'Valoración',        CURDATE() + INTERVAL 3 DAY, '16:00', 'Revisión general, hace dos años que no voy.',        'Aceptada',  1),
(5, 4, 'Blanqueamiento',    CURDATE() + INTERVAL 4 DAY, '11:00', 'Quiero mejorar el color de mis dientes.',            'Rechazada', 1);
