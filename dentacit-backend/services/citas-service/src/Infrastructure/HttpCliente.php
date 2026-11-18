<?php
declare(strict_types=1);

namespace Dentacit\Citas\Infrastructure;

/** Cliente HTTP mínimo (sin dependencias) para comunicarse con otros microservicios. */
final class HttpCliente
{
    /**
     * @param array<string, string> $encabezados
     * @return array{codigo:int, cuerpo:mixed}|null null si no hubo conexión.
     */
    public static function enviar(string $metodo, string $url, ?array $cuerpo = null, array $encabezados = [], int $timeout = 5): ?array
    {
        $lineas = ['Accept: application/json'];
        foreach ($encabezados as $k => $v) {
            $lineas[] = "$k: $v";
        }
        $opciones = ['method' => $metodo, 'timeout' => $timeout, 'ignore_errors' => true];
        if ($cuerpo !== null) {
            $lineas[] = 'Content-Type: application/json';
            $opciones['content'] = json_encode($cuerpo, JSON_UNESCAPED_UNICODE);
        }
        $opciones['header'] = implode("\r\n", $lineas);

        $respuesta = @file_get_contents($url, false, stream_context_create(['http' => $opciones]));
        if ($respuesta === false || !isset($http_response_header[0])) {
            return null;
        }
        preg_match('#HTTP/\S+\s+(\d{3})#', $http_response_header[0], $m);
        return ['codigo' => (int)($m[1] ?? 0), 'cuerpo' => json_decode($respuesta, true)];
    }
}
