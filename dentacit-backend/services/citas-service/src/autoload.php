<?php
declare(strict_types=1);

// Autocarga PSR-4 sin Composer: Dentacit\Citas\X\Y → src/X/Y.php
spl_autoload_register(function (string $clase): void {
    $prefijo = 'Dentacit\\Citas\\';
    if (str_starts_with($clase, $prefijo)) {
        $archivo = __DIR__ . '/' . str_replace('\\', '/', substr($clase, strlen($prefijo))) . '.php';
        if (is_file($archivo)) {
            require $archivo;
        }
    }
});
