<?php
declare(strict_types=1);

namespace Dentacit\Catalogo\Infrastructure;

use Dentacit\Catalogo\Domain\Servicio;
use Dentacit\Catalogo\Domain\ServicioRepository;
use PDO;

/** Implementación de ServicioRepository con PDO. */
final class PdoServicioRepository implements ServicioRepository
{
    public function __construct(private readonly PDO $db)
    {
    }

    public function listar(bool $incluirInactivos): array
    {
        $sql = 'SELECT * FROM servicios WHERE eliminado = 0' . ($incluirInactivos ? '' : ' AND activo = 1') . ' ORDER BY nombre';
        return array_map([$this, 'aServicio'], $this->db->query($sql)->fetchAll());
    }

    public function obtener(int $id): ?Servicio
    {
        $stmt = $this->db->prepare('SELECT * FROM servicios WHERE id = :id AND eliminado = 0');
        $stmt->execute([':id' => $id]);
        $f = $stmt->fetch();
        return $f ? $this->aServicio($f) : null;
    }

    public function crear(Servicio $s): Servicio
    {
        $this->db->prepare('INSERT INTO servicios (nombre, descripcion, costo, activo) VALUES (:n, :d, :c, :a)')
            ->execute([':n' => $s->nombre, ':d' => $s->descripcion, ':c' => $s->costo, ':a' => (int)$s->activo]);
        return $s->conId((int)$this->db->lastInsertId());
    }

    public function actualizar(Servicio $s): bool
    {
        $stmt = $this->db->prepare(
            'UPDATE servicios SET nombre = :n, descripcion = :d, costo = :c, activo = :a WHERE id = :id AND eliminado = 0'
        );
        $stmt->execute([':n' => $s->nombre, ':d' => $s->descripcion, ':c' => $s->costo, ':a' => (int)$s->activo, ':id' => $s->id]);
        return $stmt->rowCount() > 0;
    }

    public function eliminar(int $id): bool
    {
        $stmt = $this->db->prepare('UPDATE servicios SET eliminado = 1, activo = 0 WHERE id = :id AND eliminado = 0');
        $stmt->execute([':id' => $id]);
        return $stmt->rowCount() > 0;
    }

    private function aServicio(array $f): Servicio
    {
        return new Servicio((int)$f['id'], $f['nombre'], (string)$f['descripcion'], $f['costo'], (bool)$f['activo']);
    }
}
