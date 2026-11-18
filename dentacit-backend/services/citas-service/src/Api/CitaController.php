<?php
declare(strict_types=1);

namespace Dentacit\Citas\Api;

use Dentacit\Citas\Application\ResponderCita;
use Dentacit\Citas\Application\SolicitarCita;
use Dentacit\Citas\Domain\CitaRepository;
use Dentacit\Citas\Domain\EstatusCita;
use Dentacit\Citas\Domain\NoEncontradoException;
use Dentacit\Citas\Domain\ReglaNegocioException;

/** Controlador REST de citas. Solo traduce HTTP ↔ casos de uso. */
final class CitaController
{
    public function __construct(
        private readonly CitaRepository $citas,
        private readonly SolicitarCita $solicitarCita,
        private readonly ResponderCita $responderCita,
        private readonly \Closure $autenticar,
        private readonly \Closure $hoy,
        private readonly ?\Closure $ahora = null,
    ) {
    }

    /** GET /api/citas?estatus=  (dentista) */
    public function listar(Peticion $p): array
    {
        ($this->autenticar)($p);
        $estatus = isset($p->query['estatus']) && $p->query['estatus'] !== ''
            ? EstatusCita::desdeTexto((string)$p->query['estatus'])
            : null;
        return [200, $this->citas->listar($estatus)];
    }

    /** GET /api/citas/{id}  (dentista) */
    public function detalle(Peticion $p, string $id): array
    {
        ($this->autenticar)($p);
        $cita = $this->citas->detalle((int)$id);
        if ($cita === null) {
            throw new NoEncontradoException("La cita $id no existe.");
        }
        return [200, $cita];
    }

    /** POST /api/citas  (público: paciente) */
    public function solicitar(Peticion $p): array
    {
        $resultado = $this->solicitarCita->ejecutar($p->cuerpo, ($this->hoy)(), $this->ahora ? ($this->ahora)() : null);
        return [201, ['ok' => true, 'msg' => 'Solicitud enviada. Te avisaremos por correo.'] + $resultado];
    }

    /** GET /api/citas/paciente?correo=  (público: paciente) */
    public function delPaciente(Peticion $p): array
    {
        $correo = trim((string)($p->query['correo'] ?? ''));
        if (!filter_var($correo, FILTER_VALIDATE_EMAIL)) {
            throw new ReglaNegocioException('Indica un correo válido.');
        }
        return [200, $this->citas->listarPorCorreo($correo)];
    }

    /** PATCH /api/citas/{id}/estatus  (dentista) */
    public function responder(Peticion $p, string $id): array
    {
        ($this->autenticar)($p);
        $resultado = $this->responderCita->ejecutar((int)$id, (string)($p->cuerpo['estatus'] ?? ''));
        return [200, ['ok' => true] + $resultado];
    }
}
