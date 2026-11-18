<?php
declare(strict_types=1);

namespace Dentacit\Citas\Infrastructure;

use Dentacit\Citas\Domain\Horario;
use Dentacit\Citas\Domain\HorarioRepository;
use PDO;

/** Implementación de HorarioRepository con PDO. */
final class PdoHorarioRepository implements HorarioRepository
{
    public function __construct(private readonly PDO $db)
    {
    }

    public function listar(?string $desde = null): array
    {
        if ($desde === null) {
            $stmt = $this->db->query('SELECT * FROM horarios ORDER BY fecha, hora_inicio');
        } else {
            $stmt = $this->db->prepare('SELECT * FROM horarios WHERE fecha >= :d ORDER BY fecha, hora_inicio');
            $stmt->execute([':d' => $desde]);
        }
        return array_map([$this, 'aHorario'], $stmt->fetchAll());
    }

    public function listarPorFecha(string $fecha): array
    {
        $stmt = $this->db->prepare('SELECT * FROM horarios WHERE fecha = :f ORDER BY hora_inicio');
        $stmt->execute([':f' => $fecha]);
        return array_map([$this, 'aHorario'], $stmt->fetchAll());
    }

    public function crear(Horario $horario, int $idDentista): Horario
    {
        $this->db->prepare(
            'INSERT INTO horarios (id_dentista, fecha, hora_inicio, hora_fin) VALUES (:d, :f, :i, :fin)'
        )->execute([':d' => $idDentista, ':f' => $horario->fecha, ':i' => $horario->inicio, ':fin' => $horario->fin]);
        return new Horario((int)$this->db->lastInsertId(), $horario->fecha, $horario->inicio, $horario->fin);
    }

    public function eliminar(int $id): bool
    {
        $stmt = $this->db->prepare('DELETE FROM horarios WHERE id = :id');
        $stmt->execute([':id' => $id]);
        return $stmt->rowCount() > 0;
    }

    private function aHorario(array $f): Horario
    {
        return new Horario((int)$f['id'], $f['fecha'], substr($f['hora_inicio'], 0, 5), substr($f['hora_fin'], 0, 5));
    }
}
