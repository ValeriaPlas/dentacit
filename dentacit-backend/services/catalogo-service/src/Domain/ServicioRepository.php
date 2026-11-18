<?php
declare(strict_types=1);

namespace Dentacit\Catalogo\Domain;

/** Contrato de persistencia del catálogo de servicios. */
interface ServicioRepository
{
    /** @return Servicio[] */
    public function listar(bool $incluirInactivos): array;

    public function obtener(int $id): ?Servicio;

    public function crear(Servicio $servicio): Servicio;

    public function actualizar(Servicio $servicio): bool;

    /** Baja lógica: el servicio deja de listarse, pero las citas históricas lo conservan. */
    public function eliminar(int $id): bool;
}
