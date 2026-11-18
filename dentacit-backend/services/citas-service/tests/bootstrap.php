<?php
declare(strict_types=1);

require __DIR__ . '/../src/autoload.php';
foreach (glob(__DIR__ . '/Dobles/*.php') ?: [] as $archivo) {
    require $archivo;
}
