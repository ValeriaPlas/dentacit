<?php
declare(strict_types=1);

namespace Dentacit\Citas\Application;

use Dentacit\Citas\Domain\CitaRepository;
use Dentacit\Citas\Domain\EstatusCita;
use Dentacit\Citas\Domain\NoEncontradoException;
use Dentacit\Citas\Domain\Notificador;

/**
 * Caso de uso: el dentista acepta o rechaza una cita (RF-05) y se notifica
 * al paciente por correo (RF-06, regla Notificaciones Automáticas).
 */
final class ResponderCita
{
    public function __construct(
        private readonly CitaRepository $citas,
        private readonly Notificador $notificador,
    ) {
    }

    /**
     * @return array{id:int, estatus:string, correo_enviado:bool}
     * @throws NoEncontradoException si la cita no existe.
     */
    public function ejecutar(int $id, string $estatusTexto): array
    {
        $estatus = EstatusCita::desdeTexto($estatusTexto);
        $cita = $this->citas->obtener($id);
        if ($cita === null) {
            throw new NoEncontradoException("La cita $id no existe.");
        }

        // La regla de negocio vive en la entidad.
        $actualizada = $cita->responder($estatus);
        $this->citas->actualizarEstatus($id, $actualizada->estatus);

        // La notificación no debe deshacer la respuesta si el correo falla.
        $enviado = false;
        $detalle = $this->citas->detalle($id);
        if ($detalle !== null) {
            $enviado = $this->notificador->notificarRespuesta($detalle, $actualizada->estatus);
            if ($enviado) {
                $this->citas->marcarCorreoEnviado($id);
            }
        }

        return ['id' => $id, 'estatus' => $actualizada->estatus->value, 'correo_enviado' => $enviado];
    }
}
