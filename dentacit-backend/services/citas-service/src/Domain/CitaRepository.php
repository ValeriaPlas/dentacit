<?php
declare(strict_types=1);

namespace Dentacit\Citas\Domain;

/** Contrato de persistencia de citas y pacientes. */
interface CitaRepository
{
    /** @return array<int, array<string, mixed>> Citas con datos del paciente, para mostrar. */
    public function listar(?EstatusCita $estatus): array;

    /** @return array<string, mixed>|null Detalle de una cita con datos del paciente. */
    public function detalle(int $id): ?array;

    /** @return array<int, array<string, mixed>> Citas de un paciente por correo. */
    public function listarPorCorreo(string $correo): array;

    public function obtener(int $id): ?Cita;

    public function guardar(Cita $cita): int;

    public function actualizarEstatus(int $id, EstatusCita $estatus): void;

    public function marcarCorreoEnviado(int $id): void;

    public function pacienteTieneCitaPendiente(int $idPaciente): bool;

    /** @return string[] Horas (HH:MM) ocupadas por citas Pendientes o Aceptadas en la fecha. */
    public function horasOcupadas(string $fecha): array;

    /** Registra al paciente o actualiza sus datos si el correo ya existe; devuelve su id. */
    public function registrarPaciente(string $nombre, string $correo, string $telefono): int;
}
