package com.dentacit.admin.presentation.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/** Azul profundo de encabezados (misma paleta que el cliente web). */
val AzulProfundo = Color(0xFF0F2340)

/** Azul de acción para botones y enlaces. */
val AzulAccion = Color(0xFF2563EB)

/** Gris de fondo de pantallas. */
val GrisFondo = Color(0xFFF1F3F8)

/** Color semántico del estado Pendiente. */
val AmbarPendiente = Color(0xFFD97706)

/** Color semántico del estado Aceptada. */
val VerdeAceptada = Color(0xFF16A34A)

/** Color semántico del estado Rechazada. */
val RojoRechazada = Color(0xFFDC2626)

private val EsquemaClaro = lightColorScheme(
    primary = AzulAccion,
    onPrimary = Color.White,
    primaryContainer = AzulAccion,
    onPrimaryContainer = Color.White,
    secondary = AzulProfundo,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFDBE6FE),
    onSecondaryContainer = AzulProfundo,
    background = GrisFondo,
    onBackground = AzulProfundo,
    surface = Color.White,
    onSurface = AzulProfundo,
    surfaceVariant = Color(0xFFE5E9F2),
    onSurfaceVariant = Color(0xFF4B5563),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color.White,
    surfaceContainer = Color.White,
    surfaceContainerHigh = Color.White,
    surfaceContainerHighest = Color.White,
    error = RojoRechazada
)

/**
 * Tema visual de DentaCit para la app del dentista.
 *
 * @param content contenido al que se aplica el tema.
 */
@Composable
fun DentaCitTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = EsquemaClaro,
        typography = Typography(),
        content = content
    )
}
