<?php
declare(strict_types=1);

namespace Dentacit\Citas\Tests\Dobles;

use Dentacit\Citas\Domain\CatalogoGateway;
use Dentacit\Citas\Domain\Cita;
use Dentacit\Citas\Domain\CitaRepository;
use Dentacit\Citas\Domain\EstatusCita;
use Dentacit\Citas\Domain\Horario;
use Dentacit\Citas\Domain\HorarioRepository;
use Dentacit\Citas\Domain\Notificador;

/** Repositorio de citas en memoria para pruebas. */
final class CitasEnMemoria implements CitaRepository
{
    /** @var array<int, Cita> */
    public array $citas = [];
    /** @var array<int, array{nombre:string, correo:string, telefono:string}> */
    public array $pacientes = [];
    public array $correosEnviados = [];

    public function listar(?EstatusCita $estatus): array
    {
        return array_values(array_map(fn($c) => $this->detalle($c->id), array_filter(
            $this->citas, fn(Cita $c) => $estatus === null || $c->estatus === $estatus
        )));
    }

    public function detalle(int $id): ?array
    {
        $c = $this->citas[$id] ?? null;
        if ($c === null) {
            return null;
        }
        $p = $this->pacientes[$c->idPaciente];
        return ['id' => $id, 'paciente_nombre' => $p['nombre'], 'paciente_correo' => $p['correo'],
            'servicio' => $c->servicioNombre, 'fecha' => $c->fecha->format('Y-m-d'), 'hora' => $c->hora,
            'estatus' => $c->estatus->value];
    }

    public function listarPorCorreo(string $correo): array
    {
        return array_values(array_filter($this->listar(null), fn($c) => $c['paciente_correo'] === $correo));
    }

    public function obtener(int $id): ?Cita
    {
        return $this->citas[$id] ?? null;
    }

    public function guardar(Cita $cita): int
    {
        $id = count($this->citas) + 1;
        $this->citas[$id] = new Cita($id, $cita->idPaciente, $cita->idServicio, $cita->servicioNombre,
            $cita->fecha, $cita->hora, $cita->sintomas, $cita->estatus);
        return $id;
    }

    public function actualizarEstatus(int $id, EstatusCita $estatus): void
    {
        $this->citas[$id] = $this->citas[$id]->responder($estatus);
    }

    public function marcarCorreoEnviado(int $id): void
    {
        $this->correosEnviados[] = $id;
    }

    public function pacienteTieneCitaPendiente(int $idPaciente): bool
    {
        foreach ($this->citas as $c) {
            if ($c->idPaciente === $idPaciente && $c->estatus === EstatusCita::Pendiente) {
                return true;
            }
        }
        return false;
    }

    public function horasOcupadas(string $fecha): array
    {
        $horas = [];
        foreach ($this->citas as $c) {
            if ($c->fecha->format('Y-m-d') === $fecha && $c->estatus !== EstatusCita::Rechazada) {
                $horas[] = $c->hora;
            }
        }
        return $horas;
    }

    public function registrarPaciente(string $nombre, string $correo, string $telefono): int
    {
        foreach ($this->pacientes as $id => $p) {
            if ($p['correo'] === $correo) {
                return $id;
            }
        }
        $id = count($this->pacientes) + 1;
        $this->pacientes[$id] = compact('nombre', 'correo', 'telefono');
        return $id;
    }
}

/** Repositorio de horarios en memoria. */
final class HorariosEnMemoria implements HorarioRepository
{
    /** @param Horario[] $horarios */
    public function __construct(public array $horarios = [])
    {
    }

    public function listar(?string $desde = null): array
    {
        return $this->horarios;
    }

    public function listarPorFecha(string $fecha): array
    {
        return array_values(array_filter($this->horarios, fn(Horario $h) => $h->fecha === $fecha));
    }

    public function crear(Horario $horario, int $idDentista): Horario
    {
        $nuevo = new Horario(count($this->horarios) + 1, $horario->fecha, $horario->inicio, $horario->fin);
        $this->horarios[] = $nuevo;
        return $nuevo;
    }

    public function eliminar(int $id): bool
    {
        $antes = count($this->horarios);
        $this->horarios = array_values(array_filter($this->horarios, fn($h) => $h->id !== $id));
        return count($this->horarios) < $antes;
    }
}

/** Catálogo simulado. */
final class CatalogoFalso implements CatalogoGateway
{
    public function obtenerServicio(int $id): ?array
    {
        return match ($id) {
            1 => ['id' => 1, 'nombre' => 'Limpieza dental', 'activo' => true],
            2 => ['id' => 2, 'nombre' => 'Blanqueamiento', 'activo' => false],
            default => null,
        };
    }
}

/** Notificador que registra las llamadas. */
final class NotificadorEspia implements Notificador
{
    public array $enviados = [];

    public function __construct(private readonly bool $exito = true)
    {
    }

    public function notificarRespuesta(array $cita, EstatusCita $estatus): bool
    {
        $this->enviados[] = [$cita['paciente_correo'], $estatus->value];
        return $this->exito;
    }
}
