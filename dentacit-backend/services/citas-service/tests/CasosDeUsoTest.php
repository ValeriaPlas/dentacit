<?php
declare(strict_types=1);

use Dentacit\Citas\Application\GestionarHorarios;
use Dentacit\Citas\Application\ResponderCita;
use Dentacit\Citas\Application\SolicitarCita;
use Dentacit\Citas\Domain\EstatusCita;
use Dentacit\Citas\Domain\Horario;
use Dentacit\Citas\Domain\NoEncontradoException;
use Dentacit\Citas\Domain\ReglaNegocioException;
use Dentacit\Citas\Tests\Dobles\CatalogoFalso;
use Dentacit\Citas\Tests\Dobles\CitasEnMemoria;
use Dentacit\Citas\Tests\Dobles\HorariosEnMemoria;
use Dentacit\Citas\Tests\Dobles\NotificadorEspia;
use PHPUnit\Framework\TestCase;

/** Casos de uso de citas-service con repositorios en memoria. */
final class CasosDeUsoTest extends TestCase
{
    private const HOY = '2026-11-18';
    private CitasEnMemoria $citas;
    private HorariosEnMemoria $horarios;
    private SolicitarCita $solicitar;

    protected function setUp(): void
    {
        $this->citas = new CitasEnMemoria();
        $this->horarios = new HorariosEnMemoria([new Horario(1, '2026-11-20', '09:00', '13:00')]);
        $this->solicitar = new SolicitarCita($this->citas, $this->horarios, new CatalogoFalso(), 60);
    }

    private function datos(array $cambios = []): array
    {
        return $cambios + [
            'nombre' => 'Ana López', 'correo' => 'ana@correo.com', 'telefono' => '4771234567',
            'id_servicio' => 1, 'fecha' => '2026-11-20', 'hora' => '10:00', 'sintomas' => 'Dolor',
        ];
    }

    public function testSolicitudValidaQuedaPendiente(): void
    {
        $r = $this->solicitar->ejecutar($this->datos(), self::HOY);
        $this->assertSame('Pendiente', $r['estatus']);
        $this->assertCount(1, $this->citas->citas);
    }

    public function testUnPacienteNoPuedeTenerDosSolicitudesPendientes(): void
    {
        $this->solicitar->ejecutar($this->datos(), self::HOY);
        $this->expectExceptionMessage('Ya tienes una solicitud pendiente');
        $this->solicitar->ejecutar($this->datos(['hora' => '11:00']), self::HOY);
    }

    public function testDosPacientesNoPuedenTomarLaMismaHora(): void
    {
        $this->solicitar->ejecutar($this->datos(), self::HOY);
        $this->expectExceptionMessage('ya no está disponible');
        $this->solicitar->ejecutar($this->datos(['correo' => 'luis@correo.com']), self::HOY);
    }

    public function testServicioInactivoEsRechazado(): void
    {
        $this->expectException(ReglaNegocioException::class);
        $this->solicitar->ejecutar($this->datos(['id_servicio' => 2]), self::HOY);
    }

    public function testCorreoInvalidoEsRechazado(): void
    {
        $this->expectExceptionMessage('correo');
        $this->solicitar->ejecutar($this->datos(['correo' => 'ana-correo.com']), self::HOY);
    }

    public function testFechaPasadaEsRechazada(): void
    {
        $this->expectException(ReglaNegocioException::class);
        $this->solicitar->ejecutar($this->datos(['fecha' => '2026-11-01']), self::HOY);
    }

    public function testAceptarCitaNotificaAlPaciente(): void
    {
        $id = $this->solicitar->ejecutar($this->datos(), self::HOY)['id'];
        $espia = new NotificadorEspia();
        $r = (new ResponderCita($this->citas, $espia))->ejecutar($id, 'Aceptada');

        $this->assertSame('Aceptada', $r['estatus']);
        $this->assertTrue($r['correo_enviado']);
        $this->assertSame([['ana@correo.com', 'Aceptada']], $espia->enviados);
    }

    public function testSiElCorreoFallaLaRespuestaSeConserva(): void
    {
        $id = $this->solicitar->ejecutar($this->datos(), self::HOY)['id'];
        $r = (new ResponderCita($this->citas, new NotificadorEspia(false)))->ejecutar($id, 'Rechazada');
        $this->assertFalse($r['correo_enviado']);
        $this->assertSame(EstatusCita::Rechazada, $this->citas->obtener($id)->estatus);
    }

    public function testResponderCitaInexistenteDa404(): void
    {
        $this->expectException(NoEncontradoException::class);
        (new ResponderCita($this->citas, new NotificadorEspia()))->ejecutar(99, 'Aceptada');
    }

    public function testHoraRechazadaVuelveAEstarDisponible(): void
    {
        $id = $this->solicitar->ejecutar($this->datos(), self::HOY)['id'];
        (new ResponderCita($this->citas, new NotificadorEspia()))->ejecutar($id, 'Rechazada');
        $libres = (new GestionarHorarios($this->horarios, $this->citas))->disponibles('2026-11-20', self::HOY);
        $this->assertContains('10:00', $libres);
    }

    public function testCrearHorarioConFormatoIncorrectoEsRechazado(): void
    {
        $this->expectException(ReglaNegocioException::class);
        (new GestionarHorarios($this->horarios, $this->citas))
            ->crear(['fecha' => '20/11/2026', 'hora_inicio' => '9', 'hora_fin' => '10'], 1, self::HOY);
    }

    public function testProximosDiasSoloIncluyeFechasConLugar(): void
    {
        $this->horarios->horarios[] = new Horario(2, '2026-11-21', '09:00', '10:00');
        $this->solicitar->ejecutar($this->datos(['fecha' => '2026-11-21', 'hora' => '09:00']), self::HOY);

        $proximos = (new GestionarHorarios($this->horarios, $this->citas))->proximos(self::HOY, '08:00');
        $this->assertSame(['2026-11-20'], array_column($proximos, 'fecha'));
        $this->assertSame(['09:00', '10:00', '11:00', '12:00'], $proximos[0]['horas']);
    }

    public function testHoyNoSeOfrecenHorasPasadas(): void
    {
        $gestion = new GestionarHorarios($this->horarios, $this->citas);
        $this->assertSame(['11:00', '12:00'], $gestion->disponibles('2026-11-20', '2026-11-20', '10:30'));
    }

    public function testNoSePuedeAgendarUnaHoraQueYaPaso(): void
    {
        $this->expectException(ReglaNegocioException::class);
        $this->solicitar->ejecutar($this->datos(['fecha' => '2026-11-20', 'hora' => '10:00']), '2026-11-20', '10:30');
    }
}
