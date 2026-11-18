package com.dentacit.admin.presentation.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.dentacit.admin.DentaCitApp
import com.dentacit.admin.di.AppContainer
import com.dentacit.admin.domain.model.DentaCitException
import java.text.NumberFormat
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private val localeMx: Locale = Locale.forLanguageTag("es-MX")
private val formatoFechaLarga = DateTimeFormatter.ofPattern("EEEE d 'de' MMMM yyyy", localeMx)
private val formatoFechaCorta = DateTimeFormatter.ofPattern("EEE d MMM", localeMx)
private val formatoHora = DateTimeFormatter.ofPattern("HH:mm")

/** Obtiene el contenedor de dependencias desde cualquier composable. */
@Composable
fun appContainer(): AppContainer =
    (LocalContext.current.applicationContext as DentaCitApp).container

/** Convierte cualquier error en un mensaje entendible para el dentista. */
fun Throwable.mensajeUsuario(): String = when (this) {
    is DentaCitException -> message ?: "Ocurrió un error."
    else -> "Ocurrió un error inesperado: ${message ?: javaClass.simpleName}"
}

/** Ej.: "jueves 19 de noviembre 2026". */
fun LocalDate.formatoLargo(): String = format(formatoFechaLarga)

/** Ej.: "jue 19 nov". */
fun LocalDate.formatoCorto(): String = format(formatoFechaCorta)

/** Ej.: "09:30". */
fun LocalTime.formato(): String = format(formatoHora)

/** Ej.: "$1,250.00". */
fun Double.formatoMoneda(): String = NumberFormat.getCurrencyInstance(localeMx).format(this)
