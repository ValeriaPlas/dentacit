<?php
declare(strict_types=1);

namespace Dentacit\Catalogo\Api;

/**
 * Verificación de tokens JWT firmados con HS256 por auth-service.
 * Todos los servicios comparten la llave JWT_SECRET.
 */
final class Jwt
{
    /**
     * @return array<string, mixed> Contenido (payload) del token.
     * @throws NoAutorizadoException si el token es inválido o expiró.
     */
    public static function verificar(string $token, string $secreto): array
    {
        $partes = explode('.', $token);
        if (count($partes) !== 3) {
            throw new NoAutorizadoException('Token mal formado.');
        }
        [$h64, $p64, $firma64] = $partes;

        $encabezado = json_decode(self::b64Decodificar($h64), true);
        if (($encabezado['alg'] ?? '') !== 'HS256') {
            throw new NoAutorizadoException('Algoritmo de token no soportado.');
        }
        $esperada = self::b64Codificar(hash_hmac('sha256', "$h64.$p64", $secreto, true));
        if (!hash_equals($esperada, $firma64)) {
            throw new NoAutorizadoException('Firma de token inválida.');
        }
        $payload = json_decode(self::b64Decodificar($p64), true);
        if (!is_array($payload)) {
            throw new NoAutorizadoException('Token mal formado.');
        }
        if (isset($payload['exp']) && $payload['exp'] < time()) {
            throw new NoAutorizadoException('La sesión expiró. Inicia sesión de nuevo.');
        }
        return $payload;
    }

    /** Genera un token HS256 (usado en las pruebas). */
    public static function firmar(array $payload, string $secreto): string
    {
        $h = self::b64Codificar(json_encode(['alg' => 'HS256', 'typ' => 'JWT']));
        $p = self::b64Codificar(json_encode($payload));
        $f = self::b64Codificar(hash_hmac('sha256', "$h.$p", $secreto, true));
        return "$h.$p.$f";
    }

    private static function b64Codificar(string $datos): string
    {
        return rtrim(strtr(base64_encode($datos), '+/', '-_'), '=');
    }

    private static function b64Decodificar(string $datos): string
    {
        return (string)base64_decode(strtr($datos, '-_', '+/') . str_repeat('=', (4 - strlen($datos) % 4) % 4));
    }
}
