<?php
declare(strict_types=1);

namespace Dentacit\Citas\Infrastructure;

use DateTimeImmutable;
use Dentacit\Citas\Domain\Cita;
use Dentacit\Citas\Domain\CitaRepository;
use Dentacit\Citas\Domain\EstatusCita;
use PDO;

/** Implementación de CitaRepository con PDO (prepared statements). */
final class PdoCitaRepository implements CitaRepository
{
    private const SELECT_DETALLE = "
        SELECT c.id, p.nombre AS paciente_nombre, p.correo AS paciente_correo,
               p.telefono AS paciente_telefono, c.id_servicio, c.servicio_nombre AS servicio,
               c.fecha, c.hora, c.sintomas, c.estatus, c.correo_enviado
        FROM citas c
        JOIN pacientes p ON p.id = c.id_paciente";

    public function __construct(private readonly PDO $db)
    {
    }

    public function listar(?EstatusCita $estatus): array
    {
        if ($estatus === null) {
            $stmt = $this->db->query(self::SELECT_DETALLE . ' ORDER BY c.fecha, c.hora');
        } else {
            $stmt = $this->db->prepare(self::SELECT_DETALLE . ' WHERE c.estatus = :estatus ORDER BY c.fecha, c.hora');
            $stmt->execute([':estatus' => $estatus->value]);
        }
        return array_map([$this, 'formatear'], $stmt->fetchAll());
    }

    public function detalle(int $id): ?array
    {
        $stmt = $this->db->prepare(self::SELECT_DETALLE . ' WHERE c.id = :id');
        $stmt->execute([':id' => $id]);
        $fila = $stmt->fetch();
        return $fila ? $this->formatear($fila) : null;
    }

    public function listarPorCorreo(string $correo): array
    {
        $stmt = $this->db->prepare(self::SELECT_DETALLE . ' WHERE p.correo = :correo ORDER BY c.fecha DESC, c.hora DESC');
        $stmt->execute([':correo' => strtolower(trim($correo))]);
        return array_map([$this, 'formatear'], $stmt->fetchAll());
    }

    public function obtener(int $id): ?Cita
    {
        $stmt = $this->db->prepare('SELECT * FROM citas WHERE id = :id');
        $stmt->execute([':id' => $id]);
        $f = $stmt->fetch();
        if (!$f) {
            return null;
        }
        return new Cita(
            (int)$f['id'], (int)$f['id_paciente'], (int)$f['id_servicio'], $f['servicio_nombre'],
            new DateTimeImmutable($f['fecha']), substr($f['hora'], 0, 5), (string)$f['sintomas'],
            EstatusCita::from($f['estatus'])
        );
    }

    public function guardar(Cita $cita): int
    {
        $stmt = $this->db->prepare(
            'INSERT INTO citas (id_paciente, id_servicio, servicio_nombre, fecha, hora, sintomas, estatus)
             VALUES (:p, :s, :sn, :f, :h, :si, :e)'
        );
        $stmt->execute([
            ':p' => $cita->idPaciente,
            ':s' => $cita->idServicio,
            ':sn' => $cita->servicioNombre,
            ':f' => $cita->fecha->format('Y-m-d'),
            ':h' => $cita->hora,
            ':si' => $cita->sintomas,
            ':e' => $cita->estatus->value,
        ]);
        return (int)$this->db->lastInsertId();
    }

    public function actualizarEstatus(int $id, EstatusCita $estatus): void
    {
        $stmt = $this->db->prepare('UPDATE citas SET estatus = :e WHERE id = :id');
        $stmt->execute([':e' => $estatus->value, ':id' => $id]);
    }

    public function marcarCorreoEnviado(int $id): void
    {
        $this->db->prepare('UPDATE citas SET correo_enviado = 1 WHERE id = :id')->execute([':id' => $id]);
    }

    public function pacienteTieneCitaPendiente(int $idPaciente): bool
    {
        $stmt = $this->db->prepare("SELECT COUNT(*) FROM citas WHERE id_paciente = :p AND estatus = 'Pendiente'");
        $stmt->execute([':p' => $idPaciente]);
        return (int)$stmt->fetchColumn() > 0;
    }

    public function horasOcupadas(string $fecha): array
    {
        $stmt = $this->db->prepare(
            "SELECT hora FROM citas WHERE fecha = :f AND estatus IN ('Pendiente', 'Aceptada')"
        );
        $stmt->execute([':f' => $fecha]);
        return array_map(fn($h) => substr((string)$h, 0, 5), $stmt->fetchAll(PDO::FETCH_COLUMN));
    }

    public function registrarPaciente(string $nombre, string $correo, string $telefono): int
    {
        $stmt = $this->db->prepare('SELECT id FROM pacientes WHERE correo = :c');
        $stmt->execute([':c' => $correo]);
        $id = $stmt->fetchColumn();
        if ($id !== false) {
            $this->db->prepare('UPDATE pacientes SET nombre = :n, telefono = :t WHERE id = :id')
                ->execute([':n' => $nombre, ':t' => $telefono, ':id' => $id]);
            return (int)$id;
        }
        $this->db->prepare('INSERT INTO pacientes (nombre, correo, telefono) VALUES (:n, :c, :t)')
            ->execute([':n' => $nombre, ':c' => $correo, ':t' => $telefono]);
        return (int)$this->db->lastInsertId();
    }

    /** Da a cada fila el formato JSON que esperan los clientes web y móvil. */
    private function formatear(array $f): array
    {
        return [
            'id' => (int)$f['id'],
            'paciente_nombre' => $f['paciente_nombre'],
            'paciente_correo' => $f['paciente_correo'],
            'paciente_telefono' => $f['paciente_telefono'] ?? '',
            'id_servicio' => (int)$f['id_servicio'],
            'servicio' => $f['servicio'],
            'fecha' => $f['fecha'],
            'hora' => substr((string)$f['hora'], 0, 5),
            'sintomas' => $f['sintomas'] ?? '',
            'estatus' => $f['estatus'],
            'correo_enviado' => (bool)$f['correo_enviado'],
        ];
    }
}
