<?php
declare(strict_types=1);

namespace Dentacit\Catalogo\Api;

use Dentacit\Catalogo\Application\GestionarCatalogo;
use Dentacit\Catalogo\Domain\Servicio;

/** Controlador REST del catálogo. */
final class ServicioController
{
    public function __construct(
        private readonly GestionarCatalogo $catalogo,
        private readonly \Closure $autenticar,
        private readonly \Closure $esDentista,
    ) {
    }

    /** GET /api/servicios — público (solo activos); con token del dentista, todos. */
    public function listar(Peticion $p): array
    {
        $lista = $this->catalogo->listar(($this->esDentista)($p));
        return [200, array_map(fn(Servicio $s) => $s->aArreglo(), $lista)];
    }

    /** GET /api/servicios/{id} — público (lo usa citas-service). */
    public function obtener(Peticion $p, string $id): array
    {
        return [200, $this->catalogo->obtener((int)$id)->aArreglo()];
    }

    /** POST /api/servicios (dentista) */
    public function crear(Peticion $p): array
    {
        ($this->autenticar)($p);
        return [201, $this->catalogo->crear($p->cuerpo)->aArreglo()];
    }

    /** PUT /api/servicios/{id} (dentista) */
    public function actualizar(Peticion $p, string $id): array
    {
        ($this->autenticar)($p);
        return [200, $this->catalogo->actualizar((int)$id, $p->cuerpo)->aArreglo()];
    }

    /** DELETE /api/servicios/{id} (dentista) */
    public function eliminar(Peticion $p, string $id): array
    {
        ($this->autenticar)($p);
        $this->catalogo->eliminar((int)$id);
        return [204, null];
    }
}
