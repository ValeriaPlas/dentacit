<?php
declare(strict_types=1);

namespace Dentacit\Catalogo\Application;

use Dentacit\Catalogo\Domain\NoEncontradoException;
use Dentacit\Catalogo\Domain\Servicio;
use Dentacit\Catalogo\Domain\ServicioRepository;

/** Casos de uso del catálogo de servicios (RF-07). */
final class GestionarCatalogo
{
    public function __construct(private readonly ServicioRepository $servicios)
    {
    }

    /** @return Servicio[] Activos para pacientes; todos para el dentista. */
    public function listar(bool $esDentista): array
    {
        return $this->servicios->listar($esDentista);
    }

    public function obtener(int $id): Servicio
    {
        return $this->servicios->obtener($id) ?? throw new NoEncontradoException("El servicio $id no existe.");
    }

    /** @param array<string, mixed> $datos */
    public function crear(array $datos): Servicio
    {
        return $this->servicios->crear($this->desdeDatos(null, $datos));
    }

    /** @param array<string, mixed> $datos */
    public function actualizar(int $id, array $datos): Servicio
    {
        $this->obtener($id);
        $servicio = $this->desdeDatos($id, $datos);
        $this->servicios->actualizar($servicio);
        return $servicio;
    }

    public function eliminar(int $id): void
    {
        if (!$this->servicios->eliminar($id)) {
            throw new NoEncontradoException("El servicio $id no existe.");
        }
    }

    /** @param array<string, mixed> $d */
    private function desdeDatos(?int $id, array $d): Servicio
    {
        return new Servicio(
            $id,
            (string)($d['nombre'] ?? ''),
            (string)($d['descripcion'] ?? ''),
            $d['costo'] ?? null,
            filter_var($d['activo'] ?? true, FILTER_VALIDATE_BOOL)
        );
    }
}
