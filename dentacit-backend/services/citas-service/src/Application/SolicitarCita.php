<?php
declare(strict_types=1);

namespace Dentacit\Citas\Application;

use DateTimeImmutable;
use Dentacit\Citas\Domain\Agenda;
use Dentacit\Citas\Domain\CatalogoGateway;
use Dentacit\Citas\Domain\Cita;
use Dentacit\Citas\Domain\CitaRepository;
use Dentacit\Citas\Domain\HorarioRepository;
use Dentacit\Citas\Domain\ReglaNegocioException;

/**
 * Caso de uso: el paciente solicita una cita desde el cliente web (RF-03).
 *
 * Reglas que aplica:
 * - datos obligatorios y correo válido,
 * - el servicio existe y está activo (consulta a catalogo-service),
 * - un paciente solo tiene una solicitud pendiente a la vez (Autogestión de Citas),
 * - la fecha no es pasada y la hora cae en un espacio libre de la agenda.
 */
final class SolicitarCita
{
    public function __construct(
        private readonly CitaRepository $citas,
        private readonly HorarioRepository $horarios,
        private readonly CatalogoGateway $catalogo,
        private readonly int $duracionCita = 60,
    ) {
    }

    /**
     * @param array<string, mixed> $datos nombre, correo, telefono, id_servicio, fecha, hora, sintomas.
     * @param string $hoy Fecha actual AAAA-MM-DD (inyectable para pruebas).
     * @param string|null $ahora Hora actual HH:MM; si se indica, no se aceptan horas pasadas de hoy.
     * @return array{id:int, estatus:string}
     */
    public function ejecutar(array $datos, string $hoy, ?string $ahora = null): array
    {
        $nombre = trim((string)($datos['nombre'] ?? ''));
        $correo = strtolower(trim((string)($datos['correo'] ?? '')));
        $telefono = trim((string)($datos['telefono'] ?? ''));
        $idServicio = (int)($datos['id_servicio'] ?? 0);
        $fechaTexto = trim((string)($datos['fecha'] ?? ''));
        $hora = substr(trim((string)($datos['hora'] ?? '')), 0, 5);
        $sintomas = (string)($datos['sintomas'] ?? '');

        if ($nombre === '' || $correo === '' || $fechaTexto === '' || $hora === '' || $idServicio <= 0) {
            throw new ReglaNegocioException('Por favor completa todos los campos obligatorios.');
        }
        if (!filter_var($correo, FILTER_VALIDATE_EMAIL)) {
            throw new ReglaNegocioException('El correo no tiene un formato válido.');
        }
        $fecha = DateTimeImmutable::createFromFormat('!Y-m-d', $fechaTexto);
        if ($fecha === false || $fecha->format('Y-m-d') !== $fechaTexto) {
            throw new ReglaNegocioException('La fecha debe tener el formato AAAA-MM-DD.');
        }
        if (!preg_match('/^\d{2}:\d{2}$/', $hora)) {
            throw new ReglaNegocioException('La hora debe tener el formato HH:MM.');
        }
        if ($fechaTexto < $hoy || ($fechaTexto === $hoy && $ahora !== null && $hora <= $ahora)) {
            throw new ReglaNegocioException('No puedes agendar en una fecha u hora que ya pasó.');
        }
        if (mb_strlen($sintomas) > 1000) {
            throw new ReglaNegocioException('La descripción de síntomas no debe pasar de 1000 caracteres.');
        }

        $servicio = $this->catalogo->obtenerServicio($idServicio);
        if ($servicio === null || !$servicio['activo']) {
            throw new ReglaNegocioException('El servicio seleccionado no está disponible.');
        }

        $agenda = new Agenda(
            $this->horarios->listarPorFecha($fechaTexto),
            $this->citas->horasOcupadas($fechaTexto),
            $this->duracionCita
        );
        $agenda->validarEspacio($fechaTexto, $hora);

        $idPaciente = $this->citas->registrarPaciente($nombre, $correo, $telefono);
        if ($this->citas->pacienteTieneCitaPendiente($idPaciente)) {
            throw new ReglaNegocioException(
                'Ya tienes una solicitud pendiente. Espera a que sea aceptada o rechazada.'
            );
        }

        $cita = Cita::solicitar($idPaciente, $idServicio, $servicio['nombre'], $fecha, $hora, $sintomas);
        $id = $this->citas->guardar($cita);

        return ['id' => $id, 'estatus' => $cita->estatus->value];
    }
}
