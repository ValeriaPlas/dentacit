package com.dentacit.admin.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.dentacit.admin.presentation.common.appContainer
import com.dentacit.admin.presentation.detalle.DetalleCitaScreen
import com.dentacit.admin.presentation.home.HomeScreen
import com.dentacit.admin.presentation.login.LoginScreen
import kotlinx.coroutines.launch

/** Rutas de navegación de la app. */
object Rutas {
    const val LOGIN = "login"
    const val HOME = "home"
    const val DETALLE = "detalle/{id}"

    /** Ruta al detalle de una cita concreta. */
    fun detalle(id: Int) = "detalle/$id"
}

/**
 * Grafo de navegación: Login → Inicio (Solicitudes, Horarios, Catálogo) → Detalle de cita.
 *
 * @param destinoInicial [Rutas.LOGIN] si no hay sesión guardada, [Rutas.HOME] si la hay.
 */
@Composable
fun DentaCitNavGraph(destinoInicial: String) {
    val navController = rememberNavController()
    val container = appContainer()
    val scope = rememberCoroutineScope()

    NavHost(navController = navController, startDestination = destinoInicial) {
        composable(Rutas.LOGIN) {
            LoginScreen(
                onLoginExitoso = {
                    navController.navigate(Rutas.HOME) {
                        popUpTo(Rutas.LOGIN) { inclusive = true }
                    }
                }
            )
        }
        composable(Rutas.HOME) {
            HomeScreen(
                onAbrirCita = { id -> navController.navigate(Rutas.detalle(id)) },
                onCerrarSesion = {
                    scope.launch {
                        container.cerrarSesion()
                        navController.navigate(Rutas.LOGIN) {
                            popUpTo(Rutas.HOME) { inclusive = true }
                        }
                    }
                }
            )
        }
        composable(
            route = Rutas.DETALLE,
            arguments = listOf(navArgument("id") { type = NavType.IntType })
        ) { entrada ->
            DetalleCitaScreen(
                idCita = entrada.arguments?.getInt("id") ?: 0,
                onRegresar = { navController.popBackStack() }
            )
        }
    }
}
