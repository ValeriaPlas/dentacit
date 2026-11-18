<?php
declare(strict_types=1);

namespace Dentacit\Catalogo\Domain;

/**
 * Entidad de dominio Servicio odontológico.
 * Garantiza que todo servicio tenga nombre y un costo mayor a cero (RF-07).
 */
final class Servicio
{
    public readonly string $nombre;
    public readonly string $descripcion;
    public readonly float $costo;

    /**
     * @throws ReglaNegocioException si los datos no son válidos.
     */
    public function __construct(
        public readonly ?int $id,
        string $nombre,
        string $descripcion,
        mixed $costo,
        public readonly bool $activo = true,
    ) {
        $nombre = trim($nombre);
        if ($nombre === '') {
            throw new ReglaNegocioException('El nombre del servicio es obligatorio.');
        }
        if (mb_strlen($nombre) > 120) {
            throw new ReglaNegocioException('El nombre del servicio no debe pasar de 120 caracteres.');
        }
        if (!is_numeric($costo) || (float)$costo <= 0) {
            throw new ReglaNegocioException('El costo debe ser un número mayor a cero.');
        }
        $this->nombre = $nombre;
        $this->descripcion = trim($descripcion);
        $this->costo = round((float)$costo, 2);
    }

    /** Crea una copia con otro id (al guardar). */
    public function conId(int $id): self
    {
        return new self($id, $this->nombre, $this->descripcion, $this->costo, $this->activo);
    }

    /** @return array<string, mixed> */
    public function aArreglo(): array
    {
        return [
            'id' => $this->id,
            'nombre' => $this->nombre,
            'descripcion' => $this->descripcion,
            'costo' => $this->costo,
            'activo' => $this->activo,
        ];
    }
}
