<?php
declare(strict_types=1);

/**
 * citas-service — punto de entrada y raíz de composición.
 * Arma las dependencias (repositorios, casos de uso, controladores),
 * registra las rutas y traduce excepciones a respuestas HTTP.
 */

require __DIR__ . '/../src/autoload.php';

use Dentacit\Citas\Api\CitaController;
use Dentacit\Citas\Api\HorarioController;
use Dentacit\Citas\Api\Jwt;
use Dentacit\Citas\Api\NoAutorizadoException;
use Dentacit\Citas\Api\Peticion;
use Dentacit\Citas\Api\Router;
use Dentacit\Citas\Application\GestionarHorarios;
use Dentacit\Citas\Application\ResponderCita;
use Dentacit\Citas\Application\SolicitarCita;
use Dentacit\Citas\Domain\NoEncontradoException;
use Dentacit\Citas\Domain\ReglaNegocioException;
use Dentacit\Citas\Infrastructure\Database;
use Dentacit\Citas\Infrastructure\HttpCatalogoGateway;
use Dentacit\Citas\Infrastructure\HttpNotificador;
use Dentacit\Citas\Infrastructure\PdoCitaRepository;
use Dentacit\Citas\Infrastructure\PdoHorarioRepository;

date_default_timezone_set(getenv('TZ') ?: 'America/Mexico_City');

/** Envía la respuesta JSON y termina. */
function responder(int $codigo, mixed $cuerpo): void
{
    http_response_code($codigo);
    if ($codigo !== 204) {
        header('Content-Type: application/json; charset=utf-8');
        echo json_encode($cuerpo, JSON_UNESCAPED_UNICODE);
    }
}

$peticion = Peticion::desdeGlobales();

if ($peticion->ruta === '/api/citas/salud') {
    responder(200, ['servicio' => 'citas-service', 'ok' => true]);
    return;
}

try {
    $db = Database::conectar();
    $duracion = (int)(getenv('DURACION_CITA_MIN') ?: 60);
    $secreto = getenv('JWT_SECRET') ?: '';
    $citas = new PdoCitaRepository($db);
    $horarios = new PdoHorarioRepository($db);

    $autenticar = function (Peticion $p) use ($secreto): array {
        $token = $p->token();
        if ($token === null) {
            throw new NoAutorizadoException('Inicia sesión para continuar.');
        }
        return Jwt::verificar($token, $secreto);
    };
    $hoy = fn(): string => date('Y-m-d');
    $ahora = fn(): string => date('H:i');

    $citaCtrl = new CitaController(
        $citas,
        new SolicitarCita($citas, $horarios, new HttpCatalogoGateway(getenv('CATALOGO_URL') ?: 'http://catalogo-service'), $duracion),
        new ResponderCita($citas, new HttpNotificador(getenv('NOTIF_URL') ?: 'http://notif-service:3001', getenv('INTERNAL_KEY') ?: '')),
        $autenticar,
        $hoy,
        $ahora
    );
    $horarioCtrl = new HorarioController($horarios, new GestionarHorarios($horarios, $citas, $duracion), $autenticar, $hoy, $ahora);

    $router = new Router();
    $router->agregar('GET', '/api/citas', [$citaCtrl, 'listar']);
    $router->agregar('POST', '/api/citas', [$citaCtrl, 'solicitar']);
    $router->agregar('GET', '/api/citas/paciente', [$citaCtrl, 'delPaciente']);
    $router->agregar('GET', '/api/citas/(\d+)', [$citaCtrl, 'detalle']);
    $router->agregar('PATCH', '/api/citas/(\d+)/estatus', [$citaCtrl, 'responder']);
    $router->agregar('GET', '/api/horarios', [$horarioCtrl, 'listar']);
    $router->agregar('POST', '/api/horarios', [$horarioCtrl, 'crear']);
    $router->agregar('GET', '/api/horarios/disponibles', [$horarioCtrl, 'disponibles']);
    $router->agregar('GET', '/api/horarios/proximos', [$horarioCtrl, 'proximos']);
    $router->agregar('DELETE', '/api/horarios/(\d+)', [$horarioCtrl, 'eliminar']);

    [$codigo, $cuerpo] = $router->despachar($peticion);
    responder($codigo, $cuerpo);
} catch (ReglaNegocioException $e) {
    responder(422, ['ok' => false, 'msg' => $e->getMessage()]);
} catch (NoAutorizadoException $e) {
    responder(401, ['ok' => false, 'msg' => $e->getMessage()]);
} catch (NoEncontradoException $e) {
    responder(404, ['ok' => false, 'msg' => $e->getMessage()]);
} catch (Throwable $e) {
    error_log('[citas-service] ' . $e);
    responder(500, ['ok' => false, 'msg' => 'Error interno del servidor.']);
}
