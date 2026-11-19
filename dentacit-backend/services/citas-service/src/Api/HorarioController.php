<?php
declare(strict_types=1);

namespace Dentacit\Citas\Api;

use Dentacit\Citas\Application\GestionarHorarios;
use Dentacit\Citas\Domain\Horario;
use Dentacit\Citas\Domain\HorarioRepository;

/** Controlador REST de horarios. */
final class HorarioController
{
    public function __construct(
        private readonly HorarioRepository $horarios,
        private readonly GestionarHorarios $gestionar,
        private readonly \Closure $autenticar,
        private readonly \Closure $hoy,
        private readonly ?\Closure $ahora = null,
    ) {
    }

    /** GET /api/horarios  (dentista) — bloques de hoy en adelante. */
    public function listar(Peticion $p): array
    {
        ($this->autenticar)($p);
        return [200, array_map([self::class, 'aJson'], $this->horarios->listar(($this->hoy)()))];
    }

    /** POST /api/horarios  (dentista) */
    public function crear(Peticion $p): array
    {
        $usuario = ($this->autenticar)($p);
        $horario = $this->gestionar->crear($p->cuerpo, (int)($usuario['sub'] ?? 1), ($this->hoy)());
        return [201, self::aJson($horario)];
    }

    /** DELETE /api/horarios/{id}  (dentista) */
    public function eliminar(Peticion $p, string $id): array
    {
        ($this->autenticar)($p);
        $this->gestionar->eliminar((int)$id);
        return [204, null];
    }

    /** GET /api/horarios/disponibles?fecha=  (público: paciente) */
    public function disponibles(Peticion $p): array
    {
        $fecha = (string)($p->query['fecha'] ?? '');
        $ahora = $this->ahora ? ($this->ahora)() : null;
        return [200, [
            'fecha' => $fecha,
            'horas' => $this->gestionar->disponibles($fecha, ($this->hoy)(), $ahora),
            'bloques' => $this->gestionar->bloques($fecha, ($this->hoy)(), $ahora),
        ]];
    }

    /** GET /api/horarios/proximos  (público: paciente) — días con lugar en los próximos 30 días. */
    public function proximos(Peticion $p): array
    {
        $ahora = $this->ahora ? ($this->ahora)() : date('H:i');
        return [200, $this->gestionar->proximos(($this->hoy)(), $ahora)];
    }

    private static function aJson(Horario $h): array
    {
        return ['id' => $h->id, 'fecha' => $h->fecha, 'hora_inicio' => $h->inicio, 'hora_fin' => $h->fin];
    }
}
