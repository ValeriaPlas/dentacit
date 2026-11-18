<?php
declare(strict_types=1);

namespace Dentacit\Citas\Domain;

/** Puerto hacia notif-service para avisar al paciente. */
interface Notificador
{
    /** @param array<string, mixed> $cita Detalle de la cita. Devuelve true si se envió. */
    public function notificarRespuesta(array $cita, EstatusCita $estatus): bool;
}
