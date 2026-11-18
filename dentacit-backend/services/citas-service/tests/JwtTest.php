<?php
declare(strict_types=1);

use Dentacit\Citas\Api\Jwt;
use Dentacit\Citas\Api\NoAutorizadoException;
use PHPUnit\Framework\TestCase;

/** Verificación de los JWT emitidos por auth-service. */
final class JwtTest extends TestCase
{
    public function testTokenValidoSeAcepta(): void
    {
        $token = Jwt::firmar(['sub' => 1, 'exp' => time() + 60], 'secreto');
        $this->assertSame(1, Jwt::verificar($token, 'secreto')['sub']);
    }

    public function testTokenConOtraLlaveSeRechaza(): void
    {
        $this->expectException(NoAutorizadoException::class);
        Jwt::verificar(Jwt::firmar(['sub' => 1], 'otra'), 'secreto');
    }

    public function testTokenExpiradoSeRechaza(): void
    {
        $this->expectException(NoAutorizadoException::class);
        Jwt::verificar(Jwt::firmar(['sub' => 1, 'exp' => time() - 1], 'secreto'), 'secreto');
    }
}
