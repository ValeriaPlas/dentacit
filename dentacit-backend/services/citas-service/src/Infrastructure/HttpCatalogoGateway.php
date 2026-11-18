<?php
declare(strict_types=1);

namespace Dentacit\Citas\Infrastructure;

use Dentacit\Citas\Domain\CatalogoGateway;
use RuntimeException;

/** Consulta a catalogo-service por HTTP (comunicación entre microservicios). */
final class HttpCatalogoGateway implements CatalogoGateway
{
    public function __construct(private readonly string $urlBase)
    {
    }

    public function obtenerServicio(int $id): ?array
    {
        $r = HttpCliente::enviar('GET', rtrim($this->urlBase, '/') . "/api/servicios/$id");
        if ($r === null) {
            throw new RuntimeException('No se pudo contactar a catalogo-service.');
        }
        if ($r['codigo'] !== 200 || !is_array($r['cuerpo'])) {
            return null;
        }
        return [
            'id' => (int)$r['cuerpo']['id'],
            'nombre' => (string)$r['cuerpo']['nombre'],
            'activo' => (bool)$r['cuerpo']['activo'],
        ];
    }
}
