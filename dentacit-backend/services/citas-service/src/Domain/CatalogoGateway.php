<?php
declare(strict_types=1);

namespace Dentacit\Citas\Domain;

/** Puerto hacia catalogo-service para validar el servicio solicitado. */
interface CatalogoGateway
{
    /** @return array{id:int, nombre:string, activo:bool}|null */
    public function obtenerServicio(int $id): ?array;
}
