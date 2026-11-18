<?php
declare(strict_types=1);

/** catalogo-service — punto de entrada y raíz de composición. */

require __DIR__ . '/../src/autoload.php';

use Dentacit\Catalogo\Api\Jwt;
use Dentacit\Catalogo\Api\NoAutorizadoException;
use Dentacit\Catalogo\Api\Peticion;
use Dentacit\Catalogo\Api\Router;
use Dentacit\Catalogo\Api\ServicioController;
use Dentacit\Catalogo\Application\GestionarCatalogo;
use Dentacit\Catalogo\Domain\NoEncontradoException;
use Dentacit\Catalogo\Domain\ReglaNegocioException;
use Dentacit\Catalogo\Infrastructure\Database;
use Dentacit\Catalogo\Infrastructure\PdoServicioRepository;

function responder(int $codigo, mixed $cuerpo): void
{
    http_response_code($codigo);
    if ($codigo !== 204) {
        header('Content-Type: application/json; charset=utf-8');
        echo json_encode($cuerpo, JSON_UNESCAPED_UNICODE);
    }
}

$peticion = Peticion::desdeGlobales();

if ($peticion->ruta === '/api/servicios/salud') {
    responder(200, ['servicio' => 'catalogo-service', 'ok' => true]);
    return;
}

try {
    $secreto = getenv('JWT_SECRET') ?: '';
    $autenticar = function (Peticion $p) use ($secreto): array {
        $token = $p->token();
        if ($token === null) {
            throw new NoAutorizadoException('Inicia sesión para continuar.');
        }
        return Jwt::verificar($token, $secreto);
    };
    $esDentista = function (Peticion $p) use ($autenticar): bool {
        try {
            return $p->token() !== null && $autenticar($p) !== [];
        } catch (NoAutorizadoException) {
            return false;
        }
    };

    $ctrl = new ServicioController(
        new GestionarCatalogo(new PdoServicioRepository(Database::conectar())),
        $autenticar,
        $esDentista
    );

    $router = new Router();
    $router->agregar('GET', '/api/servicios', [$ctrl, 'listar']);
    $router->agregar('POST', '/api/servicios', [$ctrl, 'crear']);
    $router->agregar('GET', '/api/servicios/(\d+)', [$ctrl, 'obtener']);
    $router->agregar('PUT', '/api/servicios/(\d+)', [$ctrl, 'actualizar']);
    $router->agregar('DELETE', '/api/servicios/(\d+)', [$ctrl, 'eliminar']);

    [$codigo, $cuerpo] = $router->despachar($peticion);
    responder($codigo, $cuerpo);
} catch (ReglaNegocioException $e) {
    responder(422, ['ok' => false, 'msg' => $e->getMessage()]);
} catch (NoAutorizadoException $e) {
    responder(401, ['ok' => false, 'msg' => $e->getMessage()]);
} catch (NoEncontradoException $e) {
    responder(404, ['ok' => false, 'msg' => $e->getMessage()]);
} catch (Throwable $e) {
    error_log('[catalogo-service] ' . $e);
    responder(500, ['ok' => false, 'msg' => 'Error interno del servidor.']);
}
