<?php
declare(strict_types=1);

namespace Dentacit\Citas\Infrastructure;

use Dentacit\Citas\Domain\EstatusCita;
use Dentacit\Citas\Domain\Notificador;

/** Pide a notif-service que envíe el correo al paciente. */
final class HttpNotificador implements Notificador
{
    public function __construct(
        private readonly string $urlBase,
        private readonly string $llaveInterna,
    ) {
    }

    public function notificarRespuesta(array $cita, EstatusCita $estatus): bool
    {
        $r = HttpCliente::enviar(
            'POST',
            rtrim($this->urlBase, '/') . '/api/notificaciones/cita',
            [
                'estatus' => $estatus->value,
                'paciente_nombre' => $cita['paciente_nombre'],
                'paciente_correo' => $cita['paciente_correo'],
                'servicio' => $cita['servicio'],
                'fecha' => $cita['fecha'],
                'hora' => $cita['hora'],
            ],
            ['X-Internal-Key' => $this->llaveInterna],
            10
        );
        if ($r === null || $r['codigo'] >= 300) {
            error_log('[citas-service] No se pudo enviar la notificación de la cita ' . $cita['id']);
            return false;
        }
        return true;
    }
}
